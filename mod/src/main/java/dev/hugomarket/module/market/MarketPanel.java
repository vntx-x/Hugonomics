package dev.hugomarket.module.market;

import dev.hugomarket.model.ItemKey;
import dev.hugomarket.model.Money;
import dev.hugomarket.module.sync.PriceJson;
import dev.hugomarket.store.MarketSession;
import dev.hugomarket.store.PriceStats;
import dev.hugomarket.ui.Gfx;
import dev.hugomarket.ui.OverlayPanel;
import dev.hugomarket.ui.Theme;
import dev.hugomarket.ui.widget.ChartWidget;
import dev.hugomarket.ui.widget.Chip;
import dev.hugomarket.ui.widget.Label;
import dev.hugomarket.ui.widget.ListView;
import dev.hugomarket.ui.widget.TextField;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class MarketPanel extends OverlayPanel {
    private static final String[] RANGES = {"1h", "6h", "24h", "7d", "31d"};
    private static final int ICON_SIZE = 16;

    private final MarketData data;
    private final MarketSession market;
    private final TextField search;
    private final Label status;
    private final ListView<PriceJson.ItemSummary> list;
    private final Label detailTitle;
    private final Chip back;
    private final List<Chip> rangeChips = new ArrayList<>();
    private final ChartWidget chart;
    private final Map<String, ItemStack> icons = new HashMap<>();
    private String query = "";
    private PriceJson.ItemSummary selected;
    private String range = "24h";
    private boolean chartLoading;

    MarketPanel(MarketData data, MarketSession market) {
        this.data = data;
        this.market = market;
        search = add(new TextField(I18n.get("hugomarket.market.search"), "", text -> {
            query = text;
            refreshList();
        }));
        status = add(new Label(this::statusText));
        list = add(new ListView<>(Theme.ROW_HEIGHT, this::renderRow, (item, button) -> select(item)));
        detailTitle = add(new Label(() -> selected == null ? "" : selected.displayName(), Theme.ACCENT));
        back = add(new Chip(I18n.get("hugomarket.market.back"), () -> false, () -> select(null)));
        for (String r : RANGES) {
            rangeChips.add(add(new Chip(r, () -> range.equals(r), () -> setRange(r))));
        }
        chart = add(new ChartWidget());
        chart.setEmptyText(I18n.get("hugomarket.market.no_history"));
        data.onChange(this::refreshList);
        refreshList();
        select(null);
    }

    @Override
    protected void layout() {
        int innerX = x + Theme.PADDING;
        int innerWidth = width - Theme.PADDING * 2;
        int cursorY = y + Theme.PADDING;
        search.setBounds(innerX, cursorY, innerWidth, Theme.FIELD_HEIGHT);
        cursorY += Theme.FIELD_HEIGHT + Theme.GAP;
        status.setBounds(innerX, cursorY, innerWidth, 10);
        cursorY += 10 + Theme.GAP;
        list.setBounds(innerX, cursorY, innerWidth, y + height - Theme.PADDING - cursorY);

        int detailY = y + Theme.PADDING;
        back.setBounds(innerX, detailY, 60, Theme.CHIP_HEIGHT);
        detailTitle.setBounds(innerX + 64, detailY, innerWidth - 64, Theme.CHIP_HEIGHT);
        detailY += Theme.CHIP_HEIGHT + Theme.GAP;
        int chipWidth = (innerWidth - Theme.GAP * (RANGES.length - 1)) / RANGES.length;
        for (int i = 0; i < rangeChips.size(); i++) {
            rangeChips.get(i).setBounds(innerX + i * (chipWidth + Theme.GAP), detailY, chipWidth, Theme.CHIP_HEIGHT);
        }
        detailY += Theme.CHIP_HEIGHT + Theme.GAP + 24;
        chart.setBounds(innerX, detailY, innerWidth, y + height - Theme.PADDING - detailY);
    }

    @Override
    public void render(Gfx gfx, int mouseX, int mouseY) {
        gfx.fill(x, y, width, height, Theme.PANEL_BG);
        gfx.border(x, y, width, height, Theme.PANEL_BORDER);
        if (selected == null) {
            search.render(gfx, mouseX, mouseY);
            status.render(gfx, mouseX, mouseY);
            list.render(gfx, mouseX, mouseY);
            return;
        }
        back.render(gfx, mouseX, mouseY);
        detailTitle.render(gfx, mouseX, mouseY);
        for (Chip chip : rangeChips) {
            chip.render(gfx, mouseX, mouseY);
        }
        PriceStats stats = market.stats(ItemKey.parse(selected.key()));
        int infoY = rangeChips.get(0).y() + Theme.CHIP_HEIGHT + Theme.GAP + 2;
        if (stats != null) {
            gfx.textNoShadow(I18n.get("hugomarket.market.detail_prices", Money.format(stats.lowestUnitCents()), Money.format(stats.medianUnitCents()), stats.listings()), x + Theme.PADDING, infoY, Theme.TEXT);
            long minutes = Math.max(0, (System.currentTimeMillis() - stats.lastSeen()) / 60_000);
            gfx.textNoShadow(I18n.get("hugomarket.market.detail_seen", minutes), x + Theme.PADDING, infoY + 11, Theme.TEXT_DIM);
        } else {
            gfx.textNoShadow(chartLoading ? I18n.get("hugomarket.market.loading") : I18n.get("hugomarket.market.no_prices"), x + Theme.PADDING, infoY, Theme.TEXT_DIM);
        }
        chart.render(gfx, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!contains(mouseX, mouseY)) {
            search.setFocused(false);
            return false;
        }
        if (selected == null) {
            return search.mouseClicked(mouseX, mouseY, button) || list.mouseClicked(mouseX, mouseY, button) || true;
        }
        if (back.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        for (Chip chip : rangeChips) {
            if (chip.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return selected == null && list.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        return selected == null && search.isFocused() && search.keyPressed(key, scancode, modifiers);
    }

    @Override
    public boolean charTyped(char character) {
        return selected == null && search.isFocused() && search.charTyped(character);
    }

    @Override
    public boolean hasFocus() {
        return selected == null && search.isFocused();
    }

    void focusSearch() {
        search.setFocused(true);
    }

    private void select(PriceJson.ItemSummary item) {
        selected = item;
        if (item != null) {
            loadChart();
        }
    }

    private void setRange(String newRange) {
        range = newRange;
        loadChart();
    }

    private void loadChart() {
        if (selected == null) {
            return;
        }
        chartLoading = true;
        chart.setPoints(List.of());
        String key = selected.key();
        data.loadHistory(key, range, points -> {
            if (selected == null || !selected.key().equals(key)) {
                return;
            }
            chartLoading = false;
            List<ChartWidget.Point> chartPoints = new ArrayList<>(points.size());
            for (PriceJson.HistoryPoint point : points) {
                chartPoints.add(new ChartWidget.Point(point.ts(), point.lowestUnitCents(), point.medianUnitCents()));
            }
            chart.setPoints(chartPoints);
        });
    }

    private void refreshList() {
        String needle = query.trim().toLowerCase(Locale.ROOT);
        List<PriceJson.ItemSummary> rows = new ArrayList<>();
        for (PriceJson.ItemSummary item : data.items()) {
            if (needle.isEmpty() || item.displayName().toLowerCase(Locale.ROOT).contains(needle) || item.key().contains(needle)) {
                rows.add(item);
            }
        }
        list.setRows(rows);
    }

    private String statusText() {
        if (!data.error().isEmpty()) {
            return I18n.get("hugomarket.market.error", data.error());
        }
        if (data.isLoading() && data.items().isEmpty()) {
            return I18n.get("hugomarket.market.loading");
        }
        return I18n.get("hugomarket.market.status", list.rowCount(), data.items().size());
    }

    private void renderRow(Gfx gfx, PriceJson.ItemSummary item, int rowX, int rowY, int rowWidth, int rowHeight, boolean hovered) {
        ItemStack icon = icons.computeIfAbsent(item.baseId(), MarketPanel::iconFor);
        if (!icon.isEmpty()) {
            gfx.item(icon, rowX + 1, rowY + (rowHeight - ICON_SIZE) / 2);
        }
        PriceStats stats = market.stats(ItemKey.parse(item.key()));
        String price = stats == null ? "…" : Money.compact(stats.lowestUnitCents());
        int priceWidth = gfx.textWidth(price);
        int textX = rowX + ICON_SIZE + 4;
        int nameWidth = rowWidth - ICON_SIZE - 8 - priceWidth;
        String variant = ItemKey.parse(item.key()).variant();
        gfx.textNoShadow(gfx.clip(item.displayName(), nameWidth), textX, rowY + 2, Theme.TEXT);
        String detail = variant.isEmpty() ? I18n.get("hugomarket.market.row", item.activeListings()) : variant;
        gfx.textNoShadow(gfx.clip(detail, nameWidth), textX, rowY + 11, Theme.TEXT_DIM);
        gfx.textNoShadow(price, rowX + rowWidth - priceWidth - 3, rowY + 6, Theme.PRICE);
    }

    private static ItemStack iconFor(String baseId) {
        ResourceLocation id = ResourceLocation.tryParse(baseId);
        if (id == null) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.getValue(id);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }
}
