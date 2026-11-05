import { buildServer } from "./api/server.js";
import { loadConfig } from "./config.js";
import { PgStore } from "./db/pgStore.js";
import { MemoryStore } from "./store/memoryStore.js";

const config = loadConfig();
const store = config.databaseUrl ? new PgStore(config.databaseUrl) : new MemoryStore();
const app = buildServer({ config, store });

app.listen({ port: config.port, host: "0.0.0.0" }).then((address) => {
  console.log(`hugomarket backend listening on ${address} (${config.databaseUrl ? "postgres" : "memory"})`);
});
