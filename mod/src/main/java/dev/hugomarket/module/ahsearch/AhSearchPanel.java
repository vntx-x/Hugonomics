package dev.hugomarket.module.ahsearch;

import dev.hugomarket.model.Money;
import dev.hugomarket.module.sync.PriceJson;
import dev.hugomarket.store.MarketSession;
import dev.hugomarket.ui.ItemLabel;
import dev.hugomarket.ui.Gfx;
import dev.hugomarket.ui.OverlayPanel;
import dev.hugomarket.ui.Theme;
import dev.hugomarket.ui.widget.ChartWidget;
import dev.hugomarket.ui.widget.Chip;
import dev.hugomarket.ui.widget.Label;
import dev.hugomarket.ui.widget.ListView;
import dev.hugomarket.ui.widget.TextField;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

final class AhSearchPanel extends OverlayPanel {
    private static final int ICON_SIZE = 16;
    private static final int CAPTION_WIDTH = 34;
    private static final String STAR = "★ ";
    private static final int HISTORY_HEIGHT = 50;
    private static final int MAX_HISTORY_HEIGHT = 110;
    private static final int WIDE_ROW = 280;
    private static final int DETAIL_COLUMN = 64;
    private static final int HOVER_DELAY_TICKS = 6;

    private final AhSearchState state;
    private final MarketSession market;
    private final SlotHighlighter highlighter;
    private final TextField search;
    private final Label sortCaption;
    private final Label filterCaption;
    private final List<Chip> sortChips;
    private final List<Chip> filterChips;
    private final ListView<ListingIndex.Entry> results;
    private final PriceHistoryCache history;
    private final ChartWidget chart;
    private ListingIndex.Entry hovered;
    private String pendingKey;
    private String shownKey;
    private int hoverTicks;
    private int historyTitleY;
    private String historyTitle = I18n.get("hugomarket.ah.history.hint");
    private String trendText = "";
    private int trendColor = Theme.TEXT_DIM;

    AhSearchPanel(AbstractContainerScreen<?> screen, AhSearchState state, MarketSession market, PriceHistoryCache history) {
        this.state = state;
        this.market = market;
        this.history = history;
        this.highlighter = new SlotHighlighter(screen, state, market);
        setTitle(I18n.get("hugomarket.ah.title"));
        setSidebar(true);
        setHeaderRight(() -> I18n.get("hugomarket.ah.status", state.results().size(), state.indexedCount()));
        search = add(new TextField(I18n.get("hugomarket.ah.search"), state.query().text(),
                text -> state.setQuery(state.query().withText(text))));
        sortCaption = add(new Label(() -> I18n.get("hugomarket.ah.sort_caption")));
        sortChips = List.of(
                add(new Chip(I18n.get("hugomarket.ah.sort.deal"), () -> state.query().sort() == SearchQuery.Sort.DEAL,
                        () -> state.setQuery(state.query().withSort(SearchQuery.Sort.DEAL)))),
                add(new Chip(I18n.get("hugomarket.ah.sort.unit_asc"), () -> state.query().sort() == SearchQuery.Sort.UNIT_ASC,
                        () -> state.setQuery(state.query().withSort(SearchQuery.Sort.UNIT_ASC)))),
                add(new Chip(I18n.get("hugomarket.ah.sort.unit_desc"), () -> state.query().sort() == SearchQuery.Sort.UNIT_DESC,
                        () -> state.setQuery(state.query().withSort(SearchQuery.Sort.UNIT_DESC)))),
                add(new Chip(I18n.get("hugomarket.ah.sort.expires"), () -> state.query().sort() == SearchQuery.Sort.EXPIRES_ASC,
                        () -> state.setQuery(state.query().withSort(SearchQuery.Sort.EXPIRES_ASC)))),
                add(new Chip(I18n.get("hugomarket.ah.sort.newest"), () -> state.query().sort() == SearchQuery.Sort.NEWEST,
                        () -> state.setQuery(state.query().withSort(SearchQuery.Sort.NEWEST)))));
        filterCaption = add(new Label(() -> I18n.get("hugomarket.ah.filter_caption")));
        filterChips = List.of(
                add(new Chip(I18n.get("hugomarket.ah.filter.enchanted"), () -> state.query().onlyEnchanted(),
                        () -> state.setQuery(state.query().toggleEnchanted()))),
                add(new Chip(I18n.get("hugomarket.ah.filter.watched"), () -> state.query().onlyWatched(),
                        () -> state.setQuery(state.query().toggleWatched()))),
                add(new Chip(I18n.get("hugomarket.ah.filter.deals"), () -> state.query().onlyDeals(),
                        () -> state.setQuery(state.query().toggleDeals()))));
        chart = add(new ChartWidget());
        chart.setEmptyText(I18n.get("hugomarket.ah.history.empty"));
        results = add(new ListView<>(Theme.ROW_HEIGHT, this::renderRow, this::rowClicked));
        results.setHoverRenderer((gfx, entry, mouseX, mouseY) -> {
            hovered = entry;
            ItemStack stack = state.icon(entry);
            if (!stack.isEmpty()) {
                gfx.itemTooltip(stack, mouseX, mouseY);
            }
        });
        state.onChange(this::refresh);
        refresh();
    }

