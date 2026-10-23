package pl.easybot;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Główne menu EasyBot (ciemny motyw).
 *
 * Cały własny rysunek (panel, konsola, nagłówek) robimy w renderBackground(),
 * a widgety (pola, przyciski) rysuje potem standardowe Screen.render() – dzięki
 * temu leżą NA wierzchu panelu.
 */
public class EasyBotScreen extends Screen {

    // --- Paleta kolorów ---
    private static final int COL_BG = 0xFF111116;        // tło panelu #111116
    private static final int COL_BORDER = 0xFF22222A;    // ramki #22222a
    private static final int COL_FIELD_BG = 0xFF0B0B0F;  // tło pól tekstowych
    private static final int COL_CONSOLE_BG = 0xFF000000;
    private static final int COL_TEXT = 0xFFE8E8EE;
    private static final int COL_MUTED = 0xFF8A8A94;
    private static final int COL_GREEN = 0xFF4ADE80;
    private static final int COL_RED = 0xFFF87171;

    private enum Tab { CONSOLE, BOT_GUI }
    /** static – zapamiętujemy zakładkę między otwarciami */
    private static Tab currentTab = Tab.CONSOLE;

    // --- Geometria (liczona w init) ---
    private int left, top, panelW, panelH;
    private int innerL, innerR;
    private int fieldX1, fieldX2, fieldY;        // tło pola nazwy bota
    private int conX1, conY1, conX2, conY2;      // obszar konsoli
    private int inX1, inX2, inY;                 // tło dolnego pola
    private int tabY;

    // --- Widgety ---
    private TextFieldWidget nameField;
    private TextFieldWidget inputField;
    private StyledButton sendBotBtn, disconnectBtn, clearBtn;
    private StyledButton consoleTab, guiTab;
    private StyledButton sendBtn;
    private StyledButton openGuiBtn;

    private int scroll = 0;           // przesunięcie konsoli (w liniach, 0 = dół)
    private String inputDraft = "";   // szkic wiadomości (przeżywa zmianę rozmiaru okna)

    private record Line(OrderedText text, int color) {}

    public EasyBotScreen() {
        super(Text.literal("EasyBot"));
    }

