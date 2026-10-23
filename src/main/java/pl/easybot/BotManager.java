package pl.easybot;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.util.math.BlockPos;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Prosty "menedżer bota" – trzyma stan połączenia i logi konsoli.
 * Statyczny, dzięki czemu logi przeżywają zamknięcie i ponowne otwarcie GUI.
 *
 * Bot jest tu wewnętrzny (symulowany). Miejsca, w których możesz podpiąć
 * prawdziwą logikę (np. Mineflayer, socket do zewnętrznego bota), są
 * oznaczone komentarzem "TUTAJ PODPIĘCIE".
 */
public final class BotManager {

    /** Kolory logów (ARGB) */
    public static final int GRAY = 0xFF8A8A94;
    public static final int WHITE = 0xFFE8E8EE;
    public static final int GREEN = 0xFF4ADE80;
    public static final int RED = 0xFFF87171;

    /** Jeśli true – wiadomości z dolnego pola są wysyłane na serwer przez Twojego gracza */
    private static final boolean WYSYLAJ_NA_SERWER = false;

    private static final int MAX_LOGOW = 500;
    private static final DateTimeFormatter FORMAT_CZASU = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** Pojedynczy wpis w konsoli */
    public record LogEntry(String time, String text, int color) {}

    private static final List<LogEntry> LOGI = new ArrayList<>();

    private static boolean connected = true;
    private static String botName = "skibidibozydar12";
    private static long connectedAtMs = System.currentTimeMillis() - 12_000L; // "0:12" jak w przykładzie

    static {
        // Przykładowe logi początkowe (stałe godziny z wzoru)
        LOGI.add(new LogEntry("14:25:58", "Kompresja włączona (256)", GRAY));
        LOGI.add(new LogEntry("14:25:58", "Zalogowano jako " + botName + " (" + uuidDla(botName) + ")", GRAY));
        LOGI.add(new LogEntry("14:25:58", "Bot " + botName + " wszedł na serwer!", GREEN));
        LOGI.add(new LogEntry("14:26:00", "Zostałeś pomyślnie zalogowany!", WHITE));
        LOGI.add(new LogEntry("14:26:06", "[GUI] Otwarto: ...", GRAY));
    }

    private BotManager() {}

    // ------------------------------------------------------------------
    // Gettery
    // ------------------------------------------------------------------

    public static List<LogEntry> getLogs() { return LOGI; }
    public static boolean isConnected() { return connected; }
    public static String getBotName() { return botName; }

    /** Czas sesji w formacie m:ss (np. 0:12) */
    public static String getSessionTime() {
        if (!connected) return "-:--";
        long sek = (System.currentTimeMillis() - connectedAtMs) / 1000L;
        return (sek / 60) + ":" + String.format("%02d", sek % 60);
    }

    /** Koordynaty gracza w formacie "x y z" (np. 63 132 202) */
    public static String getCoords() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return "0 0 0";
        BlockPos p = mc.player.getBlockPos();
        return p.getX() + " " + p.getY() + " " + p.getZ();
    }

    // ------------------------------------------------------------------
    // Akcje
    // ------------------------------------------------------------------

    /** Dodaje wpis do konsoli z aktualną godziną */
    public static void log(String text, int color) {
        LOGI.add(new LogEntry(LocalTime.now().format(FORMAT_CZASU), text, color));
        while (LOGI.size() > MAX_LOGOW) LOGI.remove(0);
    }

    /** "Wyślij bota" – łączy bota o podanej nazwie */
    public static void connect(String name) {
        name = name == null ? "" : name.trim();
        if (name.isEmpty()) {
            log("Podaj nazwę bota!", RED);
            return;
        }
        if (connected) {
            log("Bot jest już połączony.", GRAY);
            return;
        }
        botName = name;
        connected = true;
        connectedAtMs = System.currentTimeMillis();

        // TUTAJ PODPIĘCIE: realne uruchomienie bota (proces zewnętrzny / klient Mineflayer itd.)
        log("Kompresja włączona (256)", GRAY);
        log("Zalogowano jako " + botName + " (" + uuidDla(botName) + ")", GRAY);
        log("Bot " + botName + " wszedł na serwer!", GREEN);
        log("Zostałeś pomyślnie zalogowany!", WHITE);
    }

    /** "Rozłącz" – rozłącza bota */
    public static void disconnect() {
        if (!connected) {
            log("Bot nie jest połączony.", GRAY);
            return;
        }
        connected = false;

        // TUTAJ PODPIĘCIE: realne zamknięcie połączenia bota
        log("Bot " + botName + " został rozłączony.", RED);
    }

    /** "Wyczyść" – czyści konsolę */
    public static void clear() {
        LOGI.clear();
    }

    /** Wysyła tekst z dolnego pola (czat lub /komenda) */
    public static void send(String text) {
        if (text == null || text.isBlank()) return;
        if (!connected) {
            log("Bot nie jest połączony – najpierw kliknij \"Wyślij bota\".", RED);
            return;
        }
        boolean komenda = text.startsWith("/");
        if (komenda) {
            log("> " + text, GREEN);
        } else {
            log("<" + botName + "> " + text, WHITE);
        }

        // TUTAJ PODPIĘCIE: przekazanie wiadomości do prawdziwego bota
        if (WYSYLAJ_NA_SERWER) {
            MinecraftClient mc = MinecraftClient.getInstance();
            ClientPlayNetworkHandler net = mc.getNetworkHandler();
            if (net != null) {
                if (komenda) net.sendChatCommand(text.substring(1)); // bez ukośnika
                else net.sendChatMessage(text);
            }
        }
    }

    /** Offline UUID – tylko do ładnego wyglądu w logach */
    private static UUID uuidDla(String nazwa) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + nazwa).getBytes());
    }
}
