import { z } from "zod";

const keyPattern = /^[a-z0-9_.:/-]+(#[a-z0-9_.;:-]*)?$/;

export const listingObservationSchema = z.object({
  key: z.string().min(1).max(200).regex(keyPattern),
  baseId: z.string().min(1).max(120),
  displayName: z.string().min(1).max(120),
  count: z.number().int().min(1).max(10_000),
  priceCents: z.number().int().min(1).max(1_000_000_000_000_00),
  sellerHash: z.string().regex(/^[a-f0-9]{16,64}$/),
  expiresInSeconds: z.number().int().min(-1).max(31 * 24 * 3600),
  slot: z.number().int().min(0).max(53),
});

export const observationBatchSchema = z.object({
  clientId: z.string().uuid(),
  clientVersion: z.string().min(1).max(40),
  mcVersion: z.string().min(1).max(20),
  observedAt: z.string().datetime(),
  source: z.literal("ah_list"),
  listings: z.array(listingObservationSchema).max(60),
});

export const priceBatchSchema = z.object({
  keys: z.array(z.string().min(1).max(200)).min(1).max(200),
});

export type ObservationBatchInput = z.infer<typeof observationBatchSchema>;
