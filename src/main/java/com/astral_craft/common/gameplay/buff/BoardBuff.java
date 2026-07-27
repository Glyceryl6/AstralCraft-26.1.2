package com.astral_craft.common.gameplay.buff;

import com.astral_craft.AstralCraft;
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

    protected final Properties properties;

    public BoardBuff() {
        this(Properties.of());
    }

    public BoardBuff(Properties properties) {
        this.properties = properties.copy();
    }

    public Component displayName() {
        Identifier id = AstralBoardBuffs.REGISTRY.getKey(this);
        return id == null ? Component.empty() : Component.translatable("board_buff." + id.getNamespace() + "." + id.getPath());
    }

    public Identifier icon() {
        Identifier id = AstralBoardBuffs.REGISTRY.getKey(this);
        return id == null ? AstralCraft.prefix("textures/gui/board_buff/custom.png")
                : Identifier.fromNamespaceAndPath(id.getNamespace(), "textures/gui/board_buff/" + id.getPath() + ".png");
    }

    public int color(BoardBuffInstance instance) {
        return instance == null ? this.properties.color : instance.color().orElse(this.properties.color);
    }

    public AstralPlayerStats apply(AstralPlayerStats stats, BoardBuffInstance incoming) {
        if (incoming == null) return stats;
        incoming = this.normalize(incoming);
        if (this.properties.separateStacks && stats.hasBuff(incoming.id())) {
            incoming = incoming.withId(stats.nextBuffInstanceId(incoming.id()));
        }

        BoardBuffInstance merged = this.merge(stats.buffInstance(incoming.id()), incoming);
        return merged == null ? stats.removeBuff(incoming.id()) : stats.setBuff(merged);
    }

    public BoardBuffInstance merge(BoardBuffInstance current, BoardBuffInstance incoming) {
        if (incoming == null) return current;
        incoming = this.normalize(incoming);
        if (current == null || current.buff() != this || !current.id().equals(incoming.id())) return incoming;
        int level = this.properties.addLevels ? current.level() + incoming.level() : Math.max(current.level(), incoming.level());
        int intrinsicLevels = this.properties.addLevels
                ? current.intrinsicLevels() + incoming.intrinsicLevels()
                : Math.max(current.intrinsicLevels(), incoming.intrinsicLevels());
        if (this.properties.maximumLevel > 0) {
            level = this.properties.loopLevels
                    ? (level - 1) % this.properties.maximumLevel + 1
                    : Math.min(level, this.properties.maximumLevel);
        }

        intrinsicLevels = Math.min(intrinsicLevels, level);
        int duration = this.mergeDuration(current, incoming);
        boolean fresh = level > intrinsicLevels && (current.fresh() || incoming.fresh());
        return new BoardBuffInstance(incoming.id(), this, duration, level - 1, intrinsicLevels, fresh,
                incoming.value(), incoming.customName().isPresent() ? incoming.customName() : current.customName(),
                incoming.customIcon().isPresent() ? incoming.customIcon() : current.customIcon(),
                incoming.customColor().isPresent() ? incoming.customColor() : current.customColor(),
                current.consumeAfterIncomingDamage() || incoming.consumeAfterIncomingDamage(),
                current.consumeAfterMoveRoll() || incoming.consumeAfterMoveRoll());
    }

    protected int mergeDuration(BoardBuffInstance current, BoardBuffInstance incoming) {
        if (this.properties.permanent) return BoardBuffInstance.PERMANENT;
        if (current.acquiredLevels() <= 0) return incoming.duration();
        if (incoming.acquiredLevels() <= 0) return current.duration();
        return current.permanent() || incoming.permanent() ? BoardBuffInstance.PERMANENT
                : Math.max(current.duration(), incoming.duration());
    }

    protected BoardBuffInstance normalize(BoardBuffInstance instance) {
        if (!this.properties.permanent || instance.acquiredLevels() <= 0 || instance.permanent()) return instance;
        return new BoardBuffInstance(instance.id(), instance.buff(), BoardBuffInstance.PERMANENT, instance.amplifier(),
                instance.intrinsicLevels(), instance.fresh(), instance.value(), instance.customName(), instance.customIcon(),
                instance.customColor(), instance.consumeAfterIncomingDamage(), instance.consumeAfterMoveRoll());
    }

    public AstralPlayerStats onTurnStart(AstralPlayerStats stats, BoardBuffInstance instance) {
        return stats;
    }

    public AstralPlayerStats onTurnEnd(AstralPlayerStats stats, BoardBuffInstance instance) {
        int acquiredLevels = instance.acquiredLevels();
        if (acquiredLevels <= 0) return stats;
        if (this.properties.halveLevelsAtTurnEnd) {
            BoardBuffInstance next = instance.withAcquiredLevels(acquiredLevels / 2);
            return next == null ? stats.removeBuff(instance.id()) : stats.setBuff(next);
        }

        if (this.properties.levelDecayAtTurnEnd <= 0) return stats;
        BoardBuffInstance next = instance.withAcquiredLevels(acquiredLevels - this.properties.levelDecayAtTurnEnd);
        return next == null ? stats.removeBuff(instance.id()) : stats.setBuff(next);
    }

    public int turnStartHealing(BoardBuffInstance instance) {
        return this.properties.turnStartHealingPerLevel * instance.value() * instance.level();
    }

    public int turnStartDamage(BoardBuffInstance instance) {
        return this.properties.turnStartDamagePerLevel * instance.value() * instance.level();
    }

    public int attackModifier(BoardBuffInstance instance) {
        return 0;
    }

    public int defenseModifier(BoardBuffInstance instance) {
        return 0;
    }

    public int speedModifier(BoardBuffInstance instance) {
        return 0;
    }

    public int moveDiceModifier(BoardBuffInstance instance) {
        return 0;
    }

    public int incomingDamageModifier(BoardBuffInstance instance) {
        return this.properties.flatIncomingDamage;
    }

    public int roundRewardBonus(BoardBuffInstance instance) {
        return Math.max(0, this.properties.roundRewardPerLevel * instance.value() * instance.level());
    }

    public boolean consumedAfterIncomingDamage(BoardBuffInstance instance) {
        return this.properties.consumeAfterIncomingDamage || instance.consumeAfterIncomingDamage();
    }

    public boolean consumedAfterMoveRoll(BoardBuffInstance instance) {
        return this.properties.consumeAfterMoveRoll || instance.consumeAfterMoveRoll();
    }

    public BoardParticipant onMovementFinished(
            ServerLevel level, BoardSession session,
            BoardSession.MovementState movement,
            BoardParticipant participant,
            BoardBuffInstance instance) {
        return participant;
    }

    public BoardBuffInstance afterKnockout(BoardBuffInstance instance) {
        return this.properties.clearOnKnockout ? instance.withoutAcquiredLevels() : instance;
    }

    public static class Properties {
        protected int color = 0xFFBBBBBB;
        protected int turnStartHealingPerLevel;
        protected int turnStartDamagePerLevel;
        protected int roundRewardPerLevel;
        protected int flatIncomingDamage;
        protected int levelDecayAtTurnEnd;
        protected int maximumLevel;
        protected boolean addLevels;
        protected boolean separateStacks;
        protected boolean loopLevels;
        protected boolean permanent;
        protected boolean halveLevelsAtTurnEnd;
        protected boolean consumeAfterIncomingDamage;
        protected boolean consumeAfterMoveRoll;
        protected boolean clearOnKnockout = true;

        protected Properties() {}

        public static Properties of() {
            return new Properties();
        }

        public static Properties of(int color) {
            return new Properties().color(color);
        }

        public Properties color(int value) {
            this.color = value;
            return this;
        }

        public Properties healAtTurnStart(int value) {
            this.turnStartHealingPerLevel = Math.max(0, value);
            return this;
        }

        public Properties damageAtTurnStart(int value) {
            this.turnStartDamagePerLevel = Math.max(0, value);
            return this;
        }

        public Properties roundReward(int value) {
            this.roundRewardPerLevel = Math.max(0, value);
            return this;
        }

        public Properties flatIncomingDamage(int value) {
            this.flatIncomingDamage = value;
            return this;
        }

        public Properties stacking() {
            this.addLevels = true;
            return this;
        }

        public Properties separateStacks() {
            this.separateStacks = true;
            return this;
        }

        public Properties maximumLevel(int value) {
            this.maximumLevel = Math.max(0, value);
            return this;
        }

        public Properties loopLevels() {
            this.loopLevels = true;
            return this;
        }

        public Properties permanent() {
            this.permanent = true;
            return this;
        }

        public Properties decayLevelsAtTurnEnd() {
            return this.decayLevelsAtTurnEnd(1);
        }

        public Properties decayLevelsAtTurnEnd(int value) {
            this.levelDecayAtTurnEnd = Math.max(0, value);
            return this;
        }

        public Properties halveLevelsAtTurnEnd() {
            this.halveLevelsAtTurnEnd = true;
            return this;
        }

        public Properties consumeAfterIncomingDamage() {
            this.consumeAfterIncomingDamage = true;
            return this;
        }

        public Properties consumeAfterMoveRoll() {
            this.consumeAfterMoveRoll = true;
            return this;
        }

        public Properties keepOnKnockout() {
            this.clearOnKnockout = false;
            return this;
        }

        protected Properties copy() {
            Properties result = new Properties();
            result.color = this.color;
            result.turnStartHealingPerLevel = this.turnStartHealingPerLevel;
            result.turnStartDamagePerLevel = this.turnStartDamagePerLevel;
            result.roundRewardPerLevel = this.roundRewardPerLevel;
            result.flatIncomingDamage = this.flatIncomingDamage;
            result.levelDecayAtTurnEnd = this.levelDecayAtTurnEnd;
            result.maximumLevel = this.maximumLevel;
            result.addLevels = this.addLevels;
            result.separateStacks = this.separateStacks;
            result.loopLevels = this.loopLevels;
            result.permanent = this.permanent;
            result.halveLevelsAtTurnEnd = this.halveLevelsAtTurnEnd;
            result.consumeAfterIncomingDamage = this.consumeAfterIncomingDamage;
            result.consumeAfterMoveRoll = this.consumeAfterMoveRoll;
            result.clearOnKnockout = this.clearOnKnockout;
            return result;
        }

    }

}