package dev.hugomarket.module.networth;

import com.google.gson.JsonObject;
import dev.hugomarket.store.JsonFile;

import java.nio.file.Path;

public final class NetWorthStore {
    public record Part(long cents, long at) {
        public static final Part NONE = new Part(0, 0);

        public boolean known() {
            return at > 0;
        }
    }

    private final Path file;
    private Part balance = Part.NONE;
    private Part enderchest = Part.NONE;
    private Part auctions = Part.NONE;

    public NetWorthStore(Path file) {
        this.file = file;
    }

    public void load() {
        JsonObject root = JsonFile.read(file);
        if (root == null) {
            return;
        }
        balance = part(root, "balance");
        enderchest = part(root, "enderchest");
        auctions = part(root, "auctions");
    }

    private static Part part(JsonObject root, String name) {
        if (!root.has(name)) {
            return Part.NONE;
        }
        JsonObject json = root.getAsJsonObject(name);
        return new Part(json.get("cents").getAsLong(), json.get("at").getAsLong());
    }

    public Part balance() {
        return balance;
    }

    public Part enderchest() {
        return enderchest;
    }

    public Part auctions() {
        return auctions;
    }

    public void setBalance(long cents, long at) {
        if (balance.known() && balance.cents() == cents) {
            balance = new Part(cents, at);
            return;
        }
        balance = new Part(cents, at);
        save();
    }

    public void setEnderchest(long cents, long at) {
        enderchest = new Part(cents, at);
        save();
    }

    public void setAuctions(long cents, long at) {
        auctions = new Part(cents, at);
        save();
    }

    private void save() {
        JsonObject root = new JsonObject();
        root.add("balance", json(balance));
        root.add("enderchest", json(enderchest));
        root.add("auctions", json(auctions));
        JsonFile.write(file, root);
    }

    private static JsonObject json(Part part) {
        JsonObject json = new JsonObject();
        json.addProperty("cents", part.cents());
        json.addProperty("at", part.at());
        return json;
    }
}
