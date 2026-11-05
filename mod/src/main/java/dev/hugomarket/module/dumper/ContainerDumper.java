package dev.hugomarket.module.dumper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DynamicOps;
import dev.hugomarket.HugoMarket;
import dev.hugomarket.container.ContainerListener;
import dev.hugomarket.container.ContainerSnapshot;
import dev.hugomarket.container.SlotSnapshot;
import dev.hugomarket.core.ServerDetector;
import net.minecraft.client.Minecraft;

import java.time.Instant;
import java.time.LocalDateTime;

final class ContainerDumper implements ContainerListener {
    private final DumperState state;
    private final DumpWriter writer;
    private final ServerDetector server;
    private int lastHash;
    private int lastContainerId = -1;

    ContainerDumper(DumperState state, DumpWriter writer, ServerDetector server) {
        this.state = state;
        this.writer = writer;
        this.server = server;
    }

    @Override
    public void onContainerUpdated(ContainerSnapshot snapshot) {
        if (!state.isRecording()) {
            return;
        }
        int hash = snapshot.contentHash();
        if (snapshot.containerId() == lastContainerId && hash == lastHash) {
            return;
        }
        lastContainerId = snapshot.containerId();
        lastHash = hash;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            return;
        }
        DynamicOps<JsonElement> ops = ComponentJson.ops();
        JsonObject json = new JsonObject();
        json.addProperty("ts", Instant.ofEpochMilli(snapshot.capturedAt()).toString());
        json.addProperty("server", server.address());
        json.addProperty("minecraft", HugoMarket.minecraftVersion());
        json.addProperty("mod", HugoMarket.modVersion());
        json.addProperty("title", snapshot.title().getString());
        json.add("titleJson", ComponentJson.of(snapshot.title(), ops));
        json.addProperty("menuType", snapshot.menuType());
        json.addProperty("containerId", snapshot.containerId());
        json.addProperty("containerSlots", snapshot.containerSlots());
        json.addProperty("slotCount", snapshot.slots().size());
        JsonArray slots = new JsonArray();
        for (SlotSnapshot slot : snapshot.slots()) {
            if (!slot.stack().isEmpty()) {
                slots.add(ItemJson.of(slot, client.level, client.player, ops));
            }
        }
        json.add("slots", slots);
        writer.writeContainer(DumpFileNames.container(LocalDateTime.now(), snapshot.title().getString(),
                snapshot.containerSlots()), json);
        state.countContainer();
    }

    @Override
    public void onContainerClosed(int containerId) {
        if (containerId == lastContainerId) {
            lastContainerId = -1;
            lastHash = 0;
        }
    }
}
