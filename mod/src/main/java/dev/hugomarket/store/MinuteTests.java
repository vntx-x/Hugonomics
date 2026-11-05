package dev.hugomarket.store;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class MinuteTests {
    private static final int MAX = 50;

    private final Path file;
    private final List<MinuteTest.Result> results = new ArrayList<>();

    public MinuteTests(Path file) {
        this.file = file;
    }

    public void load() {
        JsonObject root = JsonFile.read(file);
        if (root == null || !root.has("tests")) {
            return;
        }
        for (JsonElement element : root.getAsJsonArray("tests")) {
            JsonObject t = element.getAsJsonObject();
            results.add(new MinuteTest.Result(t.get("name").getAsString(), t.get("finishedAt").getAsLong(), t.get("base").getAsLong(),
                    t.get("bonus").getAsLong(), t.get("total").getAsLong(), t.get("multiplier").getAsDouble(), t.get("sales").getAsInt()));
        }
    }

    public List<MinuteTest.Result> all() {
        return List.copyOf(results);
    }

    public MinuteTest.Result add(MinuteTest.Result result) {
        MinuteTest.Result named = result.name().isEmpty() ? MinuteTest.Result.named(result, "Test " + (results.size() + 1)) : result;
        results.add(0, named);
        while (results.size() > MAX) {
            results.remove(results.size() - 1);
        }
        save();
        return named;
    }

    public void rename(int index, String name) {
        if (index < 0 || index >= results.size() || name.isBlank()) {
            return;
        }
        results.set(index, MinuteTest.Result.named(results.get(index), name.trim()));
        save();
    }

    public void remove(int index) {
        if (index >= 0 && index < results.size()) {
            results.remove(index);
            save();
        }
    }

    private void save() {
        JsonArray array = new JsonArray();
        for (MinuteTest.Result r : results) {
            JsonObject t = new JsonObject();
            t.addProperty("name", r.name());
            t.addProperty("finishedAt", r.finishedAt());
            t.addProperty("base", r.baseCents());
            t.addProperty("bonus", r.bonusCents());
            t.addProperty("total", r.totalCents());
            t.addProperty("multiplier", r.multiplier());
            t.addProperty("sales", r.sales());
            array.add(t);
        }
        JsonObject root = new JsonObject();
        root.add("tests", array);
        JsonFile.write(file, root);
    }
}
