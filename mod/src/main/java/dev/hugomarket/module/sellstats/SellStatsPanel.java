package dev.hugomarket.module.sellstats;

import dev.hugomarket.model.Money;
import dev.hugomarket.store.SellStats;
import dev.hugomarket.ui.Gfx;
import dev.hugomarket.ui.OverlayPanel;
import dev.hugomarket.ui.Theme;
import dev.hugomarket.ui.widget.Label;
import net.minecraft.client.resources.language.I18n;

import java.util.List;

final class SellStatsPanel extends OverlayPanel {
    static final int HEIGHT = Theme.PADDING * 2 + 11 * 5;
    private static final long REFRESH_MS = 1_000;

    private final SellStats stats;
    private final List<Label> labels;
    private SellStats.Summary summary;
    private long refreshedAt;

    SellStatsPanel(SellStats stats) {
        this.stats = stats;
        this.summary = stats.summary(System.currentTimeMillis());
        labels = List.of(
                add(new Label(() -> I18n.get("hugomarket.sellstats.title"), Theme.ACCENT)),
                add(new Label(() -> I18n.get("hugomarket.sellstats.session", Money.compact(summary.sessionCents()), summary.activeMinutesSession()), Theme.TEXT)),
                add(new Label(() -> I18n.get("hugomarket.sellstats.rate", Money.compact(summary.ratePerHourCents())), Theme.PRICE)),
                add(new Label(() -> I18n.get("hugomarket.sellstats.day", Money.compact(summary.last24hCents())))),
                add(new Label(() -> I18n.get("hugomarket.sellstats.all", Money.compact(summary.allTimeCents()), summary.allTimeCount()))));
    }

    @Override
    protected void layout() {
        int cursorY = y + Theme.PADDING;
        for (Label label : labels) {
            label.setBounds(x + Theme.PADDING, cursorY, width - Theme.PADDING * 2, 11);
            cursorY += 11;
        }
    }

    @Override
    public void render(Gfx gfx, int mouseX, int mouseY) {
        long now = System.currentTimeMillis();
        if (now - refreshedAt >= REFRESH_MS) {
            refreshedAt = now;
            summary = stats.summary(now);
        }
        super.render(gfx, mouseX, mouseY);
    }
}
