package com.axymod.client;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.axymod.Emote;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Skin (cor + rostinho) escolhida de cada jogador, SEM tocar o emote (lado do cliente).
 * Usa o mesmo conjunto de skins dos emotes. Enquanto um emote toca, a skin dele tem prioridade;
 * quando ele termina, volta para a skin escolhida aqui.
 */
public final class SkinManager {
    private static final Map<UUID, Emote> CHOSEN = new ConcurrentHashMap<>();

    public static void set(UUID uuid, Emote skin) {
        if (skin == Emote.NORMAL) CHOSEN.remove(uuid);
        else CHOSEN.put(uuid, skin);
    }

    public static Emote chosen(UUID uuid) {
        return CHOSEN.getOrDefault(uuid, Emote.NORMAL);
    }

    public static Emote chosen(PlayerEntity player) {
        return player == null ? Emote.NORMAL : chosen(player.getUuid());
    }

    public static void clear() {
        CHOSEN.clear();
    }

    private SkinManager() {}
}
