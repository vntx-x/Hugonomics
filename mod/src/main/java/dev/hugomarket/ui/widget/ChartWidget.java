package dev.hugomarket.ui.widget;

import dev.hugomarket.model.Money;
import dev.hugomarket.ui.Gfx;
import dev.hugomarket.ui.Theme;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class ChartWidget extends Widget {
    public record Point(long ts, long low, long high) {
    }

    private static final int LABEL_WIDTH = 46;
    private static final int LABEL_HEIGHT = 10;
    private static final int LOW_COLOR = 0xFF7FE07F;
    private static final int HIGH_COLOR = 0xFFC77DFF;
    private static final int GRID_COLOR = 0x30FFFFFF;
    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("dd.MM HH:mm");

    private int[] lowY = new int[0];
    private int[] highY = new int[0];
    private long minPrice;
    private long maxPrice;
    private long fromTs;
    private long toTs;
    private String emptyText = "";
    private String minLabel = "";
    private String maxLabel = "";
    private String fromLabel = "";
    private String toLabel = "";

    public void setEmptyText(String text) {
        this.emptyText = text;
    }

    public void setPoints(List<Point> points) {
        int plotWidth = Math.max(1, width - LABEL_WIDTH - 2);
        int plotHeight = Math.max(1, height - LABEL_HEIGHT - 2);
        if (points.isEmpty() || plotWidth < 4) {
            lowY = new int[0];
            highY = new int[0];
            return;
        }
        fromTs = points.get(0).ts();
        toTs = Math.max(points.get(points.size() - 1).ts(), fromTs + 1);
        minPrice = Long.MAX_VALUE;
        maxPrice = Long.MIN_VALUE;
        for (Point point : points) {
            minPrice = Math.min(minPrice, point.low());
            maxPrice = Math.max(maxPrice, point.high());
        }
        if (maxPrice == minPrice) {
            maxPrice = minPrice + 1;
        }
        long[] lowSum = new long[plotWidth];
        long[] highSum = new long[plotWidth];
        int[] counts = new int[plotWidth];
        for (Point point : points) {
            int column = (int) ((point.ts() - fromTs) * (plotWidth - 1) / (toTs - fromTs));
            lowSum[column] += point.low();
            highSum[column] += point.high();
            counts[column]++;
        }
        lowY = new int[plotWidth];
        highY = new int[plotWidth];
        int lastLow = -1;
        int lastHigh = -1;
        for (int i = 0; i < plotWidth; i++) {
            if (counts[i] > 0) {
                lastLow = toY(lowSum[i] / counts[i], plotHeight);
                lastHigh = toY(highSum[i] / counts[i], plotHeight);
            }
            lowY[i] = lastLow;
            highY[i] = lastHigh;
        }
        minLabel = Money.compact(minPrice);
        maxLabel = Money.compact(maxPrice);
        fromLabel = SHORT.format(Instant.ofEpochMilli(fromTs).atZone(ZoneId.systemDefault()));
        toLabel = SHORT.format(Instant.ofEpochMilli(toTs).atZone(ZoneId.systemDefault()));
    }

    private int toY(long price, int plotHeight) {
        return (int) ((maxPrice - price) * (plotHeight - 1) / (maxPrice - minPrice));
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
    }

    @Override
    public void render(Gfx gfx, int mouseX, int mouseY) {
        int plotX = x + LABEL_WIDTH;
        int plotY = y + 1;
        int plotWidth = width - LABEL_WIDTH - 2;
        int plotHeight = height - LABEL_HEIGHT - 2;
        gfx.fill(plotX, plotY, plotWidth, plotHeight, Theme.FIELD_BG);
        gfx.border(plotX, plotY, plotWidth, plotHeight, Theme.FIELD_BORDER);
        if (lowY.length == 0) {
            gfx.textNoShadow(emptyText, plotX + 4, plotY + plotHeight / 2 - 4, Theme.TEXT_DIM);
            return;
        }
        for (int i = 1; i < 4; i++) {
            gfx.fill(plotX, plotY + plotHeight * i / 4, plotWidth, 1, GRID_COLOR);
        }
        drawSeries(gfx, highY, plotX, plotY, HIGH_COLOR);
        drawSeries(gfx, lowY, plotX, plotY, LOW_COLOR);
        gfx.textNoShadow(maxLabel, x, plotY, Theme.TEXT_DIM);
        gfx.textNoShadow(minLabel, x, plotY + plotHeight - 9, Theme.TEXT_DIM);
        gfx.textNoShadow(fromLabel, plotX, plotY + plotHeight + 2, Theme.TEXT_DIM);
        gfx.textNoShadow(toLabel, plotX + plotWidth - gfx.textWidth(toLabel), plotY + plotHeight + 2, Theme.TEXT_DIM);
    }

    private static void drawSeries(Gfx gfx, int[] ys, int plotX, int plotY, int color) {
        int previous = -1;
        for (int i = 0; i < ys.length; i++) {
            int current = ys[i];
            if (current < 0) {
                continue;
            }
            if (previous < 0) {
                gfx.fill(plotX + i, plotY + current, 1, 1, color);
            } else {
                int top = Math.min(previous, current);
                int bottom = Math.max(previous, current);
                gfx.fill(plotX + i, plotY + top, 1, bottom - top + 1, color);
            }
            previous = current;
        }
    }
}
