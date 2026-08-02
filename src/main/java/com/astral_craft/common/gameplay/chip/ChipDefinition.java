package com.astral_craft.common.gameplay.chip;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.StatBundle;
import com.astral_craft.common.gameplay.board.BoardParticipant;
import com.astral_craft.common.gameplay.buff.BoardBuff;
import com.astral_craft.common.registry.AstralBoardBuffs;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Supplier;

public class ChipDefinition {

    private static final ParticipantEffect NO_EFFECT = (_, participant) -> participant;
    private final String id;
    private final String nameKey;
    private final String effectKey;
    private final ChipRarity rarity;
    private final @Nullable Supplier<? extends BoardBuff> keyword;
    private final StatBundle stats;
    private final ChipPool pool;
    private final @Nullable Identifier mapRestriction;
    private final ParticipantEffect turnStartEffect;
    private final ParticipantEffect effectCardPlayedEffect;
    private final ParticipantEffect turnEndEffect;

    public ChipDefinition(String id, String nameKey, String effectKey, ChipRarity rarity,
                          @Nullable Supplier<? extends BoardBuff> keyword, StatBundle stats, ChipPool pool,
                          @Nullable Identifier mapRestriction, @Nullable ParticipantEffect turnStartEffect,
                          @Nullable ParticipantEffect effectCardPlayedEffect, @Nullable ParticipantEffect turnEndEffect) {
        this.id = id;
        this.nameKey = nameKey;
        this.effectKey = effectKey;
        this.rarity = rarity;
        this.keyword = keyword;
        this.stats = stats == null ? StatBundle.EMPTY : stats;
        this.pool = pool == null ? ChipPool.GENERAL : pool;
        this.mapRestriction = mapRestriction;
        this.turnStartEffect = turnStartEffect == null ? NO_EFFECT : turnStartEffect;
        this.effectCardPlayedEffect = effectCardPlayedEffect == null ? NO_EFFECT : effectCardPlayedEffect;
        this.turnEndEffect = turnEndEffect == null ? NO_EFFECT : turnEndEffect;
    }

    public String id() {
        return this.id;
    }

    public String nameKey() {
        return this.nameKey;
    }

    public String effectKey() {
        return this.effectKey;
    }

    public ChipRarity rarity() {
        return this.rarity;
    }

    public @Nullable BoardBuff keyword() {
        return this.keyword == null ? null : this.keyword.get();
    }

    public StatBundle stats() {
        return this.stats;
    }

    public ChipPool pool() {
        return this.pool;
    }

    public @Nullable Identifier mapRestriction() {
        return this.mapRestriction;
    }

    public Component displayName() {
        return Component.translatable(this.nameKey);
    }

    public Component effectText() {
        return Component.translatable(this.effectKey);
    }

    public Identifier registryId() {
        return AstralCraft.prefix(this.id);
    }

    public Optional<Identifier> keywordId() {
        BoardBuff keyword = this.keyword();
        return Optional.ofNullable(keyword == null ? null : AstralBoardBuffs.REGISTRY.getKey(keyword));
    }

    public boolean availableOn(@Nullable Identifier mapId) {
        return this.mapRestriction == null || this.mapRestriction.equals(mapId);
    }

    public BoardParticipant beforeTurnStart(ServerLevel level, BoardParticipant participant) {
        return this.turnStartEffect.apply(level, participant);
    }

    public BoardParticipant afterEffectCardPlayed(ServerLevel level, BoardParticipant participant) {
        return this.effectCardPlayedEffect.apply(level, participant);
    }

    public BoardParticipant afterTurnEnd(ServerLevel level, BoardParticipant participant) {
        return this.turnEndEffect.apply(level, participant);
    }

    public static String nameKey(String id) {
        return "chip." + AstralCraft.MOD_ID + "." + id;
    }

    public static String effectKey(String id) {
        return "chip." + AstralCraft.MOD_ID + "." + id + ".desc";
    }

    @FunctionalInterface
    public interface ParticipantEffect {
        BoardParticipant apply(ServerLevel level, BoardParticipant participant);
    }

}