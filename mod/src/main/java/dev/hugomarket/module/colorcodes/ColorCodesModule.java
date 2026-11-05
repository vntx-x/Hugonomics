package dev.hugomarket.module.colorcodes;

import dev.hugomarket.core.Module;
import dev.hugomarket.core.ModuleContext;
import net.minecraft.client.Minecraft;

public final class ColorCodesModule implements Module {
    private final ColorCodesState state = new ColorCodesState();

    @Override
    public String id() {
        return "colorcodes";
    }

    @Override
    public boolean enabledByDefault() {
        return true;
    }

    @Override
    public void onEnable(ModuleContext context) {
        context.menu().add("hugomarket.colors.open", () -> {
            Minecraft client = Minecraft.getInstance();
            client.setScreen(new ColorCodesScreen(client.screen, state));
        });
        new ChatColorPreview().register();
    }

    @Override
    public void onDisable() {
    }
}
