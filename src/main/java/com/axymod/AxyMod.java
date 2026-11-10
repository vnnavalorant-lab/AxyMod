package com.axymod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parte comum (servidor + integrado):
 * - retransmite os emotes para os jogadores próximos;
 * - guarda quem está transformado na Axy (comandos /axy morph e /axy unmorph) e avisa todos os clientes.
 * Quem não usou /axy morph continua com o modelo vanilla normal.
 */
public class AxyMod implements ModInitializer {
    public static final String MOD_ID = "axymod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Jogadores transformados (salvo por mundo em axymod_morphed.txt). */
    private static final Set<UUID> MORPHED = ConcurrentHashMap.newKeySet();
    private static Path saveFile;

    /** Skin escolhida por jogador (ordinal do Emote; salvo em axymod_skins.txt). Ausente = normal. */
    private static final Map<UUID, Integer> SKINS = new ConcurrentHashMap<>();
    private static Path skinsFile;

    @Override
    public void onInitialize() {
        ServerPlayNetworking.registerGlobalReceiver(AxyNetworking.EMOTE_C2S,
                (server, player, handler, buf, responseSender) -> {
                    int ordinal = buf.readVarInt(); // ler antes de sair da thread de rede
                    server.execute(() -> broadcastEmote(player, Emote.byId(ordinal)));
                });

        ServerPlayNetworking.registerGlobalReceiver(AxyNetworking.SKIN_C2S,
                (server, player, handler, buf, responseSender) -> {
                    int ordinal = buf.readVarInt();
                    server.execute(() -> setSkin(server, player, Emote.byId(ordinal)));
                });

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            loadMorphed(server);
            loadSkins(server);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            MORPHED.clear();
            saveFile = null;
            SKINS.clear();
            skinsFile = null;
        });

        // quem entra recebe a lista de quem já está transformado
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity joined = handler.getPlayer();
            for (UUID id : MORPHED) {
                sendMorph(joined, id, true);
            }
            // skins escolhidas: quem entra recebe as de todos, e todos recebem a de quem entrou
            for (Map.Entry<UUID, Integer> e : SKINS.entrySet()) {
                sendSkin(joined, e.getKey(), e.getValue());
            }
            Integer own = SKINS.get(joined.getUuid());
            if (own != null) broadcastSkin(server, joined.getUuid(), own);
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("axy")
                        .then(CommandManager.literal("morph")
                                .executes(ctx -> run(ctx, null, true))
                                .then(CommandManager.argument("jogadores", EntityArgumentType.players())
                                        .requires(src -> src.hasPermissionLevel(2))
                                        .executes(ctx -> run(ctx, EntityArgumentType.getPlayers(ctx, "jogadores"), true))))
                        .then(CommandManager.literal("unmorph")
                                .executes(ctx -> run(ctx, null, false))
                                .then(CommandManager.argument("jogadores", EntityArgumentType.players())
                                        .requires(src -> src.hasPermissionLevel(2))
                                        .executes(ctx -> run(ctx, EntityArgumentType.getPlayers(ctx, "jogadores"), false))))));

        LOGGER.info("Axy Mod carregado!");
    }

    // ------------------------------------------------------------------ comandos

    private static int run(CommandContext<ServerCommandSource> ctx, Collection<ServerPlayerEntity> targets, boolean morph)
            throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();
        Collection<ServerPlayerEntity> list = targets != null ? targets : List.of(source.getPlayerOrThrow());
        MinecraftServer server = source.getServer();

        for (ServerPlayerEntity target : list) {
            if (morph) MORPHED.add(target.getUuid());
            else MORPHED.remove(target.getUuid());
            broadcastMorph(server, target.getUuid(), morph);
        }
        saveMorphed();

        String key = (morph ? "command.axymod.morph" : "command.axymod.unmorph") + (targets == null ? ".self" : ".others");
        int count = list.size();
        source.sendFeedback(() -> targets == null ? Text.translatable(key) : Text.translatable(key, count), false);
        return count;
    }

    // ------------------------------------------------------------------ rede

    private static void broadcastMorph(MinecraftServer server, UUID id, boolean morphed) {
        for (ServerPlayerEntity target : server.getPlayerManager().getPlayerList()) {
            sendMorph(target, id, morphed);
        }
    }

    private static void sendMorph(ServerPlayerEntity target, UUID id, boolean morphed) {
        if (!ServerPlayNetworking.canSend(target, AxyNetworking.MORPH_S2C)) return;
        PacketByteBuf out = PacketByteBufs.create();
        out.writeUuid(id);
        out.writeBoolean(morphed);
        ServerPlayNetworking.send(target, AxyNetworking.MORPH_S2C, out);
    }

    private static void sendSkin(ServerPlayerEntity target, UUID id, int ordinal) {
        if (!ServerPlayNetworking.canSend(target, AxyNetworking.SKIN_S2C)) return;
        PacketByteBuf out = PacketByteBufs.create();
        out.writeUuid(id);
        out.writeVarInt(ordinal);
        ServerPlayNetworking.send(target, AxyNetworking.SKIN_S2C, out);
    }

    private static void broadcastSkin(MinecraftServer server, UUID id, int ordinal) {
        for (ServerPlayerEntity target : server.getPlayerManager().getPlayerList()) {
            sendSkin(target, id, ordinal);
        }
    }

    /** Muda a skin/cor do jogador sem tocar emote. Só quem está transformado na Axy pode. */
    private static void setSkin(MinecraftServer server, ServerPlayerEntity player, Emote skin) {
        if (!MORPHED.contains(player.getUuid())) return;
        if (skin == Emote.NORMAL) SKINS.remove(player.getUuid());
        else SKINS.put(player.getUuid(), skin.ordinal());
        saveSkins();
        broadcastSkin(server, player.getUuid(), skin.ordinal());
    }

    private static void broadcastEmote(ServerPlayerEntity player, Emote emote) {
        // só quem está transformado toca emotes
        if (!MORPHED.contains(player.getUuid())) return;
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

    // ------------------------------------------------------------------ persistência

    private static void loadMorphed(MinecraftServer server) {
        MORPHED.clear();
        saveFile = server.getSavePath(WorldSavePath.ROOT).resolve("axymod_morphed.txt");
        if (!Files.exists(saveFile)) return;
        try {
            for (String line : Files.readAllLines(saveFile)) {
                line = line.trim();
                if (line.isEmpty()) continue;
                try {
                    MORPHED.add(UUID.fromString(line));
                } catch (IllegalArgumentException ignored) {
                    // linha inválida
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Não consegui ler a lista de jogadores transformados", e);
        }
    }

    private static void loadSkins(MinecraftServer server) {
        SKINS.clear();
        skinsFile = server.getSavePath(WorldSavePath.ROOT).resolve("axymod_skins.txt");
        if (!Files.exists(skinsFile)) return;
        try {
            for (String line : Files.readAllLines(skinsFile)) {
                String[] parts = line.trim().split("=");
                if (parts.length != 2) continue;
                try {
                    SKINS.put(UUID.fromString(parts[0].trim()), Integer.parseInt(parts[1].trim()));
                } catch (IllegalArgumentException ignored) {
                    // linha inválida
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Não consegui ler as skins escolhidas", e);
        }
    }

    private static void saveSkins() {
        if (skinsFile == null) return;
        try {
            List<String> lines = SKINS.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toList();
            Files.write(skinsFile, lines);
        } catch (IOException e) {
            LOGGER.warn("Não consegui salvar as skins escolhidas", e);
        }
    }

    private static void saveMorphed() {
        if (saveFile == null) return;
        try {
            List<String> lines = MORPHED.stream().map(UUID::toString).toList();
            Files.write(saveFile, lines);
        } catch (IOException e) {
            LOGGER.warn("Não consegui salvar a lista de jogadores transformados", e);
        }
    }
}
