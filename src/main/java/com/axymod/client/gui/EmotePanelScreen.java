package com.axymod.client.gui;

import java.util.ArrayList;
import java.util.List;

import com.axymod.Emote;
import com.axymod.client.AxyConfig;
import com.axymod.client.AxyModClient;
import com.axymod.client.EmoteManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

/**
 * Painel de emotes: lista todos pelo nome (com o rostinho da skin), mostra uma prévia
 * do seu personagem tocando o emote e tem um botão para ligar/desligar o visual Axy.
 */
public class EmotePanelScreen extends Screen {
    private static final int BTN_W = 120;
    private static final int BTN_H = 24;
    private static final int GAP = 4;
    private static final int PREVIEW_W = 110;
    private static final int PAD = 10;
    private static final int TITLE_H = 22;
    private static final int COLS = 2;

    private final List<ButtonWidget> emoteButtons = new ArrayList<>();
    private int left;
    private int top;
    private int totalW;
    private int totalH;

    public EmotePanelScreen() {
        super(Text.translatable("screen.axymod.panel.title"));
    }

    @Override
    protected void init() {
        emoteButtons.clear();
        Emote[] emotes = Emote.values();
        int rows = (emotes.length + COLS - 1) / COLS;
        int gridW = BTN_W * COLS + GAP * (COLS - 1);
        int gridH = rows * BTN_H + (rows - 1) * GAP;

        totalW = PREVIEW_W + PAD + gridW;
        totalH = TITLE_H + gridH + GAP + BTN_H + GAP + BTN_H;
        left = (this.width - totalW) / 2;
        top = Math.max(12, (this.height - totalH) / 2);

        int gx = left + PREVIEW_W + PAD;
        int gy = top + TITLE_H;

        for (int i = 0; i < emotes.length; i++) {
            Emote emote = emotes[i];
            int col = i % COLS;
            int row = i / COLS;
            ButtonWidget button = ButtonWidget.builder(Text.literal(emote.displayName), b -> AxyModClient.requestEmote(emote))
                    .dimensions(gx + col * (BTN_W + GAP), gy + row * (BTN_H + GAP), BTN_W, BTN_H)
                    .build();
            emoteButtons.add(this.addDrawableChild(button));
        }

        int by = gy + gridH + GAP;
        this.addDrawableChild(ButtonWidget.builder(toggleText(), b -> {
            AxyConfig.enabled = !AxyConfig.enabled;
            AxyConfig.save();
            b.setMessage(toggleText());
        }).dimensions(gx, by, gridW, BTN_H).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> this.close())
                .dimensions(gx, by + BTN_H + GAP, gridW, BTN_H)
                .build());
    }

    private static Text toggleText() {
        return Text.translatable(AxyConfig.enabled ? "screen.axymod.panel.mode_on" : "screen.axymod.panel.mode_off");
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);

        // fundo do painel
        context.fill(left - 8, top - 8, left + totalW + 8, top + totalH + 8, 0xC0101018);
        // área da prévia
        int pvTop = top + TITLE_H;
        int pvBottom = top + totalH;
        context.fill(left, pvTop, left + PREVIEW_W, pvBottom, 0x80000000);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, top + 6, 0xFFFFFF);

        super.render(context, mouseX, mouseY, delta);

        // rostinho da skin de cada emote em cima do botão
        Emote[] emotes = Emote.values();
        for (int i = 0; i < emoteButtons.size(); i++) {
            ButtonWidget b = emoteButtons.get(i);
            context.drawTexture(emotes[i].texture, b.getX() + 5, b.getY() + 4, 16, 16, 8f, 8f, 8, 8, 64, 64);
        }

        // prévia do personagem tocando o emote
        ClientPlayerEntity player = this.client == null ? null : this.client.player;
        if (player != null) {
            int cx = left + PREVIEW_W / 2;
            InventoryScreen.drawEntity(context, cx, pvBottom - 30, 60, (float) (cx - mouseX), (float) (pvTop + 40 - mouseY), player);

            Emote current = EmoteManager.current(player, player.age + delta);
            Text status = current == Emote.NORMAL
                    ? Text.translatable("screen.axymod.panel.idle")
                    : Text.translatable("screen.axymod.panel.playing", current.displayName,
                            String.format("%.1f", EmoteManager.remainingSeconds(player)));
            context.drawCenteredTextWithShadow(this.textRenderer, status, cx, pvBottom - 16, 0xFFFFFF);
        }
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
