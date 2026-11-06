package dev.hugomarket.module.ahsearch;

import dev.hugomarket.model.Listing;
import dev.hugomarket.store.MarketSession;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;

public final class AhJump {
    private AhJump() {
    }

    public static void open(MarketSession market, Listing listing) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        String term = AhSearchTerm.of(listing);
        market.requestAhFocus(listing.key().toString());
        if (client.screen != null && !(client.screen instanceof AbstractContainerScreen<?>)) {
            client.setScreen(null);
        }
        client.player.connection.sendCommand("ah " + term);
        client.gui.setOverlayMessage(Component.translatable("hugomarket.ah.jump", term), false);
    }
}
