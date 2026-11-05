package dev.hugomarket.mixin;

import dev.hugomarket.container.ContainerTracker;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ContainerPacketMixin {
    @Inject(method = "handleContainerContent", at = @At("TAIL"))
    private void hugomarket$onContainerContent(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        ContainerTracker.notifyContentChanged();
    }

    @Inject(method = "handleContainerSetSlot", at = @At("TAIL"))
    private void hugomarket$onContainerSetSlot(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        ContainerTracker.notifyContentChanged();
    }
}
