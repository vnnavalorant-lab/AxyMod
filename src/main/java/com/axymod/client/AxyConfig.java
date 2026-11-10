package com.axymod.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import com.axymod.AxyMod;
import net.fabricmc.loader.api.FabricLoader;

/** Configuração simples (config/axymod.properties). */
public final class AxyConfig {
    /** Liga/desliga o visual Axy (proporções, skins, cauda e guelras) para todos os jogadores. */
    public static boolean enabled = true;

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("axymod.properties");
    }

    public static void load() {
        Path f = file();
        if (!Files.exists(f)) return;
        try (InputStream in = Files.newInputStream(f)) {
            Properties p = new Properties();
            p.load(in);
            enabled = Boolean.parseBoolean(p.getProperty("enabled", "true"));
        } catch (IOException e) {
            AxyMod.LOGGER.warn("Não consegui ler a config do Axy Mod", e);
        }
    }

    public static void save() {
        try (OutputStream out = Files.newOutputStream(file())) {
            Properties p = new Properties();
            p.setProperty("enabled", Boolean.toString(enabled));
            p.store(out, "Axy Mod");
        } catch (IOException e) {
            AxyMod.LOGGER.warn("Não consegui salvar a config do Axy Mod", e);
        }
    }

    private AxyConfig() {}
}
