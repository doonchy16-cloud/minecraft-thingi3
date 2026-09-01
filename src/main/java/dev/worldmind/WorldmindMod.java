package dev.worldmind;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.worldmind.config.WorldmindConfigLoader;
import dev.worldmind.ai.WorldmindAIConfigLoader;
import dev.worldmind.ai.WorldmindAIManager;
import dev.worldmind.command.WorldmindCommands;
import dev.worldmind.content.WorldmindBlocks;
import dev.worldmind.sim.WorldmindRuntime;

public final class WorldmindMod implements ModInitializer {
    public static final String MOD_ID = "worldmind";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        WorldmindBlocks.initialize();
        WorldmindConfigLoader.load();
        WorldmindAIManager.global().reconfigure(WorldmindAIConfigLoader.load());
        WorldmindCommands.register();
        ServerTickEvents.END_SERVER_TICK.register(WorldmindRuntime::tick);
        ServerLifecycleEvents.SERVER_STOPPING.register(WorldmindRuntime::onServerStopping);
        LOGGER.info("Worldmind 1.2.0 initialized. The world is listening.");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
