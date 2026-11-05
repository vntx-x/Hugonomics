package dev.hugomarket.module.sellassist;

import dev.hugomarket.mixin.AnvilScreenAccessor;
import dev.hugomarket.model.Money;
import dev.hugomarket.store.Fees;
import dev.hugomarket.store.MarketSession;
import dev.hugomarket.store.PriceStats;
import dev.hugomarket.store.UserData;
import dev.hugomarket.ui.Gfx;
import dev.hugomarket.ui.OverlayPanel;
import dev.hugomarket.ui.Theme;
import dev.hugomarket.ui.widget.Chip;
import dev.hugomarket.ui.widget.Label;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.resources.language.I18n;

import java.util.List;
import java.util.OptionalLong;

final class SellAssistPanel extends OverlayPanel {
    private static final int WARNING_COLOR = Theme.DANGER;
    private static final int LINE = Theme.LINE;

    private final AnvilScreen screen;
    private final PendingSale sale;
    private final MarketSession market;
    private final Fees fees;
    private final OptionalLong lastPrice;
    private final List<Chip> buttons;
    private final Chip lastButton;
    private final List<Label> lines;
    private PriceStats stats;
    private String typed = "";
    private String feeText = "";
    private String warningText = "";

    SellAssistPanel(AnvilScreen screen, PendingSale sale, MarketSession market, Fees fees, UserData userData) {
        this.screen = screen;
        this.sale = sale;
        this.market = market;
        this.fees = fees;
        this.stats = market.stats(sale.key());
        this.lastPrice = userData.lastUnitPrice(sale.key().toString());
        String title = sale.count() > 1 ? sale.count() + "x " + sale.item().displayName() : sale.item().displayName();
        setTitle(I18n.get("hugomarket.sell.title"));
        lines = List.of(
                add(new Label(() -> title, Theme.GOLD)),
                add(new Label(this::lowestText)),
                add(new Label(this::medianText)),
                add(new Label(this::seenText)),
                add(new Label(() -> feeText)),
                add(new Label(() -> warningText, WARNING_COLOR)));
        buttons = List.of(
                add(new Chip(I18n.get("hugomarket.sell.undercut"), () -> false, () -> applyStats(SellSuggestion::undercutStackDollars))),
                add(new Chip(I18n.get("hugomarket.sell.lowest"), () -> false, () -> applyStats(SellSuggestion::lowestStackDollars))),
                add(new Chip(I18n.get("hugomarket.sell.median"), () -> false, () -> applyStats(SellSuggestion::medianStackDollars))));
        lastButton = add(new Chip(lastPrice.isPresent()
                ? I18n.get("hugomarket.sell.last", Money.format(lastPrice.getAsLong()), Money.format(lastPrice.getAsLong() * sale.count()))
                : I18n.get("hugomarket.sell.no_last"), () -> false, this::applyLast));
        market.onChange(() -> stats = market.stats(sale.key()));
    }

    private interface Suggestion {
        long dollars(PriceStats stats, int count);
    }

    @Override
    protected void layout() {
        int innerX = x + Theme.PADDING;
        int innerWidth = width - Theme.PADDING * 2;
        int cursorY = contentTop();
        for (int i = 0; i < 4; i++) {
            lines.get(i).setBounds(innerX, cursorY, innerWidth, LINE);
            cursorY += LINE;
        }
        cursorY += Theme.GAP;
        int chipWidth = (innerWidth - Theme.GAP * 2) / 3;
        for (int i = 0; i < buttons.size(); i++) {
            buttons.get(i).setBounds(innerX + i * (chipWidth + Theme.GAP), cursorY, chipWidth, Theme.CHIP_HEIGHT);
        }
        cursorY += Theme.CHIP_HEIGHT + Theme.GAP;
        lastButton.setBounds(innerX, cursorY, innerWidth, Theme.CHIP_HEIGHT);
        cursorY += Theme.CHIP_HEIGHT + Theme.GAP;
        lines.get(4).setBounds(innerX, cursorY, innerWidth, LINE);
        cursorY += LINE;
        lines.get(5).setBounds(innerX, cursorY, innerWidth, LINE * 2);
    }

    @Override
    public void render(Gfx gfx, int mouseX, int mouseY) {
        updateTyped();
        super.render(gfx, mouseX, mouseY);
    }

    private void applyStats(Suggestion suggestion) {
        if (stats != null) {
            apply(suggestion.dollars(stats, sale.count()));
        }
    }

    private void applyLast() {
        if (lastPrice.isPresent()) {
            apply(Math.max(1, lastPrice.getAsLong() * sale.count() / 100));
        }
    }

    private void apply(long dollars) {
        EditBox field = ((AnvilScreenAccessor) screen).hugomarket$nameField();
        if (field != null) {
            field.setValue(Long.toString(dollars));
        }
    }

    private void updateTyped() {
        EditBox field = ((AnvilScreenAccessor) screen).hugomarket$nameField();
        String value = field == null ? "" : field.getValue();
        if (value.equals(typed)) {
            return;
        }
        typed = value;
        OptionalLong dollars = SellSuggestion.parseTypedDollars(value);
        if (dollars.isEmpty()) {
            feeText = "";
            warningText = "";
            return;
        }
        long cents = dollars.getAsLong() * 100;
        feeText = I18n.get("hugomarket.sell.fees", Money.format(fees.listingFee(cents)),
                Money.format(fees.saleTax(cents)), Money.format(fees.netAfterSale(cents)));
        warningText = switch (SellSuggestion.check(stats, sale.count(), dollars.getAsLong())) {
            case FAR_BELOW -> I18n.get("hugomarket.sell.warn_below");
            case FAR_ABOVE -> I18n.get("hugomarket.sell.warn_above");
            case NONE -> "";
        };
    }

    private String lowestText() {
        if (stats == null) {
            return I18n.get("hugomarket.sell.no_data");
        }
        return I18n.get("hugomarket.sell.lowest_line", Money.format(stats.lowestUnitCents()),
                Money.format(stats.lowestUnitCents() * sale.count()));
    }

    private String medianText() {
        if (stats == null) {
            return I18n.get("hugomarket.sell.no_data_hint");
        }
        return I18n.get("hugomarket.sell.median_line", Money.format(stats.medianUnitCents()),
                Money.format(stats.medianUnitCents() * sale.count()));
    }

    private String seenText() {
        if (stats == null) {
            return "";
        }
        long minutes = Math.max(0, (System.currentTimeMillis() - stats.lastSeen()) / 60_000);
        return I18n.get("hugomarket.sell.seen_line", stats.listings(), minutes);
    }
}
