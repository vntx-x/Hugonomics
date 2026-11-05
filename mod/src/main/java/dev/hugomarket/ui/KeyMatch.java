package dev.hugomarket.ui;

import net.minecraft.client.KeyMapping;

//? if >=1.21.9 {
/*import net.minecraft.client.input.KeyEvent;
*///?}

public final class KeyMatch {
    private KeyMatch() {
    }

    public static boolean matches(KeyMapping mapping, int key, int scancode) {
        if (mapping.isUnbound()) {
            return false;
        }
        //? if >=1.21.9 {
        /*return mapping.matches(new KeyEvent(key, scancode, 0));
        *///?} else
        return mapping.matches(key, scancode);
    }
}
