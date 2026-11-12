import type { FastifyInstance, FastifyReply, FastifyRequest } from "fastify";
import {
  authVerifySchema,
  spawnerOfferInputSchema,
  spawnerRateSchema,
  spawnerRequestInputSchema,
  spawnerRespondSchema,
} from "../domain/spawnerOffers.js";
import type { SpawnerPriceList } from "../domain/spawnerPrices.js";
import type { PlayerAuth, PlayerSession } from "../service/playerAuth.js";
import { SpawnerMarket, type Result } from "../service/spawnerMarket.js";
import type { Store } from "../store/store.js";
import { RateLimiter } from "./rateLimit.js";

export interface SpawnerRouteDeps {
  store: Store;
  auth: PlayerAuth;
  prices: SpawnerPriceList;
  now: () => number;
}

export function registerSpawnerRoutes(app: FastifyInstance, deps: SpawnerRouteDeps): void {
  const { auth, now } = deps;
  const market = new SpawnerMarket(deps.store);
  const authLimiter = new RateLimiter(10, 60_000);
  const readLimiter = new RateLimiter(120, 60_000);
  const writeLimiter = new RateLimiter(30, 60_000);
  const knownTypes = () => deps.prices.prices.map((p) => p.type);

  const send = (reply: FastifyReply, result: Result) => reply.code(result.code).send(result.body);

  const player = (request: FastifyRequest, reply: FastifyReply, limiter: RateLimiter): PlayerSession | null => {
    const session = auth.session(request.headers.authorization, now());
    if (!session) {
      reply.code(401).send({ error: "not_verified" });
      return null;
    }
    if (!limiter.allow(session.ownerHash, now())) {
      reply.code(429).send({ error: "rate_limited" });
      return null;
    }
    return session;
  };

  app.post("/v1/auth/challenge", async (request, reply) => {
    if (!authLimiter.allow(request.ip, now())) {
      return reply.code(429).send({ error: "rate_limited" });
    }
    return { serverId: auth.challenge(now()) };
  });

  app.post("/v1/auth/verify", async (request, reply) => {
    const parsed = authVerifySchema.safeParse(request.body);
    if (!parsed.success) {
      return reply.code(400).send({ error: "invalid" });
    }
    if (!authLimiter.allow(request.ip, now())) {
      return reply.code(429).send({ error: "rate_limited" });
    }
    const result = await auth.verify(parsed.data.name, parsed.data.serverId, now());
    if (!result) {
      return reply.code(401).send({ error: "not_verified" });
    }
    return { token: result.token, name: result.session.name };
  });

  app.get("/v1/spawner-offers", async (request, reply) => {
    if (!readLimiter.allow(request.ip, now())) {
      return reply.code(429).send({ error: "rate_limited" });
    }
    return send(reply, await market.offers(now()));
  });

  app.post("/v1/spawner-offers", async (request, reply) => {
    const session = player(request, reply, writeLimiter);
    if (!session) {
      return reply;
    }
    const parsed = spawnerOfferInputSchema.safeParse(request.body);
    if (!parsed.success) {
      return reply.code(400).send({ error: "invalid" });
    }
    return send(reply, await market.createOffer(session, parsed.data, knownTypes(), now()));
  });

  app.delete<{ Params: { id: string } }>("/v1/spawner-offers/:id", async (request, reply) => {
    const session = player(request, reply, writeLimiter);
    if (!session) {
      return reply;
    }
    return send(reply, await market.deleteOffer(session, request.params.id, now()));
  });

  app.post<{ Params: { id: string } }>("/v1/spawner-offers/:id/requests", async (request, reply) => {
    const session = player(request, reply, writeLimiter);
    if (!session) {
      return reply;
    }
    const parsed = spawnerRequestInputSchema.safeParse(request.body);
    if (!parsed.success) {
      return reply.code(400).send({ error: "invalid" });
    }
    return send(reply, await market.request(session, request.params.id, parsed.data, now()));
  });

  app.get("/v1/spawner-requests", async (request, reply) => {
    const session = player(request, reply, readLimiter);
    if (!session) {
      return reply;
    }
    return send(reply, await market.myRequests(session, now()));
  });

  app.post<{ Params: { id: string } }>("/v1/spawner-requests/:id/respond", async (request, reply) => {
    const session = player(request, reply, writeLimiter);
    if (!session) {
      return reply;
    }
    const parsed = spawnerRespondSchema.safeParse(request.body);
    if (!parsed.success) {
      return reply.code(400).send({ error: "invalid" });
    }
    return send(reply, await market.respond(session, request.params.id, parsed.data, now()));
  });

  app.post<{ Params: { id: string } }>("/v1/spawner-requests/:id/rate", async (request, reply) => {
    const session = player(request, reply, writeLimiter);
    if (!session) {
      return reply;
    }
    const parsed = spawnerRateSchema.safeParse(request.body);
    if (!parsed.success) {
      return reply.code(400).send({ error: "invalid" });
    }
    return send(reply, await market.rate(session, request.params.id, parsed.data.stars, now()));
  });
}
