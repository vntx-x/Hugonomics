package dev.hugomarket.model;

public enum ScreenKind {
    AH_LIST,
    AH_SELL_CONFIRM,
    AH_CREATE_CONFIRM,
    PRICE_INPUT,
    ORDER_CREATE_CONFIRM,
    ORDER_LIST,
    ORDER_ITEM_PICKER,
    SELL_GUI,
    ENDERCHEST,
    UNKNOWN_HUGO,
    VANILLA_CONTAINER,
    PLAYER_INVENTORY;

    public boolean isHugo() {
        return this != VANILLA_CONTAINER && this != PLAYER_INVENTORY;
    }
}
