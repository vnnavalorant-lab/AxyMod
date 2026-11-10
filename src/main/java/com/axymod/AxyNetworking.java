package com.axymod;

import net.minecraft.util.Identifier;

/** Identificadores dos pacotes de rede (cliente -> servidor e servidor -> clientes). */
public final class AxyNetworking {
    /** Cliente pede para tocar um emote: [varint ordinal]. */
    public static final Identifier EMOTE_C2S = new Identifier(AxyMod.MOD_ID, "emote_c2s");
    /** Servidor avisa quem está por perto: [uuid do jogador][varint ordinal]. */
    public static final Identifier EMOTE_S2C = new Identifier(AxyMod.MOD_ID, "emote_s2c");

    /** Servidor avisa todos os clientes quem está transformado na Axy: [uuid do jogador][boolean transformado]. */
    public static final Identifier MORPH_S2C = new Identifier(AxyMod.MOD_ID, "morph_s2c");

    /** Cliente escolhe a skin/cor sem tocar emote: [varint ordinal]. */
    public static final Identifier SKIN_C2S = new Identifier(AxyMod.MOD_ID, "skin_c2s");
    /** Servidor avisa todos os clientes a skin escolhida: [uuid do jogador][varint ordinal]. */
    public static final Identifier SKIN_S2C = new Identifier(AxyMod.MOD_ID, "skin_s2c");

    private AxyNetworking() {}
}
