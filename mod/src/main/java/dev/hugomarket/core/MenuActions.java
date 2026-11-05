package dev.hugomarket.core;

import java.util.ArrayList;
import java.util.List;

public final class MenuActions {
    public record Action(String labelKey, Runnable run) {
    }

    private final List<Action> actions = new ArrayList<>();

    public void add(String labelKey, Runnable run) {
        actions.add(new Action(labelKey, run));
    }

    public List<Action> all() {
        return List.copyOf(actions);
    }
}
