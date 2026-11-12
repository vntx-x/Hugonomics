import { createHash, randomBytes } from "node:crypto";

export interface VerifiedPlayer {
  id: string;
  name: string;
}

export interface SessionVerifier {
  hasJoined(name: string, serverId: string): Promise<VerifiedPlayer | null>;
}

export class MojangSessionVerifier implements SessionVerifier {
  async hasJoined(name: string, serverId: string): Promise<VerifiedPlayer | null> {
    const url = `https://sessionserver.mojang.com/session/minecraft/hasJoined?username=${encodeURIComponent(name)}&serverId=${encodeURIComponent(serverId)}`;
    const response = await fetch(url, { signal: AbortSignal.timeout(5_000) });
    if (response.status !== 200) {
      return null;
    }
    const body = (await response.json()) as { id?: unknown; name?: unknown };
    return typeof body.id === "string" && typeof body.name === "string" ? { id: body.id, name: body.name } : null;
  }
}

export interface PlayerSession {
  name: string;
  ownerHash: string;
}

const CHALLENGE_TTL_MS = 2 * 60 * 1000;
const SESSION_TTL_MS = 24 * 60 * 60 * 1000;
const MAX_PENDING = 10_000;

export class PlayerAuth {
  private readonly challenges = new Map<string, number>();
  private readonly sessions = new Map<string, PlayerSession & { expiresAt: number }>();

  constructor(
    private readonly verifier: SessionVerifier,
    private readonly pepper: string,
  ) {}

  challenge(now: number): string {
    this.prune(now);
    if (this.challenges.size >= MAX_PENDING) {
      this.challenges.clear();
    }
    const serverId = randomBytes(20).toString("hex");
    this.challenges.set(serverId, now + CHALLENGE_TTL_MS);
    return serverId;
  }

  async verify(name: string, serverId: string, now: number): Promise<{ token: string; session: PlayerSession } | null> {
    const expiresAt = this.challenges.get(serverId);
    this.challenges.delete(serverId);
    if (expiresAt === undefined || expiresAt < now) {
      return null;
    }
    const player = await this.verifier.hasJoined(name, serverId);
    if (!player) {
      return null;
    }
    const session = {
      name: player.name,
      ownerHash: createHash("sha256").update(`${this.pepper}|${player.id}`).digest("hex"),
    };
    const token = randomBytes(32).toString("hex");
    this.sessions.set(token, { ...session, expiresAt: now + SESSION_TTL_MS });
    return { token, session };
  }

  session(authorization: string | undefined, now: number): PlayerSession | null {
    const token = authorization?.startsWith("Bearer ") ? authorization.slice(7) : null;
    const session = token ? this.sessions.get(token) : undefined;
    if (!session || session.expiresAt < now) {
      return null;
    }
    return { name: session.name, ownerHash: session.ownerHash };
  }

  private prune(now: number): void {
    for (const [id, expiresAt] of this.challenges) {
      if (expiresAt < now) {
        this.challenges.delete(id);
      }
    }
    for (const [token, session] of this.sessions) {
      if (session.expiresAt < now) {
        this.sessions.delete(token);
      }
    }
  }
}
