import { describe, expect, it } from "vitest";
import { buildServer } from "../src/api/server.js";
import { loadConfig } from "../src/config.js";
import { loadSpawnerPrices } from "../src/domain/spawnerPrices.js";
import type { SessionVerifier } from "../src/service/playerAuth.js";
import { MemoryStore } from "../src/store/memoryStore.js";

const players: Record<string, string> = { Alice: "uuid-alice", Bob: "uuid-bob" };

const verifier: SessionVerifier = {
  async hasJoined(name) {
    const id = players[name];
    return id ? { id, name } : null;
  },
};

describe("spawner offers", () => {
  let clock = Date.parse("2026-11-12T12:00:00Z");

  function server() {
    return buildServer({
      config: loadConfig({}),
      store: new MemoryStore(),
      now: () => clock,
      spawnerPrices: loadSpawnerPrices("data/spawner-prices.json"),
      sessionVerifier: verifier,
    });
  }

  async function login(app: ReturnType<typeof server>, name: string): Promise<string> {
    const challenge = await app.inject({ method: "POST", url: "/v1/auth/challenge" });
    const serverId = challenge.json().serverId;
    const verified = await app.inject({ method: "POST", url: "/v1/auth/verify", payload: { name, serverId } });
    expect(verified.statusCode).toBe(200);
    return verified.json().token;
  }

  const offer = { type: "skeleton", side: "sell", count: 8, unitCents: 2_750_000_000 };

  it("rejects offers without a verified player", async () => {
    const app = server();
    const response = await app.inject({ method: "POST", url: "/v1/spawner-offers", payload: offer });
    expect(response.statusCode).toBe(401);
  });

  it("rejects a name the session server does not confirm", async () => {
    const app = server();
    const challenge = await app.inject({ method: "POST", url: "/v1/auth/challenge" });
    const response = await app.inject({
      method: "POST",
      url: "/v1/auth/verify",
      payload: { name: "Mallory", serverId: challenge.json().serverId },
    });
    expect(response.statusCode).toBe(401);
  });

  it("does not accept a challenge twice", async () => {
    const app = server();
    const serverId = (await app.inject({ method: "POST", url: "/v1/auth/challenge" })).json().serverId;
    await app.inject({ method: "POST", url: "/v1/auth/verify", payload: { name: "Alice", serverId } });
    const again = await app.inject({ method: "POST", url: "/v1/auth/verify", payload: { name: "Alice", serverId } });
    expect(again.statusCode).toBe(401);
  });

  it("publishes offers under the verified name and lets only the owner delete them", async () => {
    const app = server();
    const alice = await login(app, "Alice");
    const bob = await login(app, "Bob");
    const created = await app.inject({
      method: "POST",
      url: "/v1/spawner-offers",
      headers: { authorization: `Bearer ${alice}` },
      payload: offer,
    });
    expect(created.statusCode).toBe(200);
    const id = created.json().id;
    const list = (await app.inject({ method: "GET", url: "/v1/spawner-offers" })).json();
    expect(list.offers).toHaveLength(1);
    expect(list.offers[0].player).toBe("Alice");
    expect(list.offers[0].ownerHash).toBeUndefined();
    const byBob = await app.inject({ method: "DELETE", url: `/v1/spawner-offers/${id}`, headers: { authorization: `Bearer ${bob}` } });
    expect(byBob.statusCode).toBe(404);
    const byAlice = await app.inject({ method: "DELETE", url: `/v1/spawner-offers/${id}`, headers: { authorization: `Bearer ${alice}` } });
    expect(byAlice.statusCode).toBe(200);
    expect((await app.inject({ method: "GET", url: "/v1/spawner-offers" })).json().offers).toHaveLength(0);
  });

  it("rejects unknown spawner types and expires offers after three days", async () => {
    const app = server();
    const token = await login(app, "Alice");
    const headers = { authorization: `Bearer ${token}` };
    const unknown = await app.inject({ method: "POST", url: "/v1/spawner-offers", headers, payload: { ...offer, type: "wither" } });
    expect(unknown.statusCode).toBe(400);
    await app.inject({ method: "POST", url: "/v1/spawner-offers", headers, payload: offer });
    clock += 3 * 24 * 60 * 60 * 1000 + 1;
    expect((await app.inject({ method: "GET", url: "/v1/spawner-offers" })).json().offers).toHaveLength(0);
  });

  async function createOffer(app: ReturnType<typeof server>, token: string) {
    const response = await app.inject({ method: "POST", url: "/v1/spawner-offers", headers: { authorization: `Bearer ${token}` }, payload: offer });
    return response.json().id as string;
  }

  it("accepts a request at the listed price immediately", async () => {
    const app = server();
    const alice = await login(app, "Alice");
    const bob = await login(app, "Bob");
    const offerId = await createOffer(app, alice);
    const own = await app.inject({ method: "POST", url: `/v1/spawner-offers/${offerId}/requests`, headers: { authorization: `Bearer ${alice}` }, payload: { count: 1, unitCents: offer.unitCents } });
    expect(own.statusCode).toBe(400);
    const taken = await app.inject({
      method: "POST",
      url: `/v1/spawner-offers/${offerId}/requests`,
      headers: { authorization: `Bearer ${bob}` },
      payload: { count: 5, unitCents: offer.unitCents },
    });
    expect(taken.json().status).toBe("accepted");
  });

  it("negotiates with counter offers in turns", async () => {
    const app = server();
    const alice = await login(app, "Alice");
    const bob = await login(app, "Bob");
    const offerId = await createOffer(app, alice);
    const asBob = { authorization: `Bearer ${bob}` };
    const asAlice = { authorization: `Bearer ${alice}` };
    const request = await app.inject({ method: "POST", url: `/v1/spawner-offers/${offerId}/requests`, headers: asBob, payload: { count: 8, unitCents: 2_600_000_000 } });
    const id = request.json().id;
    expect(request.json().status).toBe("open");
    expect(request.json().myTurn).toBe(false);
    const early = await app.inject({ method: "POST", url: `/v1/spawner-requests/${id}/respond`, headers: asBob, payload: { action: "accept" } });
    expect(early.statusCode).toBe(409);
    const incoming = (await app.inject({ method: "GET", url: "/v1/spawner-requests", headers: asAlice })).json();
    expect(incoming.requests[0].otherPlayer).toBe("Bob");
    expect(incoming.requests[0].myTurn).toBe(true);
    const counter = await app.inject({ method: "POST", url: `/v1/spawner-requests/${id}/respond`, headers: asAlice, payload: { action: "counter", count: 8, unitCents: 2_700_000_000 } });
    expect(counter.json().unitCents).toBe(2_700_000_000);
    const accepted = await app.inject({ method: "POST", url: `/v1/spawner-requests/${id}/respond`, headers: asBob, payload: { action: "accept" } });
    expect(accepted.json().status).toBe("accepted");
  });

  it("lets both sides rate an accepted deal once and shows the stars on offers", async () => {
    const app = server();
    const alice = await login(app, "Alice");
    const bob = await login(app, "Bob");
    const offerId = await createOffer(app, alice);
    const asBob = { authorization: `Bearer ${bob}` };
    const id = (await app.inject({ method: "POST", url: `/v1/spawner-offers/${offerId}/requests`, headers: asBob, payload: { count: 1, unitCents: offer.unitCents } })).json().id;
    const rated = await app.inject({ method: "POST", url: `/v1/spawner-requests/${id}/rate`, headers: asBob, payload: { stars: 4 } });
    expect(rated.statusCode).toBe(200);
    const twice = await app.inject({ method: "POST", url: `/v1/spawner-requests/${id}/rate`, headers: asBob, payload: { stars: 5 } });
    expect(twice.statusCode).toBe(409);
    const offers = (await app.inject({ method: "GET", url: "/v1/spawner-offers" })).json().offers;
    expect(offers[0].rating).toEqual({ average: 4, count: 1 });
    const mine = (await app.inject({ method: "GET", url: "/v1/spawner-requests", headers: asBob })).json();
    expect(mine.requests[0].rated).toBe(true);
  });

  it("does not allow rating an open request", async () => {
    const app = server();
    const alice = await login(app, "Alice");
    const bob = await login(app, "Bob");
    const offerId = await createOffer(app, alice);
    const asBob = { authorization: `Bearer ${bob}` };
    const id = (await app.inject({ method: "POST", url: `/v1/spawner-offers/${offerId}/requests`, headers: asBob, payload: { count: 1, unitCents: 100_00 } })).json().id;
    const rated = await app.inject({ method: "POST", url: `/v1/spawner-requests/${id}/rate`, headers: asBob, payload: { stars: 1 } });
    expect(rated.statusCode).toBe(409);
  });

  it("limits each player to ten open offers", async () => {
    const app = server();
    const token = await login(app, "Bob");
    const headers = { authorization: `Bearer ${token}` };
    for (let i = 0; i < 10; i++) {
      clock += 7_000;
      const response = await app.inject({ method: "POST", url: "/v1/spawner-offers", headers, payload: offer });
      expect(response.statusCode).toBe(200);
    }
    clock += 7_000;
    const eleventh = await app.inject({ method: "POST", url: "/v1/spawner-offers", headers, payload: offer });
    expect(eleventh.statusCode).toBe(409);
  });
});
