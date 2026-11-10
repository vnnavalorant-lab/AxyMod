package com.axymod.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;

import com.axymod.AxyMod;
import com.axymod.Emote;
import com.axymod.client.AxyModClient;
import com.axymod.client.EmoteManager;
import com.axymod.client.MorphManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * Painel de emotes. Visual: fundo preto com linhas de grade finas, título pixelado colorido, brilho de
 * cor por emote e botão azul de destaque (inspirado no site do Essential), mantendo a prévia do
 * personagem e o rostinho de cada emote do painel original. A logo da Scute AI aparece no cabeçalho e no rodapé.
 */
public class EmotePanelScreen extends Screen {
    private static final Identifier LOGO_20 = new Identifier(AxyMod.MOD_ID, "textures/gui/scute_logo_20.png");
    private static final Identifier LOGO_12 = new Identifier(AxyMod.MOD_ID, "textures/gui/scute_logo_12.png");

    // ---- layout ----
    private static final int BTN_W = 120;
    private static final int BTN_H = 24;
    private static final int LOW_H = 22;
    private static final int GAP = 4;
    private static final int COL_GAP = 8;
    private static final int PREVIEW_W = 112;
    private static final int PAD = 12;
    private static final int HEADER_H = 30;
    private static final int FOOTER_H = 20;
    private static final int COLS = 2;

    // ---- cores (ARGB) ----
    private static final int BG = 0xF2050507;
    private static final int GRID = 0x14FFFFFF;
    private static final int BORDER = 0xFF26262C;
    private static final int CELL = 0xFF0B0B0F;
    private static final int BLUE = 0xFF2B63F0;
    private static final int BLUE_HOVER = 0xFF4A7BFF;
    private static final int GREEN = 0xFF4BF37A;
    private static final int TEXT = 0xFFE8E8EE;
    private static final int TEXT_DIM = 0xFF6E6E78;
    private static final int TEXT_OFF = 0xFF55555C;
    private static final int WARN = 0xFFFFA63D;
    private static final int[] TITLE_COLORS = {0xFFD7F34B, 0xFFA070FF, 0xFFFF6EC7, 0xFF4BF37A};
    /** Cor de destaque de cada emote, na ordem do enum Emote. */
    private static final int[] ACCENTS = {
            0xFFFF8FD0, // normal
            0xFFFFA63D, // confused
            0xFFFFE03D, // excited
            0xFFFF4D4D, // mad
            0xFF4DA3FF, // sad
            0xFFB07CFF, // scared
            0xFF6BE06B, // sick
            0xFFFF6EC7  // silly
    };

    private enum Kind { EMOTE, PRIMARY, GHOST }

    private static final class Btn {
        final int x, y, w, h;
        final Text label;
        final Kind kind;
        final Emote emote;
        final Runnable action;
        final BooleanSupplier enabled;

        Btn(int x, int y, int w, int h, Text label, Kind kind, Emote emote, Runnable action, BooleanSupplier enabled) {
            this.x = x; this.y = y; this.w = w; this.h = h;
            this.label = label; this.kind = kind; this.emote = emote;
            this.action = action; this.enabled = enabled;
        }

        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private final List<Btn> buttons = new ArrayList<>();
    private int left;
    private int top;
    private int totalW;
    private int totalH;
    private int colH;

    public EmotePanelScreen() {
        super(Text.translatable("screen.axymod.panel.title"));
    }

    @Override
    protected void init() {
        buttons.clear();
        Emote[] emotes = Emote.values();
        int rows = (emotes.length + COLS - 1) / COLS;
        int gridW = BTN_W * COLS + GAP * (COLS - 1);
        int gridH = rows * BTN_H + (rows - 1) * GAP;

        colH = gridH + GAP + LOW_H + GAP + LOW_H;
        totalW = PREVIEW_W + COL_GAP + gridW;
        totalH = HEADER_H + colH + FOOTER_H;
        left = (this.width - totalW) / 2;
        top = Math.max(PAD, (this.height - totalH) / 2);

        int gx = left + PREVIEW_W + COL_GAP;
        int gy = top + HEADER_H;

        for (int i = 0; i < emotes.length; i++) {
            Emote emote = emotes[i];
            int col = i % COLS;
            int row = i / COLS;
            buttons.add(new Btn(gx + col * (BTN_W + GAP), gy + row * (BTN_H + GAP), BTN_W, BTN_H,
                    Text.literal(emote.displayName), Kind.EMOTE, emote,
                    () -> AxyModClient.requestEmote(emote), this::isMorphed));
        }

        int by = gy + gridH + GAP;
        // liga/desliga o modo Axy usando /axy morph e /axy unmorph (o texto atualiza quando o servidor responde)
        buttons.add(new Btn(gx, by, gridW, LOW_H, Text.empty(), Kind.PRIMARY, null, () -> {
            if (this.client != null && this.client.player != null) {
                this.client.player.networkHandler.sendChatCommand(isMorphed() ? "axy unmorph" : "axy morph");
            }
        }, () -> true));
        buttons.add(new Btn(gx, by + LOW_H + GAP, gridW, LOW_H, Text.translatable("gui.done"), Kind.GHOST, null,
                this::close, () -> true));
    }

