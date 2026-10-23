package com.devcode.timefreeze.client;

import com.devcode.timefreeze.Config;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** GUI moda (klawisz K): suwak jasności, przełączniki zamrożenia i zapis stanu. */
public class TimeFreezeScreen extends Screen {
    private static final int W = 200;
    private static final int H = 20;
    private static final int STEP = 24;

    public TimeFreezeScreen() {
        super(Component.translatable("gui.timefreeze.title"));
    }

    @Override
    protected void init() {
        Config cfg = Config.get();
        int x = this.width / 2 - W / 2;
        int y = this.height / 2 - 84;

        // 1) Suwak jasności (gamma / fullbright 0..15)
        double currentGamma = cfg.gammaOverride ? cfg.gamma : this.minecraft.options.gamma().get();
        this.addRenderableWidget(new GammaSlider(x, y, W, H, currentGamma));

        // 2) Włącz/wyłącz zamrożenie obrazu
        this.addRenderableWidget(Button.builder(label("gui.timefreeze.freeze", FreezeHandler.isFrozen()), b -> {
            FreezeHandler.toggle();
            b.setMessage(label("gui.timefreeze.freeze", FreezeHandler.isFrozen()));
        }).bounds(x, y += STEP, W, H).build());

        // 3) Blokada ruchu i akcji
        this.addRenderableWidget(Button.builder(label("gui.timefreeze.lock_move", cfg.lockMovement), b -> {
            cfg.lockMovement = !cfg.lockMovement;
            b.setMessage(label("gui.timefreeze.lock_move", cfg.lockMovement));
        }).bounds(x, y += STEP, W, H).build());

        // 4) Blokada kamery
        this.addRenderableWidget(Button.builder(label("gui.timefreeze.lock_camera", cfg.lockCamera), b -> {
            cfg.lockCamera = !cfg.lockCamera;
            b.setMessage(label("gui.timefreeze.lock_camera", cfg.lockCamera));
        }).bounds(x, y += STEP, W, H).build());

        // 5) Czy przy włączeniu zamrożenia ustawiać zapisaną pozycję kamery
        this.addRenderableWidget(Button.builder(label("gui.timefreeze.use_saved", cfg.useSavedView), b -> {
            cfg.useSavedView = !cfg.useSavedView;
            b.setMessage(label("gui.timefreeze.use_saved", cfg.useSavedView));
        }).bounds(x, y += STEP, W, H).build());

        // 6) "Zapisz pozycję i stan" (Set/Save State)
        this.addRenderableWidget(Button.builder(Component.translatable("gui.timefreeze.save"), b -> saveState())
                .bounds(x, y += STEP, W, H).build());

        // 7) Zamknij
        this.addRenderableWidget(Button.builder(Component.translatable("gui.timefreeze.done"), b -> this.onClose())
                .bounds(x, y += STEP, W, H).build());
    }

    /** Zapisuje bieżący kąt kamery, jasność i przełączniki do pliku konfiguracyjnego. */
    private void saveState() {
        LocalPlayer player = this.minecraft.player;
        if (player != null) {
            Config cfg = Config.get();
            cfg.savedYaw = player.getYRot();
            cfg.savedPitch = player.getXRot();
            cfg.hasSavedState = true;
            player.displayClientMessage(Component.translatable("msg.timefreeze.saved"), true);
        }
        Config.save();
    }

    private static Component label(String key, boolean on) {
        return Component.translatable(key, Component.translatable(on ? "gui.timefreeze.on" : "gui.timefreeze.off"));
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        // W 1.21.x Screen#render samo rysuje tło (z rozmyciem) - nie wolno wołać renderBackground drugi raz
        super.render(gg, mouseX, mouseY, partialTick);
        gg.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 104, 0xFFFFFF);
    }

    /** K zamyka GUI tym samym klawiszem, którym zostało otwarte. */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (KeyBindings.OPEN_GUI.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** Zapis configu dopiero przy zamknięciu (a nie przy każdym ruchu suwaka). */
    @Override
    public void removed() {
        Config.save();
    }

    /** Ważne: GUI NIE pauzuje gry - świat musi działać dalej. */
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Płynny suwak jasności 0..15 (Gammalight / Fullbright). */
    private static class GammaSlider extends AbstractSliderButton {
        GammaSlider(int x, int y, int w, int h, double gamma) {
            super(x, y, w, h, Component.empty(), Math.min(gamma, Config.MAX_GAMMA) / Config.MAX_GAMMA);
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.translatable("gui.timefreeze.brightness",
                    String.format(Locale.ROOT, "%.1f", this.value * Config.MAX_GAMMA)));
        }

        @Override
        protected void applyValue() {
            double gamma = this.value * Config.MAX_GAMMA;
            GammaHelper.set(gamma);
            Config.get().gamma = gamma;
            Config.get().gammaOverride = true;
        }
    }
}
