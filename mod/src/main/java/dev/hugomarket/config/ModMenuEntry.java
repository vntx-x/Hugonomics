package dev.hugomarket.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.hugomarket.HugoMarketClient;

public final class ModMenuEntry implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new MenuScreen(parent, HugoMarketClient.registry(), HugoMarketClient.menuActions());
    }
}
