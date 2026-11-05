package dev.hugomarket.core;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class ClientScheduler {
    private final ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<>();
    private final List<Consumer<Minecraft>> tickers = new CopyOnWriteArrayList<>();

    public ClientScheduler() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    public void post(Runnable task) {
        queue.add(task);
    }

    public void onTick(Consumer<Minecraft> ticker) {
        tickers.add(ticker);
    }

    private void tick(Minecraft client) {
        Runnable task;
        while ((task = queue.poll()) != null) {
            task.run();
        }
        for (Consumer<Minecraft> ticker : tickers) {
            ticker.accept(client);
        }
    }
}
