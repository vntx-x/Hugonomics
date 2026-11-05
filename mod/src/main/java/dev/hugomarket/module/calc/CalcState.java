package dev.hugomarket.module.calc;

import java.util.ArrayList;
import java.util.List;

public final class CalcState {
    private static final int MAX_HISTORY = 6;
    private static final int MAX_LENGTH = 80;
    private static final String INPUT_CHARS = "0123456789,.+-*/×÷%()kKmMbB";

    public record HistoryEntry(String expression, double value) {
    }

    private final StringBuilder expression = new StringBuilder();
    private final List<HistoryEntry> history = new ArrayList<>();
    private final List<Runnable> listeners = new ArrayList<>();
    private CalcEngine.Result result = CalcEngine.evaluate("");
    private boolean justEvaluated;
    private boolean open;

    public void onChange(Runnable listener) {
        listeners.add(listener);
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
        changed();
    }

    public String expression() {
        return expression.toString();
    }

    public CalcEngine.Result result() {
        return result;
    }

    public List<HistoryEntry> history() {
        return history;
    }

    public boolean input(char c) {
        if (INPUT_CHARS.indexOf(c) < 0 || expression.length() >= MAX_LENGTH) {
            return false;
        }
        char normalized = switch (c) {
            case '*' -> '×';
            case '/' -> '÷';
            case 'k' -> 'K';
            case 'm' -> 'M';
            case 'b' -> 'B';
            default -> c;
        };
        boolean operator = "+-×÷%)".indexOf(normalized) >= 0;
        if (justEvaluated && !operator) {
            expression.setLength(0);
        }
        justEvaluated = false;
        expression.append(normalized);
        reevaluate();
        return true;
    }

    public void backspace() {
        if (!expression.isEmpty()) {
            expression.setLength(expression.length() - 1);
        }
        justEvaluated = false;
        reevaluate();
    }

    public void clear() {
        expression.setLength(0);
        justEvaluated = false;
        reevaluate();
    }

    public void evaluate() {
        if (!result.ok()) {
            return;
        }
        history.add(0, new HistoryEntry(expression.toString(), result.value()));
        if (history.size() > MAX_HISTORY) {
            history.remove(history.size() - 1);
        }
        expression.setLength(0);
        expression.append(CalcFormat.asInput(result.value()));
        justEvaluated = true;
        reevaluate();
    }

    public void recall(HistoryEntry entry) {
        expression.setLength(0);
        expression.append(CalcFormat.asInput(entry.value()));
        justEvaluated = true;
        reevaluate();
    }

    private void reevaluate() {
        result = CalcEngine.evaluate(expression.toString());
        changed();
    }

    private void changed() {
        for (Runnable listener : listeners) {
            listener.run();
        }
    }
}
