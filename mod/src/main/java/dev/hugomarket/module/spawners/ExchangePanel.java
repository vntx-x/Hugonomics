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
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalLong;

final class ExchangePanel extends OverlayPanel {
    private enum Tab { MARKET, DEALS }

    private enum Pane { CREATE, OFFER, OWN_OFFER, DEAL, NONE }

    private static final int ICON_SIZE = 16;
    private static final int TYPE_WIDTH = 132;
    private static final int PANE_WIDTH = 196;
    private static final int LABEL_WIDTH = 50;
    private static final int STEP_WIDTH = 22;
    private static final int TAB_WIDTH = 90;
    private static final int MESSAGE_MS = 4_000;
    private static final Comparator<ExchangeOffer> OFFER_ORDER = Comparator
            .comparing((ExchangeOffer o) -> !o.selling())
            .thenComparingLong(o -> o.selling() ? o.unitCents() : -o.unitCents());

    private final MarketSession market;
    private final SpawnerExchange exchange;
    private final Map<String, Integer> owned;
    private final ItemStack icon = new ItemStack(Items.SPAWNER);

    private final Chip marketTab;
    private final Chip dealsTab;
    private final ListView<SpawnerRows.Row> typeList;
    private final ListView<ExchangeOffer> offerList;
    private final ListView<ExchangeDeal> dealList;
    private final Chip sellChip;
    private final Chip buyChip;
    private final TextField countField;
    private final List<Chip> countSteps;
    private final TextField priceField;
    private final Chip referenceChip;
    private final Chip createChip;
    private final Chip copyChip;
    private final Chip takeChip;
    private final Chip acceptChip;
    private final Chip counterChip;
    private final Chip declineChip;
    private final Chip deleteChip;
    private final Chip msgChip;
    private final Chip backChip;
    private final List<Chip> starChips = new ArrayList<>();

    private Tab tab = Tab.MARKET;
    private Pane pane = Pane.CREATE;
    private String selectedType;
    private ExchangeOffer selectedOffer;
    private ExchangeDeal selectedDeal;
    private boolean selling = true;
    private boolean busy;
    private int marketVersion = -1;
    private int exchangeVersion = -1;
    private int paneX;
    private int columnsTop;
    private int columnsBottom;
    private int infoY;
    private int countY;
    private int priceY;
    private int resultY;
    private String message = "";
    private int messageColor = Theme.ACCENT;
    private long messageUntil;

    ExchangePanel(MarketSession market, SpawnerExchange exchange, Map<String, Integer> owned) {
        this.market = market;
        this.exchange = exchange;
        this.owned = owned;
        setTitle(I18n.get("hugomarket.spawners.title"));
        setHeaderRight(this::loginText);
        marketTab = new Chip(I18n.get("hugomarket.exchange.tab.market"), () -> tab == Tab.MARKET, () -> switchTab(Tab.MARKET));
        dealsTab = new Chip(this::dealsTabLabel, () -> tab == Tab.DEALS, () -> switchTab(Tab.DEALS));
        typeList = new ListView<>(Theme.ROW_HEIGHT, this::renderType, (row, button) -> selectType(row.type()));
        offerList = new ListView<>(Theme.ROW_HEIGHT, this::renderOffer, (row, button) -> selectOffer(row));
        dealList = new ListView<>(Theme.ROW_HEIGHT, this::renderDeal, (row, button) -> selectDeal(row));
        sellChip = new Chip(I18n.get("hugomarket.spawners.sell"), () -> selling, () -> selling = true);
        buyChip = new Chip(I18n.get("hugomarket.spawners.buy"), () -> !selling, () -> selling = false);
        countField = new TextField("1", "1", 6, unused -> { });
        countSteps = List.of(
                new Chip("-1", () -> false, () -> stepCount(-1)),
                new Chip("+1", () -> false, () -> stepCount(1)),
                new Chip("+10", () -> false, () -> stepCount(10)),
                new Chip(I18n.get("hugomarket.spawners.all"), () -> false, this::useOwned));
        priceField = new TextField(I18n.get("hugomarket.spawners.price_hint"), "", 20, unused -> { });
        referenceChip = new Chip(this::referenceLabel, () -> false, this::resetPrice);
        createChip = new Chip(I18n.get("hugomarket.exchange.create"), () -> false, this::createOffer);
        copyChip = new Chip(I18n.get("hugomarket.spawners.copy"), () -> false, this::copyOffer);
        takeChip = new Chip(this::takeLabel, () -> false, this::sendRequest);
        acceptChip = new Chip(I18n.get("hugomarket.exchange.accept"), () -> false, this::acceptDeal);
        counterChip = new Chip(I18n.get("hugomarket.exchange.counter"), () -> false, this::counterDeal);
        declineChip = new Chip(this::declineLabel, () -> false, this::declineDeal);
        deleteChip = new Chip(I18n.get("hugomarket.exchange.delete"), () -> false, this::deleteOffer);
        msgChip = new Chip(this::msgLabel, () -> false, this::openMsg);
        backChip = new Chip(I18n.get("hugomarket.exchange.back"), () -> false, this::backToCreate);
        for (int stars = 1; stars <= 5; stars++) {
            int value = stars;
            starChips.add(new Chip(stars + "★", () -> false, () -> rate(value)));
        }
        rebuild();
        arrange();
    }

