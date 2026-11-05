package dev.hugomarket.module.dumper;

import dev.hugomarket.core.Module;
import dev.hugomarket.core.ModuleContext;

public final class DumperModule implements Module {
    private final DumperState state = new DumperState();
    private DumpWriter writer;
    private ContainerDumper containers;
    private ModuleContext context;

    @Override
    public String id() {
        return "dumper";
    }

    @Override
    public boolean enabledByDefault() {
        return false;
    }

    @Override
    public void onEnable(ModuleContext context) {
        this.context = context;
        writer = new DumpWriter(context.dataDir().resolve("dumps"));
        containers = new ContainerDumper(state, writer, context.server());
        context.containers().addListener(containers);
        new ChatDumper(state, writer, context.server()).register();
        new SidebarDumper(state, writer, context.server()).register(context.scheduler());
        new DumperToggle(state, context.scheduler()).register();
        new DumperHud(state).register();
    }

    @Override
    public void onDisable() {
        state.setRecording(false);
        state.setEnabled(false);
        if (context != null && containers != null) {
            context.containers().removeListener(containers);
        }
        if (writer != null) {
            writer.close();
        }
    }
}
