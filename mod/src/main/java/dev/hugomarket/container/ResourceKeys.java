package dev.hugomarket.container;

import net.minecraft.resources.ResourceKey;

final class ResourceKeys {
    private ResourceKeys() {
    }

    static String id(ResourceKey<?> key) {
        //? if >=1.21.11 {
        /*return key.identifier().toString();
        *///?} else
        return key.location().toString();
    }
}
