export interface AppConfig {
  port: number;
  databaseUrl: string | null;
  pepper: string;
  minClientVersion: string;
  activeWindowMs: number;
  confirmGapMs: number;
  monitorFlag: boolean;
}

export function loadConfig(env: NodeJS.ProcessEnv = process.env): AppConfig {
  return {
    port: Number(env.PORT ?? 8080),
    databaseUrl: env.DATABASE_URL ?? null,
    pepper: env.PEPPER ?? "dev-pepper",
    minClientVersion: env.MIN_CLIENT_VERSION ?? "0.0.1",
    activeWindowMs: Number(env.ACTIVE_WINDOW_MS ?? 6 * 60 * 60 * 1000),
    confirmGapMs: Number(env.CONFIRM_GAP_MS ?? 60 * 1000),
    monitorFlag: env.MONITOR_FLAG === "true",
  };
}
