package com.axymod.client;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.entity.player.PlayerEntity;

/** Quem está transformado na Axy (lado do cliente). Quem não está aqui usa o modelo vanilla. */
public final class MorphManager {
    private static final Set<UUID> MORPHED = ConcurrentHashMap.newKeySet();

    public static void set(UUID uuid, boolean morphed) {
        if (morphed) MORPHED.add(uuid);
        else MORPHED.remove(uuid);
    }

    public static boolean isMorphed(PlayerEntity player) {
        return player != null && MORPHED.contains(player.getUuid());
    }

    public static void clear() {
        MORPHED.clear();
    }

    private MorphManager() {}
}
