package dev.hugomarket.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("leftPos")
    int hugomarket$leftPos();

    @Accessor("topPos")
    int hugomarket$topPos();

    @Accessor("imageWidth")
    int hugomarket$imageWidth();

    @Accessor("imageHeight")
    int hugomarket$imageHeight();

    @Accessor("hoveredSlot")
    Slot hugomarket$hoveredSlot();
}
