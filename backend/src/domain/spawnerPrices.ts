import { readFileSync } from "node:fs";
import { z } from "zod";

export const spawnerPriceListSchema = z.object({
  updatedAt: z.string().max(40),
  source: z.string().max(120),
  prices: z
    .array(
      z.object({
        type: z.string().regex(/^[a-z0-9_]+$/),
        priceCents: z.number().int().min(1).max(1_000_000_000_000_00),
      }),
    )
    .max(200),
});

export type SpawnerPriceList = z.infer<typeof spawnerPriceListSchema>;

export const EMPTY_SPAWNER_PRICES: SpawnerPriceList = { updatedAt: "", source: "", prices: [] };

export function loadSpawnerPrices(path: string): SpawnerPriceList {
  return spawnerPriceListSchema.parse(JSON.parse(readFileSync(path, "utf8")));
}
