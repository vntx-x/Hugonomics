import { priceInfo, type ConsensusRules } from "../domain/consensus.js";
import type { PriceInfo } from "../domain/types.js";
import type { Store } from "../store/store.js";

export class PriceService {
  constructor(
    private readonly store: Store,
    private readonly rules: ConsensusRules,
  ) {}

  async prices(keys: string[], now: number): Promise<Record<string, PriceInfo | null>> {
    const records = await this.store.activeListingsByKeys(keys, now - this.rules.activeWindowMs);
    const result: Record<string, PriceInfo | null> = {};
    for (const key of keys) {
      result[key] = priceInfo(key, records, now, this.rules);
    }
    return result;
  }
}
