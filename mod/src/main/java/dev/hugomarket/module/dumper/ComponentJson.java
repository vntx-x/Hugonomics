package dev.hugomarket.module.dumper;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.RegistryOps;

final class ComponentJson {
    private ComponentJson() {
    }

    static DynamicOps<JsonElement> ops() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return JsonOps.INSTANCE;
        }
        return RegistryOps.create(JsonOps.INSTANCE, client.level.registryAccess());
    }

    static JsonElement of(Component component) {
        return of(component, ops());
    }

    static JsonElement of(Component component, DynamicOps<JsonElement> ops) {
        return ComponentSerialization.CODEC.encodeStart(ops, component).result().orElse(JsonNull.INSTANCE);
    }
}
