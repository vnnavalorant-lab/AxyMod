package com.axymod;

import java.util.HashSet;
import java.util.Set;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parte comum (servidor + integrado). Só retransmite os emotes para os jogadores
 * próximos, para que todo mundo veja a skin/animação do emote.
 */
public class AxyMod implements ModInitializer {
    public static final String MOD_ID = "axymod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ServerPlayNetworking.registerGlobalReceiver(AxyNetworking.EMOTE_C2S,
                (server, player, handler, buf, responseSender) -> {
                    int ordinal = buf.readVarInt(); // ler antes de sair da thread de rede
                    server.execute(() -> broadcast(player, Emote.byId(ordinal)));
                });
        LOGGER.info("Axy Mod carregado!");
    }

    private static void broadcast(ServerPlayerEntity player, Emote emote) {
        Set<ServerPlayerEntity> targets = new HashSet<>(PlayerLookup.tracking(player));
        targets.add(player);
        for (ServerPlayerEntity target : targets) {
            if (!ServerPlayNetworking.canSend(target, AxyNetworking.EMOTE_S2C)) continue;
            PacketByteBuf out = PacketByteBufs.create();
            out.writeUuid(player.getUuid());
            out.writeVarInt(emote.ordinal());
            ServerPlayNetworking.send(target, AxyNetworking.EMOTE_S2C, out);
        }
    }
}
