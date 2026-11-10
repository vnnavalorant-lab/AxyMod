package com.axymod;

import net.minecraft.util.Identifier;

/**
 * Os emotes do Axy. Cada emote tem uma skin própria (expressão do rosto) e uma
 * animação procedural (ver {@code AxyAnimator}). A ordem do enum é usada na rede.
 */
public enum Emote {
    NORMAL("normal", "Normal", 0),
    CONFUSED("confused", "Confused", 100),
    EXCITED("excited", "Excited", 100),
    MAD("mad", "Mad", 100),
    SAD("sad", "Sad", 120),
    SCARED("scared", "Scared", 100),
    SICK("sick", "Sick", 120),
    SILLY("silly", "Silly", 100);

    public final String id;
    public final String displayName;
    /** Duração em ticks (20 ticks = 1 segundo). 0 = pose padrão, sem duração. */
    public final int duration;
    public final Identifier texture;

    Emote(String id, String displayName, int duration) {
        this.id = id;
        this.displayName = displayName;
        this.duration = duration;
        this.texture = new Identifier(AxyMod.MOD_ID, "textures/skins/axy_" + id + ".png");
    }

    public static Emote byId(int ordinal) {
        Emote[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : NORMAL;
    }
}