    void tick(int ticks) {
        if (exchange != null && ticks % 200 == 0) {
            exchange.refreshOffers();
            exchange.refreshDeals(true);
        }
    }

    private String loginText() {
        if (exchange == null) {
            return I18n.get("hugomarket.exchange.offline");
        }
        return exchange.loggedIn() ? I18n.get("hugomarket.exchange.logged_in", exchange.name()) : I18n.get("hugomarket.exchange.logged_out");
    }

    private String dealsTabLabel() {
        int waiting = 0;
        if (exchange != null) {
            for (ExchangeDeal deal : exchange.deals()) {
                if (deal.myTurn()) {
                    waiting++;
                }
            }
        }
        return waiting > 0 ? I18n.get("hugomarket.exchange.tab.deals_waiting", waiting) : I18n.get("hugomarket.exchange.tab.deals");
    }

    private void switchTab(Tab next) {
        tab = next;
        if (next == Tab.DEALS && exchange != null && !exchange.loggedIn()) {
            busy = true;
            exchange.login(this::finished);
        }
        pane = next == Tab.MARKET ? Pane.CREATE : selectedDeal == null ? Pane.NONE : Pane.DEAL;
        arrange();
    }

    private void rebuild() {
        marketVersion = market.version();
        exchangeVersion = exchange == null ? -1 : exchange.version();
        List<SpawnerRows.Row> rows = SpawnerRows.build(market.spawnerPrices());
        typeList.setRows(rows);
        if (selectedType == null && !rows.isEmpty()) {
            String first = rows.get(0).type();
            for (SpawnerRows.Row row : rows) {
                if (owned.getOrDefault(row.type(), 0) > 0) {
                    first = row.type();
                    break;
                }
            }
            selectType(first);
        }
        offerList.setRows(offersFor(selectedType));
        dealList.setRows(exchange == null ? List.of() : exchange.deals());
        if (selectedOffer != null) {
            selectedOffer = find(offersFor(selectedType), selectedOffer.id());
            if (selectedOffer == null && (pane == Pane.OFFER || pane == Pane.OWN_OFFER)) {
                backToCreate();
            }
        }
        if (selectedDeal != null && exchange != null) {
            ExchangeDeal updated = null;
            for (ExchangeDeal deal : exchange.deals()) {
                if (deal.id().equals(selectedDeal.id())) {
                    updated = deal;
                }
            }
            boolean changed = updated == null || !updated.signature().equals(selectedDeal.signature()) || updated.rated() != selectedDeal.rated();
            selectedDeal = updated;
            if (changed && tab == Tab.DEALS) {
                pane = updated == null ? Pane.NONE : Pane.DEAL;
                arrange();
            }
        }
    }

    private List<ExchangeOffer> offersFor(String type) {
        if (exchange == null || type == null) {
            return List.of();
        }
        List<ExchangeOffer> out = new ArrayList<>();
        for (ExchangeOffer offer : exchange.offers()) {
            if (offer.type().equals(type)) {
                out.add(offer);
            }
        }
        out.sort(OFFER_ORDER);
        return out;
    }

    private static ExchangeOffer find(List<ExchangeOffer> offers, String id) {
        for (ExchangeOffer offer : offers) {
            if (offer.id().equals(id)) {
                return offer;
            }
        }
        return null;
    }

    private void selectType(String type) {
        selectedType = type;
        offerList.setRows(offersFor(type));
        backToCreate();
    }

