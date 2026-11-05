package dev.hugomarket.module.dumper;

import dev.hugomarket.core.ClientScheduler;
import dev.hugomarket.ui.KeyBindings;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

final class DumperToggle {
    private static final String KEY_NAME = "key.hugomarket.dumper_toggle";

    private final DumperState state;
    private final ClientScheduler scheduler;
    private KeyMapping key;

    DumperToggle(DumperState state, ClientScheduler scheduler) {
        this.state = state;
        this.scheduler = scheduler;
    }

    void register() {
        key = KeyBindings.register(KEY_NAME, GLFW.GLFW_KEY_F8);
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
