package dev.hugomarket.parse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.hugomarket.model.HugoControl;
import dev.hugomarket.model.ItemView;
import dev.hugomarket.model.ScreenKind;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class HugoGuiRules {
    private static final String RESOURCE = "/assets/hugomarket/hugo-gui.json";

    public record ScreenRule(ScreenKind kind, String title, String menuType, String secured, int contentFrom, int contentTo) {
        public boolean isContentSlot(int index) {
            return index >= contentFrom && index <= contentTo;
        }
    }

    private final List<ScreenRule> screens;
    private final Map<Integer, HugoControl> controls;

    private HugoGuiRules(List<ScreenRule> screens, Map<Integer, HugoControl> controls) {
        this.screens = List.copyOf(screens);
        this.controls = Map.copyOf(controls);
    }

    public static HugoGuiRules load() {
        InputStream in = HugoGuiRules.class.getResourceAsStream(RESOURCE);
        if (in == null) {
            throw new IllegalStateException("missing " + RESOURCE);
        }
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return parse(JsonParser.parseReader(reader).getAsJsonObject());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static HugoGuiRules parse(JsonObject json) {
        String prefix = json.get("titlePrefix").getAsString();
        Map<Integer, HugoControl> controls = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("controls").entrySet()) {
            controls.put(Integer.parseInt(entry.getKey()), HugoControl.valueOf(entry.getValue().getAsString()));
        }
        List<ScreenRule> screens = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray("screens")) {
            JsonObject rule = element.getAsJsonObject();
            String title = rule.has("title") ? rule.get("title").getAsString() : prefix + rule.get("titleGlyph").getAsString();
            String secured = rule.has("secured") ? rule.get("secured").getAsString() : null;
            int from = -1;
            int to = -1;
            if (rule.has("contentSlots")) {
                JsonArray range = rule.getAsJsonArray("contentSlots");
                from = range.get(0).getAsInt();
                to = range.get(1).getAsInt();
            }
            screens.add(new ScreenRule(ScreenKind.valueOf(rule.get("kind").getAsString()), title,
                    rule.get("menuType").getAsString(), secured, from, to));
        }
        return new HugoGuiRules(screens, controls);
    }

    public List<ScreenRule> screens() {
        return screens;
    }

    public ScreenRule rule(ScreenKind kind) {
        for (ScreenRule rule : screens) {
            if (rule.kind() == kind) {
                return rule;
            }
        }
        return null;
    }

    public HugoControl controlOf(ItemView item) {
        if (item == null || item.customModelData().isEmpty()) {
            return null;
        }
        return controls.get(Math.round(item.customModelData().get(0)));
    }
}