    private void selectOffer(ExchangeOffer offer) {
        selectedOffer = offer;
        boolean own = exchange != null && exchange.loggedIn() && offer.player().equalsIgnoreCase(exchange.name());
        pane = own ? Pane.OWN_OFFER : Pane.OFFER;
        int have = owned.getOrDefault(offer.type(), 0);
        int count = offer.selling() || have <= 0 ? offer.count() : Math.min(have, offer.count());
        countField.setValue(String.valueOf(count));
        priceField.setValue(Money.compact(offer.unitCents()));
        arrange();
    }

    private void selectDeal(ExchangeDeal deal) {
        selectedDeal = deal;
        pane = Pane.DEAL;
        countField.setValue(String.valueOf(deal.count()));
        priceField.setValue(Money.compact(deal.unitCents()));
        arrange();
    }

    private void backToCreate() {
        selectedOffer = null;
        pane = tab == Tab.MARKET ? Pane.CREATE : Pane.NONE;
        int have = selectedType == null ? 0 : owned.getOrDefault(selectedType, 0);
        countField.setValue(String.valueOf(Math.max(1, have)));
        priceField.setValue(referenceCents() > 0 ? Money.compact(referenceCents()) : "");
        arrange();
    }

    private long referenceCents() {
        String type = pane == Pane.DEAL && selectedDeal != null ? selectedDeal.type() : selectedType;
        Long cents = type == null ? null : market.spawnerPrices().unitCentsByType().get(type);
        return cents == null ? 0 : cents;
    }

    private String referenceLabel() {
        return pane == Pane.OFFER ? I18n.get("hugomarket.exchange.offer_price") : I18n.get("hugomarket.spawners.reference");
    }

    private void resetPrice() {
        long cents = pane == Pane.OFFER && selectedOffer != null ? selectedOffer.unitCents() : referenceCents();
        priceField.setValue(cents > 0 ? Money.compact(cents) : "");
    }

    private void stepCount(int delta) {
        long current = SpawnerDeal.parseCount(countField.value()).orElse(0);
        countField.setValue(String.valueOf(Math.max(1, current + delta)));
    }

    private void useOwned() {
        int have = selectedType == null ? 0 : owned.getOrDefault(selectedType, 0);
        if (have > 0) {
            countField.setValue(String.valueOf(have));
        }
    }

    private OptionalLong count() {
        return SpawnerDeal.parseCount(countField.value());
    }

    private OptionalLong price() {
        return SpawnerDeal.parsePriceCents(priceField.value());
    }

    private boolean takesOffer() {
        return selectedOffer != null && price().isPresent() && price().getAsLong() == selectedOffer.unitCents()
                && count().isPresent() && count().getAsLong() <= selectedOffer.count();
    }

    private String takeLabel() {
        return I18n.get(takesOffer() ? "hugomarket.exchange.take" : "hugomarket.exchange.send_counter");
    }

    private String declineLabel() {
        boolean withdraw = selectedDeal != null && !selectedDeal.myTurn();
        return I18n.get(withdraw ? "hugomarket.exchange.withdraw" : "hugomarket.exchange.decline");
    }

    private String msgLabel() {
        String player = pane == Pane.OFFER && selectedOffer != null ? selectedOffer.player()
                : selectedDeal != null ? selectedDeal.otherPlayer() : "";
        return "/msg " + player;
    }

    private boolean ready() {
        if (exchange == null) {
            flash(I18n.get("hugomarket.exchange.offline"), Theme.DANGER);
            return false;
        }
        if (busy) {
            return false;
        }
        busy = true;
        if (!exchange.loggedIn()) {
            flash(I18n.get("hugomarket.exchange.logging_in"), Theme.TEXT_DIM);
        }
        return true;
    }

    private boolean validAmount() {
        if (count().isEmpty() || price().isEmpty()) {
            flash(I18n.get("hugomarket.spawners.invalid"), Theme.DANGER);
            return false;
        }
        return true;
    }

    private void createOffer() {
        if (selectedType == null || !validAmount() || !ready()) {
            return;
        }
        exchange.createOffer(selectedType, selling, (int) count().getAsLong(), price().getAsLong(), success("hugomarket.exchange.created"));
    }

    private void sendRequest() {
        if (selectedOffer == null || !validAmount() || !ready()) {
            return;
        }
        String done = takesOffer() ? "hugomarket.exchange.taken" : "hugomarket.exchange.counter_sent";
        exchange.request(selectedOffer.id(), (int) count().getAsLong(), price().getAsLong(), success(done));
    }

