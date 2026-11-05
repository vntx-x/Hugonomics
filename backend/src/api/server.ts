import Fastify, { type FastifyInstance } from "fastify";
import type { AppConfig } from "../config.js";
import { observationBatchSchema, priceBatchSchema } from "../domain/validation.js";
import { IngestService } from "../service/ingest.js";
import { PriceService } from "../service/prices.js";
import type { Store } from "../store/store.js";
import { RateLimiter } from "./rateLimit.js";

export interface ServerDeps {
  config: AppConfig;
  store: Store;
  now?: () => number;
}

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
  const rules = { confirmGapMs: deps.config.confirmGapMs, activeWindowMs: deps.config.activeWindowMs };
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
    flags: { ahpaging: false, sellmacro: false, monitor: false, cmdhotkeys: false },
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

  app.get("/v1/items", async () => ({
    serverTime: now(),
    items: await deps.store.items(now() - deps.config.activeWindowMs),
  }));

  app.get("/healthz", async () => ({ ok: true }));

  return app;
}