    private boolean isMorphed() {
        return this.client != null && MorphManager.isMorphed(this.client.player);
    }

    private Text toggleText() {
        return Text.translatable(isMorphed() ? "screen.axymod.panel.mode_on" : "screen.axymod.panel.mode_off");
    }

    // ------------------------------------------------------------------ render

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);

        ClientPlayerEntity player = this.client == null ? null : this.client.player;
        boolean morphed = isMorphed();
        Emote current = player == null ? Emote.NORMAL : EmoteManager.current(player, player.age + delta);
        int accent = ACCENTS[current.ordinal()];

        int x0 = left - PAD, y0 = top - PAD, x1 = left + totalW + PAD, y1 = top + totalH + PAD;
        int footerY = top + HEADER_H + colH;

        // fundo, brilho colorido na base (muda de cor com o emote) e linhas de grade
        context.fill(x0, y0, x1, y1, BG);
        context.fillGradient(x0 + 1, y1 - 80, x1 - 1, y1 - 1, 0x00000000, (accent & 0x00FFFFFF) | 0x3A000000);
        for (int i = 1; i <= 4; i++) {
            int gxLine = x0 + (x1 - x0) * i / 5;
            context.drawVerticalLine(gxLine, y0, y1 - 1, GRID);
        }
        context.drawHorizontalLine(x0 + 1, x1 - 2, top + HEADER_H - 5, GRID);
        context.drawHorizontalLine(x0 + 1, x1 - 2, footerY, GRID);
        context.drawBorder(x0, y0, x1 - x0, y1 - y0, BORDER);

        drawHeader(context);
        drawPreview(context, mouseX, mouseY, player, morphed, current, accent);

        for (Btn b : buttons) {
            drawButton(context, b, mouseX, mouseY, current, morphed);
        }

