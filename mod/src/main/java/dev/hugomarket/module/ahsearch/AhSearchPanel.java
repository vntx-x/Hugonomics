package dev.hugomarket.module.ahsearch;

import dev.hugomarket.model.Money;
import dev.hugomarket.store.MarketSession;
import dev.hugomarket.ui.Gfx;
import dev.hugomarket.ui.OverlayPanel;
import dev.hugomarket.ui.Theme;
import dev.hugomarket.ui.widget.Chip;
import dev.hugomarket.ui.widget.Label;
import dev.hugomarket.ui.widget.ListView;
import dev.hugomarket.ui.widget.TextField;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.ItemStack;

import java.util.List;

final class AhSearchPanel extends OverlayPanel {
    private static final int ICON_SIZE = 16;

    private final AhSearchState state;
    private final SlotHighlighter highlighter;
    private final TextField search;
    private final List<Chip> chips;
    private final Label status;
    private final ListView<ListingIndex.Entry> results;
    private final Runnable changeListener = this::refresh;

    AhSearchPanel(AbstractContainerScreen<?> screen, AhSearchState state, MarketSession market) {
        this.state = state;
        this.highlighter = new SlotHighlighter(screen, state, market);
        search = add(new TextField(I18n.get("hugomarket.ah.search"), state.query().text(),
                text -> state.setQuery(state.query().withText(text))));
        chips = List.of(
                add(new Chip(I18n.get("hugomarket.ah.sort.unit_asc"), () -> state.query().sort() == SearchQuery.Sort.UNIT_ASC,
                        () -> state.setQuery(state.query().withSort(SearchQuery.Sort.UNIT_ASC)))),
                add(new Chip(I18n.get("hugomarket.ah.sort.unit_desc"), () -> state.query().sort() == SearchQuery.Sort.UNIT_DESC,
                        () -> state.setQuery(state.query().withSort(SearchQuery.Sort.UNIT_DESC)))),
                add(new Chip(I18n.get("hugomarket.ah.sort.expires"), () -> state.query().sort() == SearchQuery.Sort.EXPIRES_ASC,
                        () -> state.setQuery(state.query().withSort(SearchQuery.Sort.EXPIRES_ASC)))),
                add(new Chip(I18n.get("hugomarket.ah.sort.newest"), () -> state.query().sort() == SearchQuery.Sort.NEWEST,
                        () -> state.setQuery(state.query().withSort(SearchQuery.Sort.NEWEST)))),
                add(new Chip(I18n.get("hugomarket.ah.filter.enchanted"), () -> state.query().onlyEnchanted(),
                        () -> state.setQuery(state.query().toggleEnchanted()))),
                add(new Chip(I18n.get("hugomarket.ah.filter.spawner"), () -> state.query().onlySpawner(),
                        () -> state.setQuery(state.query().toggleSpawner()))));
        status = add(new Label(() -> I18n.get("hugomarket.ah.status", state.results().size(), state.indexedCount())));
        results = add(new ListView<>(Theme.ROW_HEIGHT, this::renderRow, entry -> {
        }));
        results.setHoverRenderer((gfx, entry, mouseX, mouseY) -> {
            ItemStack stack = state.icon(entry);
            if (!stack.isEmpty()) {
                gfx.itemTooltip(stack, mouseX, mouseY);
            }
        });
        state.onChange(changeListener);
        refresh();
    }

    @Override
    protected void layout() {
        int innerX = x + Theme.PADDING;
        int innerWidth = width - Theme.PADDING * 2;
        int cursorY = y + Theme.PADDING;
        search.setBounds(innerX, cursorY, innerWidth, Theme.FIELD_HEIGHT);
        cursorY += Theme.FIELD_HEIGHT + Theme.GAP;
        int chipX = innerX;
        int chipWidth = (innerWidth - Theme.GAP) / 2;
        for (int i = 0; i < chips.size(); i++) {
            chips.get(i).setBounds(chipX, cursorY, chipWidth, Theme.CHIP_HEIGHT);
            if (i % 2 == 0) {
                chipX += chipWidth + Theme.GAP;
            } else {
                chipX = innerX;
                cursorY += Theme.CHIP_HEIGHT + Theme.GAP;
            }
        }
        status.setBounds(innerX, cursorY, innerWidth, 10);
        cursorY += 10 + Theme.GAP;
        results.setBounds(innerX, cursorY, innerWidth, y + height - Theme.PADDING - cursorY);
    }

    private void refresh() {
        results.setRows(state.results());
        highlighter.refresh();
    }

    private void renderRow(Gfx gfx, ListingIndex.Entry entry, int rowX, int rowY, int rowWidth, int rowHeight, boolean hovered) {
        ItemStack stack = state.icon(entry);
        if (!stack.isEmpty()) {
            gfx.item(stack, rowX + 1, rowY + (rowHeight - ICON_SIZE) / 2);
        }
        String price = Money.compact(entry.listing().unitPriceCents());
        int priceWidth = gfx.textWidth(price);
        int textX = rowX + ICON_SIZE + 4;
        int nameWidth = rowWidth - ICON_SIZE - 8 - priceWidth;
        String name = entry.listing().item().displayName();
        if (entry.listing().item().count() > 1) {
            name = entry.listing().item().count() + "x " + name;
        }
        gfx.textNoShadow(gfx.clip(name, nameWidth), textX, rowY + 2, Theme.TEXT);
        gfx.textNoShadow(gfx.clip(entry.listing().seller(), nameWidth), textX, rowY + 11, Theme.TEXT_DIM);
        gfx.textNoShadow(price, rowX + rowWidth - priceWidth - 3, rowY + 6, Theme.PRICE);
    }

    @Override
    public void renderBehind(Gfx gfx, int mouseX, int mouseY) {
        highlighter.render(gfx);
    }

    void focusSearch() {
        search.setFocused(true);
    }
}
