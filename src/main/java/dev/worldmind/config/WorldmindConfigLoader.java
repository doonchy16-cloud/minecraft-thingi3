package dev.worldmind.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import dev.worldmind.WorldmindMod;

public final class WorldmindConfigLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("worldmind.json");
    private static WorldmindConfig config = WorldmindConfig.defaults();

    private WorldmindConfigLoader() {}

    public static WorldmindConfig get() { return config; }

    public static void load() {
        try {
            if (Files.exists(PATH)) {
                WorldmindConfig read = GSON.fromJson(Files.readString(PATH), WorldmindConfig.class);
                config = (read == null ? WorldmindConfig.defaults() : read).validated();
            } else {
                config = WorldmindConfig.defaults();
            }
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(config));
        } catch (IOException | RuntimeException e) {
            config = WorldmindConfig.defaults();
            WorldmindMod.LOGGER.warn("Could not load Worldmind config; using safe defaults", e);
        }
    }
}
