package dev.hugomarket.module.hudstatus;

import dev.hugomarket.core.Module;
import dev.hugomarket.core.ModuleContext;
import dev.hugomarket.model.Money;
import dev.hugomarket.store.HudBoard;
import dev.hugomarket.store.StatusBoard;
import dev.hugomarket.ui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;

import java.util.ArrayList;
import java.util.List;

public final class HudStatusModule implements Module {
    private static final int REFRESH_TICKS = 20;

    private ModuleContext context;
    private boolean enabled;
    private int ticks;
    private int version = -1;

    @Override
    public String id() {
        return "hudstatus";
    }

    @Override
    public boolean enabledByDefault() {
        return true;
    }

    @Override
    public void onEnable(ModuleContext context) {
        this.context = context;
        enabled = true;
        version = -1;
        context.scheduler().onTick(this::tick);
    }

    @Override
    public void onDisable() {
        enabled = false;
        context.hud().set(HudBoard.STATUS, List.of());
    }

    private void tick(Minecraft client) {
        StatusBoard status = context.status();
        if (!enabled || ++ticks % REFRESH_TICKS != 0 || status.version() == version) {
            return;
        }
        version = status.version();
        List<HudBoard.Line> lines = new ArrayList<>(2);
        if (status.todayKnown()) {
            long net = status.todayNetCents();
            String amount = (net >= 0 ? "+" : "−") + Money.compact(Math.abs(net));
            lines.add(new HudBoard.Line(I18n.get("hugomarket.status.today", amount), net >= 0 ? Theme.BARGAIN_FRAME : Theme.DANGER));
        }
        if (status.netWorthKnown()) {
            lines.add(new HudBoard.Line(I18n.get("hugomarket.status.worth", Money.compact(status.netWorthCents())), Theme.GOLD));
        }
        context.hud().set(HudBoard.STATUS, lines);
    }
}