    @Override
    protected void init() {
        panelW = Math.min(width - 20, 640);
        panelH = Math.min(height - 20, 340);
        left = (width - panelW) / 2;
        top = (height - panelH) / 2;
        int pad = 8, gap = 4;
        innerL = left + pad;
        innerR = left + panelW - pad;

        // ---------- Górny panel: pole nazwy + 3 przyciski ----------
        int rowY = top + 26;
        int w3 = 70, w2 = 70, w1 = 84;
        int x3 = innerR - w3;
        int x2 = x3 - gap - w2;
        int x1 = x2 - gap - w1;

        fieldX1 = innerL;
        fieldX2 = x1 - gap;
        fieldY = rowY;

        String poprzedniaNazwa = nameField != null ? nameField.getText() : BotManager.getBotName();
        nameField = new TextFieldWidget(textRenderer, fieldX1 + 5, rowY + 6, fieldX2 - fieldX1 - 10, 10,
                Text.literal("Nazwa bota"));
        nameField.setDrawsBackground(false); // własne tło rysujemy sami
        nameField.setMaxLength(16);
        nameField.setText(poprzedniaNazwa);
        nameField.setPlaceholder(Text.literal("Nazwa bota").formatted(Formatting.DARK_GRAY));
        addDrawableChild(nameField);

        sendBotBtn = addDrawableChild(new StyledButton(x1, rowY, w1, 20, Text.literal("Wyślij bota"),
                StyledButton.Style.GRAY, b -> BotManager.connect(nameField.getText())));
        disconnectBtn = addDrawableChild(new StyledButton(x2, rowY, w2, 20, Text.literal("Rozłącz"),
                StyledButton.Style.RED, b -> BotManager.disconnect()));
        clearBtn = addDrawableChild(new StyledButton(x3, rowY, w3, 20, Text.literal("Wyczyść"),
                StyledButton.Style.GRAY, b -> { BotManager.clear(); scroll = 0; }));

        // ---------- Zakładki ----------
        tabY = top + 54;
        consoleTab = addDrawableChild(new StyledButton(innerL, tabY, 70, 18, Text.literal("Konsola"),
                StyledButton.Style.TAB, b -> currentTab = Tab.CONSOLE));
        guiTab = addDrawableChild(new StyledButton(innerL + 74, tabY, 70, 18, Text.literal("GUI bota"),
                StyledButton.Style.TAB, b -> currentTab = Tab.BOT_GUI));

        // ---------- Obszar konsoli ----------
        conX1 = innerL;
        conX2 = innerR;
        conY1 = tabY + 22;
        conY2 = top + panelH - 38;

        // Przycisk w zakładce "GUI bota"
        openGuiBtn = addDrawableChild(new StyledButton(conX1 + 10, conY1 + 72, 150, 20,
                Text.literal("Otwórz GUI bota"), StyledButton.Style.GRAY, b -> {
            if (BotManager.isConnected()) BotManager.log("[GUI] Otwarto: Menu bota", BotManager.GRAY);
            else BotManager.log("Bot nie jest połączony.", BotManager.RED);
        }));

        // ---------- Dolny panel: pole + "Wyślij" ----------
        inY = top + panelH - 30;
        int sendW = 60;
        inX1 = innerL;
        inX2 = innerR - sendW - gap;

        inputField = new TextFieldWidget(textRenderer, inX1 + 5, inY + 6, inX2 - inX1 - 10, 10,
                Text.literal("Wiadomość"));
        inputField.setDrawsBackground(false);
        inputField.setMaxLength(256);
        inputField.setText(inputDraft);
        inputField.setChangedListener(s -> inputDraft = s);
        inputField.setPlaceholder(Text.literal("Napisz jako bot albo wpisz /komendę i Enter")
                .formatted(Formatting.DARK_GRAY));
        addDrawableChild(inputField);

        sendBtn = addDrawableChild(new StyledButton(innerR - sendW, inY, sendW, 20, Text.literal("Wyślij"),
                StyledButton.Style.GRAY, b -> sendInput()));

        setInitialFocus(inputField);
    }

