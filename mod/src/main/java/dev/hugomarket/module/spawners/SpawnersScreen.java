package dev.hugomarket.module.spawners;

import dev.hugomarket.model.Money;
import dev.hugomarket.module.ahsearch.Deals;
import dev.hugomarket.store.MarketSession;
import dev.hugomarket.ui.Gfx;
import dev.hugomarket.ui.OverlayPanel;
import dev.hugomarket.ui.Theme;
import dev.hugomarket.ui.widget.Chip;
import dev.hugomarket.ui.widget.ListView;
import dev.hugomarket.ui.widget.TextField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalLong;

//? if >=1.21.9 {
/*import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
*///?}

final class SpawnersScreen extends Screen {
    private static final int WIDTH = 430;
    private static final int MAX_HEIGHT = 260;
    private static final int MARGIN = 16;

    private final Screen parent;
    private final SpawnersPanel panel;

    SpawnersScreen(Screen parent, MarketSession market, boolean online, Map<String, Integer> owned) {
        super(Component.translatable("hugomarket.spawners.title"));
        this.parent = parent;
        this.panel = new SpawnersPanel(market, online, owned);
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(WIDTH, width - MARGIN * 2);
        int panelHeight = Math.min(MAX_HEIGHT, height - MARGIN * 2);
        panel.setBounds((width - panelWidth) / 2, (height - panelHeight) / 2, panelWidth, panelHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        panel.render(new Gfx(graphics), mouseX, mouseY);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean handleKey(int key, int scancode, int modifiers) {
        if (panel.keyPressed(key, scancode, modifiers)) {
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return false;
    }

    //? if >=1.21.9 {
    /*@Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return panel.mouseClicked(event.x(), event.y(), event.button()) || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return handleKey(event.key(), event.scancode(), event.modifiers()) || super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return (Character.isBmpCodePoint(event.codepoint()) && panel.charTyped((char) event.codepoint())) || super.charTyped(event);
    }
    *///?} else {
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return panel.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        return handleKey(key, scancode, modifiers) || super.keyPressed(key, scancode, modifiers);
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        return panel.charTyped(character) || super.charTyped(character, modifiers);
    }
    //?}

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        return panel.mouseScrolled(mouseX, mouseY, vertical) || super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    private static final class SpawnersPanel extends OverlayPanel {
        private static final int ICON_SIZE = 16;
        private static final int LABEL_WIDTH = 52;
        private static final int STEP_WIDTH = 24;
        private static final int MESSAGE_MS = 2_500;

        private final MarketSession market;
        private final boolean online;
        private final Map<String, Integer> owned;
        private final ListView<SpawnerRows.Row> list;
        private final ItemStack icon = new ItemStack(Items.SPAWNER);
        private final Map<String, String> names = new HashMap<>();
        private final String perPiece = I18n.get("hugomarket.spawners.per_piece");
        private final Chip sellChip;
        private final Chip buyChip;
        private final TextField countField;
        private final List<Chip> countSteps;
        private final TextField priceField;
        private final Chip referenceChip;
        private final Chip copyChip;
        private int version = -1;
        private String hint = "";
        private String selected;
        private boolean selling = true;
        private int calcX;
        private int calcWidth;
        private int titleY;
        private int countY;
        private int priceY;
        private int resultY;
        private String message = "";
        private long messageUntil;

        SpawnersPanel(MarketSession market, boolean online, Map<String, Integer> owned) {
            this.market = market;
            this.online = online;
            this.owned = owned;
            setTitle(I18n.get("hugomarket.spawners.title"));
            list = add(new ListView<>(Theme.ROW_HEIGHT, this::renderRow, (row, button) -> select(row.type())));
            setHeaderRight(() -> I18n.get("hugomarket.spawners.count", list.rowCount()));
            sellChip = add(new Chip(I18n.get("hugomarket.spawners.sell"), () -> selling, () -> selling = true));
            buyChip = add(new Chip(I18n.get("hugomarket.spawners.buy"), () -> !selling, () -> selling = false));
            countField = add(new TextField("1", "1", 6, unused -> { }));
            countSteps = List.of(
                    add(new Chip("-1", () -> false, () -> stepCount(-1))),
                    add(new Chip("+1", () -> false, () -> stepCount(1))),
                    add(new Chip("+10", () -> false, () -> stepCount(10))),
                    add(new Chip(I18n.get("hugomarket.spawners.all"), () -> false, this::useOwned)));
            priceField = add(new TextField(I18n.get("hugomarket.spawners.price_hint"), "", 20, unused -> { }));
            referenceChip = add(new Chip(I18n.get("hugomarket.spawners.reference"), () -> false, this::useReference));
            copyChip = add(new Chip(I18n.get("hugomarket.spawners.copy"), () -> false, this::copyOffer));
            rebuild();
        }

        private void rebuild() {
            version = market.version();
            List<SpawnerRows.Row> rows = SpawnerRows.build(market.spawnerPrices());
            list.setRows(rows);
            if (!online) {
                hint = I18n.get("hugomarket.spawners.offline");
            } else if (rows.isEmpty()) {
                hint = I18n.get("hugomarket.spawners.empty");
            } else {
                hint = I18n.get("hugomarket.spawners.hint", market.spawnerPrices().updatedAt());
            }
            if (selected == null && !rows.isEmpty()) {
                String first = rows.get(0).type();
                for (SpawnerRows.Row row : rows) {
                    if (owned.getOrDefault(row.type(), 0) > 0) {
                        first = row.type();
                        break;
                    }
                }
                select(first);
            }
        }

        private void select(String type) {
            selected = type;
            int have = owned.getOrDefault(type, 0);
            countField.setValue(String.valueOf(Math.max(1, have)));
            useReference();
        }

        private long referenceCents() {
            Long cents = selected == null ? null : market.spawnerPrices().unitCentsByType().get(selected);
            return cents == null ? 0 : cents;
        }

        private void useReference() {
            long reference = referenceCents();
            priceField.setValue(reference > 0 ? Money.compact(reference) : "");
        }

        private void stepCount(int delta) {
            long current = SpawnerDeal.parseCount(countField.value()).orElse(0);
            countField.setValue(String.valueOf(Math.max(1, current + delta)));
        }

        private void useOwned() {
            int have = selected == null ? 0 : owned.getOrDefault(selected, 0);
            if (have > 0) {
                countField.setValue(String.valueOf(have));
            }
        }

        private SpawnerDeal deal() {
            OptionalLong count = SpawnerDeal.parseCount(countField.value());
            OptionalLong price = SpawnerDeal.parsePriceCents(priceField.value());
            if (selected == null || count.isEmpty() || price.isEmpty()) {
                return null;
            }
            return new SpawnerDeal((int) count.getAsLong(), price.getAsLong(), referenceCents());
        }

        private void copyOffer() {
            SpawnerDeal deal = deal();
            if (deal == null) {
                flash(I18n.get("hugomarket.spawners.invalid"));
                return;
            }
            String text = I18n.get(selling ? "hugomarket.spawners.offer.sell" : "hugomarket.spawners.offer.buy",
                    deal.count(), name(selected), Money.format(deal.unitCents()), Money.format(deal.totalCents()));
            Minecraft.getInstance().keyboardHandler.setClipboard(text);
            flash(I18n.get("hugomarket.spawners.copied"));
        }

        private void flash(String text) {
            message = text;
            messageUntil = System.currentTimeMillis() + MESSAGE_MS;
        }

        private String name(String type) {
            return names.computeIfAbsent(type, unused -> {
                String entityKey = "entity.minecraft." + type;
                String entity = I18n.exists(entityKey) ? I18n.get(entityKey) : type.replace('_', ' ');
                return I18n.get("hugomarket.spawners.name", entity);
            });
        }

        @Override
        protected void layout() {
            int innerX = x + Theme.PADDING;
            int innerWidth = width - Theme.PADDING * 2;
            int top = contentTop();
            int bottom = y + height - Theme.PADDING - Theme.LINE - Theme.GAP;
            int listWidth = innerWidth * 9 / 20;
            list.setBounds(innerX, top, listWidth, bottom - top);
            calcX = innerX + listWidth + Theme.PADDING * 2;
            calcWidth = innerX + innerWidth - calcX;
            titleY = top + 2;
            int modeY = titleY + ICON_SIZE + Theme.GAP;
            int half = (calcWidth - Theme.GAP) / 2;
            sellChip.setBounds(calcX, modeY, half, Theme.FIELD_HEIGHT);
            buyChip.setBounds(calcX + half + Theme.GAP, modeY, calcWidth - half - Theme.GAP, Theme.FIELD_HEIGHT);
            countY = modeY + Theme.FIELD_HEIGHT + Theme.GAP * 2;
            int fieldX = calcX + LABEL_WIDTH;
            int stepsWidth = countSteps.size() * (STEP_WIDTH + Theme.GAP);
            countField.setBounds(fieldX, countY, calcWidth - LABEL_WIDTH - stepsWidth, Theme.FIELD_HEIGHT);
            int stepX = fieldX + countField.width() + Theme.GAP;
            for (Chip step : countSteps) {
                step.setBounds(stepX, countY + 1, STEP_WIDTH, Theme.FIELD_HEIGHT - 2);
                stepX += STEP_WIDTH + Theme.GAP;
            }
            priceY = countY + Theme.FIELD_HEIGHT + Theme.GAP;
            int referenceWidth = 62;
            priceField.setBounds(fieldX, priceY, calcWidth - LABEL_WIDTH - referenceWidth - Theme.GAP, Theme.FIELD_HEIGHT);
            referenceChip.setBounds(fieldX + priceField.width() + Theme.GAP, priceY + 1, referenceWidth, Theme.FIELD_HEIGHT - 2);
            resultY = priceY + Theme.FIELD_HEIGHT + Theme.GAP * 2;
            copyChip.setBounds(calcX, resultY + Theme.LINE * 3 + Theme.GAP * 2, calcWidth, Theme.FIELD_HEIGHT + 2);
        }

        @Override
        public void render(Gfx gfx, int mouseX, int mouseY) {
            if (market.version() != version) {
                rebuild();
            }
            super.render(gfx, mouseX, mouseY);
            gfx.fill(calcX - Theme.PADDING - 1, contentTop(), 1, list.height(), Theme.PANEL_BORDER);
            if (selected == null) {
                gfx.textNoShadow(gfx.clip(I18n.get("hugomarket.spawners.pick"), calcWidth), calcX, titleY + 4, Theme.TEXT_DIM);
            } else {
                renderCalc(gfx);
            }
            gfx.textNoShadow(gfx.clip(hint, width - Theme.PADDING * 2), x + Theme.PADDING, y + height - Theme.PADDING - Theme.LINE + 2, Theme.TEXT_DIM);
        }

        private void renderCalc(Gfx gfx) {
            gfx.item(icon, calcX, titleY);
            int have = owned.getOrDefault(selected, 0);
            String title = name(selected);
            gfx.textNoShadow(gfx.clip(title, calcWidth - ICON_SIZE - 4), calcX + ICON_SIZE + 4, titleY, Theme.TEXT);
            String sub = have > 0 ? I18n.get("hugomarket.spawners.owned", have) : I18n.get("hugomarket.spawners.reference_line", Money.format(referenceCents()));
            gfx.textNoShadow(gfx.clip(sub, calcWidth - ICON_SIZE - 4), calcX + ICON_SIZE + 4, titleY + 9, Theme.TEXT_DIM);
            gfx.textNoShadow(I18n.get("hugomarket.spawners.amount"), calcX, countY + 4, Theme.TEXT_DIM);
            gfx.textNoShadow(I18n.get("hugomarket.spawners.unit"), calcX, priceY + 4, Theme.TEXT_DIM);
            SpawnerDeal deal = deal();
            int lineY = resultY;
            if (deal == null) {
                gfx.textNoShadow(gfx.clip(I18n.get("hugomarket.spawners.invalid"), calcWidth), calcX, lineY, Theme.DANGER);
            } else {
                gfx.textNoShadow(I18n.get("hugomarket.spawners.total"), calcX, lineY, Theme.TEXT_DIM);
                String total = Money.format(deal.totalCents());
                gfx.textNoShadow(total, calcX + calcWidth - gfx.textWidth(total), lineY, Theme.GOLD);
                lineY += Theme.LINE;
                if (deal.referenceUnitCents() > 0) {
                    gfx.textNoShadow(I18n.get("hugomarket.spawners.reference_total"), calcX, lineY, Theme.TEXT_DIM);
                    String reference = Money.format(deal.referenceTotalCents());
                    gfx.textNoShadow(reference, calcX + calcWidth - gfx.textWidth(reference), lineY, Theme.PRICE);
                    lineY += Theme.LINE;
                    OptionalDouble ratio = deal.ratio();
                    if (ratio.isPresent()) {
                        boolean good = selling ? ratio.getAsDouble() >= 1 : ratio.getAsDouble() <= 1;
                        long diff = deal.totalCents() - deal.referenceTotalCents();
                        String delta = Deals.deltaText(ratio.getAsDouble()) + "  (" + (diff >= 0 ? "+" : "−") + Money.compact(Math.abs(diff)) + ")";
                        gfx.textNoShadow(I18n.get("hugomarket.spawners.delta"), calcX, lineY, Theme.TEXT_DIM);
                        gfx.textNoShadow(delta, calcX + calcWidth - gfx.textWidth(delta), lineY, good ? Theme.BARGAIN_FRAME : Theme.DANGER);
                    }
                }
            }
            if (System.currentTimeMillis() < messageUntil) {
                gfx.textNoShadow(gfx.clip(message, calcWidth), calcX, copyChip.y() + copyChip.height() + Theme.GAP, Theme.ACCENT);
            }
        }

        private void renderRow(Gfx gfx, SpawnerRows.Row row, int rowX, int rowY, int rowWidth, int rowHeight, boolean hovered) {
            if (row.type().equals(selected)) {
                gfx.fill(rowX, rowY, rowWidth, rowHeight, Theme.ROW_HOVER);
                gfx.fill(rowX, rowY + 1, 2, rowHeight - 2, Theme.ACCENT);
            }
            gfx.item(icon, rowX + 3, rowY + (rowHeight - ICON_SIZE) / 2);
            String price = Money.compact(row.unitCents());
            int have = owned.getOrDefault(row.type(), 0);
            String detail = have > 0 ? I18n.get("hugomarket.spawners.owned_short", have) : perPiece;
            int priceWidth = Math.max(gfx.textWidth(price), gfx.textWidth(detail));
            int textX = rowX + ICON_SIZE + 6;
            gfx.textNoShadow(gfx.clip(name(row.type()), rowWidth - ICON_SIZE - 12 - priceWidth), textX, rowY + (rowHeight - 8) / 2, Theme.TEXT);
            gfx.textNoShadow(price, rowX + rowWidth - gfx.textWidth(price) - 3, rowY + 2, Theme.PRICE);
            gfx.textNoShadow(detail, rowX + rowWidth - gfx.textWidth(detail) - 3, rowY + 11, have > 0 ? Theme.GOLD : Theme.TEXT_DIM);
        }
    }
}
