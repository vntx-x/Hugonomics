package dev.hugomarket.module.spawners;

import dev.hugomarket.ui.GermanItemNames;
import net.minecraft.client.resources.language.I18n;

final class SpawnerNames {
    private SpawnerNames() {
    }

    static String of(String type) {
        String entityKey = "entity.minecraft." + type;
        String entity = I18n.exists(entityKey) ? I18n.get(entityKey) : type.replace('_', ' ');
        return I18n.get("hugomarket.spawners.name", entity);
    }

    static String german(String type) {
        return GermanItemNames.text("hugomarket.spawners.name", GermanItemNames.text("entity.minecraft." + type));
    }
}