        drawFooter(context, footerY);
    }

    private void drawHeader(DrawContext context) {
        context.drawTexture(LOGO_20, left, top + 2, 20, 20, 0f, 0f, 20, 20, 20, 20);

        // título pixelado: cada letra de uma cor (como o letreiro do site de referência)
        String title = this.title.getString().toUpperCase(Locale.ROOT);
        MatrixStack m = context.getMatrices();
        m.push();
        m.translate(left + 28, top + 4, 0);
        m.scale(2f, 2f, 1f);
        int cx = 0;
        int colorIndex = 0;
        for (char c : title.toCharArray()) {
            Text t = Text.literal(String.valueOf(c)).formatted(Formatting.BOLD);
            int color = c == ' ' ? 0xFFFFFFFF : TITLE_COLORS[colorIndex++ % TITLE_COLORS.length];
            context.drawTextWithShadow(this.textRenderer, t, cx, 0, color);
            cx += this.textRenderer.getWidth(t);
        }
        m.pop();
    }

    private void drawPreview(DrawContext context, int mouseX, int mouseY, ClientPlayerEntity player,
                             boolean morphed, Emote current, int accent) {
        int px = left, py = top + HEADER_H, pw = PREVIEW_W, ph = colH;

        // cartão com brilho da cor do emote subindo da base
        context.fill(px, py, px + pw, py + ph, 0xFF08080B);
        context.fillGradient(px + 1, py + ph / 3, px + pw - 1, py + ph - 1, 0x00000000,
                (accent & 0x00FFFFFF) | (morphed ? 0x66000000 : 0x22000000));
        context.drawBorder(px, py, pw, ph, BORDER);

        if (player != null) {
            int cx = px + pw / 2;
            context.enableScissor(px + 1, py + 1, px + pw - 1, py + ph - 16);
            InventoryScreen.drawEntity(context, cx, py + ph - 34, 56,
                    (float) (cx - mouseX), (float) (py + 40 - mouseY), player);
            context.disableScissor();
        }

        // legenda no estilo "`-- texto"
        Text caption;
        int captionColor = TEXT_DIM;
        if (!morphed) {
            caption = Text.translatable("screen.axymod.panel.hint");
            captionColor = WARN;
        } else if (current == Emote.NORMAL) {
            caption = Text.translatable("screen.axymod.panel.idle");
        } else {
            caption = Text.translatable("screen.axymod.panel.playing", current.displayName,
                    String.format(Locale.ROOT, "%.1f", EmoteManager.remainingSeconds(player)));
            captionColor = accent;
        }
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("`-- ").append(caption),
                px + pw / 2, py + ph - 12, captionColor);
    }

    private void drawButton(DrawContext c, Btn b, int mx, int my, Emote current, boolean morphed) {
        boolean enabled = b.enabled.getAsBoolean();
        boolean hover = enabled && b.contains(mx, my);
        int textY = b.y + (b.h - 8) / 2 + 1;

        switch (b.kind) {
            case EMOTE -> {
                int accent = ACCENTS[b.emote.ordinal()];
                boolean playing = morphed && current == b.emote;
                c.fill(b.x, b.y, b.x + b.w, b.y + b.h, CELL);
                if (hover) {
                    c.fillGradient(b.x, b.y, b.x + b.w, b.y + b.h, (accent & 0x00FFFFFF) | 0x10000000,
                            (accent & 0x00FFFFFF) | 0x40000000);
                }
                int border = hover || playing ? accent : (enabled ? BORDER : 0xFF18181C);
                c.drawBorder(b.x, b.y, b.w, b.h, border);
                if (playing) c.fill(b.x, b.y, b.x + 2, b.y + b.h, accent);

                // rostinho da skin do emote
                if (!enabled) c.setShaderColor(1f, 1f, 1f, 0.35f);
                c.drawTexture(b.emote.texture, b.x + 6, b.y + (b.h - 16) / 2, 16, 16, 8f, 8f, 8, 8, 64, 64);
                c.setShaderColor(1f, 1f, 1f, 1f);

                String label = b.label.getString().toUpperCase(Locale.ROOT);
                int color = !enabled ? TEXT_OFF : (hover || playing ? accent : TEXT);
                c.drawCenteredTextWithShadow(this.textRenderer, label, b.x + 26 + (b.w - 30) / 2, textY, color);
            }
            case PRIMARY -> {
                c.fill(b.x, b.y, b.x + b.w, b.y + b.h, hover ? BLUE_HOVER : BLUE);
                c.fill(b.x, b.y, b.x + b.w, b.y + 1, 0x33FFFFFF);
                // bolinha de estado (verde = transformado)
                int dot = isMorphed() ? GREEN : 0xFFFFFFFF;
                c.fill(b.x + 9, b.y + b.h / 2 - 3, b.x + 15, b.y + b.h / 2 + 3, 0x66000000);
                c.fill(b.x + 10, b.y + b.h / 2 - 2, b.x + 14, b.y + b.h / 2 + 2, dot);
                String label = toggleText().getString().toUpperCase(Locale.ROOT);
                c.drawCenteredTextWithShadow(this.textRenderer, label, b.x + b.w / 2, textY, 0xFFFFFFFF);
            }
            case GHOST -> {
                c.fill(b.x, b.y, b.x + b.w, b.y + b.h, CELL);
                c.drawBorder(b.x, b.y, b.w, b.h, hover ? 0xFFFFFFFF : BORDER);
                String label = b.label.getString().toUpperCase(Locale.ROOT);
                c.drawCenteredTextWithShadow(this.textRenderer, label, b.x + b.w / 2, textY, hover ? 0xFFFFFFFF : TEXT_DIM);
            }
        }
    }

    private void drawFooter(DrawContext context, int footerY) {
        context.drawTexture(LOGO_12, left, footerY + 4, 12, 12, 0f, 0f, 12, 12, 12, 12);
        context.drawTextWithShadow(this.textRenderer, "SCUTE AI", left + 17, footerY + 6, TEXT_DIM);

        String key = AxyModClient.PANEL_KEY != null ? AxyModClient.PANEL_KEY.getBoundKeyLocalizedText().getString() : "G";
        String hint = "[" + key + "]";
        context.drawTextWithShadow(this.textRenderer, hint, left + totalW - this.textRenderer.getWidth(hint), footerY + 6, TEXT_DIM);
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (Btn b : buttons) {
                if (b.enabled.getAsBoolean() && b.contains(mouseX, mouseY)) {
                    if (this.client != null) {
                        this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                    }
                    b.action.run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (AxyModClient.PANEL_KEY != null && AxyModClient.PANEL_KEY.matchesKey(keyCode, scanCode)) {
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false; // o mundo continua rodando para a animação tocar
    }
}
