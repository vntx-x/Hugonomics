package dev.hugomarket.mixin;

import dev.hugomarket.ui.OverlayManager;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >=1.21.9 {
/*import net.minecraft.client.input.CharacterEvent;
*///?}

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    //? if >=1.21.9 {
    /*@Inject(method = "charTyped(JLnet/minecraft/client/input/CharacterEvent;)V", at = @At("HEAD"), cancellable = true)
    private void hugomarket$charTyped(long window, CharacterEvent event, CallbackInfo ci) {
        if (OverlayManager.charTyped(event.codepoint(), event.modifiers())) {
            ci.cancel();
        }
    }
    *///?} else {
    @Inject(method = "charTyped(JII)V", at = @At("HEAD"), cancellable = true)
    private void hugomarket$charTyped(long window, int codePoint, int modifiers, CallbackInfo ci) {
        if (OverlayManager.charTyped(codePoint, modifiers)) {
            ci.cancel();
        }
    }
    //?}
}