    private void deleteOffer() {
        if (selectedOffer == null || !ready()) {
            return;
        }
        exchange.deleteOffer(selectedOffer.id(), error -> {
            finished(error);
            if (error == null) {
                flash(I18n.get("hugomarket.exchange.deleted"), Theme.ACCENT);
                backToCreate();
            }
        });
    }

    private void acceptDeal() {
        if (selectedDeal == null || !ready()) {
            return;
        }
        exchange.accept(selectedDeal.id(), success("hugomarket.exchange.accepted"));
    }

    private void counterDeal() {
        if (selectedDeal == null || !validAmount() || !ready()) {
            return;
        }
        exchange.counter(selectedDeal.id(), (int) count().getAsLong(), price().getAsLong(), success("hugomarket.exchange.counter_sent"));
    }

    private void declineDeal() {
        if (selectedDeal == null || !ready()) {
            return;
        }
        exchange.decline(selectedDeal.id(), success("hugomarket.exchange.declined"));
    }

    private void rate(int stars) {
        if (selectedDeal == null || !ready()) {
            return;
        }
        exchange.rate(selectedDeal.id(), stars, success("hugomarket.exchange.rated"));
    }

    private SpawnerExchange.Done success(String messageKey) {
        return error -> {
            finished(error);
            if (error == null) {
                flash(I18n.get(messageKey), Theme.BARGAIN_FRAME);
            }
        };
    }

    private void finished(String error) {
        busy = false;
        if (error != null) {
            String key = "hugomarket.exchange.error." + error;
            flash(I18n.exists(key) ? I18n.get(key) : I18n.get("hugomarket.exchange.error.failed"), Theme.DANGER);
        }
    }

    private void copyOffer() {
        if (selectedType == null || !validAmount()) {
            return;
        }
        long unit = price().getAsLong();
        long count = count().getAsLong();
        String text = GermanOffer.text(selling, count, selectedType, unit);
        Minecraft.getInstance().keyboardHandler.setClipboard(text);
        flash(I18n.get("hugomarket.spawners.copied"), Theme.ACCENT);
    }

    private void openMsg() {
        String label = msgLabel();
        if (label.length() <= 5) {
            return;
        }
        //? if >=1.21.9 {
        /*Minecraft.getInstance().setScreen(new ChatScreen(label + " ", false));
        *///?} else
        Minecraft.getInstance().setScreen(new ChatScreen(label + " "));
    }

    private void flash(String text, int color) {
        message = text;
        messageColor = color;
        messageUntil = System.currentTimeMillis() + MESSAGE_MS;
    }

    private void arrange() {
        widgets.clear();
        widgets.add(marketTab);
        widgets.add(dealsTab);
        if (tab == Tab.MARKET) {
            widgets.add(typeList);
            widgets.add(offerList);
        } else {
            widgets.add(dealList);
        }
        int fieldX = paneX + LABEL_WIDTH;
        int half = (PANE_WIDTH - Theme.GAP) / 2;
        int actionY = resultY + Theme.LINE * 3 + Theme.GAP;
        int rowStep = Theme.FIELD_HEIGHT + Theme.GAP;
        switch (pane) {
            case CREATE -> {
                place(sellChip, paneX, infoY + Theme.LINE * 2, half);
                place(buyChip, paneX + half + Theme.GAP, infoY + Theme.LINE * 2, PANE_WIDTH - half - Theme.GAP);
                addAmountRows(fieldX, true);
                place(createChip, paneX, actionY, PANE_WIDTH);
                place(copyChip, paneX, actionY + rowStep, PANE_WIDTH);
            }
            case OFFER -> {
                addAmountRows(fieldX, true);
                place(takeChip, paneX, actionY, PANE_WIDTH);
                place(msgChip, paneX, actionY + rowStep, half);
                place(backChip, paneX + half + Theme.GAP, actionY + rowStep, PANE_WIDTH - half - Theme.GAP);
            }
            case OWN_OFFER -> {
                place(deleteChip, paneX, actionY, half);
                place(backChip, paneX + half + Theme.GAP, actionY, PANE_WIDTH - half - Theme.GAP);
            }
            case DEAL -> arrangeDeal(fieldX, half, actionY, rowStep);
            case NONE -> {
            }
        }
    }

