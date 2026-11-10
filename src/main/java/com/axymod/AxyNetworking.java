package com.axymod;

import net.minecraft.util.Identifier;

/** Identificadores dos pacotes de rede (cliente -> servidor e servidor -> clientes). */
public final class AxyNetworking {
    /** Cliente pede para tocar um emote: [varint ordinal]. */
    public static final Identifier EMOTE_C2S = new Identifier(AxyMod.MOD_ID, "emote_c2s");
    /** Servidor avisa quem está por perto: [uuid do jogador][varint ordinal]. */
    public static final Identifier EMOTE_S2C = new Identifier(AxyMod.MOD_ID, "emote_s2c");

    private AxyNetworking() {}
}
