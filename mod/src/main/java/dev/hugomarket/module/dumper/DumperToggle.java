package dev.hugomarket.module.dumper;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hugomarket.core.ClientScheduler;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

//? if >=1.21.9 {
/*import dev.hugomarket.HugoMarket;
import net.minecraft.resources.ResourceLocation;
*///?}

final class DumperToggle {
    private static final String KEY_NAME = "key.hugomarket.dumper_toggle";
    //? if >=1.21.9 {
    /*private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(ResourceLocation.fromNamespaceAndPath(HugoMarket.MOD_ID, "main"));
    *///?} else
    private static final String CATEGORY = "key.categories.hugomarket";

    private final DumperState state;
    private final ClientScheduler scheduler;
    private KeyMapping key;

    DumperToggle(DumperState state, ClientScheduler scheduler) {
        this.state = state;
        this.scheduler = scheduler;
    }

    void register() {
        key = KeyBindingHelper.registerKeyBinding(
                new KeyMapping(KEY_NAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F8, CATEGORY));
        scheduler.onTick(this::tick);
    }

    private void tick(Minecraft client) {
        while (key.consumeClick()) {
            boolean recording = state.toggleRecording();
            if (client.player != null) {
                client.gui.getChat().addMessage(
                        Component.translatable(recording ? "hugomarket.dumper.on" : "hugomarket.dumper.off"));
            }
        }
    }
}
