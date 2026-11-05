export class RateLimiter {
  private readonly buckets = new Map<string, { tokens: number; updatedAt: number }>();

  constructor(
    private readonly capacity: number,
    private readonly refillMs: number,
  ) {}

  allow(id: string, now: number): boolean {
    const bucket = this.buckets.get(id) ?? { tokens: this.capacity, updatedAt: now };
    const refilled = Math.min(this.capacity, bucket.tokens + ((now - bucket.updatedAt) / this.refillMs) * this.capacity);
    if (refilled < 1) {
      this.buckets.set(id, { tokens: refilled, updatedAt: now });
      return false;
    }
    this.buckets.set(id, { tokens: refilled - 1, updatedAt: now });
    if (this.buckets.size > 10_000) {
      this.evict(now);
    }
    return true;
  }

  private evict(now: number): void {
    for (const [id, bucket] of this.buckets) {
      if (now - bucket.updatedAt > this.refillMs * 2) {
        this.buckets.delete(id);
      }
    }
  }
}
