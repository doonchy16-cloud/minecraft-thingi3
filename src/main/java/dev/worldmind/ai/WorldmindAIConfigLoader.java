package dev.worldmind.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.worldmind.WorldmindMod;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.UnaryOperator;
import net.fabricmc.loader.api.FabricLoader;

public final class WorldmindAIConfigLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("worldmind-ai.json");
    private static volatile WorldmindAIConfig config = WorldmindAIConfig.defaults();

    private WorldmindAIConfigLoader() {}

    public static WorldmindAIConfig get() { return config; }

    public static synchronized WorldmindAIConfig load() {
        try {
            if (Files.exists(PATH)) {
                WorldmindAIConfig read = GSON.fromJson(Files.readString(PATH), WorldmindAIConfig.class);
                config = (read == null ? WorldmindAIConfig.defaults() : read).validated();
            } else {
                config = WorldmindAIConfig.defaults();
            }
            saveInternal(config);
        } catch (IOException | RuntimeException e) {
            config = WorldmindAIConfig.defaults();
            WorldmindMod.LOGGER.warn("Could not load Worldmind AI config; using safe disabled defaults", e);
        }
        return config;
    }

    public static synchronized WorldmindAIConfig reload() { return load(); }

    public static synchronized WorldmindAIConfig save(WorldmindAIConfig next) {
        config = (next == null ? WorldmindAIConfig.defaults() : next).validated();
        try { saveInternal(config); }
        catch (IOException e) { WorldmindMod.LOGGER.warn("Could not save Worldmind AI config", e); }
        return config;
    }

    public static synchronized WorldmindAIConfig update(UnaryOperator<WorldmindAIConfig> updater) {
        WorldmindAIConfig current = config;
        WorldmindAIConfig next = updater == null ? current : updater.apply(current);
        return save(next);
    }

    private static void saveInternal(WorldmindAIConfig value) throws IOException {
        Files.createDirectories(PATH.getParent());
        Files.writeString(PATH, GSON.toJson(value));
    }
}