    @Override
    protected void layout() {
        int innerX = x + Theme.PADDING;
        int innerWidth = width - Theme.PADDING * 2;
        int cursorY = contentTop();
        search.setBounds(innerX, cursorY, innerWidth, Theme.FIELD_HEIGHT);
        cursorY += Theme.FIELD_HEIGHT + Theme.GAP;
        cursorY = chipRow(sortCaption, sortChips, innerX, cursorY, innerWidth);
        cursorY = chipRow(filterCaption, filterChips, innerX, cursorY, innerWidth);
        cursorY += Theme.GAP;
        int historyHeight = Math.max(HISTORY_HEIGHT, Math.min(MAX_HISTORY_HEIGHT, height / 4));
        int historyTop = y + height - Theme.PADDING - historyHeight;
        results.setBounds(innerX, cursorY, innerWidth, historyTop - Theme.GAP - cursorY);
        historyTitleY = historyTop;
        chart.setBounds(innerX, historyTop + Theme.LINE, innerWidth, historyHeight - Theme.LINE);
    }

    @Override
    public void render(Gfx gfx, int mouseX, int mouseY) {
        hovered = null;
        super.render(gfx, mouseX, mouseY);
        if (!isVisible()) {
            return;
        }
        int innerX = x + Theme.PADDING;
        int innerWidth = width - Theme.PADDING * 2;
        gfx.fill(innerX, historyTitleY - Theme.GAP / 2 - 1, innerWidth, 1, Theme.PANEL_BORDER);
        int trendWidth = trendText.isEmpty() ? 0 : gfx.textWidth(trendText) + Theme.GAP;
        gfx.textNoShadow(gfx.clip(historyTitle, innerWidth - trendWidth), innerX, historyTitleY + 1, Theme.TEXT_DIM);
        if (!trendText.isEmpty()) {
            gfx.textNoShadow(trendText, innerX + innerWidth - gfx.textWidth(trendText), historyTitleY + 1, trendColor);
        }
    }

    void tick() {
        String key = hovered == null || hovered.listing().key().filled() ? null : hovered.listing().key().toString();
        if (key == null || !key.equals(pendingKey)) {
            pendingKey = key;
            hoverTicks = 0;
            return;
        }
        if (++hoverTicks != HOVER_DELAY_TICKS || key.equals(shownKey)) {
            return;
        }
        shownKey = key;
        String name = ItemLabel.of(hovered.listing().key(), hovered.listing().item().displayName());
        historyTitle = I18n.get("hugomarket.ah.history.title", name);
        trendText = "";
        chart.setPoints(List.of());
        history.load(key, points -> {
            if (!key.equals(shownKey)) {
                return;
            }
            List<ChartWidget.Point> chartPoints = new ArrayList<>(points.size());
            for (PriceJson.HistoryPoint point : points) {
                chartPoints.add(new ChartWidget.Point(point.ts(), point.lowestUnitCents(), point.medianUnitCents()));
            }
            chart.setPoints(chartPoints);
            OptionalDouble trend = PriceTrend.ratio(points);
            trendText = trend.isPresent() ? I18n.get("hugomarket.ah.history.trend", Deals.deltaText(trend.getAsDouble())) : "";
            trendColor = trend.isPresent() && trend.getAsDouble() >= 1 ? Theme.BARGAIN_FRAME : Theme.DANGER;
        });
    }

