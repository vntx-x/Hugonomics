package dev.hugomarket.module.myauctions;

import dev.hugomarket.model.Money;
import dev.hugomarket.parse.AhListingParser;
import dev.hugomarket.ui.OverlayPanel;
import dev.hugomarket.ui.Theme;
import dev.hugomarket.ui.widget.Label;
import net.minecraft.client.resources.language.I18n;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

final class MyAuctionsPanel extends OverlayPanel {
    static final int HEIGHT = Theme.PADDING * 2 + headerHeight() + Theme.LINE * 5;

    private final List<Label> labels;
    private final String[] lines = new String[5];

    MyAuctionsPanel() {
        Arrays.fill(lines, "");
        setTitle(I18n.get("hugomarket.own.title"));
        labels = List.of(
                add(new Label(() -> lines[0], Theme.TEXT)),
                add(new Label(() -> lines[1], Theme.PRICE)),
                add(new Label(() -> lines[2])),
                add(new Label(() -> lines[3])),
                add(new Label(() -> lines[4])));
        setVisible(false);
    }

    void show(OwnAuctionSummary summary, AhListingParser.AhTotals totals) {
        setTitle(I18n.get("hugomarket.own.view." + summary.view().name().toLowerCase(Locale.ROOT)));
        lines[0] = I18n.get("hugomarket.own.count", summary.count(), Money.compact(summary.priceSumCents()));
        lines[1] = switch (summary.view()) {
            case OWN_ACTIVE -> I18n.get("hugomarket.own.net_expected", Money.compact(summary.netSumCents()));
            case OWN_SOLD -> I18n.get("hugomarket.own.net_received", Money.compact(summary.netSumCents()));
            case OWN_BOUGHT -> I18n.get("hugomarket.own.spent", Money.compact(summary.priceSumCents()));
            default -> "";
        };
        lines[2] = summary.withMarketPrice() == 0 ? I18n.get("hugomarket.own.no_market")
                : I18n.get("hugomarket.own.market", Money.compact(summary.marketSumCents()), summary.withMarketPrice(), summary.overpriced());
        lines[3] = summary.soonestExpirySeconds() < 0 ? "" : I18n.get("hugomarket.own.expiry", formatDuration(summary.soonestExpirySeconds()));
        lines[4] = totals == null ? "" : I18n.get("hugomarket.own.totals", Money.compact(totals.earnedCents()), Money.compact(totals.spentCents()));
        setVisible(true);
    }

    private static String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        return hours > 0 ? hours + "h " + minutes + "m" : minutes + "m";
    }

    @Override
    protected void layout() {
        int cursorY = contentTop();
        for (Label label : labels) {
            label.setBounds(x + Theme.PADDING, cursorY, width - Theme.PADDING * 2, Theme.LINE);
            cursorY += Theme.LINE;
        }
    }
}
