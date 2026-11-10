package dev.hugomarket.store;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public final class HudBoard {
    public record Line(String text, int color) {
    }

    public static final int STATUS = 0;
    public static final int WATCHLIST = 1;

    private final TreeMap<Integer, List<Line>> sections = new TreeMap<>();
    private List<Line> lines = List.of();

    public void set(int section, List<Line> sectionLines) {
        List<Line> previous = sections.get(section);
        if (sectionLines.isEmpty() ? previous == null : sectionLines.equals(previous)) {
            return;
        }
        if (sectionLines.isEmpty()) {
            sections.remove(section);
        } else {
            sections.put(section, List.copyOf(sectionLines));
        }
        List<Line> merged = new ArrayList<>();
        sections.values().forEach(merged::addAll);
        lines = List.copyOf(merged);
    }

    public List<Line> lines() {
        return lines;
    }
}