    private void arrangeDeal(int fieldX, int half, int actionY, int rowStep) {
        if (selectedDeal == null) {
            return;
        }
        switch (selectedDeal.status()) {
            case OPEN -> {
                if (selectedDeal.myTurn()) {
                    addAmountRows(fieldX, false);
                    place(acceptChip, paneX, actionY, half);
                    place(counterChip, paneX + half + Theme.GAP, actionY, PANE_WIDTH - half - Theme.GAP);
                    place(declineChip, paneX, actionY + rowStep, half);
                    place(msgChip, paneX + half + Theme.GAP, actionY + rowStep, PANE_WIDTH - half - Theme.GAP);
                } else {
                    place(declineChip, paneX, actionY, half);
                    place(msgChip, paneX + half + Theme.GAP, actionY, PANE_WIDTH - half - Theme.GAP);
                }
            }
            case ACCEPTED -> {
                place(msgChip, paneX, actionY, PANE_WIDTH);
                if (!selectedDeal.rated()) {
                    int starWidth = (PANE_WIDTH - Theme.GAP * 4) / 5;
                    for (int i = 0; i < starChips.size(); i++) {
                        place(starChips.get(i), paneX + i * (starWidth + Theme.GAP), actionY + rowStep * 2, starWidth);
                    }
                }
            }
            case DECLINED -> {
            }
        }
    }

    private void addAmountRows(int fieldX, boolean withSteps) {
        int stepsWidth = withSteps ? countSteps.size() * (STEP_WIDTH + Theme.GAP) : 0;
        countField.setBounds(fieldX, countY, PANE_WIDTH - LABEL_WIDTH - stepsWidth, Theme.FIELD_HEIGHT);
        widgets.add(countField);
        if (withSteps) {
            int stepX = fieldX + countField.width() + Theme.GAP;
            for (Chip step : countSteps) {
                step.setBounds(stepX, countY + 1, STEP_WIDTH, Theme.FIELD_HEIGHT - 2);
                widgets.add(step);
                stepX += STEP_WIDTH + Theme.GAP;
            }
        }
        int referenceWidth = 62;
        priceField.setBounds(fieldX, priceY, PANE_WIDTH - LABEL_WIDTH - referenceWidth - Theme.GAP, Theme.FIELD_HEIGHT);
        widgets.add(priceField);
        referenceChip.setBounds(fieldX + priceField.width() + Theme.GAP, priceY + 1, referenceWidth, Theme.FIELD_HEIGHT - 2);
        widgets.add(referenceChip);
    }

    private void place(Chip chip, int chipX, int chipY, int chipWidth) {
        chip.setBounds(chipX, chipY, chipWidth, Theme.FIELD_HEIGHT);
        widgets.add(chip);
    }

    @Override
    protected void layout() {
        int innerX = x + Theme.PADDING;
        int innerWidth = width - Theme.PADDING * 2;
        int top = contentTop();
        marketTab.setBounds(innerX, top, TAB_WIDTH, Theme.FIELD_HEIGHT);
        dealsTab.setBounds(innerX + TAB_WIDTH + Theme.GAP, top, TAB_WIDTH + 20, Theme.FIELD_HEIGHT);
        columnsTop = top + Theme.FIELD_HEIGHT + Theme.GAP * 2;
        columnsBottom = y + height - Theme.PADDING - Theme.LINE - Theme.GAP;
        paneX = innerX + innerWidth - PANE_WIDTH;
        int listRight = paneX - Theme.PADDING * 2;
        typeList.setBounds(innerX, columnsTop, TYPE_WIDTH, columnsBottom - columnsTop);
        offerList.setBounds(innerX + TYPE_WIDTH + Theme.PADDING, columnsTop, listRight - innerX - TYPE_WIDTH - Theme.PADDING, columnsBottom - columnsTop);
        dealList.setBounds(innerX, columnsTop, listRight - innerX, columnsBottom - columnsTop);
        infoY = columnsTop;
        countY = infoY + Theme.LINE * 2 + Theme.FIELD_HEIGHT + Theme.GAP * 2;
        priceY = countY + Theme.FIELD_HEIGHT + Theme.GAP;
        resultY = priceY + Theme.FIELD_HEIGHT + Theme.GAP * 2;
        arrange();
    }

