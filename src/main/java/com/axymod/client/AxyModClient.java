package com.axymod.client;

import java.util.UUID;

import com.axymod.AxyNetworking;
import com.axymod.Emote;
import com.axymod.client.gui.EmotePanelScreen;
import com.axymod.client.render.AxyPartsFeatureRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import org.lwjgl.glfw.GLFW;

public class AxyModClient implements ClientModInitializer {
    /** Tecla do painel de emotes (padrão: G). */
    public static KeyBinding PANEL_KEY;

    @Override
    public void onInitializeClient() {
        PANEL_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.axymod.panel", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "category.axymod"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (PANEL_KEY.wasPressed()) {
                if (client.player != null && client.currentScreen == null) {
                    client.setScreen(new EmotePanelScreen());
                }
            }
        });

        // emote de outro jogador (ou o nosso, devolvido pelo servidor)
        ClientPlayNetworking.registerGlobalReceiver(AxyNetworking.EMOTE_S2C,
                (client, handler, buf, responseSender) -> {
                    UUID uuid = buf.readUuid();
                    int ordinal = buf.readVarInt();
                    client.execute(() -> EmoteManager.play(uuid, Emote.byId(ordinal)));
                });

        // quem está (ou deixou de estar) transformado na Axy
        ClientPlayNetworking.registerGlobalReceiver(AxyNetworking.MORPH_S2C,
                (client, handler, buf, responseSender) -> {
                    UUID uuid = buf.readUuid();
                    boolean morphed = buf.readBoolean();
                    client.execute(() -> MorphManager.set(uuid, morphed));
                });

        // skin/cor escolhida (sem emote) de algum jogador
        ClientPlayNetworking.registerGlobalReceiver(AxyNetworking.SKIN_S2C,
                (client, handler, buf, responseSender) -> {
                    UUID uuid = buf.readUuid();
                    int ordinal = buf.readVarInt();
                    client.execute(() -> SkinManager.set(uuid, Emote.byId(ordinal)));
                });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            EmoteManager.clear();
            MorphManager.clear();
            SkinManager.clear();
        });

        // cauda e guelras no jogador
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
            if (entityRenderer instanceof PlayerEntityRenderer) {
                registrationHelper.register(new AxyPartsFeatureRenderer((PlayerEntityRenderer) entityRenderer));
            }
        });
    }

    /** Escolhe a skin/cor do jogador local SEM tocar emote (via servidor; sem servidor com o mod, só local). */
    public static void requestSkin(Emote skin) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || !MorphManager.isMorphed(mc.player)) return;
        if (ClientPlayNetworking.canSend(AxyNetworking.SKIN_C2S)) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeVarInt(skin.ordinal());
            ClientPlayNetworking.send(AxyNetworking.SKIN_C2S, buf);
        } else {
            SkinManager.set(mc.player.getUuid(), skin);
        }
    }

    /** Toca um emote no jogador local (via servidor, para os outros verem; sem servidor com o mod, só local). */
    public static void requestEmote(Emote emote) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || !MorphManager.isMorphed(mc.player)) return;
        if (ClientPlayNetworking.canSend(AxyNetworking.EMOTE_C2S)) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeVarInt(emote.ordinal());
            ClientPlayNetworking.send(AxyNetworking.EMOTE_C2S, buf);
        } else {
            EmoteManager.play(mc.player.getUuid(), emote);
        }
    }
}
