package pl.easybot;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Własny, ciemny przycisk (zamiast domyślnej tekstury Minecrafta).
 * Style: GRAY (szary), RED (ciemnoczerwony), TAB (zakładka z zieloną linią).
 */
public class StyledButton extends ButtonWidget {

    public enum Style { GRAY, RED, TAB }

    private final Style style;
    private boolean selected; // używane tylko dla TAB

    public StyledButton(int x, int y, int w, int h, Text message, Style style, PressAction onPress) {
        super(x, y, w, h, message, onPress, DEFAULT_NARRATION_SUPPLIER);
        this.style = style;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        boolean hover = this.isHovered() && this.active;
        int textColor;

        switch (style) {
            case RED -> {
                int bg = !active ? 0xFF2A1414 : hover ? 0xFF8E1B1B : 0xFF6E1414;
                ctx.fill(x, y, x + w, y + h, bg);
                ctx.drawBorder(x, y, w, h, active ? 0xFF9B2226 : 0xFF3A1A1A);
                textColor = active ? 0xFFFFE4E4 : 0xFF6A4A4A;
            }
            case TAB -> {
                // Zakładka: brak tła, aktywna ma zieloną linię u dołu
                if (hover && !selected) ctx.fill(x, y, x + w, y + h, 0xFF1A1A21);
                if (selected) ctx.fill(x, y + h - 2, x + w, y + h, 0xFF4ADE80);
                textColor = selected ? 0xFF4ADE80 : hover ? 0xFFE0E0E6 : 0xFF8A8A94;
            }
            default -> { // GRAY
                int bg = !active ? 0xFF15151B : hover ? 0xFF2E2E38 : 0xFF1B1B22;
                ctx.fill(x, y, x + w, y + h, bg);
                ctx.drawBorder(x, y, w, h, 0xFF2C2C36);
                textColor = active ? 0xFFE0E0E6 : 0xFF5A5A64;
            }
        }

        // Tekst wyśrodkowany w przycisku
        ctx.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer,
                getMessage(), x + w / 2, y + (h - 8) / 2, textColor);
    }
}
