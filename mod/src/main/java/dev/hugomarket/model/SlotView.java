package dev.hugomarket.model;

public record SlotView(int index, boolean playerInventory, ItemView item) {
    public boolean isEmpty() {
        return item == null;
    }
}
