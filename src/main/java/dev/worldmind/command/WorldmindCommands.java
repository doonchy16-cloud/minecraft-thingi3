package dev.worldmind.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.worldmind.ai.AIFeature;
import dev.worldmind.ai.AIProviderType;
import dev.worldmind.ai.WorldmindAIConfig;
import dev.worldmind.ai.WorldmindAIConfigLoader;
import dev.worldmind.ai.WorldmindAIManager;
import dev.worldmind.config.WorldmindConfigLoader;
import dev.worldmind.diagnostic.WorldmindDiagnostics;
import dev.worldmind.sim.WorldmindRuntime;
import dev.worldmind.state.WorldmindSavedData;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;

public final class WorldmindCommands {
    private static final WorldmindAdminService ADMIN = new WorldmindAdminService();
    private static final WorldmindAIAdminService AI_ADMIN = new WorldmindAIAdminService();
    private WorldmindCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(Commands.literal("worldmind")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
                .executes(context -> status(context.getSource()))
                .then(Commands.literal("status").executes(context -> status(context.getSource())))
                .then(Commands.literal("inspect").executes(context -> inspect(context.getSource())))
                .then(Commands.literal("advance")
                    .then(Commands.argument("days", IntegerArgumentType.integer(1, 365))
                        .executes(context -> advance(context.getSource(), IntegerArgumentType.getInteger(context, "days")))))
                .then(Commands.literal("materialize").executes(context -> materialize(context.getSource())))
                .then(Commands.literal("history").executes(context -> history(context.getSource())))
                .then(Commands.literal("debug")
                    .then(Commands.literal("on").executes(context -> debug(context.getSource(), true)))
                    .then(Commands.literal("off").executes(context -> debug(context.getSource(), false))))
                .then(aiTree())
            )
        );
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> aiTree() {
        return Commands.literal("ai")
                .executes(context -> aiStatus(context.getSource()))
                .then(Commands.literal("status").executes(context -> aiStatus(context.getSource())))
                .then(Commands.literal("enable").executes(context -> aiEnable(context.getSource(), true)))
                .then(Commands.literal("disable").executes(context -> aiEnable(context.getSource(), false)))
                .then(Commands.literal("provider")
                    .then(Commands.literal("builtin").executes(context -> aiProvider(context.getSource(), AIProviderType.BUILTIN)))
                    .then(Commands.literal("ollama").executes(context -> aiProvider(context.getSource(), AIProviderType.OLLAMA)))
                    .then(Commands.literal("forgey").executes(context -> aiProvider(context.getSource(), AIProviderType.FORGEY)))
                    .then(Commands.literal("compatible").executes(context -> aiProvider(context.getSource(), AIProviderType.COMPATIBLE))))
                .then(Commands.literal("endpoint")
                    .then(Commands.argument("url", StringArgumentType.greedyString())
                        .executes(context -> aiEndpoint(context.getSource(), StringArgumentType.getString(context, "url")))))
                .then(Commands.literal("model")
                    .then(Commands.argument("model", StringArgumentType.greedyString())
                        .executes(context -> aiModel(context.getSource(), StringArgumentType.getString(context, "model")))))
                .then(Commands.literal("test").executes(context -> aiTest(context.getSource())))
                .then(Commands.literal("reload").executes(context -> aiReload(context.getSource())))
                .then(Commands.literal("use")
                    .then(aiFeature("structure", AIFeature.STRUCTURE))
                    .then(aiFeature("transformation", AIFeature.TRANSFORMATION))
                    .then(aiFeature("civilization", AIFeature.CIVILIZATION))
                    .then(aiFeature("history", AIFeature.HISTORY))
                    .then(aiFeature("naming", AIFeature.NAMING)))
                .then(Commands.literal("ollama").executes(context -> aiOllama(context.getSource())))
                .then(Commands.literal("forgey")
                    .then(Commands.argument("endpoint", StringArgumentType.greedyString())
                        .executes(context -> aiForgey(context.getSource(), StringArgumentType.getString(context, "endpoint")))));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> aiFeature(
            String name, AIFeature feature) {
        return Commands.literal(name)
                .then(Commands.literal("on").executes(context -> aiFeatureSet(context.getSource(), feature, true)))
                .then(Commands.literal("off").executes(context -> aiFeatureSet(context.getSource(), feature, false)));
    }

    private static int status(CommandSourceStack source) {
        var state = WorldmindSavedData.get(source.getServer()).state();
        source.sendSuccess(() -> Component.literal(ADMIN.status(state)), false);
        return 1;
    }

    private static int inspect(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = source.getPlayerOrException();
        var state = WorldmindSavedData.get(source.getServer()).state();
        var pos = player.blockPosition();
        String dimension = player.level().dimension().identifier().toString();
        source.sendSuccess(() -> Component.literal(ADMIN.inspect(state, dimension, pos.getX(), pos.getZ())), false);
        return 1;
    }

    private static int advance(CommandSourceStack source, int days) {
        var saved = WorldmindSavedData.get(source.getServer());
        var result = ADMIN.advance(saved.state(), WorldmindConfigLoader.get().validated(), days);
        saved.changed();
        source.sendSuccess(() -> Component.literal("Advanced Worldmind " + days + "d: regions=" + result.regionsEvaluated()
                + " plans=" + result.regionalPlans() + " history=" + result.historyEvents()), true);
        return Math.max(1, result.regionsEvaluated());
    }

    private static int materialize(CommandSourceStack source) {
        int applied = WorldmindRuntime.materializeNow(source.getServer());
        source.sendSuccess(() -> Component.literal("Worldmind materialization applied " + applied + " plan(s) near players."), true);
        return Math.max(1, applied);
    }

    private static int history(CommandSourceStack source) {
        var state = WorldmindSavedData.get(source.getServer()).state();
        source.sendSuccess(() -> Component.literal(ADMIN.history(state, 8)), false);
        return 1;
    }

    private static int debug(CommandSourceStack source, boolean enabled) {
        WorldmindDiagnostics.setRuntimeDebug(enabled);
        source.sendSuccess(() -> Component.literal("Worldmind debug " + (enabled ? "ON" : "OFF")), true);
        return 1;
    }

    private static int aiStatus(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal(AI_ADMIN.status(WorldmindAIConfigLoader.get(), WorldmindAIManager.global().status())), false);
        return 1;
    }

    private static int aiEnable(CommandSourceStack source, boolean enabled) {
        return saveAI(source, AI_ADMIN.enable(WorldmindAIConfigLoader.get(), enabled),
                "Worldmind AI " + (enabled ? "enable requested" : "disabled"));
    }

    private static int aiProvider(CommandSourceStack source, AIProviderType provider) {
        return saveAI(source, AI_ADMIN.provider(WorldmindAIConfigLoader.get(), provider),
                "Worldmind AI provider set to " + provider);
    }

    private static int aiEndpoint(CommandSourceStack source, String endpoint) {
        WorldmindAIConfig next = AI_ADMIN.endpoint(WorldmindAIConfigLoader.get(), endpoint);
        return saveAI(source, next, next.endpoint().isEmpty()
                ? "Worldmind AI endpoint rejected; use a credential-free http:// or https:// URL"
                : "Worldmind AI endpoint set to " + next.endpoint());
    }

    private static int aiModel(CommandSourceStack source, String model) {
        WorldmindAIConfig next = AI_ADMIN.model(WorldmindAIConfigLoader.get(), model);
        return saveAI(source, next, "Worldmind AI model set to " + (next.model().isEmpty() ? "<unset>" : next.model()));
    }

    private static int aiFeatureSet(CommandSourceStack source, AIFeature feature, boolean enabled) {
        WorldmindAIConfig next = AI_ADMIN.feature(WorldmindAIConfigLoader.get(), feature, enabled);
        return saveAI(source, next, "Worldmind AI use " + feature.name().toLowerCase(java.util.Locale.ROOT)
                + " " + (enabled ? "ON" : "OFF"));
    }

    private static int aiOllama(CommandSourceStack source) {
        WorldmindAIConfig next = AI_ADMIN.ollama(WorldmindAIConfigLoader.get());
        return saveAI(source, next, "Worldmind AI configured for Ollama at " + next.endpoint());
    }

    private static int aiForgey(CommandSourceStack source, String endpoint) {
        WorldmindAIConfig next = AI_ADMIN.forgey(WorldmindAIConfigLoader.get(), endpoint);
        return saveAI(source, next, next.endpoint().isEmpty()
                ? "Worldmind Forgey endpoint rejected"
                : "Worldmind AI configured for Forgey at " + next.endpoint());
    }

    private static int aiReload(CommandSourceStack source) {
        WorldmindAIConfig reloaded = WorldmindAIConfigLoader.reload();
        WorldmindAIManager.global().reconfigure(reloaded);
        source.sendSuccess(() -> Component.literal("Worldmind AI config reloaded. "
                + AI_ADMIN.status(reloaded, WorldmindAIManager.global().status())), true);
        return 1;
    }

    private static int aiTest(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Worldmind AI connection test started asynchronously..."), false);
        var server = source.getServer();
        WorldmindAIManager.global().testConnection().thenAccept(result -> server.execute(() -> {
            if (result.success()) {
                source.sendSuccess(() -> Component.literal("Worldmind AI CONNECTED: provider="
                        + WorldmindAIManager.global().status().provider() + " latency=" + result.latencyMillis() + "ms"), false);
            } else {
                source.sendFailure(Component.literal("Worldmind AI test failed: " + result.category()
                        + (result.detail().isEmpty() ? "" : " (" + result.detail() + ")")));
            }
        }));
        return 1;
    }

    private static int saveAI(CommandSourceStack source, WorldmindAIConfig next, String message) {
        WorldmindAIConfig saved = WorldmindAIConfigLoader.save(next);
        WorldmindAIManager.global().reconfigure(saved);
        source.sendSuccess(() -> Component.literal(message + ". " + AI_ADMIN.status(saved, WorldmindAIManager.global().status())), true);
        return 1;
    }
}
