import { buildServer } from "./api/server.js";
import { loadConfig } from "./config.js";
import { PgStore } from "./db/pgStore.js";
import { startJobs } from "./jobs/snapshots.js";
import { MemoryStore } from "./store/memoryStore.js";

const config = loadConfig();
const store = config.databaseUrl ? new PgStore(config.databaseUrl) : new MemoryStore();
const app = buildServer({ config, store });
const stopJobs = startJobs(store, { confirmGapMs: config.confirmGapMs, activeWindowMs: config.activeWindowMs }, (message) =>
  console.log(`[jobs] ${message}`),
);

app.listen({ port: config.port, host: "0.0.0.0" }).then((address) => {
  console.log(`hugomarket backend listening on ${address} (${config.databaseUrl ? "postgres" : "memory"})`);
});

for (const signal of ["SIGINT", "SIGTERM"] as const) {
  process.on(signal, () => {
    stopJobs();
    app.close().then(() => process.exit(0));
  });
}
