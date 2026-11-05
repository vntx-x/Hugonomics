package dev.hugomarket.module.myauctions;

import dev.hugomarket.model.AhView;
import dev.hugomarket.model.Money;
import dev.hugomarket.parse.AhListingParser;
import dev.hugomarket.ui.OverlayPanel;
import dev.hugomarket.ui.Theme;
import dev.hugomarket.ui.widget.Label;
import net.minecraft.client.resources.language.I18n;

import java.util.List;

final class MyAuctionsPanel extends OverlayPanel {
    static final int HEIGHT = Theme.PADDING * 2 + 11 * 6;

    private final List<Label> labels;
    private String[] lines = new String[6];

    MyAuctionsPanel() {
        labels = List.of(
                add(new Label(() -> lines[0], Theme.ACCENT)),
                add(new Label(() -> lines[1], Theme.TEXT)),
                add(new Label(() -> lines[2], Theme.PRICE)),
                add(new Label(() -> lines[3])),
                add(new Label(() -> lines[4])),
                add(new Label(() -> lines[5])));
        java.util.Arrays.fill(lines, "");
        setVisible(false);
    }

    void show(OwnAuctionSummary summary, AhListingParser.AhTotals totals) {
        lines[0] = I18n.get("hugomarket.own.view." + summary.view().name().toLowerCase(java.util.Locale.ROOT));
        lines[1] = I18n.get("hugomarket.own.count", summary.count(), Money.compact(summary.priceSumCents()));
        lines[2] = switch (summary.view()) {
            case OWN_ACTIVE -> I18n.get("hugomarket.own.net_expected", Money.compact(summary.netSumCents()));
            case OWN_SOLD -> I18n.get("hugomarket.own.net_received", Money.compact(summary.netSumCents()));
            case OWN_BOUGHT -> I18n.get("hugomarket.own.spent", Money.compact(summary.priceSumCents()));
            default -> "";
        };
        lines[3] = summary.withMarketPrice() == 0 ? I18n.get("hugomarket.own.no_market")
                : I18n.get("hugomarket.own.market", Money.compact(summary.marketSumCents()), summary.withMarketPrice(), summary.overpriced());
        lines[4] = summary.soonestExpirySeconds() < 0 ? "" : I18n.get("hugomarket.own.expiry", formatDuration(summary.soonestExpirySeconds()));
        lines[5] = totals == null ? "" : I18n.get("hugomarket.own.totals", Money.compact(totals.earnedCents()), Money.compact(totals.spentCents()));
        setVisible(true);
    }

    private static String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        return hours > 0 ? hours + "h " + minutes + "m" : minutes + "m";
    }

    @Override
    protected void layout() {
        int cursorY = y + Theme.PADDING;
        for (Label label : labels) {
            label.setBounds(x + Theme.PADDING, cursorY, width - Theme.PADDING * 2, 11);
            cursorY += 11;
        }
    }
}
