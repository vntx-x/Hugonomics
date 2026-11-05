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
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.OptionalDouble;

final class AhSearchPanel extends OverlayPanel {
    private static final int ICON_SIZE = 16;
    private static final int CAPTION_WIDTH = 34;
    private static final String STAR = "★ ";

    private final AhSearchState state;
    private final SlotHighlighter highlighter;
    private final TextField search;
    private final Label sortCaption;
    private final Label filterCaption;
    private final List<Chip> sortChips;
    private final List<Chip> filterChips;
    private final ListView<ListingIndex.Entry> results;

    AhSearchPanel(AbstractContainerScreen<?> screen, AhSearchState state, MarketSession market) {
        this.state = state;
        this.highlighter = new SlotHighlighter(screen, state, market);
        setTitle(I18n.get("hugomarket.ah.title"));
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
                        () -> state.setQuery(state.query().toggleWatched()))));
        results = add(new ListView<>(Theme.ROW_HEIGHT, this::renderRow, this::rowClicked));
        results.setHoverRenderer((gfx, entry, mouseX, mouseY) -> {
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
        results.setBounds(innerX, cursorY, innerWidth, y + height - Theme.PADDING - cursorY);
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
        boolean watched = state.isWatched(entry.listing());
        boolean bargain = ratio.isPresent() && ratio.getAsDouble() <= Deals.BARGAIN_FACTOR;
        if (bargain) {
            gfx.fill(rowX, rowY + 1, 2, rowHeight - 2, Theme.BARGAIN_FRAME);
        }
        String name = entry.listing().item().displayName();
        if (entry.listing().item().count() > 1) {
            name = entry.listing().item().count() + "x " + name;
        }
        if (watched) {
            name = STAR + name;
        }
        gfx.textNoShadow(gfx.clip(name, nameWidth), textX, rowY + 2, watched ? Theme.WATCH_FRAME : Theme.TEXT);
        gfx.textNoShadow(gfx.clip(entry.listing().seller(), nameWidth), textX, rowY + 11, Theme.TEXT_DIM);
        int priceY = delta.isEmpty() ? rowY + 6 : rowY + 2;
        gfx.textNoShadow(price, rowX + rowWidth - gfx.textWidth(price) - 3, priceY, Theme.PRICE);
        if (!delta.isEmpty()) {
            int deltaColor = bargain ? Theme.BARGAIN_FRAME : ratio.getAsDouble() > 1.15 ? Theme.DANGER : Theme.TEXT_DIM;
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