    @Override
    public void render(Gfx gfx, int mouseX, int mouseY) {
        if (market.version() != marketVersion || (exchange != null && exchange.version() != exchangeVersion)) {
            rebuild();
        }
        super.render(gfx, mouseX, mouseY);
        gfx.fill(paneX - Theme.PADDING - 1, columnsTop, 1, columnsBottom - columnsTop, Theme.PANEL_BORDER);
        if (tab == Tab.MARKET) {
            if (offerList.rowCount() == 0) {
                gfx.textNoShadow(gfx.clip(I18n.get("hugomarket.exchange.no_offers"), offerList.width()), offerList.x() + 4, columnsTop + 4, Theme.TEXT_DIM);
            }
        } else if (dealList.rowCount() == 0) {
            String empty = exchange != null && exchange.loggedIn() ? "hugomarket.exchange.no_deals" : "hugomarket.exchange.login_needed";
            gfx.textNoShadow(gfx.clip(I18n.get(empty), dealList.width()), dealList.x() + 4, columnsTop + 4, Theme.TEXT_DIM);
        }
        renderPane(gfx);
        if (System.currentTimeMillis() < messageUntil) {
            gfx.textNoShadow(gfx.clip(message, PANE_WIDTH), paneX, columnsBottom - Theme.LINE + 2, messageColor);
        }
        String hint = I18n.get("hugomarket.spawners.hint", market.spawnerPrices().updatedAt());
        gfx.textNoShadow(gfx.clip(hint, width - Theme.PADDING * 2), x + Theme.PADDING, y + height - Theme.PADDING - Theme.LINE + 2, Theme.TEXT_DIM);
    }

    private void renderPane(Gfx gfx) {
        switch (pane) {
            case CREATE -> {
                if (selectedType == null) {
                    return;
                }
                renderTitle(gfx, SpawnerNames.of(selectedType), owned.getOrDefault(selectedType, 0) > 0
                        ? I18n.get("hugomarket.spawners.owned", owned.get(selectedType))
                        : I18n.get("hugomarket.spawners.reference_line", Money.format(referenceCents())));
                renderAmountLabels(gfx);
                renderResults(gfx, selling);
            }
            case OFFER, OWN_OFFER -> {
                if (selectedOffer == null) {
                    return;
                }
                String head = I18n.get(selectedOffer.selling() ? "hugomarket.exchange.offer_sells" : "hugomarket.exchange.offer_buys",
                        selectedOffer.player(), selectedOffer.count());
                renderTitle(gfx, head, I18n.get("hugomarket.exchange.offer_detail", Money.format(selectedOffer.unitCents()), stars(selectedOffer.rating())));
                if (pane == Pane.OFFER) {
                    renderAmountLabels(gfx);
                    renderResults(gfx, !selectedOffer.selling());
                }
            }
            case DEAL -> renderDealPane(gfx);
            case NONE -> gfx.textNoShadow(gfx.clip(I18n.get("hugomarket.exchange.pick_deal"), PANE_WIDTH), paneX, infoY + 4, Theme.TEXT_DIM);
        }
    }

    private void renderDealPane(Gfx gfx) {
        if (selectedDeal == null) {
            return;
        }
        ExchangeDeal deal = selectedDeal;
        String head = I18n.get(deal.iSell() ? "hugomarket.exchange.deal_sell" : "hugomarket.exchange.deal_buy",
                deal.count(), SpawnerNames.of(deal.type()), deal.otherPlayer());
        renderTitle(gfx, head, I18n.get("hugomarket.exchange.offer_detail", Money.format(deal.unitCents()), stars(deal.otherRating())));
        String state = switch (deal.status()) {
            case OPEN -> deal.myTurn() ? I18n.get("hugomarket.exchange.state.your_turn")
                    : I18n.get("hugomarket.exchange.state.waiting", deal.otherPlayer());
            case ACCEPTED -> I18n.get("hugomarket.exchange.state.accepted");
            case DECLINED -> I18n.get("hugomarket.exchange.state.declined");
        };
        int stateColor = deal.status() == ExchangeDeal.Status.ACCEPTED ? Theme.BARGAIN_FRAME
                : deal.status() == ExchangeDeal.Status.DECLINED ? Theme.DANGER : deal.myTurn() ? Theme.GOLD : Theme.TEXT_DIM;
        gfx.textNoShadow(gfx.clip(state, PANE_WIDTH), paneX, infoY + Theme.LINE * 2 + 3, stateColor);
        if (deal.status() == ExchangeDeal.Status.OPEN && deal.myTurn()) {
            renderAmountLabels(gfx);
            renderResults(gfx, deal.iSell());
        } else if (deal.status() == ExchangeDeal.Status.ACCEPTED) {
            gfx.textNoShadow(gfx.clip(I18n.get("hugomarket.exchange.total_line", Money.format(deal.unitCents() * deal.count())), PANE_WIDTH),
                    paneX, countY + 4, Theme.GOLD);
            String rateText = deal.rated() ? I18n.get("hugomarket.exchange.rated_already") : I18n.get("hugomarket.exchange.rate_prompt", deal.otherPlayer());
            int actionY = resultY + Theme.LINE * 3 + Theme.GAP;
            gfx.textNoShadow(gfx.clip(rateText, PANE_WIDTH), paneX, actionY + Theme.FIELD_HEIGHT + Theme.GAP + 3, Theme.TEXT_DIM);
        }
    }

