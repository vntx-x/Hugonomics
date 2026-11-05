package dev.hugomarket.module.containervalue;

import dev.hugomarket.model.Money;
import dev.hugomarket.store.ContainerValuation;
import dev.hugomarket.ui.OverlayPanel;
import dev.hugomarket.ui.Theme;
import dev.hugomarket.ui.widget.Label;
import net.minecraft.client.resources.language.I18n;

import java.util.List;

final class ContainerValuePanel extends OverlayPanel {
    static final int HEIGHT = Theme.PADDING * 2 + headerHeight() + Theme.LINE * 4;

    private final List<Label> labels;
    private String hugo = "";
    private String ahLowest = "";
    private String ahMedian = "";
    private String coverage = "";

    ContainerValuePanel() {
        setTitle(I18n.get("hugomarket.value.title"));
        labels = List.of(
                add(new Label(() -> hugo, Theme.TEXT)),
                add(new Label(() -> ahLowest, Theme.PRICE)),
                add(new Label(() -> ahMedian)),
                add(new Label(() -> coverage)));
        setVisible(false);
    }

    void show(ContainerValuation value) {
        hugo = value.hugoWorthCents() == value.hugoBaseCents()
                ? I18n.get("hugomarket.value.hugo", Money.format(value.hugoWorthCents()))
                : I18n.get("hugomarket.value.hugo_bonus", Money.format(value.hugoWorthCents()), Money.format(value.hugoBaseCents()));
        if (value.hasAhValue()) {
            ahLowest = I18n.get("hugomarket.value.ah_lowest", Money.compact(value.ahLowestCents()));
            ahMedian = I18n.get("hugomarket.value.ah_median", Money.compact(value.ahMedianCents()));
        } else {
            ahLowest = I18n.get("hugomarket.value.ah_none");
            ahMedian = "";
        }
        coverage = I18n.get("hugomarket.value.coverage", value.items(), value.unvaluedByAh());
        setVisible(true);
    }

    @Override
    protected void layout() {
        int cursorY = contentTop();
        for (Label label : labels) {
            label.setBounds(x + Theme.PADDING, cursorY, width - Theme.PADDING * 2, Theme.LINE);
            cursorY += Theme.LINE;
        }
    }
}
