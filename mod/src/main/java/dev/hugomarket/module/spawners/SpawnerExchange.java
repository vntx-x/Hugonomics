package dev.hugomarket.module.spawners;

import com.google.gson.JsonObject;
import dev.hugomarket.HugoMarket;
import dev.hugomarket.config.ConfigStore;
import dev.hugomarket.core.ClientScheduler;
import dev.hugomarket.net.BackendClient;
import dev.hugomarket.net.RejectedRequestException;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SpawnerExchange {
    interface Done {
        void finished(String errorCode);
    }

    private static final Pattern ERROR_CODE = Pattern.compile("\"error\"\\s*:\\s*\"([a-z_]+)\"");
    private static final int UNAUTHORIZED = 401;

    private final BackendClient backend;
    private final ClientScheduler scheduler;
    private final ConfigStore config;
    private final DealEvents events = new DealEvents();
    private Consumer<List<DealEvents.Event>> eventListener = unused -> { };
    private String token;
    private String name = "";
    private boolean loggingIn;
    private List<ExchangeOffer> offers = List.of();
    private List<ExchangeDeal> deals = List.of();
    private int version;

    SpawnerExchange(BackendClient backend, ClientScheduler scheduler, ConfigStore config) {
        this.backend = backend;
        this.scheduler = scheduler;
        this.config = config;
    }

    void onEvents(Consumer<List<DealEvents.Event>> listener) {
        eventListener = listener;
    }

    boolean loggedIn() {
        return token != null;
    }

    boolean joined() {
        return config.get().spawnerExchangeJoined;
    }

    String name() {
        return name;
    }

    int version() {
        return version;
    }

    List<ExchangeOffer> offers() {
        return offers;
    }

    List<ExchangeDeal> deals() {
        return deals;
    }

    void forget() {
        token = null;
        offers = List.of();
        deals = List.of();
        events.reset();
        version++;
    }

    void refreshOffers() {
        backend.get("/v1/spawner-offers").whenComplete((body, error) -> scheduler.post(() -> {
            if (error == null) {
                offers = ExchangeJson.offers(body);
                version++;
            }
        }));
    }

    void refreshDeals(boolean notify) {
        if (token == null) {
            return;
        }
        authed(t -> backend.get("/v1/spawner-requests", t), body -> {
            deals = ExchangeJson.deals(body);
            List<DealEvents.Event> fresh = events.update(deals);
            if (notify && !fresh.isEmpty()) {
                eventListener.accept(fresh);
            }
            version++;
        }, unused -> { });
    }

    void login(Done done) {
        if (loggingIn) {
            done.finished("busy");
            return;
        }
        loggingIn = true;
        MojangJoin join = MojangJoin.of(Minecraft.getInstance());
        backend.post("/v1/auth/challenge", new JsonObject())
                .thenCompose(challenge -> {
                    String serverId = challenge.get("serverId").getAsString();
                    return backend.runAsync(() -> {
                        join.join(serverId);
                        return serverId;
                    });
                })
                .thenCompose(serverId -> {
                    JsonObject body = new JsonObject();
                    body.addProperty("name", join.name());
                    body.addProperty("serverId", serverId);
                    return backend.post("/v1/auth/verify", body);
                })
                .whenComplete((result, error) -> scheduler.post(() -> {
                    loggingIn = false;
                    if (error != null) {
                        HugoMarket.LOGGER.warn("spawner exchange login failed: {}", error.toString());
                        done.finished("login_failed");
                        return;
                    }
                    token = result.get("token").getAsString();
                    name = result.get("name").getAsString();
                    if (!config.get().spawnerExchangeJoined) {
                        config.get().spawnerExchangeJoined = true;
                        config.save();
                    }
                    version++;
                    done.finished(null);
                    refreshDeals(false);
                }));
    }

    void createOffer(String type, boolean selling, int count, long unitCents, Done done) {
        JsonObject body = amount(count, unitCents);
        body.addProperty("type", type);
        body.addProperty("side", selling ? "sell" : "buy");
        authed(t -> backend.post("/v1/spawner-offers", body, t), unused -> refreshOffers(), done);
    }

    void deleteOffer(String id, Done done) {
        authed(t -> backend.delete("/v1/spawner-offers/" + id, t), unused -> {
            refreshOffers();
            refreshDeals(false);
        }, done);
    }

    void request(String offerId, int count, long unitCents, Done done) {
        authed(t -> backend.post("/v1/spawner-offers/" + offerId + "/requests", amount(count, unitCents), t), this::dealChanged, done);
    }

    void accept(String dealId, Done done) {
        respond(dealId, action("accept"), done);
    }

    void decline(String dealId, Done done) {
        respond(dealId, action("decline"), done);
    }

    void counter(String dealId, int count, long unitCents, Done done) {
        JsonObject body = amount(count, unitCents);
        body.addProperty("action", "counter");
        respond(dealId, body, done);
    }

    void rate(String dealId, int stars, Done done) {
        JsonObject body = new JsonObject();
        body.addProperty("stars", stars);
        authed(t -> backend.post("/v1/spawner-requests/" + dealId + "/rate", body, t), unused -> {
            refreshDeals(false);
            refreshOffers();
        }, done);
    }

    private void respond(String dealId, JsonObject body, Done done) {
        authed(t -> backend.post("/v1/spawner-requests/" + dealId + "/respond", body, t), this::dealChanged, done);
    }

    private void dealChanged(JsonObject body) {
        events.acknowledge(ExchangeJson.deal(body));
        refreshDeals(false);
    }

    private void authed(Function<String, CompletableFuture<JsonObject>> call, Consumer<JsonObject> onOk, Done done) {
        if (token == null) {
            login(error -> {
                if (error != null) {
                    done.finished(error);
                } else {
                    authed(call, onOk, done);
                }
            });
            return;
        }
        call.apply(token).whenComplete((body, error) -> scheduler.post(() -> {
            if (error == null) {
                onOk.accept(body);
                done.finished(null);
                return;
            }
            if (statusOf(error) == UNAUTHORIZED) {
                token = null;
                login(loginError -> {
                    if (loginError != null) {
                        done.finished(loginError);
                    } else {
                        call.apply(token).whenComplete((retry, retryError) -> scheduler.post(() -> {
                            if (retryError == null) {
                                onOk.accept(retry);
                            }
                            done.finished(retryError == null ? null : codeOf(retryError));
                        }));
                    }
                });
                return;
            }
            done.finished(codeOf(error));
        }));
    }

    private static JsonObject amount(int count, long unitCents) {
        JsonObject body = new JsonObject();
        body.addProperty("count", count);
        body.addProperty("unitCents", unitCents);
        return body;
    }

    private static JsonObject action(String action) {
        JsonObject body = new JsonObject();
        body.addProperty("action", action);
        return body;
    }

    private static int statusOf(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof RejectedRequestException rejected) {
                return rejected.status();
            }
        }
        return 0;
    }

    static String codeOf(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof RejectedRequestException) {
                Matcher m = ERROR_CODE.matcher(cause.getMessage());
                return m.find() ? m.group(1) : "failed";
            }
        }
        return "offline";
    }
}