    private void renderTitle(Gfx gfx, String title, String sub) {
        gfx.item(icon, paneX, infoY);
        gfx.textNoShadow(gfx.clip(title, PANE_WIDTH - ICON_SIZE - 4), paneX + ICON_SIZE + 4, infoY, Theme.TEXT);
        gfx.textNoShadow(gfx.clip(sub, PANE_WIDTH - ICON_SIZE - 4), paneX + ICON_SIZE + 4, infoY + 9, Theme.TEXT_DIM);
    }

    private void renderAmountLabels(Gfx gfx) {
        gfx.textNoShadow(I18n.get("hugomarket.spawners.amount"), paneX, countY + 4, Theme.TEXT_DIM);
        gfx.textNoShadow(I18n.get("hugomarket.spawners.unit"), paneX, priceY + 4, Theme.TEXT_DIM);
    }

    private void renderResults(Gfx gfx, boolean iSell) {
        OptionalLong count = count();
        OptionalLong price = price();
        if (count.isEmpty() || price.isEmpty()) {
            gfx.textNoShadow(gfx.clip(I18n.get("hugomarket.spawners.invalid"), PANE_WIDTH), paneX, resultY, Theme.DANGER);
            return;
        }
        SpawnerDeal deal = new SpawnerDeal((int) count.getAsLong(), price.getAsLong(), referenceCents());
        line(gfx, I18n.get("hugomarket.spawners.total"), Money.format(deal.totalCents()), resultY, Theme.GOLD);
        if (deal.referenceUnitCents() <= 0) {
            return;
        }
        line(gfx, I18n.get("hugomarket.spawners.reference_total"), Money.format(deal.referenceTotalCents()), resultY + Theme.LINE, Theme.PRICE);
        OptionalDouble ratio = deal.ratio();
        if (ratio.isPresent()) {
            boolean good = iSell ? ratio.getAsDouble() >= 1 : ratio.getAsDouble() <= 1;
            long diff = deal.totalCents() - deal.referenceTotalCents();
            String delta = Deals.deltaText(ratio.getAsDouble()) + "  (" + (diff >= 0 ? "+" : "−") + Money.compact(Math.abs(diff)) + ")";
            line(gfx, I18n.get("hugomarket.spawners.delta"), delta, resultY + Theme.LINE * 2, good ? Theme.BARGAIN_FRAME : Theme.DANGER);
        }
    }

    private void line(Gfx gfx, String label, String value, int lineY, int color) {
        gfx.textNoShadow(label, paneX, lineY, Theme.TEXT_DIM);
        gfx.textNoShadow(value, paneX + PANE_WIDTH - gfx.textWidth(value), lineY, color);
    }

    private static String stars(ExchangeOffer.Rating rating) {
        if (rating.count() == 0) {
            return I18n.get("hugomarket.exchange.unrated");
        }
        return "★" + String.format(Locale.ROOT, "%.1f", rating.average()) + " (" + rating.count() + ")";
    }

    private void renderType(Gfx gfx, SpawnerRows.Row row, int rowX, int rowY, int rowWidth, int rowHeight, boolean hovered) {
        if (row.type().equals(selectedType)) {
            gfx.fill(rowX, rowY, rowWidth, rowHeight, Theme.ROW_HOVER);
            gfx.fill(rowX, rowY + 1, 2, rowHeight - 2, Theme.ACCENT);
        }
        gfx.item(icon, rowX + 3, rowY + (rowHeight - ICON_SIZE) / 2);
        int textX = rowX + ICON_SIZE + 6;
        int textWidth = rowWidth - ICON_SIZE - 9;
        gfx.textNoShadow(gfx.clip(SpawnerNames.of(row.type()), textWidth), textX, rowY + 2, Theme.TEXT);
        int offers = offersFor(row.type()).size();
        int have = owned.getOrDefault(row.type(), 0);
        String detail = Money.compact(row.unitCents()) + (offers > 0 ? " · " + I18n.get("hugomarket.exchange.offer_count", offers) : "")
                + (have > 0 ? " · " + I18n.get("hugomarket.spawners.owned_short", have) : "");
        gfx.textNoShadow(gfx.clip(detail, textWidth), textX, rowY + 11, offers > 0 ? Theme.PRICE : Theme.TEXT_DIM);
    }

