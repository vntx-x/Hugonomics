package dev.hugomarket.config;

import dev.hugomarket.core.ClientScheduler;
import dev.hugomarket.core.ServerDetector;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class FirstStartHint {
    private static final int DELAY_TICKS = 20 * 8;

    private final ConfigStore config;
    private final ServerDetector server;
    private final ClientScheduler scheduler;
    private int ticksOnServer;
    private boolean done;

    public FirstStartHint(ConfigStore config, ServerDetector server, ClientScheduler scheduler) {
        this.config = config;
        this.server = server;
        this.scheduler = scheduler;
    }

    public void register() {
        done = config.get().tutorialShown;
        scheduler.onTick(this::tick);
    }

    private void tick(Minecraft client) {
        if (done || client.player == null || !server.active()) {
            ticksOnServer = 0;
            return;
        }
        if (++ticksOnServer < DELAY_TICKS) {
            return;
        }
        done = true;
        config.get().tutorialShown = true;
        config.save();
        client.gui.getChat().addMessage(Component.literal("[HugoMarket] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.translatable("hugomarket.help.first_start").withStyle(ChatFormatting.WHITE)));
    }
}
