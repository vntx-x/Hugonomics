package dev.hugomarket.testsupport;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.hugomarket.model.ContainerView;
import dev.hugomarket.model.ItemView;
import dev.hugomarket.model.SlotView;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class DumpLoader {
    private DumpLoader() {
    }

    public static ContainerView container(String name) {
        JsonObject dump = json("/dumps/" + name);
        List<SlotView> slots = new ArrayList<>();
        for (JsonElement element : dump.getAsJsonArray("slots")) {
            JsonObject slot = element.getAsJsonObject();
            JsonObject stack = slot.getAsJsonObject("stack");
            ItemView item = item(stack.get("id").getAsString(), stack.has("count") ? stack.get("count").getAsInt() : 1,
                    slot.get("name").getAsString(), lore(slot.getAsJsonArray("lore")), stack.getAsJsonObject("components"));
            slots.add(new SlotView(slot.get("index").getAsInt(), slot.get("player").getAsBoolean(), item));
        }
        return new ContainerView(dump.get("title").getAsString(), dump.get("menuType").getAsString(),
                dump.get("containerSlots").getAsInt(), List.copyOf(slots));
    }

    public static List<String> chatLines(String name) {
        try (InputStream in = open("/dumps/" + name)) {
            List<String> lines = new ArrayList<>();
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                if (!line.isBlank()) {
                    lines.add(JsonParser.parseString(line).getAsJsonObject().get("text").getAsString());
                }
            }
            return lines;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static ItemView item(String id, int count, String name, List<String> lore, JsonObject components) {
        JsonObject c = components == null ? new JsonObject() : components;
        return new ItemView(
                id,
                count,
                name,
                lore,
                enchantments(c.get("minecraft:enchantments")),
                enchantments(c.get("minecraft:stored_enchantments")),
                potion(c.get("minecraft:potion_contents")),
                c.has("minecraft:item_model") ? c.get("minecraft:item_model").getAsString() : null,
                floats(c.get("minecraft:custom_model_data")),
                spawner(c.get("minecraft:block_entity_data")),
                c.has("minecraft:custom_name") ? name : null,
                bukkitValues(c.get("minecraft:custom_data")),
                contents(c.get("minecraft:container")));
    }

    private static Map<String, Integer> enchantments(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return Map.of();
        }
        JsonObject object = element.getAsJsonObject();
        if (object.has("levels")) {
            object = object.getAsJsonObject("levels");
        }
        Map<String, Integer> out = new TreeMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (entry.getValue().isJsonPrimitive()) {
                out.put(entry.getKey(), entry.getValue().getAsInt());
            }
        }
        return Map.copyOf(out);
    }

    private static String potion(JsonElement element) {
        if (element == null) {
            return null;
        }
        if (element.isJsonPrimitive()) {
            return element.getAsString();
        }
        JsonObject object = element.getAsJsonObject();
        return object.has("potion") ? object.get("potion").getAsString() : null;
    }

    private static List<Float> floats(JsonElement element) {
        if (element == null || !element.isJsonObject() || !element.getAsJsonObject().has("floats")) {
            return List.of();
        }
        List<Float> out = new ArrayList<>();
        for (JsonElement value : element.getAsJsonObject().getAsJsonArray("floats")) {
            out.add(value.getAsFloat());
        }
        return List.copyOf(out);
    }

    private static String spawner(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return null;
        }
        JsonObject data = element.getAsJsonObject();
        if (!data.has("SpawnData")) {
            return null;
        }
        JsonObject entity = data.getAsJsonObject("SpawnData").getAsJsonObject("entity");
        return entity != null && entity.has("id") ? entity.get("id").getAsString() : null;
    }

    private static Map<String, String> bukkitValues(JsonElement element) {
        if (element == null || !element.isJsonObject() || !element.getAsJsonObject().has("PublicBukkitValues")) {
            return Map.of();
        }
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().getAsJsonObject("PublicBukkitValues").entrySet()) {
            JsonElement value = entry.getValue();
            out.put(entry.getKey(), value.isJsonPrimitive() ? value.getAsString() : value.toString());
        }
        return Map.copyOf(out);
    }

    private static List<ItemView> contents(JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            return List.of();
        }
        List<ItemView> out = new ArrayList<>();
        for (JsonElement entry : element.getAsJsonArray()) {
            JsonObject stack = entry.getAsJsonObject().getAsJsonObject("item");
            String id = stack.get("id").getAsString();
            out.add(item(id, stack.has("count") ? stack.get("count").getAsInt() : 1, id, List.of(),
                    stack.has("components") ? stack.getAsJsonObject("components") : null));
        }
        return List.copyOf(out);
    }

    private static List<String> lore(JsonArray array) {
        List<String> out = new ArrayList<>();
        if (array != null) {
            for (JsonElement line : array) {
                out.add(line.getAsString());
            }
        }
        return List.copyOf(out);
    }

    private static JsonObject json(String resource) {
        try (Reader reader = new InputStreamReader(open(resource), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static InputStream open(String resource) {
        InputStream in = DumpLoader.class.getResourceAsStream(resource);
        if (in == null) {
            throw new IllegalArgumentException("missing test resource " + resource);
        }
        return in;
    }
}
