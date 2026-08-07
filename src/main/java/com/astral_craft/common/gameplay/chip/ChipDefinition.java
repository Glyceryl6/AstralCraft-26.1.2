package com.astral_craft.common.gameplay.chip;

import com.astral_craft.common.gameplay.board.BoardParticipant;
import com.astral_craft.common.stats.AstralPlayerStats;
import com.astral_craft.common.stats.AstralStats;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public abstract class ChipDefinition {

    private final Identifier id;
    private final ChipRarity rarity;
    private final ChipPool pool;
    private final @Nullable Identifier keywordId;
    private final @Nullable Identifier mapRestriction;

    protected ChipDefinition(Identifier id, ChipRarity rarity, ChipPool pool) {
        this(id, rarity, pool, null, null);
    }

    protected ChipDefinition(Identifier id, ChipRarity rarity, ChipPool pool, @Nullable Identifier keywordId) {
        this(id, rarity, pool, keywordId, null);
    }

    protected ChipDefinition(Identifier id, ChipRarity rarity, ChipPool pool, @Nullable Identifier keywordId,
                             @Nullable Identifier mapRestriction) {
        this.id = id;
        this.rarity = rarity;
        this.pool = pool == null ? ChipPool.GENERAL : pool;
        this.keywordId = keywordId;
        this.mapRestriction = mapRestriction;
    }

    public Identifier id() {
        return this.id;
    }

    public String nameKey() {
        return "chip." + this.id.getNamespace() + "." + this.id.getPath();
    }

    public String effectKey() {
        return this.nameKey() + ".desc";
    }

    public ChipRarity rarity() {
        return this.rarity;
    }

    public ChipPool pool() {
        return this.pool;
    }

    public Optional<Identifier> keywordId() {
        return Optional.ofNullable(this.keywordId);
    }

    public Optional<Identifier> mapRestriction() {
        return Optional.ofNullable(this.mapRestriction);
    }

    public Component displayName() {
        return Component.translatable(this.nameKey());
    }

    public Component effectText() {
        return Component.translatable(this.effectKey());
    }

    public Identifier registryId() {
        return this.id;
    }

    public boolean availableOn(@Nullable Identifier mapId) {
        return this.mapRestriction == null || this.mapRestriction.equals(mapId);
    }

    public void applyToPlayer(Player player) {
        if (player == null) return;
        AstralStats.set(player, this.applyStats(AstralStats.get(player)));
    }

    public BoardParticipant applyToBoard(BoardParticipant participant) {
        return participant == null ? null : participant.withStats(this.applyStats(participant.stats()));
    }

    protected AstralPlayerStats applyStats(AstralPlayerStats stats) {
        return stats;
    }

    public int skillCooldownReduction() {
        return 0;
    }

    public BoardParticipant beforeTurnStart(ServerLevel level, BoardParticipant participant) {
        return participant;
    }

    public BoardParticipant afterEffectCardPlayed(ServerLevel level, BoardParticipant participant) {
        return participant;
    }

    public BoardParticipant afterTurnEnd(ServerLevel level, BoardParticipant participant) {
        return participant;
    }
}
