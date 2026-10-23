package com.devcode.timefreeze;

import com.devcode.timefreeze.client.FreezeHandler;
import com.devcode.timefreeze.client.KeyBindings;
import net.fabricmc.api.ClientModInitializer;

/** Punkt wejścia moda (czysto kliencki - entrypoint "client" w fabric.mod.json). */
public class TimeFreezeClient implements ClientModInitializer {
    public static final String MODID = "timefreeze";

    @Override
    public void onInitializeClient() {
        Config.load();
        KeyBindings.register();
        FreezeHandler.init();
    }
}
