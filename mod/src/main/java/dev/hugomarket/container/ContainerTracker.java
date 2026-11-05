package dev.hugomarket.container;

import dev.hugomarket.core.ClientScheduler;
import dev.hugomarket.core.ServerDetector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ContainerTracker {
    private static final int DEBOUNCE_TICKS = 2;
    private static volatile ContainerTracker instance;

    private final ServerDetector server;
    private final List<ContainerListener> listeners = new CopyOnWriteArrayList<>();
    private volatile int pendingTicks = -1;
    private int openContainerId = -1;

    public ContainerTracker(ServerDetector server, ClientScheduler scheduler) {
        this.server = server;
        scheduler.onTick(this::tick);
        instance = this;
    }

    public static void notifyContentChanged() {
        ContainerTracker tracker = instance;
        if (tracker != null) {
            tracker.pendingTicks = DEBOUNCE_TICKS;
        }
    }

    public void addListener(ContainerListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ContainerListener listener) {
        listeners.remove(listener);
    }

    private void tick(Minecraft client) {
        AbstractContainerScreen<?> screen = client.screen instanceof AbstractContainerScreen<?> s ? s : null;
        if (screen == null) {
            if (openContainerId != -1) {
                int closed = openContainerId;
                openContainerId = -1;
                for (ContainerListener listener : listeners) {
                    listener.onContainerClosed(closed);
                }
            }
            pendingTicks = -1;
            return;
        }
        if (pendingTicks < 0) {
            return;
        }
        if (pendingTicks > 0) {
            pendingTicks--;
            return;
        }
        pendingTicks = -1;
        if (client.player == null || !server.active()) {
            return;
        }
        ContainerSnapshot snapshot = ContainerSnapshot.capture(screen, client.player);
        openContainerId = snapshot.containerId();
        for (ContainerListener listener : listeners) {
            listener.onContainerUpdated(snapshot);
        }
    }
}
