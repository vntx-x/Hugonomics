package dev.hugomarket.module.minutetest;

import dev.hugomarket.model.Money;
import dev.hugomarket.store.MinuteTest;
import dev.hugomarket.store.MinuteTests;
import dev.hugomarket.ui.Gfx;
import dev.hugomarket.ui.OverlayPanel;
import dev.hugomarket.ui.Theme;
import dev.hugomarket.ui.widget.Chip;
import dev.hugomarket.ui.widget.Label;
import dev.hugomarket.ui.widget.ListView;
import dev.hugomarket.ui.widget.TextField;
import net.minecraft.client.resources.language.I18n;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

final class MinuteTestPanel extends OverlayPanel {
    static final int HEIGHT = Theme.PADDING * 2 + headerHeight() + Theme.CHIP_HEIGHT + Theme.GAP + 11 * 2 + Theme.GAP + 10 * 4 + Theme.GAP + Theme.FIELD_HEIGHT + Theme.GAP + 10 * 8;

    private final MinuteTest test;
    private final MinuteTests saved;
    private final Chip startStop;
    private final Label status;
    private final Label resultLine;
    private final ListView<MinuteTest.Result> history;
    private final TextField rename;
    private final List<Label> table = new ArrayList<>();
    private MinuteTest.Result shown;
    private int shownIndex = -1;

    MinuteTestPanel(MinuteTest test, MinuteTests saved) {
        this.test = test;
        this.saved = saved;
        setTitle(I18n.get("hugomarket.minute.title"));
        startStop = add(new Chip(() -> test.isRunning() ? I18n.get("hugomarket.minute.stop") : I18n.get("hugomarket.minute.start"), test::isRunning, this::toggle));
        status = add(new Label(this::statusText, Theme.TEXT));
        resultLine = add(new Label(this::resultText, Theme.PRICE));
        for (double m : MinuteTest.MULTIPLIERS) {
            table.add(add(new Label(() -> tableLine(m))));
        }
        rename = add(new TextField(I18n.get("hugomarket.minute.rename"), "", text -> {
        }));
        history = add(new ListView<>(10, this::renderHistory, this::historyClicked));
        refreshHistory();
    }

    private void toggle() {
        if (test.isRunning()) {
            test.cancel();
        } else {
            test.start(System.currentTimeMillis());
        }
    }

    void finished(MinuteTest.Result result) {
        shown = result;
        shownIndex = 0;
        refreshHistory();
    }

    private void refreshHistory() {
        history.setRows(saved.all());
    }

    private void historyClicked(MinuteTest.Result result, int button) {
        int index = saved.all().indexOf(result);
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            saved.remove(index);
            if (shown == result) {
                shown = null;
                shownIndex = -1;
            }
            refreshHistory();
            return;
        }
        shown = result;
        shownIndex = index;
        rename.setValue(result.name());
    }

    private String statusText() {
        if (test.isRunning()) {
            return I18n.get("hugomarket.minute.running", test.activeMs() / 1000, MinuteTest.TARGET_ACTIVE_MS / 1000, Money.compact(test.baseCentsSoFar()));
        }
        return I18n.get("hugomarket.minute.idle");
    }

    private String resultText() {
        if (shown == null) {
            return "";
        }
        return I18n.get("hugomarket.minute.result", shown.name(), Money.compact(shown.baseCents()), Money.compact(shown.totalCents()),
                String.format(java.util.Locale.ROOT, "%.2f", shown.multiplier()), shown.sales());
    }

    private String tableLine(double multiplier) {
        if (shown == null) {
            return "";
        }
        long perMinute = shown.projected(multiplier);
        return I18n.get("hugomarket.minute.row", String.format(java.util.Locale.ROOT, "%.2fx", multiplier), Money.compact(perMinute), Money.compact(perMinute * 60));
    }

    private void renderHistory(Gfx gfx, MinuteTest.Result result, int rowX, int rowY, int rowWidth, int rowHeight, boolean hovered) {
        String right = Money.compact(result.baseCents()) + "/min";
        int rightWidth = gfx.textWidth(right);
        gfx.textNoShadow(gfx.clip(result.name(), rowWidth - rightWidth - 6), rowX + 1, rowY + 1, result == shown ? Theme.ACCENT : Theme.TEXT);
        gfx.textNoShadow(right, rowX + rowWidth - rightWidth - 3, rowY + 1, Theme.TEXT_DIM);
    }

    @Override
    protected void layout() {
        int innerX = x + Theme.PADDING;
        int innerWidth = width - Theme.PADDING * 2;
        int cursorY = contentTop();
        startStop.setBounds(innerX, cursorY, innerWidth, Theme.CHIP_HEIGHT);
        cursorY += Theme.CHIP_HEIGHT + Theme.GAP;
        status.setBounds(innerX, cursorY, innerWidth, 11);
        cursorY += 11;
        resultLine.setBounds(innerX, cursorY, innerWidth, 11);
        cursorY += 11 + Theme.GAP;
        int half = (innerWidth - Theme.GAP) / 2;
        for (int i = 0; i < table.size(); i++) {
            table.get(i).setBounds(innerX + (i % 2) * (half + Theme.GAP), cursorY + (i / 2) * 10, half, 10);
        }
        cursorY += ((table.size() + 1) / 2) * 10 + Theme.GAP;
        rename.setBounds(innerX, cursorY, innerWidth, Theme.FIELD_HEIGHT);
        cursorY += Theme.FIELD_HEIGHT + Theme.GAP;
        history.setBounds(innerX, cursorY, innerWidth, Math.max(0, y + height - Theme.PADDING - cursorY));
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        if (rename.isFocused() && (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER)) {
            if (shownIndex >= 0) {
                saved.rename(shownIndex, rename.value());
                shown = saved.all().get(shownIndex);
                refreshHistory();
            }
            rename.setFocused(false);
            return true;
        }
        return super.keyPressed(key, scancode, modifiers);
    }
}