    private int chipRow(Label caption, List<Chip> chips, int innerX, int cursorY, int innerWidth) {
        caption.setBounds(innerX, cursorY, CAPTION_WIDTH, Theme.CHIP_HEIGHT);
        int chipsX = innerX + CAPTION_WIDTH;
        int chipWidth = (innerWidth - CAPTION_WIDTH - Theme.GAP * (chips.size() - 1)) / chips.size();
        for (int i = 0; i < chips.size(); i++) {
            chips.get(i).setBounds(chipsX + i * (chipWidth + Theme.GAP), cursorY, chipWidth, Theme.CHIP_HEIGHT);
        }
        return cursorY + Theme.CHIP_HEIGHT + Theme.GAP;
    }

    private void refresh() {
        results.setRows(state.results());
        highlighter.refresh();
    }

    private void rowClicked(ListingIndex.Entry entry, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            state.toggleWatch(entry.listing());
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            AhJump.open(market, entry);
        }
    }

    private void renderRow(Gfx gfx, ListingIndex.Entry entry, int rowX, int rowY, int rowWidth, int rowHeight, boolean hovered) {
        ItemStack stack = state.icon(entry);
        if (!stack.isEmpty()) {
            gfx.item(stack, rowX + 3, rowY + (rowHeight - ICON_SIZE) / 2);
        }
        String price = Money.compact(entry.listing().unitPriceCents());
        OptionalDouble ratio = state.deal(entry.listing());
        String delta = ratio.isPresent() ? Deals.deltaText(ratio.getAsDouble()) : "";
        int rightWidth = Math.max(gfx.textWidth(price), gfx.textWidth(delta));
        int textX = rowX + ICON_SIZE + 6;
        int nameWidth = rowWidth - ICON_SIZE - 12 - rightWidth;
        if (rowWidth >= WIDE_ROW) {
            nameWidth -= DETAIL_COLUMN;
            int detailX = rowX + rowWidth - rightWidth - 9 - DETAIL_COLUMN;
            gfx.textNoShadow(I18n.get("hugomarket.ah.total", Money.compact(entry.listing().priceCents())), detailX, rowY + 2, Theme.TEXT_DIM);
            long left = entry.expiresAt() <= 0 ? -1 : (entry.expiresAt() - System.currentTimeMillis()) / 60_000;
            if (left >= 0) {
                String expiry = left >= 120 ? I18n.get("hugomarket.ah.left_hours", left / 60) : I18n.get("hugomarket.ah.left_minutes", left);
                gfx.textNoShadow(expiry, detailX, rowY + 11, left < 30 ? Theme.WATCH_FRAME : Theme.TEXT_PLACEHOLDER);
            }
        }
        boolean watched = state.isWatched(entry.listing());
        boolean bargain = ratio.isPresent() && Deals.isBargain(ratio.getAsDouble());
        if (bargain) {
            gfx.fill(rowX, rowY + 1, 2, rowHeight - 2, Theme.BARGAIN_FRAME);
        }
        String name = ItemLabel.of(entry.listing().key(), entry.listing().item().displayName());
        if (entry.listing().item().count() > 1) {
            name = entry.listing().item().count() + "x " + name;
        }
        if (watched) {
            name = STAR + name;
        }
        gfx.textNoShadow(gfx.clip(name, nameWidth), textX, rowY + 2, watched ? Theme.WATCH_FRAME : Theme.TEXT);
        String origin = ListingIndex.isRemote(entry)
                ? I18n.get("hugomarket.ah.remote", Math.max(0, (System.currentTimeMillis() - entry.lastSeen()) / 60_000))
                : entry.listing().seller();
        gfx.textNoShadow(gfx.clip(origin, nameWidth), textX, rowY + 11, ListingIndex.isRemote(entry) ? Theme.TEXT_PLACEHOLDER : Theme.TEXT_DIM);
        int priceY = delta.isEmpty() ? rowY + 6 : rowY + 2;
        gfx.textNoShadow(price, rowX + rowWidth - gfx.textWidth(price) - 3, priceY, Theme.PRICE);
        if (!delta.isEmpty()) {
            int deltaColor = Deals.isSuspicious(ratio.getAsDouble()) ? Theme.WATCH_FRAME
                    : bargain ? Theme.BARGAIN_FRAME : ratio.getAsDouble() > 1.15 ? Theme.DANGER : Theme.TEXT_DIM;
            gfx.textNoShadow(delta, rowX + rowWidth - gfx.textWidth(delta) - 3, rowY + 11, deltaColor);
        }
    }

    @Override
    public void renderBehind(Gfx gfx, int mouseX, int mouseY) {
        highlighter.render(gfx);
    }

    void focusSearch() {
        search.setFocused(true);
    }
}
