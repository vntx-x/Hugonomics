import { z } from "zod";

export const SPAWNER_OFFER_TTL_MS = 3 * 24 * 60 * 60 * 1000;
export const DEAL_RATING_WINDOW_MS = 7 * 24 * 60 * 60 * 1000;
export const MAX_SPAWNER_OFFERS_PER_PLAYER = 10;
export const MAX_OPEN_REQUESTS_PER_PLAYER = 20;
export const MAX_NEGOTIATION_ROUNDS = 10;

export interface SpawnerOffer {
  id: string;
  type: string;
  side: "sell" | "buy";
  count: number;
  unitCents: number;
  player: string;
  ownerHash: string;
  createdAt: number;
  expiresAt: number;
}

export type RequestStatus = "open" | "accepted" | "declined";

export interface SpawnerRequest {
  id: string;
  offerId: string;
  type: string;
  offerSide: "sell" | "buy";
  offerOwnerHash: string;
  offerOwnerPlayer: string;
  fromHash: string;
  fromPlayer: string;
  count: number;
  unitCents: number;
  turn: "owner" | "from";
  status: RequestStatus;
  rounds: number;
  createdAt: number;
  updatedAt: number;
  expiresAt: number;
}

export interface SpawnerRating {
  requestId: string;
  fromHash: string;
  toHash: string;
  stars: number;
  createdAt: number;
}

export interface RatingSummary {
  average: number;
  count: number;
}

const amountSchema = {
  count: z.number().int().min(1).max(10_000),
  unitCents: z.number().int().min(100).max(1_000_000_000_000_00),
};

export const spawnerOfferInputSchema = z.object({
  type: z.string().regex(/^[a-z0-9_]{1,40}$/),
  side: z.enum(["sell", "buy"]),
  ...amountSchema,
});

export const spawnerRequestInputSchema = z.object(amountSchema);

export const spawnerRespondSchema = z.discriminatedUnion("action", [
  z.object({ action: z.literal("accept") }),
  z.object({ action: z.literal("decline") }),
  z.object({ action: z.literal("counter"), ...amountSchema }),
]);

export const spawnerRateSchema = z.object({ stars: z.number().int().min(1).max(5) });

export const authVerifySchema = z.object({
  name: z.string().regex(/^[A-Za-z0-9_]{1,16}$/),
  serverId: z.string().regex(/^[a-f0-9]{40}$/),
});

export function summarizeRatings(ratings: SpawnerRating[]): Map<string, RatingSummary> {
  const sums = new Map<string, { total: number; count: number }>();
  for (const rating of ratings) {
    const entry = sums.get(rating.toHash) ?? { total: 0, count: 0 };
    entry.total += rating.stars;
    entry.count++;
    sums.set(rating.toHash, entry);
  }
  const out = new Map<string, RatingSummary>();
  for (const [hash, { total, count }] of sums) {
    out.set(hash, { average: Math.round((total / count) * 10) / 10, count });
  }
  return out;
}

const NO_RATING: RatingSummary = { average: 0, count: 0 };

export function publicOffer(offer: SpawnerOffer, ratings: Map<string, RatingSummary>) {
  return {
    id: offer.id,
    type: offer.type,
    side: offer.side,
    count: offer.count,
    unitCents: offer.unitCents,
    player: offer.player,
    rating: ratings.get(offer.ownerHash) ?? NO_RATING,
    createdAt: offer.createdAt,
    expiresAt: offer.expiresAt,
  };
}

export function publicRequest(request: SpawnerRequest, viewerHash: string, ratings: Map<string, RatingSummary>, ratedByViewer: boolean) {
  const viewerIsOwner = request.offerOwnerHash === viewerHash;
  const otherHash = viewerIsOwner ? request.fromHash : request.offerOwnerHash;
  return {
    id: request.id,
    offerId: request.offerId,
    type: request.type,
    offerSide: request.offerSide,
    role: viewerIsOwner ? "owner" : "from",
    otherPlayer: viewerIsOwner ? request.fromPlayer : request.offerOwnerPlayer,
    otherRating: ratings.get(otherHash) ?? NO_RATING,
    count: request.count,
    unitCents: request.unitCents,
    myTurn: request.status === "open" && (request.turn === "owner") === viewerIsOwner,
    status: request.status,
    rounds: request.rounds,
    rated: ratedByViewer,
    createdAt: request.createdAt,
    updatedAt: request.updatedAt,
  };
}