    /** Wysyła treść dolnego pola do bota */
    private void sendInput() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;
        BotManager.send(text);
        inputField.setText("");
        scroll = 0;
    }

    // ------------------------------------------------------------------
    // Rysowanie
    // ------------------------------------------------------------------

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.renderBackground(ctx, mouseX, mouseY, delta); // tło gry (przyciemnienie/rozmycie)

        // Aktualizacja stanu widgetów
        boolean polaczony = BotManager.isConnected();
        sendBotBtn.active = !polaczony;
        disconnectBtn.active = polaczony;
        consoleTab.setSelected(currentTab == Tab.CONSOLE);
        guiTab.setSelected(currentTab == Tab.BOT_GUI);
        openGuiBtn.visible = currentTab == Tab.BOT_GUI;
        openGuiBtn.active = currentTab == Tab.BOT_GUI;

        // Panel główny
        ctx.fill(left, top, left + panelW, top + panelH, COL_BG);
        ctx.drawBorder(left, top, panelW, panelH, COL_BORDER);

        drawHeader(ctx);

        // Tło pola nazwy bota
        ctx.fill(fieldX1, fieldY, fieldX2, fieldY + 20, COL_FIELD_BG);
        ctx.drawBorder(fieldX1, fieldY, fieldX2 - fieldX1, 20,
                nameField.isFocused() ? 0xFF3A3A48 : COL_BORDER);

        // Linia pod zakładkami
        ctx.fill(innerL, tabY + 18, innerR, tabY + 19, COL_BORDER);

        // Obszar główny (czarna konsola / panel GUI bota)
        ctx.fill(conX1, conY1, conX2, conY2, COL_CONSOLE_BG);
        ctx.drawBorder(conX1, conY1, conX2 - conX1, conY2 - conY1, COL_BORDER);
        if (currentTab == Tab.CONSOLE) drawConsole(ctx);
        else drawBotInfo(ctx);

        // Tło dolnego pola
        ctx.fill(inX1, inY, inX2, inY + 20, COL_FIELD_BG);
        ctx.drawBorder(inX1, inY, inX2 - inX1, 20, inputField.isFocused() ? 0xFF3A3A48 : COL_BORDER);
    }

    /** Nagłówek: "EasyBot · anarchia.gg" po lewej, status po prawej */
    private void drawHeader(DrawContext ctx) {
        Text tytul = Text.literal("EasyBot").formatted(Formatting.WHITE, Formatting.BOLD)
                .append(Text.literal(" · anarchia.gg").formatted(Formatting.GRAY));
        ctx.drawText(textRenderer, tytul, innerL, top + 9, COL_TEXT, false);

        boolean on = BotManager.isConnected();
        String status = on
                ? "Na serwerze · " + BotManager.getSessionTime() + " · " + BotManager.getCoords()
                : "Rozłączony";
        int sw = textRenderer.getWidth(status);
        int sx = innerR - sw;
        ctx.drawText(textRenderer, status, sx, top + 9, COL_MUTED, false);

        // Kropka statusu (zielona = aktywny, czerwona = rozłączony), "zaokrąglona" rogami
        int dx = sx - 11, dy = top + 10;
        int c = on ? COL_GREEN : COL_RED;
        ctx.fill(dx + 1, dy, dx + 5, dy + 6, c);
        ctx.fill(dx, dy + 1, dx + 6, dy + 5, c);

        // Linia pod nagłówkiem
        ctx.fill(left, top + 22, left + panelW, top + 23, COL_BORDER);
    }

    /** Konsola logów z zawijaniem linii i przewijaniem kółkiem myszy */
    private void drawConsole(DrawContext ctx) {
        int maxWidth = conX2 - conX1 - 12;
        List<Line> linie = new ArrayList<>();
        for (BotManager.LogEntry e : BotManager.getLogs()) {
            String tekst = e.time() + " " + e.text();
            for (OrderedText ot : textRenderer.wrapLines(Text.literal(tekst), maxWidth)) {
                linie.add(new Line(ot, e.color()));
            }
        }

        int lineH = 10;
        int maxLines = (conY2 - conY1 - 8) / lineH;
        int maxScroll = Math.max(0, linie.size() - maxLines);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        int start = Math.max(0, linie.size() - maxLines - scroll);
        int end = Math.min(linie.size(), start + maxLines);

        ctx.enableScissor(conX1 + 1, conY1 + 1, conX2 - 1, conY2 - 1);
        for (int i = start; i < end; i++) {
            Line l = linie.get(i);
            ctx.drawText(textRenderer, l.text(), conX1 + 6, conY1 + 4 + (i - start) * lineH, l.color(), false);
        }
        ctx.disableScissor();
    }

    /** Zakładka "GUI bota" – podgląd informacji o bocie */
    private void drawBotInfo(DrawContext ctx) {
        boolean on = BotManager.isConnected();
        int x = conX1 + 10, y = conY1 + 10;
        ctx.drawText(textRenderer, "Panel bota", x, y, COL_TEXT, false);
        ctx.drawText(textRenderer, "Status: " + (on ? "połączony" : "rozłączony"), x, y + 14,
                on ? COL_GREEN : COL_RED, false);
        ctx.drawText(textRenderer, "Nazwa: " + BotManager.getBotName(), x, y + 26, COL_MUTED, false);
        ctx.drawText(textRenderer, "Czas sesji: " + BotManager.getSessionTime(), x, y + 38, COL_MUTED, false);
        ctx.drawText(textRenderer, "Pozycja: " + BotManager.getCoords(), x, y + 50, COL_MUTED, false);
    }

    // ------------------------------------------------------------------
    // Wejście
    // ------------------------------------------------------------------

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Enter w dolnym polu = wyślij
        if (inputField.isFocused() && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            sendInput();
            return true;
        }
        // Enter w polu nazwy = "Wyślij bota"
        if (nameField.isFocused() && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            BotManager.connect(nameField.getText());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (currentTab == Tab.CONSOLE
                && mouseX >= conX1 && mouseX <= conX2 && mouseY >= conY1 && mouseY <= conY2) {
            scroll += (int) Math.signum(verticalAmount) * 3; // w górę = starsze logi
            if (scroll < 0) scroll = 0;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean shouldPause() {
        return false; // gra działa dalej w tle
    }
}
