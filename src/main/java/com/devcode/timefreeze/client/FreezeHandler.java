package com.devcode.timefreeze.client;

import com.devcode.timefreeze.Config;
import com.devcode.timefreeze.TimeFreezeClient;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * Serce moda - "Zatrzymanie Percepcji Czasu" (Fabric 1.21.4, mapowania Mojang).
 *
 * JAK DZIAŁA BLOKADA OBRAZU (bez mixinów):
 *  1) Silnik gry (ticki, fizyka, mobki, bloki) oraz SoundEngine NIE są dotykane - świat
 *     dalej żyje, a dźwięki grają normalnie, bo audio jest całkowicie niezależne od renderu.
 *  2) Rejestrujemy własną WARSTWĘ HUD-a (HudLayerRegistrationCallback) podpiętą jako ostatnią,
 *     czyli nad całym HUD-em. W momencie jej rysowania świat i HUD są już w głównym framebufferze.
 *  3) Po włączeniu zamrożenia warstwa w pierwszej klatce kopiuje cały framebuffer do własnego
 *     TextureTarget (glBlitFramebuffer) - to jest "ostatnia klatka". W każdej kolejnej klatce
 *     rysuje tę teksturę na pełnym ekranie. Prawdziwy render świata nadal się wykonuje, ale gracz
 *     widzi tylko zamrożony obraz. Ekrany (GUI moda, czat, ESC) rysuje gra PO HUD-zie, więc
 *     zostają widoczne nad zamrożonym obrazem.
 *  4) Ruch blokujemy zerując stan klawiszy (WASD, skok, shift, atak, użycie) na początku każdego
 *     ticku, a obrót kamery przywracając do zapisanej wartości na początku ticku i klatki.
 */
public final class FreezeHandler {
    private static final ResourceLocation LAYER_ID =
            ResourceLocation.fromNamespaceAndPath(TimeFreezeClient.MODID, "frozen_frame");

    private static boolean frozen;
    private static boolean captured;
    private static float lockYaw;
    private static float lockPitch;
    private static TextureTarget frozenFrame;

