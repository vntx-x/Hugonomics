package dev.hugomarket.ui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hugomarket.HugoMarket;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;

//? if >=1.21.9 {
/*import net.minecraft.resources.ResourceLocation;
*///?}

public final class KeyBindings {
    //? if >=1.21.9 {
    /*private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(ResourceLocation.fromNamespaceAndPath(HugoMarket.MOD_ID, "main"));
    *///?} else
    private static final String CATEGORY = "key.categories." + HugoMarket.MOD_ID;

    private KeyBindings() {
    }

    public static KeyMapping register(String name, int defaultKey) {
        return KeyBindingHelper.registerKeyBinding(new KeyMapping(name, InputConstants.Type.KEYSYM, defaultKey, CATEGORY));
    }
}
