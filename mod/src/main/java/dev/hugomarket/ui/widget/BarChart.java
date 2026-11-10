package dev.hugomarket.ui.widget;

import dev.hugomarket.model.Money;
import dev.hugomarket.ui.Gfx;
import dev.hugomarket.ui.Theme;

public final class BarChart extends Widget {
    private static final int LABEL_HEIGHT = 10;
    private static final int BAR_GAP = 2;
    private static final int GRID_COLOR = 0x30FFFFFF;

    private long[] values = new long[0];
    private String[] labels = new String[0];
    private String[] tooltips = new String[0];
    private int[] barTop = new int[0];
    private int[] barHeight = new int[0];
    private int barWidth;
    private int zeroY;
    private int laidOutWidth = -1;
    private int laidOutHeight = -1;

    public void setData(long[] values, String[] labels) {
        this.values = values;
        this.labels = labels;
        tooltips = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            tooltips[i] = labels[i] + "  " + (values[i] >= 0 ? "+" : "−") + Money.compact(Math.abs(values[i]));
        }
        laidOutWidth = -1;
    }

    private void layoutBars() {
        laidOutWidth = width;
        laidOutHeight = height;
        int plotHeight = Math.max(1, height - LABEL_HEIGHT - 2);
        int count = Math.max(1, values.length);
        barWidth = Math.max(1, (width - 2 - BAR_GAP * (count - 1)) / count);
        long max = 0;
        long min = 0;
        for (long value : values) {
            max = Math.max(max, value);
            min = Math.min(min, value);
        }
        long span = Math.max(1, max - min);
        zeroY = (int) (max * (plotHeight - 1) / span);
        barTop = new int[values.length];
        barHeight = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            int valueY = (int) ((max - values[i]) * (plotHeight - 1) / span);
            barTop[i] = Math.min(zeroY, valueY);
            barHeight[i] = values[i] == 0 ? 0 : Math.max(1, Math.abs(valueY - zeroY));
        }
    }

    @Override
    public void render(Gfx gfx, int mouseX, int mouseY) {
        if (laidOutWidth != width || laidOutHeight != height) {
            layoutBars();
        }
        int plotY = y + 1;
        int plotHeight = height - LABEL_HEIGHT - 2;
        gfx.fill(x, plotY, width, plotHeight, Theme.FIELD_BG);
        gfx.border(x, plotY, width, plotHeight, Theme.FIELD_BORDER);
        gfx.fill(x, plotY + zeroY, width, 1, GRID_COLOR);
        int hovered = -1;
        for (int i = 0; i < values.length; i++) {
            int barX = x + 1 + i * (barWidth + BAR_GAP);
            if (mouseX >= barX && mouseX < barX + barWidth && mouseY >= y && mouseY < y + height) {
                hovered = i;
            }
            if (barHeight[i] > 0) {
                int color = values[i] >= 0 ? Theme.PRICE : Theme.DANGER;
                gfx.fill(barX, plotY + barTop[i], barWidth, barHeight[i], hovered == i ? Theme.ACCENT : color);
            }
        }
        if (labels.length > 0) {
            gfx.textNoShadow(labels[0], x, plotY + plotHeight + 2, Theme.TEXT_DIM);
            String last = labels[labels.length - 1];
            gfx.textNoShadow(last, x + width - gfx.textWidth(last), plotY + plotHeight + 2, Theme.TEXT_DIM);
        }
        if (hovered >= 0) {
            String tip = tooltips[hovered];
            int tipWidth = gfx.textWidth(tip) + 6;
            int tipX = Math.min(Math.max(x, mouseX - tipWidth / 2), x + width - tipWidth);
            gfx.fill(tipX, plotY + 2, tipWidth, 11, Theme.PANEL_BG);
            gfx.border(tipX, plotY + 2, tipWidth, 11, Theme.PANEL_BORDER);
            gfx.textNoShadow(tip, tipX + 3, plotY + 4, values[hovered] >= 0 ? Theme.PRICE : Theme.DANGER);
        }
    }
}
