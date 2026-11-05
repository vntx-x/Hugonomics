package dev.hugomarket.module.dumper;

import java.util.concurrent.atomic.AtomicInteger;

public final class DumperState {
    private volatile boolean enabled = true;
    private volatile boolean recording;
    private final AtomicInteger containers = new AtomicInteger();
    private final AtomicInteger chatLines = new AtomicInteger();

    public boolean isRecording() {
        return enabled && recording;
    }

    public void setRecording(boolean recording) {
        this.recording = recording;
    }

    public boolean toggleRecording() {
        recording = !recording;
        return recording;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void countContainer() {
        containers.incrementAndGet();
    }

    public void countChatLine() {
        chatLines.incrementAndGet();
    }

    public int containerCount() {
        return containers.get();
    }

    public int chatLineCount() {
        return chatLines.get();
    }
}
