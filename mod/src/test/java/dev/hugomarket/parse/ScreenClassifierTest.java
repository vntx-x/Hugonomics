package dev.hugomarket.parse;

import dev.hugomarket.model.ScreenKind;
import dev.hugomarket.testsupport.DumpLoader;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScreenClassifierTest {
    private final ScreenClassifier classifier = new ScreenClassifier(HugoGuiRules.load());

    @ParameterizedTest
    @CsvSource({
            "ah-list/ah-list-1.json, AH_LIST",
            "ah-list/ah-list-2.json, AH_LIST",
            "ah-sell-confirm/ah-sell-confirm-1.json, AH_SELL_CONFIRM",
            "ah-price-anvil/ah-price-anvil-1.json, PRICE_INPUT",
            "order-create-confirm/order-create-confirm-1.json, ORDER_CREATE_CONFIRM",
            "ah-create-confirm/ah-create-confirm-1.json, AH_CREATE_CONFIRM",
            "order-list/order-list-1.json, ORDER_LIST",
            "order-item-picker/order-item-picker-1.json, ORDER_ITEM_PICKER",
            "sell-gui/sell-gui-1.json, SELL_GUI",
            "enderchest/enderchest-1.json, ENDERCHEST",
            "chest/chest-1.json, VANILLA_CONTAINER",
            "shulker/shulker-1.json, VANILLA_CONTAINER"
    })
    void classifiesDumps(String dump, ScreenKind expected) {
        assertEquals(expected, classifier.classify(DumpLoader.container(dump)));
    }
}
