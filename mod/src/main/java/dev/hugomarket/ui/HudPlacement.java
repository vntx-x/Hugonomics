package dev.hugomarket.ui;

public final class HudPlacement {
    public static final int MARGIN = 4;

    private HudPlacement() {
    }

    public static int position(double fraction, int screen, int box) {
        int free = Math.max(0, screen - box - MARGIN * 2);
        return MARGIN + (int) Math.round(clamp(fraction) * free);
    }

    public static double fraction(int position, int screen, int box) {
        int free = screen - box - MARGIN * 2;
        return free <= 0 ? 0 : clamp((double) (position - MARGIN) / free);
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }
}
