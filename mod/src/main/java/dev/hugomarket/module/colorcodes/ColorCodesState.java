package dev.hugomarket.module.colorcodes;

final class ColorCodesState {
    enum Target {
        RENAME("/rename ", 35),
        SIGN("/sign ", 30);

        final String command;
        final int limit;

        Target(String command, int limit) {
            this.command = command;
            this.limit = limit;
        }
    }

    private Target target = Target.RENAME;
    private String text = "";

    Target target() {
        return target;
    }

    void setTarget(Target target) {
        this.target = target;
    }

    String text() {
        return text;
    }

    void setText(String text) {
        this.text = text;
    }

    String command() {
        return target.command + text;
    }

    int visibleLength() {
        return ColorCodes.visibleLength(text);
    }

    boolean overLimit() {
        return visibleLength() > target.limit;
    }
}
