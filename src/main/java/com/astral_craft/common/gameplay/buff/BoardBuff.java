package com.astral_craft.common.gameplay.buff;

import com.astral_craft.common.gameplay.board.BoardParticipant;
import com.astral_craft.common.gameplay.board.BoardSession;
import com.astral_craft.common.registry.AstralBoardBuffs;
import com.astral_craft.common.stats.AstralPlayerStats;
import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

public class BoardBuff {

    public static final Codec<BoardBuff> CODEC = AstralBoardBuffs.REGISTRY.byNameCodec();

    protected final int color;

    public BoardBuff(int color) {
        this.color = color;
    }

    public int color() {
        return this.color;
    }

    public Component displayName() {
        Identifier id = AstralBoardBuffs.REGISTRY.getKey(this);
        return id == null ? Component.empty() : Component.translatable("board_buff." + id.getNamespace() + "." + id.getPath());
    }

    public BoardBuffInstance merge(BoardBuffInstance current, BoardBuffInstance incoming) {
        if (current == null) return incoming;
        int duration = current.permanent() || incoming.permanent() ? BoardBuffInstance.PERMANENT
                : Math.max(current.duration(), incoming.duration());
        return new BoardBuffInstance(duration, Math.max(current.amplifier(), incoming.amplifier()));
    }

    /**
     * Applies an incoming instance. Subclasses may replace this buff with another registered buff,
     * implement level loops, or reject the application entirely without adding manager-side branches.
     */
    public AstralPlayerStats apply(AstralPlayerStats stats, BoardBuffInstance incoming) {
        BoardBuffInstance merged = this.merge(stats.buffInstance(this), incoming);
        return merged == null ? stats.removeBuff(this) : stats.setBuff(this, merged.duration(), merged.amplifier());
    }

    public AstralPlayerStats onTurnStart(AstralPlayerStats stats, BoardBuffInstance instance) {
        return stats;
    }

    public AstralPlayerStats onTurnEnd(AstralPlayerStats stats, BoardBuffInstance instance) {
        return stats;
    }

    public int turnStartHealing(BoardBuffInstance instance) {
        return 0;
    }

    public int turnStartDamage(BoardBuffInstance instance) {
        return 0;
    }

    public int modifyAttack(int value, BoardBuffInstance instance) {
        return value;
    }

    public int modifyDefense(int value, BoardBuffInstance instance) {
        return value;
    }

    public int modifySpeed(int value, BoardBuffInstance instance) {
        return value;
    }

    public int modifyIncomingDamage(int value, BoardBuffInstance instance) {
        return value;
    }

    public int resolveIncomingDamage(int damage, BoardBuffInstance instance) {
        return damage;
    }

    public boolean consumedAfterIncomingDamage(BoardBuffInstance instance) {
        return false;
    }

    public BoardParticipant onMovementFinished(
            ServerLevel level, BoardSession session, BoardSession.MovementState movement,
            BoardParticipant participant, BoardBuffInstance instance) {
        return participant;
    }

    public boolean preventsEvade(BoardBuffInstance instance) {
        return false;
    }

    public boolean knocksDownOwnerWhenAttackFails(BoardBuffInstance instance) {
        return false;
    }

    public boolean consumedAfterAttack(BoardBuffInstance instance) {
        return false;
    }

}