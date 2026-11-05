package dev.hugomarket.container;

import dev.hugomarket.model.ContainerView;
import dev.hugomarket.model.ScreenKind;
import dev.hugomarket.parse.ScreenClassifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class ScreenStateTracker implements ContainerListener {
    private final ScreenClassifier classifier;
    private final List<Consumer<ScreenUpdate>> listeners = new CopyOnWriteArrayList<>();
    private final List<Runnable> closeListeners = new CopyOnWriteArrayList<>();
    private ScreenUpdate current;

    public ScreenStateTracker(ScreenClassifier classifier, ContainerTracker containers) {
        this.classifier = classifier;
        containers.addListener(this);
    }

    public void onUpdate(Consumer<ScreenUpdate> listener) {
        listeners.add(listener);
    }

    public void onClose(Runnable listener) {
        closeListeners.add(listener);
    }

    public ScreenUpdate current() {
        return current;
    }

    @Override
    public void onContainerUpdated(ContainerSnapshot snapshot) {
        if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen)) {
            return;
        }
        ContainerView view = ContainerViews.of(snapshot);
        ScreenKind kind = classifier.classify(view);
        current = new ScreenUpdate(kind, view, snapshot, screen);
        for (Consumer<ScreenUpdate> listener : listeners) {
            listener.accept(current);
        }
    }

    @Override
    public void onContainerClosed(int containerId) {
        current = null;
        for (Runnable listener : closeListeners) {
            listener.run();
        }
    }
}