    public static void init() {
        // Nasza warstwa ląduje za ostatnią warstwą vanilli (napisy dla niesłyszących) = na wierzchu HUD-a
        HudLayerRegistrationCallback.EVENT.register(drawer ->
                drawer.attachLayerAfter(IdentifiedLayer.SUBTITLES, LAYER_ID, FreezeHandler::onRenderLayer));

        ClientTickEvents.START_CLIENT_TICK.register(FreezeHandler::onStartTick);
        ClientTickEvents.END_CLIENT_TICK.register(FreezeHandler::onEndTick);
        // Dodatkowy reset kamery tuż przed renderem świata w każdej klatce
        WorldRenderEvents.START.register(ctx -> applyCameraLock(Minecraft.getInstance().player));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (Config.get().gammaOverride) {
                GammaHelper.set(Config.get().gamma);
            }
        });
        // Zwolnij teksturę i wyłącz zamrożenie przy wyjściu ze świata
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> disable());
    }

    public static boolean isFrozen() {
        return frozen;
    }

    public static void toggle() {
        if (frozen) {
            disable();
        } else {
            enable();
        }
    }

    private static void enable() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        Config cfg = Config.get();
        // Opcjonalnie: ustaw kamerę na pozycję zapisaną przyciskiem "Zapisz pozycję i stan"
        if (cfg.useSavedView && cfg.hasSavedState) {
            player.setYRot(cfg.savedYaw);
            player.setXRot(cfg.savedPitch);
            player.yRotO = player.getYRot();
            player.xRotO = player.getXRot();
        }
        lockYaw = player.getYRot();
        lockPitch = player.getXRot();
        captured = false; // klatka zostanie skopiowana w następnej klatce HUD-a
        frozen = true;
    }

    public static void disable() {
        frozen = false;
        captured = false;
        if (frozenFrame != null) {
            frozenFrame.destroyBuffers();
            frozenFrame = null;
        }
    }

    /* ------------------------- RENDER: zamrożony obraz ------------------------- */

    private static void onRenderLayer(GuiGraphics gg, DeltaTracker delta) {
        if (!frozen) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        // GuiGraphics buforuje rysowanie - wypychamy HUD do framebuffera przed kopiowaniem/zasłonięciem klatki
        gg.flush();

        if (!captured) {
            captureFrame(mc);
            captured = true;
            return; // ta klatka jest jeszcze "żywa" - zamrażamy od następnej
        }
        drawFrozenFrame(gg);

        // Mały wskaźnik, żeby nie pomylić zamrożenia z zawieszeniem gry (usuń jeśli niepotrzebny)
        gg.drawString(mc.font, Component.translatable("hud.timefreeze.frozen"), 4, 4, 0x55FFFF, true);
    }

    /** Kopiuje główny framebuffer (ostatnia klatka) do własnej tekstury. */
    private static void captureFrame(Minecraft mc) {
        RenderTarget main = mc.getMainRenderTarget();
        if (frozenFrame == null) {
            frozenFrame = new TextureTarget(main.width, main.height, false);
        } else if (frozenFrame.width != main.width || frozenFrame.height != main.height) {
            frozenFrame.resize(main.width, main.height);
        }
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, frozenFrame.frameBufferId);
        GL30.glBlitFramebuffer(0, 0, main.width, main.height,
                0, 0, frozenFrame.width, frozenFrame.height,
                GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        main.bindWrite(true); // wracamy do rysowania w głównym buforze
    }

    /** Rysuje zapisaną teksturę na całym ekranie (przykrywa świat i HUD). */
    private static void drawFrozenFrame(GuiGraphics gg) {
        if (frozenFrame == null) {
            return;
        }
        float w = gg.guiWidth();
        float h = gg.guiHeight();
        Matrix4f m = gg.pose().last().pose();

        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.setShader(CoreShaders.POSITION_TEX);
        RenderSystem.setShaderTexture(0, frozenFrame.getColorTextureId());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        // Tekstura framebuffera jest odwrócona w pionie (początek układu OpenGL = lewy dół),
        // dlatego V biegnie od 0 (dół ekranu) do 1 (góra ekranu).
        buf.addVertex(m, 0, h, 0).setUv(0, 0);
        buf.addVertex(m, w, h, 0).setUv(1, 0);
        buf.addVertex(m, w, 0, 0).setUv(1, 1);
        buf.addVertex(m, 0, 0, 0).setUv(0, 1);
        BufferUploader.drawWithShader(buf.buildOrThrow());

        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
    }

    /* ------------------------- BLOKADA RUCHU I KAMERY ------------------------- */

    private static void onStartTick(Minecraft mc) {
        if (!frozen || mc.player == null) {
            return;
        }
        // Początek ticku = przed obsługą klawiszy i LocalPlayer#tick (który wysyła rotację na serwer)
        applyCameraLock(mc.player);

        if (Config.get().lockMovement) {
            Options o = mc.options;
            // Ruch: KeyboardInput czyta te klawisze dopiero w trakcie ticku gracza
            for (KeyMapping k : new KeyMapping[]{o.keyUp, o.keyDown, o.keyLeft, o.keyRight,
                    o.keyJump, o.keyShift, o.keySprint}) {
                k.setDown(false);
            }
            // Akcje: gracz nie widzi efektów kliknięć, więc blokujemy atak/użycie/wybór bloku
            for (KeyMapping k : new KeyMapping[]{o.keyAttack, o.keyUse, o.keyPickItem}) {
                while (k.consumeClick()) {
                    // opróżniamy kolejkę kliknięć
                }
                k.setDown(false);
            }
        }
    }

    private static void onEndTick(Minecraft mc) {
        if (mc.player == null) {
            return;
        }
        while (KeyBindings.TOGGLE_FREEZE.consumeClick()) {
            toggle();
        }
        while (KeyBindings.OPEN_GUI.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new TimeFreezeScreen());
            }
        }
    }

    private static void applyCameraLock(LocalPlayer player) {
        if (!frozen || player == null || !Config.get().lockCamera) {
            return;
        }
        player.setYRot(lockYaw);
        player.setXRot(lockPitch);
        player.yRotO = lockYaw;
        player.xRotO = lockPitch;
    }

    private FreezeHandler() {}
}
