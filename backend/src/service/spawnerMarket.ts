import { randomBytes } from "node:crypto";
import {
  DEAL_RATING_WINDOW_MS,
  MAX_NEGOTIATION_ROUNDS,
  MAX_OPEN_REQUESTS_PER_PLAYER,
  MAX_SPAWNER_OFFERS_PER_PLAYER,
  publicOffer,
  publicRequest,
  SPAWNER_OFFER_TTL_MS,
  summarizeRatings,
  type SpawnerOffer,
  type SpawnerRequest,
} from "../domain/spawnerOffers.js";
import type { Store } from "../store/store.js";
import type { PlayerSession } from "./playerAuth.js";

export interface Result {
  code: number;
  body: unknown;
}

interface Amount {
  count: number;
  unitCents: number;
}

type Respond = { action: "accept" } | { action: "decline" } | ({ action: "counter" } & Amount);

const ok = (body: unknown): Result => ({ code: 200, body });
const fail = (code: number, error: string): Result => ({ code, body: { error } });

export class SpawnerMarket {
  constructor(private readonly store: Store) {}

  async offers(now: number): Promise<Result> {
    const ratings = summarizeRatings(await this.store.spawnerRatings());
    const offers = (await this.store.spawnerOffers(now)).sort((a, b) => b.createdAt - a.createdAt);
    return ok({ serverTime: now, offers: offers.map((o) => publicOffer(o, ratings)) });
  }

  async createOffer(session: PlayerSession, input: Omit<SpawnerOffer, "id" | "player" | "ownerHash" | "createdAt" | "expiresAt">, knownTypes: string[], now: number): Promise<Result> {
    if (knownTypes.length > 0 && !knownTypes.includes(input.type)) {
      return fail(400, "unknown_type");
    }
    const own = (await this.store.spawnerOffers(now)).filter((o) => o.ownerHash === session.ownerHash);
    if (own.length >= MAX_SPAWNER_OFFERS_PER_PLAYER) {
      return fail(409, "too_many_offers");
    }
    const offer: SpawnerOffer = {
      id: newId(),
      ...input,
      player: session.name,
      ownerHash: session.ownerHash,
      createdAt: now,
      expiresAt: now + SPAWNER_OFFER_TTL_MS,
    };
    await this.store.putSpawnerOffer(offer);
    return ok(publicOffer(offer, summarizeRatings(await this.store.spawnerRatings())));
  }

  async deleteOffer(session: PlayerSession, id: string, now: number): Promise<Result> {
    if (!(await this.store.deleteSpawnerOffer(id, session.ownerHash))) {
      return fail(404, "not_found");
    }
    const open = (await this.store.spawnerRequests(now)).filter((r) => r.offerId === id && r.status === "open");
    await this.store.deleteSpawnerRequests(open.map((r) => r.id));
    return ok({ ok: true });
  }

  async request(session: PlayerSession, offerId: string, amount: Amount, now: number): Promise<Result> {
    const offer = (await this.store.spawnerOffers(now)).find((o) => o.id === offerId);
    if (!offer) {
      return fail(404, "not_found");
    }
    if (offer.ownerHash === session.ownerHash) {
      return fail(400, "own_offer");
    }
    const requests = await this.store.spawnerRequests(now);
    const mineOpen = requests.filter((r) => r.fromHash === session.ownerHash && r.status === "open");
    if (mineOpen.some((r) => r.offerId === offerId)) {
      return fail(409, "already_open");
    }
    if (mineOpen.length >= MAX_OPEN_REQUESTS_PER_PLAYER) {
      return fail(409, "too_many_requests");
    }
    const takesOffer = amount.unitCents === offer.unitCents && amount.count <= offer.count;
    const request: SpawnerRequest = {
      id: newId(),
      offerId,
      type: offer.type,
      offerSide: offer.side,
      offerOwnerHash: offer.ownerHash,
      offerOwnerPlayer: offer.player,
      fromHash: session.ownerHash,
      fromPlayer: session.name,
      count: amount.count,
      unitCents: amount.unitCents,
      turn: "owner",
      status: takesOffer ? "accepted" : "open",
      rounds: 1,
      createdAt: now,
      updatedAt: now,
      expiresAt: takesOffer ? now + DEAL_RATING_WINDOW_MS : offer.expiresAt,
    };
    await this.store.putSpawnerRequest(request);
    return ok(publicRequest(request, session.ownerHash, summarizeRatings(await this.store.spawnerRatings()), false));
  }

  async respond(session: PlayerSession, id: string, action: Respond, now: number): Promise<Result> {
    const request = (await this.store.spawnerRequests(now)).find((r) => r.id === id);
    if (!request || (request.offerOwnerHash !== session.ownerHash && request.fromHash !== session.ownerHash)) {
      return fail(404, "not_found");
    }
    if (request.status !== "open") {
      return fail(409, "closed");
    }
    const myTurn = (request.turn === "owner") === (request.offerOwnerHash === session.ownerHash);
    if (action.action !== "decline" && !myTurn) {
      return fail(409, "not_your_turn");
    }
    const updated: SpawnerRequest = { ...request, updatedAt: now };
    if (action.action === "decline") {
      updated.status = "declined";
    } else if (action.action === "accept") {
      updated.status = "accepted";
      updated.expiresAt = now + DEAL_RATING_WINDOW_MS;
    } else {
      if (request.rounds >= MAX_NEGOTIATION_ROUNDS) {
        return fail(409, "too_many_rounds");
      }
      updated.count = action.count;
      updated.unitCents = action.unitCents;
      updated.rounds = request.rounds + 1;
      updated.turn = request.turn === "owner" ? "from" : "owner";
    }
    await this.store.putSpawnerRequest(updated);
    return ok(publicRequest(updated, session.ownerHash, summarizeRatings(await this.store.spawnerRatings()), false));
  }

  async myRequests(session: PlayerSession, now: number): Promise<Result> {
    const all = await this.store.spawnerRatings();
    const ratings = summarizeRatings(all);
    const ratedByMe = new Set(all.filter((r) => r.fromHash === session.ownerHash).map((r) => r.requestId));
    const mine = (await this.store.spawnerRequests(now))
      .filter((r) => r.offerOwnerHash === session.ownerHash || r.fromHash === session.ownerHash)
      .sort((a, b) => b.updatedAt - a.updatedAt);
    return ok({ serverTime: now, name: session.name, requests: mine.map((r) => publicRequest(r, session.ownerHash, ratings, ratedByMe.has(r.id))) });
  }

  async rate(session: PlayerSession, id: string, stars: number, now: number): Promise<Result> {
    const request = (await this.store.spawnerRequests(now)).find((r) => r.id === id);
    if (!request || (request.offerOwnerHash !== session.ownerHash && request.fromHash !== session.ownerHash)) {
      return fail(404, "not_found");
    }
    if (request.status !== "accepted") {
      return fail(409, "not_accepted");
    }
    const toHash = request.offerOwnerHash === session.ownerHash ? request.fromHash : request.offerOwnerHash;
    const stored = await this.store.putSpawnerRating({ requestId: id, fromHash: session.ownerHash, toHash, stars, createdAt: now });
    return stored ? ok({ ok: true }) : fail(409, "already_rated");
  }
}

function newId(): string {
  return randomBytes(16).toString("hex");
}
