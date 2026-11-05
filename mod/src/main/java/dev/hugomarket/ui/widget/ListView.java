package dev.hugomarket.ui.widget;

import dev.hugomarket.ui.Gfx;
import dev.hugomarket.ui.Theme;

import java.util.List;
import java.util.function.Consumer;

public final class ListView<T> extends Widget {
    public interface RowRenderer<T> {
        void render(Gfx gfx, T row, int x, int y, int width, int height, boolean hovered);
    }

    public interface HoverRenderer<T> {
        void render(Gfx gfx, T row, int mouseX, int mouseY);
    }

    private final int rowHeight;
    private final RowRenderer<T> rowRenderer;
    private final Consumer<T> onClick;
    private HoverRenderer<T> hoverRenderer;
    private List<T> rows = List.of();
    private int scroll;

    public ListView(int rowHeight, RowRenderer<T> rowRenderer, Consumer<T> onClick) {
        this.rowHeight = rowHeight;
        this.rowRenderer = rowRenderer;
        this.onClick = onClick;
    }

    public void setHoverRenderer(HoverRenderer<T> hoverRenderer) {
        this.hoverRenderer = hoverRenderer;
    }

    public void setRows(List<T> rows) {
        this.rows = rows;
        clampScroll();
    }

    public int rowCount() {
        return rows.size();
    }

    @Override
    public void render(Gfx gfx, int mouseX, int mouseY) {
        if (rows.isEmpty()) {
            return;
        }
        int hoveredIndex = hoveredIndex(mouseX, mouseY);
        gfx.scissor(x, y, width, height);
        int first = scroll / rowHeight;
        int offset = scroll % rowHeight;
        int rowY = y - offset;
        for (int i = first; i < rows.size() && rowY < y + height; i++, rowY += rowHeight) {
            boolean hovered = i == hoveredIndex;
            if (hovered) {
                gfx.fill(x, rowY, width, rowHeight, Theme.ROW_HOVER);
            }
            rowRenderer.render(gfx, rows.get(i), x, rowY, width, rowHeight, hovered);
        }
        gfx.unscissor();
        int total = rows.size() * rowHeight;
        if (total > height) {
            int barHeight = Math.max(8, height * height / total);
            int barY = y + (height - barHeight) * scroll / (total - height);
            gfx.fill(x + width - 2, barY, 2, barHeight, Theme.TEXT_DIM);
        }
        if (hoverRenderer != null && hoveredIndex >= 0) {
            hoverRenderer.render(gfx, rows.get(hoveredIndex), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int index = hoveredIndex((int) mouseX, (int) mouseY);
        if (index < 0) {
            return contains(mouseX, mouseY);
        }
        onClick.accept(rows.get(index));
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!contains(mouseX, mouseY)) {
            return false;
        }
        scroll -= (int) (delta * rowHeight);
        clampScroll();
        return true;
    }

    private int hoveredIndex(int mouseX, int mouseY) {
        if (!contains(mouseX, mouseY)) {
            return -1;
        }
        int index = (mouseY - y + scroll) / rowHeight;
        return index >= 0 && index < rows.size() ? index : -1;
    }

    private void clampScroll() {
        int max = Math.max(0, rows.size() * rowHeight - height);
        scroll = Math.max(0, Math.min(scroll, max));
    }
}
