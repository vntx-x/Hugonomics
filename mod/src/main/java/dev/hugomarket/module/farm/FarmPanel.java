package dev.hugomarket.module.farm;

import dev.hugomarket.model.Money;
import dev.hugomarket.store.FarmSession;
import dev.hugomarket.store.Inflow;
import dev.hugomarket.store.MarketSession;
import dev.hugomarket.ui.Gfx;
import dev.hugomarket.ui.OverlayPanel;
import dev.hugomarket.ui.Theme;
import dev.hugomarket.ui.widget.Chip;
import dev.hugomarket.ui.widget.Label;
import net.minecraft.client.resources.language.I18n;

import java.util.List;
import java.util.Locale;

final class FarmPanel extends OverlayPanel {
    static final int HEIGHT = Theme.PADDING * 2 + headerHeight() + Theme.LINE * 6 + 10 * 3 + Theme.GAP * 2 + Theme.CHIP_HEIGHT;
    private static final long REFRESH_MS = 1_000;

    private final FarmSession session;
    private final Inflow inflow;
    private final MarketSession market;
    private final List<Label> inflowLines;
    private final List<Label> lines;
    private final List<Label> topLines;
    private final Chip reset;
    private FarmSession.Summary summary;
    private Inflow.Rate rate;
    private long refreshedAt;

    FarmPanel(FarmSession session, Inflow inflow, MarketSession market) {
        this.session = session;
        this.inflow = inflow;
        this.market = market;
        long now = System.currentTimeMillis();
        this.summary = session.summary(market, now);
        this.rate = inflow.rate(market, now);
        setTitle(I18n.get("hugomarket.farm.inflow_title"));
        setHeaderRight(() -> I18n.get("hugomarket.farm.title", formatDuration(summary.durationMs())));
        inflowLines = List.of(
                add(new Label(this::inflowLine, Theme.TEXT)),
                add(new Label(this::inflowValueLine, Theme.PRICE)));
        lines = List.of(
                add(new Label(() -> I18n.get("hugomarket.farm.session_caption"), Theme.ACCENT)),
                add(new Label(() -> I18n.get("hugomarket.farm.collected", summary.itemsCollected(), Money.compact(summary.collectedValueCents())), Theme.TEXT)),
                add(new Label(() -> I18n.get("hugomarket.farm.income", Money.compact(summary.incomeCents()), Money.compact(summary.incomePerHourCents())), Theme.PRICE)),
                add(new Label(() -> I18n.get("hugomarket.farm.value_rate", Money.compact(summary.collectedPerHourCents())))));
        topLines = List.of(add(new Label(() -> topLine(0))), add(new Label(() -> topLine(1))), add(new Label(() -> topLine(2))));
        reset = add(new Chip(I18n.get("hugomarket.farm.reset"), () -> false, () -> session.reset(System.currentTimeMillis())));
    }

    private String inflowLine() {
        if (!rate.ready()) {
            return rate.items() == 0 ? I18n.get("hugomarket.farm.inflow_waiting", rate.observedMs() / 1000) : I18n.get("hugomarket.farm.inflow_measuring");
        }
        return I18n.get("hugomarket.farm.inflow_rate", String.format(Locale.ROOT, "%.1f", rate.itemsPerMinute()), rate.perHour(), rate.perDay());
    }

    private String inflowValueLine() {
        if (!rate.ready() || rate.valuePerHourCents() == 0) {
            return "";
        }
        return I18n.get("hugomarket.farm.inflow_value", Money.compact(rate.valuePerHourCents()), Money.compact(rate.valuePerHourCents() * 24), rate.topKey());
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
        int cursorY = contentTop();
        for (Label label : inflowLines) {
            label.setBounds(innerX, cursorY, innerWidth, Theme.LINE);
            cursorY += Theme.LINE;
        }
        cursorY += Theme.GAP;
        for (Label label : lines) {
            label.setBounds(innerX, cursorY, innerWidth, Theme.LINE);
            cursorY += Theme.LINE;
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
            rate = inflow.rate(market, now);
        }
        super.render(gfx, mouseX, mouseY);
    }
}
