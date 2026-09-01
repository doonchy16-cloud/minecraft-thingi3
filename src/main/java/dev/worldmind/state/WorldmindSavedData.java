package dev.worldmind.state;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import dev.worldmind.WorldmindMod;

public final class WorldmindSavedData extends SavedData {
    private static final Gson GSON = new GsonBuilder().create();
    private static final Codec<WorldmindSavedData> CODEC = Codec.STRING.xmap(
            WorldmindSavedData::fromJson,
            WorldmindSavedData::toJson);
    private static final SavedDataType<WorldmindSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(WorldmindMod.MOD_ID, "state"),
            WorldmindSavedData::new,
            CODEC,
            null);

    private WorldmindState state;

    public WorldmindSavedData() { this(new WorldmindState()); }
    private WorldmindSavedData(WorldmindState state) { this.state = state == null ? new WorldmindState() : state; }

    public WorldmindState state() { return state; }
    public void changed() { setDirty(); }

    public static WorldmindSavedData get(MinecraftServer server) {
        ServerLevel level = server.getLevel(ServerLevel.OVERWORLD);
        if (level == null) return new WorldmindSavedData();
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    private static WorldmindSavedData fromJson(String json) {
        try {
            WorldmindState decoded = GSON.fromJson(json, WorldmindState.class);
            return new WorldmindSavedData(decoded == null ? new WorldmindState() : decoded);
        } catch (RuntimeException ignored) {
            return new WorldmindSavedData();
        }
    }

    private String toJson() { return GSON.toJson(state); }
}
