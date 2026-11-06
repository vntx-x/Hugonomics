package dev.hugomarket.module.market;

import dev.hugomarket.model.ItemKey;
import dev.hugomarket.model.Money;
import dev.hugomarket.module.ahsearch.ListingIndex;
import dev.hugomarket.module.ahsearch.SearchQuery;
import dev.hugomarket.module.calc.CalcEngine;
import dev.hugomarket.module.calc.CalcFormat;
import dev.hugomarket.module.sync.PriceJson;
import dev.hugomarket.store.MarketSession;
import dev.hugomarket.store.PriceStats;
import dev.hugomarket.store.UserData;
import dev.hugomarket.ui.CopyName;
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
import java.util.Comparator;
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
    private final ListView<PriceJson.ItemSummary> list;
    private final Label detailTitle;
    private final Chip back;
    private final List<Chip> rangeChips = new ArrayList<>();
    private final ChartWidget chart;
    private final TextField alertField;
    private final Chip alertSet;
    private final Chip alertClear;
    private final UserData userData;
    private final Runnable requestListings;
    private final ListView<ListingIndex.Entry> live;
    private final Map<String, ItemStack> icons = new HashMap<>();
    private String query = "";
    private PriceJson.ItemSummary selected;
    private String range = "24h";
    private boolean chartLoading;

    MarketPanel(MarketData data, MarketSession market, UserData userData, Runnable requestListings) {
        this.data = data;
        this.market = market;
        this.userData = userData;
        this.requestListings = requestListings;
        live = add(new ListView<>(Theme.ROW_HEIGHT, this::renderLiveRow,
                (entry, button) -> CopyName.copy(entry.listing().item().displayName())));
        market.onChange(this::refreshLive);
        setTitle(I18n.get("hugomarket.market.title"));
        setHeaderRight(this::statusText);
        alertField = add(new TextField(I18n.get("hugomarket.market.alert_placeholder"), "", text -> {
        }));
        alertSet = add(new Chip(I18n.get("hugomarket.market.alert_set"), () -> selected != null && userData.alert(selected.key()).isPresent(), this::applyAlert));
        alertClear = add(new Chip(I18n.get("hugomarket.market.alert_clear"), () -> false, this::clearAlert));
        search = add(new TextField(I18n.get("hugomarket.market.search"), "", text -> {
            query = text;
            refreshList();
        }));
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
        int cursorY = contentTop();
        search.setBounds(innerX, cursorY, innerWidth, Theme.FIELD_HEIGHT);
        cursorY += Theme.FIELD_HEIGHT + Theme.GAP;
        list.setBounds(innerX, cursorY, innerWidth, y + height - Theme.PADDING - cursorY);

        int detailY = contentTop();
        back.setBounds(innerX, detailY, 60, Theme.CHIP_HEIGHT);
        detailTitle.setBounds(innerX + 64, detailY, innerWidth - 64, Theme.CHIP_HEIGHT);
        detailY += Theme.CHIP_HEIGHT + Theme.GAP;
        int chipWidth = (innerWidth - Theme.GAP * (RANGES.length - 1)) / RANGES.length;
        for (int i = 0; i < rangeChips.size(); i++) {
            rangeChips.get(i).setBounds(innerX + i * (chipWidth + Theme.GAP), detailY, chipWidth, Theme.CHIP_HEIGHT);
        }
        detailY += Theme.CHIP_HEIGHT + Theme.GAP + 24;
        int fieldWidth = innerWidth - 2 * (60 + Theme.GAP);
        alertField.setBounds(innerX, detailY, fieldWidth, Theme.FIELD_HEIGHT);
        alertSet.setBounds(innerX + fieldWidth + Theme.GAP, detailY + 1, 60, Theme.CHIP_HEIGHT);
        alertClear.setBounds(innerX + fieldWidth + Theme.GAP * 2 + 60, detailY + 1, 60, Theme.CHIP_HEIGHT);
        detailY += Theme.FIELD_HEIGHT + Theme.GAP;
        int remaining = y + height - Theme.PADDING - detailY;
        int chartHeight = Math.max(50, Math.min(90, remaining / 2));
        chart.setBounds(innerX, detailY, innerWidth, chartHeight);
        detailY += chartHeight + Theme.GAP + Theme.LINE;
        live.setBounds(innerX, detailY, innerWidth, Math.max(0, y + height - Theme.PADDING - detailY));
    }

    private void refreshLive() {
        if (selected == null) {
            live.setRows(List.of());
            return;
        }
        List<ListingIndex.Entry> rows = new ArrayList<>();
        for (ListingIndex.Entry entry : market.index().query(SearchQuery.EMPTY)) {
            if (entry.listing().isMarketListing() && entry.listing().key().toString().equals(selected.key())) {
                rows.add(entry);
            }
        }
        rows.sort(Comparator.comparingLong(e -> e.listing().unitPriceCents()));
        live.setRows(rows);
    }

    private void renderLiveRow(Gfx gfx, ListingIndex.Entry entry, int rowX, int rowY, int rowWidth, int rowHeight, boolean hovered) {
        String origin = ListingIndex.isRemote(entry)
                ? I18n.get("hugomarket.ah.remote", Math.max(0, (System.currentTimeMillis() - entry.lastSeen()) / 60_000))
                : entry.listing().seller();
        String price = Money.compact(entry.listing().unitPriceCents());
        int priceWidth = gfx.textWidth(price);
        gfx.textNoShadow(gfx.clip(I18n.get("hugomarket.market.live_row", entry.listing().item().count(),
                Money.format(entry.listing().priceCents()), Money.format(entry.listing().unitPriceCents())), rowWidth - priceWidth - 8), rowX + 3, rowY + 2, Theme.TEXT);
        gfx.textNoShadow(gfx.clip(origin, rowWidth - priceWidth - 8), rowX + 3, rowY + 11, Theme.TEXT_DIM);
        gfx.textNoShadow(price, rowX + rowWidth - priceWidth - 3, rowY + 6, Theme.PRICE);
    }

    @Override
    public void render(Gfx gfx, int mouseX, int mouseY) {
        renderFrame(gfx);
        if (selected == null) {
            search.render(gfx, mouseX, mouseY);
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
        alertField.render(gfx, mouseX, mouseY);
        alertSet.render(gfx, mouseX, mouseY);
        alertClear.render(gfx, mouseX, mouseY);
        chart.render(gfx, mouseX, mouseY);
        int liveTitleY = live.y() - Theme.LINE;
        gfx.textNoShadow(I18n.get("hugomarket.market.live_title") + " (" + live.rowCount() + ")", x + Theme.PADDING, liveTitleY + 1, Theme.ACCENT);
        if (live.rowCount() == 0) {
            gfx.textNoShadow(I18n.get("hugomarket.market.live_empty"), x + Theme.PADDING + 3, live.y() + 2, Theme.TEXT_DIM);
        }
        live.render(gfx, mouseX, mouseY);
    }

    private void applyAlert() {
        if (selected == null) {
            return;
        }
        CalcEngine.Result result = CalcEngine.evaluate(alertField.value());
        if (result.ok() && result.value() > 0) {
            userData.setAlert(selected.key(), Math.round(result.value() * 100));
            alertField.setFocused(false);
        }
    }

    private void clearAlert() {
        if (selected != null) {
            userData.setAlert(selected.key(), 0);
            alertField.setValue("");
        }
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
        if (!alertField.contains(mouseX, mouseY)) {
            alertField.setFocused(false);
        }
        return alertField.mouseClicked(mouseX, mouseY, button) || alertSet.mouseClicked(mouseX, mouseY, button)
                || alertClear.mouseClicked(mouseX, mouseY, button) || true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return selected == null ? list.mouseScrolled(mouseX, mouseY, delta) : live.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        TextField field = selected == null ? search : alertField;
        if (!field.isFocused()) {
            return false;
        }
        if (field == alertField && (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || key == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER)) {
            applyAlert();
            return true;
        }
        return field.keyPressed(key, scancode, modifiers);
    }

    @Override
    public boolean charTyped(char character) {
        TextField field = selected == null ? search : alertField;
        return field.isFocused() && field.charTyped(character);
    }

    @Override
    public boolean hasFocus() {
        return (selected == null ? search : alertField).isFocused();
    }

    void focusSearch() {
        search.setFocused(true);
    }

    void tick() {
        if (selected != null) {
            requestListings.run();
        }
    }

    private void select(PriceJson.ItemSummary item) {
        selected = item;
        search.setFocused(false);
        alertField.setFocused(false);
        if (item != null) {
            alertField.setValue(userData.alert(item.key()).isPresent() ? CalcFormat.asInput(userData.alert(item.key()).getAsLong() / 100.0) : "");
            loadChart();
            requestListings.run();
        }
        refreshLive();
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
