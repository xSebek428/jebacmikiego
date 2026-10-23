package com.devcode.timefreeze.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** Skróty klawiszowe (zmienisz je w Opcje -> Sterowanie). */
public final class KeyBindings {
    public static final String CATEGORY = "key.categories.timefreeze";

    /** K - otwiera GUI moda. */
    public static KeyMapping OPEN_GUI;
    /** J - nagłe włączenie/wyłączenie zamrożenia obrazu. */
    public static KeyMapping TOGGLE_FREEZE;

    public static void register() {
        OPEN_GUI = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.timefreeze.open_gui", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY));
        TOGGLE_FREEZE = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.timefreeze.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, CATEGORY));
    }

    private KeyBindings() {}
}
