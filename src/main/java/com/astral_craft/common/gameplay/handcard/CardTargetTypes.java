package com.astral_craft.common.gameplay.handcard;

import com.astral_craft.AstralCraft;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class CardTargetTypes {

    public static final int MAX_TARGET_TYPES = 16;
    public static final List<Class<? extends LivingEntity>> NONE = List.of();
    public static final List<Class<? extends LivingEntity>> LIVING_ENTITIES = List.of(LivingEntity.class);
    public static final List<Class<? extends LivingEntity>> PLAYERS = List.of(Player.class);
    public static final List<Class<? extends LivingEntity>> MOBS = List.of(Mob.class);
    public static final List<Class<? extends LivingEntity>> PLAYERS_AND_MOBS = List.of(Player.class, Mob.class);

    private static final Map<Identifier, Class<? extends LivingEntity>> TYPES_BY_ID = new LinkedHashMap<>();
    private static final Map<Class<? extends LivingEntity>, Identifier> IDS_BY_TYPE = new LinkedHashMap<>();
    private static final Map<String, List<Class<? extends LivingEntity>>> LEGACY_TYPES = Map.ofEntries(
            Map.entry("none", NONE),
            Map.entry("self", NONE),
            Map.entry("panel", NONE),
            Map.entry("choice", NONE),
            Map.entry("ally", PLAYERS),
            Map.entry("enemy_player", PLAYERS),
            Map.entry("any_player", PLAYERS_AND_MOBS),
            Map.entry("two_players", PLAYERS_AND_MOBS),
            Map.entry("monster", MOBS));

    private static final Codec<Class<? extends LivingEntity>> TARGET_TYPE_CODEC = Identifier.CODEC.flatXmap(CardTargetTypes::decode, CardTargetTypes::encode);
    public static final Codec<List<Class<? extends LivingEntity>>> CODEC = TARGET_TYPE_CODEC.sizeLimitedListOf(MAX_TARGET_TYPES).xmap(CardTargetTypes::copyOf, CardTargetTypes::copyOf);
    public static final Codec<List<Class<? extends LivingEntity>>> LEGACY_CODEC = Codec.STRING.flatXmap(
            CardTargetTypes::decodeLegacy, _ -> DataResult.error(() -> "Legacy card target modes are decode-only"));
    public static final StreamCodec<ByteBuf, List<Class<? extends LivingEntity>>> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    static {
        register(AstralCraft.prefix("living_entity"), LivingEntity.class);
        register(AstralCraft.prefix("player"), Player.class);
        register(AstralCraft.prefix("mob"), Mob.class);
    }

    public static synchronized void register(Identifier id, Class<? extends LivingEntity> targetType) {
        if (id == null) {
            throw new IllegalArgumentException("Card target type id cannot be null");
        }

        if (targetType == null) {
            throw new IllegalArgumentException("Card target entity class cannot be null");
        }

        Class<? extends LivingEntity> existingType = TYPES_BY_ID.get(id);
        if (existingType != null && existingType != targetType) {
            throw new IllegalStateException("Card target type id " + id + " is already assigned to " + existingType.getName());
        }

        Identifier existingId = IDS_BY_TYPE.get(targetType);
        if (existingId != null && !existingId.equals(id)) {
            throw new IllegalStateException("Card target entity class " + targetType.getName() + " is already assigned to " + existingId);
        }

        TYPES_BY_ID.put(id, targetType);
        IDS_BY_TYPE.put(targetType, id);
    }

    public static List<Class<? extends LivingEntity>> copyOf(List<Class<? extends LivingEntity>> targetTypes) {
        if (targetTypes == null || targetTypes.isEmpty()) return NONE;
        LinkedHashSet<Class<? extends LivingEntity>> uniqueTypes = new LinkedHashSet<>();
        for (Class<? extends LivingEntity> targetType : targetTypes) {
            if (targetType == null) {
                throw new IllegalArgumentException("Card target entity classes cannot contain null");
            }

            uniqueTypes.add(targetType);
        }

        return List.copyOf(uniqueTypes);
    }

    protected static synchronized DataResult<Class<? extends LivingEntity>> decode(Identifier id) {
        Class<? extends LivingEntity> targetType = TYPES_BY_ID.get(id);
        if (targetType == null) {
            return DataResult.error(() -> "Unknown card target entity class id: " + id);
        }
        return DataResult.success(targetType);
    }

    protected static synchronized DataResult<Identifier> encode(Class<? extends LivingEntity> targetType) {
        Identifier id = IDS_BY_TYPE.get(targetType);
        if (id == null) {
            return DataResult.error(() -> "Unregistered card target entity class: " + targetType.getName());
        }

        return DataResult.success(id);
    }

    protected static DataResult<List<Class<? extends LivingEntity>>> decodeLegacy(String mode) {
        List<Class<? extends LivingEntity>> targetTypes = LEGACY_TYPES.get(mode);
        if (targetTypes == null) {
            return DataResult.error(() -> "Unknown legacy card target mode: " + mode);
        }

        return DataResult.success(targetTypes);
    }

}