    private void renderOffer(Gfx gfx, ExchangeOffer offer, int rowX, int rowY, int rowWidth, int rowHeight, boolean hovered) {
        if (selectedOffer != null && offer.id().equals(selectedOffer.id())) {
            gfx.fill(rowX, rowY, rowWidth, rowHeight, Theme.ROW_HOVER);
        }
        boolean own = exchange != null && exchange.loggedIn() && offer.player().equalsIgnoreCase(exchange.name());
        String tag = I18n.get(offer.selling() ? "hugomarket.exchange.tag.sell" : "hugomarket.exchange.tag.buy");
        gfx.fill(rowX + 2, rowY + 3, 3, rowHeight - 6, offer.selling() ? Theme.BARGAIN_FRAME : Theme.WATCH_FRAME);
        String price = Money.compact(offer.unitCents()) + " × " + offer.count();
        int priceWidth = gfx.textWidth(price);
        int textX = rowX + 9;
        gfx.textNoShadow(gfx.clip(tag + " " + offer.player(), rowWidth - priceWidth - 16), textX, rowY + 2, own ? Theme.GOLD : Theme.TEXT);
        gfx.textNoShadow(gfx.clip(stars(offer.rating()), rowWidth - priceWidth - 16), textX, rowY + 11, Theme.TEXT_DIM);
        gfx.textNoShadow(price, rowX + rowWidth - priceWidth - 3, rowY + 2, Theme.PRICE);
        long reference = market.spawnerPrices().unitCentsByType().getOrDefault(offer.type(), 0L);
        if (reference > 0) {
            String delta = Deals.deltaText((double) offer.unitCents() / reference);
            gfx.textNoShadow(delta, rowX + rowWidth - gfx.textWidth(delta) - 3, rowY + 11, Theme.TEXT_DIM);
        }
    }

    private void renderDeal(Gfx gfx, ExchangeDeal deal, int rowX, int rowY, int rowWidth, int rowHeight, boolean hovered) {
        if (selectedDeal != null && deal.id().equals(selectedDeal.id())) {
            gfx.fill(rowX, rowY, rowWidth, rowHeight, Theme.ROW_HOVER);
        }
        int mark = deal.status() == ExchangeDeal.Status.ACCEPTED ? Theme.BARGAIN_FRAME
                : deal.status() == ExchangeDeal.Status.DECLINED ? Theme.DANGER : deal.myTurn() ? Theme.GOLD : Theme.TEXT_DIM;
        gfx.fill(rowX + 2, rowY + 3, 3, rowHeight - 6, mark);
        String head = SpawnerNames.of(deal.type()) + " · " + deal.otherPlayer();
        String amount = deal.count() + " × " + Money.compact(deal.unitCents());
        int amountWidth = gfx.textWidth(amount);
        gfx.textNoShadow(gfx.clip(head, rowWidth - amountWidth - 16), rowX + 9, rowY + 2, Theme.TEXT);
        gfx.textNoShadow(amount, rowX + rowWidth - amountWidth - 3, rowY + 2, Theme.PRICE);
        String state = switch (deal.status()) {
            case OPEN -> deal.myTurn() ? I18n.get("hugomarket.exchange.state.your_turn") : I18n.get("hugomarket.exchange.state.waiting", deal.otherPlayer());
            case ACCEPTED -> I18n.get(deal.rated() ? "hugomarket.exchange.state.accepted" : "hugomarket.exchange.state.rate");
            case DECLINED -> I18n.get("hugomarket.exchange.state.declined");
        };
        gfx.textNoShadow(gfx.clip(I18n.get(deal.iSell() ? "hugomarket.exchange.you_sell" : "hugomarket.exchange.you_buy") + " · " + state,
                rowWidth - 12), rowX + 9, rowY + 11, mark);
    }
}
