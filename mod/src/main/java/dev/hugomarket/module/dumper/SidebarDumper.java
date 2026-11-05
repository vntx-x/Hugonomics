package dev.hugomarket.module.dumper;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.hugomarket.core.ClientScheduler;
import dev.hugomarket.core.ServerDetector;
import dev.hugomarket.ui.Sidebar;
import net.minecraft.client.Minecraft;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

final class SidebarDumper {
    private static final int INTERVAL_TICKS = 20;

    private final DumperState state;
    private final DumpWriter writer;
    private final ServerDetector server;
    private List<String> last = List.of();
    private int ticks;

    SidebarDumper(DumperState state, DumpWriter writer, ServerDetector server) {
        this.state = state;
        this.writer = writer;
        this.server = server;
    }

    void register(ClientScheduler scheduler) {
        scheduler.onTick(this::tick);
    }

    private void tick(Minecraft client) {
        if (++ticks % INTERVAL_TICKS != 0 || !state.isRecording() || !server.active()) {
            return;
        }
        List<String> lines = Sidebar.lines(client);
        if (lines.equals(last)) {
            return;
        }
        last = lines;
        JsonObject json = new JsonObject();
        json.addProperty("ts", Instant.now().toString());
        json.addProperty("kind", "sidebar");
        JsonArray array = new JsonArray();
        lines.forEach(array::add);
        json.add("lines", array);
        writer.appendChat(DumpFileNames.chat(LocalDateTime.now()), json);
        state.countChatLine();
    }
}
