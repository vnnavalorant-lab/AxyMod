package com.axymod.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.axymod.Emote;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/** Guarda qual emote cada jogador está tocando (lado do cliente). */
public final class EmoteManager {
    private record Active(Emote emote, int startAge) {}

    private static final Map<UUID, Active> ACTIVE = new HashMap<>();

    public static void play(UUID uuid, Emote emote) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;
        PlayerEntity player = mc.world.getPlayerByUuid(uuid);
        if (player == null) return;
        if (emote == Emote.NORMAL) {
            ACTIVE.remove(uuid);
        } else {
            ACTIVE.put(uuid, new Active(emote, player.age));
        }
    }

    public static void clear() {
        ACTIVE.clear();
    }

    /** Emote atual do jogador; {@code progress} = idade do jogador + tickDelta. */
    public static Emote current(PlayerEntity player, float progress) {
        Active a = ACTIVE.get(player.getUuid());
        if (a == null) return Emote.NORMAL;
        if (progress - a.startAge() >= a.emote().duration) {
            ACTIVE.remove(player.getUuid());
            return Emote.NORMAL;
        }
        return a.emote();
    }

    /** Ticks desde o início do emote. */
    public static float elapsed(PlayerEntity player, float progress) {
        Active a = ACTIVE.get(player.getUuid());
        return a == null ? 0f : Math.max(0f, progress - a.startAge());
    }

    /** Fade-in/fade-out suave (0..1) para as animações. */
    public static float envelope(float elapsed, int duration) {
        return MathHelper.clamp(Math.min(elapsed, duration - elapsed) / 6f, 0f, 1f);
    }

    /** Segundos restantes do emote (para a UI). */
    public static float remainingSeconds(PlayerEntity player) {
        Active a = ACTIVE.get(player.getUuid());
        if (a == null) return 0f;
        return Math.max(0f, (a.emote().duration - (player.age - a.startAge())) / 20f);
    }

    /** Skin que vale agora: a do emote que está tocando; senão a skin escolhida no painel. */
    public static Emote skinEmote(PlayerEntity player) {
        Emote playing = current(player, player.age);
        return playing != Emote.NORMAL ? playing : SkinManager.chosen(player);
    }

    public static Identifier skinFor(PlayerEntity player) {
        return skinEmote(player).texture;
    }

    private EmoteManager() {}
}
