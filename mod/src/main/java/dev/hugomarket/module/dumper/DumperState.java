package dev.hugomarket.module.dumper;

public final class DumperState {
    private volatile boolean enabled = true;
    private volatile boolean recording;

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
}
