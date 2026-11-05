import Fastify, { type FastifyInstance } from "fastify";
import type { AppConfig } from "../config.js";
import { observationBatchSchema, priceBatchSchema } from "../domain/validation.js";
import { IngestService } from "../service/ingest.js";
import { PriceService } from "../service/prices.js";
import type { Store } from "../store/store.js";
import { RateLimiter } from "./rateLimit.js";
import { registerLegalRoutes, type LegalInfo } from "./legal.js";

export interface ServerDeps {
  config: AppConfig;
  store: Store;
  now?: () => number;
  legal?: LegalInfo | null;
}

const HOUR = 60 * 60 * 1000;
const HISTORY_RANGES: Record<string, number> = {
  "1h": HOUR,
  "6h": 6 * HOUR,
  "24h": 24 * HOUR,
  "7d": 7 * 24 * HOUR,
  "31d": 31 * 24 * HOUR,
};

const MAX_LISTINGS = 3000;

const FEES = {
  auctionListingFeePercent: 1,
  auctionSaleTaxTiers: [
    { upToCents: 1_000_000, percent: 1 },
    { upToCents: 100_000_000, percent: 2 },
    { upToCents: -1, percent: 3 },
  ],
  orderCreationFeePercent: 1,
};

export function buildServer(deps: ServerDeps): FastifyInstance {
  const now = deps.now ?? (() => Date.now());
  const rules = {
    confirmGapMs: deps.config.confirmGapMs,
    activeWindowMs: deps.config.activeWindowMs,
    trustedClients: deps.config.trustedClients,
  };
  const ingest = new IngestService(deps.store, rules);
  const prices = new PriceService(deps.store, rules);
  const uploadLimiter = new RateLimiter(30, 60_000);
  const queryLimiter = new RateLimiter(120, 60_000);
  const app = Fastify({ logger: false });

  app.get("/v1/meta", async () => ({
    minClientVersion: deps.config.minClientVersion,
    serverTime: new Date(now()).toISOString(),
    pepper: deps.config.pepper,
    fees: FEES,
    flags: { ahpaging: false, sellmacro: false, monitor: deps.config.monitorFlag, cmdhotkeys: false },
  }));

  app.post("/v1/observations", async (request, reply) => {
    const parsed = observationBatchSchema.safeParse(request.body);
    if (!parsed.success) {
      return reply.code(400).send({ error: "invalid", issues: parsed.error.issues.slice(0, 5) });
    }
    if (!uploadLimiter.allow(parsed.data.clientId, now())) {
      return reply.code(429).send({ error: "rate_limited" });
    }
    return ingest.ingest(parsed.data, now());
  });

  app.post("/v1/prices/batch", async (request, reply) => {
    const parsed = priceBatchSchema.safeParse(request.body);
    if (!parsed.success) {
      return reply.code(400).send({ error: "invalid" });
    }
    if (!queryLimiter.allow(request.ip, now())) {
      return reply.code(429).send({ error: "rate_limited" });
    }
    const unique = [...new Set(parsed.data.keys)];
    return { serverTime: now(), prices: await prices.prices(unique, now()) };
  });

  app.get<{ Querystring: { maxAge?: string; limit?: string } }>("/v1/listings", async (request, reply) => {
    if (!queryLimiter.allow(request.ip, now())) {
      return reply.code(429).send({ error: "rate_limited" });
    }
    const maxAge = Math.min(deps.config.activeWindowMs, Math.max(60_000, Number(request.query.maxAge ?? 20 * 60_000) || 20 * 60_000));
    const limit = Math.min(MAX_LISTINGS, Math.max(1, Number(request.query.limit ?? MAX_LISTINGS) || MAX_LISTINGS));
    const current = now();
    const records = (await deps.store.activeListings(current - maxAge))
      .filter((r) => r.expiresAt <= 0 || r.expiresAt > current)
      .sort((a, b) => b.lastSeen - a.lastSeen)
      .slice(0, limit)
      .map((r) => ({
        id: r.listingId,
        key: r.key,
        baseId: r.baseId,
        displayName: r.displayName,
        count: r.count,
        priceCents: r.priceCents,
        expiresAt: r.expiresAt,
        lastSeen: r.lastSeen,
        confirmed: r.confirmed,
      }));
    return { serverTime: current, listings: records };
  });

  app.get("/v1/items", async () => ({
    serverTime: now(),
    items: await deps.store.items(now() - deps.config.activeWindowMs),
  }));

  app.get<{ Params: { key: string }; Querystring: { range?: string } }>("/v1/items/:key/history", async (request, reply) => {
    const rangeMs = HISTORY_RANGES[request.query.range ?? "24h"];
    if (!rangeMs) {
      return reply.code(400).send({ error: "invalid_range", ranges: Object.keys(HISTORY_RANGES) });
    }
    if (!queryLimiter.allow(request.ip, now())) {
      return reply.code(429).send({ error: "rate_limited" });
    }
    const to = now();
    const points = await deps.store.history(request.params.key, to - rangeMs, to);
    return { key: request.params.key, from: to - rangeMs, to, points };
  });

  app.get("/healthz", async () => ({ ok: true }));
  registerLegalRoutes(app, deps.legal ?? null);

  return app;
}
