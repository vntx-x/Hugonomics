package dev.hugomarket.module.sellassist;

import dev.hugomarket.model.ItemKey;
import dev.hugomarket.model.ItemView;

public record PendingSale(ItemView item, ItemKey key) {
    public int count() {
        return item.count();
    }
}
