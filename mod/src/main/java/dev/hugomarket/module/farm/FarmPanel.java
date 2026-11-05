package dev.hugomarket.module.farm;

import dev.hugomarket.model.Money;
import dev.hugomarket.store.FarmSession;
import dev.hugomarket.store.MarketSession;
import dev.hugomarket.ui.Gfx;
import dev.hugomarket.ui.OverlayPanel;
import dev.hugomarket.ui.Theme;
import dev.hugomarket.ui.widget.Chip;
import dev.hugomarket.ui.widget.Label;
import net.minecraft.client.resources.language.I18n;

import java.util.List;

final class FarmPanel extends OverlayPanel {
    static final int HEIGHT = Theme.PADDING * 2 + 11 * 4 + Theme.CHIP_HEIGHT + Theme.GAP + 10 * 3;
    private static final long REFRESH_MS = 1_000;

    private final FarmSession session;
    private final MarketSession market;
    private final List<Label> lines;
    private final List<Label> topLines;
    private final Chip reset;
    private FarmSession.Summary summary;
    private long refreshedAt;

    FarmPanel(FarmSession session, MarketSession market) {
        this.session = session;
        this.market = market;
        this.summary = session.summary(market, System.currentTimeMillis());
        lines = List.of(
                add(new Label(() -> I18n.get("hugomarket.farm.title", formatDuration(summary.durationMs())), Theme.ACCENT)),
                add(new Label(() -> I18n.get("hugomarket.farm.collected", summary.itemsCollected(), Money.compact(summary.collectedValueCents())), Theme.TEXT)),
                add(new Label(() -> I18n.get("hugomarket.farm.income", Money.compact(summary.incomeCents()), Money.compact(summary.incomePerHourCents())), Theme.PRICE)),
                add(new Label(() -> I18n.get("hugomarket.farm.value_rate", Money.compact(summary.collectedPerHourCents())))));
        topLines = List.of(add(new Label(() -> topLine(0))), add(new Label(() -> topLine(1))), add(new Label(() -> topLine(2))));
        reset = add(new Chip(I18n.get("hugomarket.farm.reset"), () -> false, () -> session.reset(System.currentTimeMillis())));
    }

    private String topLine(int index) {
        if (index >= summary.top().size()) {
            return "";
        }
        FarmSession.TopItem item = summary.top().get(index);
        return item.count() + "x " + item.key() + (item.valueCents() > 0 ? "  " + Money.compact(item.valueCents()) : "");
    }

    private static String formatDuration(long ms) {
        long minutes = ms / 60_000;
        return minutes >= 60 ? (minutes / 60) + "h " + (minutes % 60) + "m" : minutes + "m";
    }

    @Override
    protected void layout() {
        int innerX = x + Theme.PADDING;
        int innerWidth = width - Theme.PADDING * 2;
        int cursorY = y + Theme.PADDING;
        for (Label label : lines) {
            label.setBounds(innerX, cursorY, innerWidth, 11);
            cursorY += 11;
        }
        for (Label label : topLines) {
            label.setBounds(innerX, cursorY, innerWidth, 10);
            cursorY += 10;
        }
        cursorY += Theme.GAP;
        reset.setBounds(innerX, cursorY, 60, Theme.CHIP_HEIGHT);
    }

    @Override
    public void render(Gfx gfx, int mouseX, int mouseY) {
        long now = System.currentTimeMillis();
        if (now - refreshedAt >= REFRESH_MS) {
            refreshedAt = now;
            summary = session.summary(market, now);
        }
        super.render(gfx, mouseX, mouseY);
    }
}
