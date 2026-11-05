package dev.hugomarket.core;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import java.util.Locale;
import java.util.function.BooleanSupplier;

public final class ServerDetector {
    private static final String HUGO_HOST = "hugosmp.net";

    private final BooleanSupplier anyServerOverride;
    private volatile boolean onHugo;
    private volatile String address = "";

    public ServerDetector(BooleanSupplier anyServerOverride) {
        this.anyServerOverride = anyServerOverride;
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> update(client));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            onHugo = false;
            address = "";
        });
    }

    private void update(Minecraft client) {
        ServerData data = client.getCurrentServer();
        address = data == null || data.ip == null ? "" : data.ip;
        onHugo = address.toLowerCase(Locale.ROOT).contains(HUGO_HOST);
    }

    public boolean active() {
        return onHugo || anyServerOverride.getAsBoolean();
    }

    public String address() {
        return address;
    }
}
