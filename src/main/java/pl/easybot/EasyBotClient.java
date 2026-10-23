package pl.easybot;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Punkt wejścia moda (tylko klient).
 * Rejestruje klawisz (domyślnie B), który otwiera menu EasyBot.
 */
public class EasyBotClient implements ClientModInitializer {

    private static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        // W 1.21.4 kategoria klawisza to zwykły String (klucz tłumaczenia)
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.easybotgui.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "category.easybotgui"
        ));

        // Co tick sprawdzamy, czy klawisz został wciśnięty
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new EasyBotScreen());
                }
            }
        });
    }
}
