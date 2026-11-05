package dev.hugomarket.container;

public interface ContainerListener {
    void onContainerUpdated(ContainerSnapshot snapshot);

    default void onContainerClosed(int containerId) {
    }
}
