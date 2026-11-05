package dev.hugomarket.module.dumper;

import com.google.gson.JsonObject;
import dev.hugomarket.core.ServerDetector;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.network.chat.Component;

import java.time.Instant;
import java.time.LocalDateTime;

final class ChatDumper {
    private final DumperState state;
    private final DumpWriter writer;
    private final ServerDetector server;

    ChatDumper(DumperState state, DumpWriter writer, ServerDetector server) {
        this.state = state;
        this.writer = writer;
        this.server = server;
    }

    void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) ->
                record(overlay ? "actionbar" : "system", message, null));
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) ->
                //? if >=1.21.9 {
                /*record("chat", message, sender == null ? null : sender.name()));
                *///?} else
                record("chat", message, sender == null ? null : sender.getName()));
    }

    private void record(String kind, Component message, String sender) {
        if (!state.isRecording() || !server.active()) {
            return;
        }
        JsonObject json = new JsonObject();
        json.addProperty("ts", Instant.now().toString());
        json.addProperty("kind", kind);
        if (sender != null) {
            json.addProperty("sender", sender);
        }
        json.addProperty("text", message.getString());
        json.add("json", ComponentJson.of(message));
        writer.appendChat(DumpFileNames.chat(LocalDateTime.now()), json);
        state.countChatLine();
    }
}
