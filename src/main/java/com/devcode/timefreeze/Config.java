package com.devcode.timefreeze;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Prosty config JSON (config/timefreeze.json) - tu ląduje "Zapisz pozycję i stan". */
public final class Config {
    public static final double MAX_GAMMA = 15.0D;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Config instance = new Config();

    /** Czy mod nadpisuje jasność (gamma) po wejściu do świata. */
    public boolean gammaOverride = false;
    /** Gamma: 1.0 = 'Bright' z vanilli, >1 = fullbright. */
    public double gamma = 1.0D;
    /** Blokuj ruch i akcje (atak/użycie) gdy obraz jest zamrożony. */
    public boolean lockMovement = true;
    /** Blokuj obrót kamery gdy obraz jest zamrożony. */
    public boolean lockCamera = true;
    /** Przy włączeniu zamrożenia ustaw zapisaną pozycję kamery. */
    public boolean useSavedView = false;
    public boolean hasSavedState = false;
    public float savedYaw = 0.0F;
    public float savedPitch = 0.0F;

    public static Config get() {
        return instance;
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("timefreeze.json");
    }

    public static void load() {
        Path p = file();
        if (Files.exists(p)) {
            try (Reader r = Files.newBufferedReader(p)) {
                Config loaded = GSON.fromJson(r, Config.class);
                if (loaded != null) {
                    instance = loaded;
                }
            } catch (IOException | RuntimeException e) {
                System.err.println("[timefreeze] Nie udało się wczytać configu: " + e);
            }
        }
    }

    public static void save() {
        try (Writer w = Files.newBufferedWriter(file())) {
            GSON.toJson(instance, w);
        } catch (IOException e) {
            System.err.println("[timefreeze] Nie udało się zapisać configu: " + e);
        }
    }

    private Config() {}
}
