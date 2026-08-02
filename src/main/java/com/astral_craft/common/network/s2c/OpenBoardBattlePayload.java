package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardDecisionProgress;
import com.astral_craft.common.network.BoardNetworkCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ByIdMap;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;
import java.util.function.IntFunction;

public record OpenBoardBattlePayload(
        UUID boardId, int attackerEntityId, int defenderEntityId,
        String attackerName, String defenderName, List<CombatCardView> cards,
        PlayedCardsView playedCards, BattleRole role, BoardDecisionProgress decision,
        int maximumCost, BattleView view)
        implements CustomPacketPayload {

    private static final int MAXIMUM_COMBAT_CARDS = 7;

    public static final Type<OpenBoardBattlePayload> TYPE = new Type<>(AstralCraft.prefix("open_board_battle"));
    private static final StreamCodec<RegistryFriendlyByteBuf, List<CombatCardView>> COMBAT_CARDS_STREAM_CODEC =
            CombatCardView.STREAM_CODEC.apply(ByteBufCodecs.list(MAXIMUM_COMBAT_CARDS));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenBoardBattlePayload> STREAM_CODEC =
            StreamCodec.ofMember(OpenBoardBattlePayload::encode, OpenBoardBattlePayload::new);

    public OpenBoardBattlePayload {
        cards = List.copyOf(cards);
        playedCards = playedCards == null ? PlayedCardsView.EMPTY : playedCards;
        maximumCost = Math.max(0, maximumCost);
    }

    private OpenBoardBattlePayload(RegistryFriendlyByteBuf buffer) {
        this(BoardNetworkCodecs.UUID_STREAM_CODEC.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer),
                ByteBufCodecs.STRING_UTF8.decode(buffer), ByteBufCodecs.STRING_UTF8.decode(buffer),
                COMBAT_CARDS_STREAM_CODEC.decode(buffer), PlayedCardsView.STREAM_CODEC.decode(buffer),
                BattleRole.STREAM_CODEC.decode(buffer), BoardDecisionProgress.STREAM_CODEC.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer), BattleView.STREAM_CODEC.decode(buffer));
    }

    private void encode(RegistryFriendlyByteBuf buffer) {
        BoardNetworkCodecs.UUID_STREAM_CODEC.encode(buffer, this.boardId);
        ByteBufCodecs.VAR_INT.encode(buffer, this.attackerEntityId);
        ByteBufCodecs.VAR_INT.encode(buffer, this.defenderEntityId);
        ByteBufCodecs.STRING_UTF8.encode(buffer, this.attackerName);
        ByteBufCodecs.STRING_UTF8.encode(buffer, this.defenderName);
        COMBAT_CARDS_STREAM_CODEC.encode(buffer, this.cards);
        PlayedCardsView.STREAM_CODEC.encode(buffer, this.playedCards);
        BattleRole.STREAM_CODEC.encode(buffer, this.role);
        BoardDecisionProgress.STREAM_CODEC.encode(buffer, this.decision);
        ByteBufCodecs.VAR_INT.encode(buffer, this.maximumCost);
        BattleView.STREAM_CODEC.encode(buffer, this.view);
    }

    public int decisionTicks() {
        return this.decision.remainingTicks();
    }

    public int decisionDurationTicks() {
        return this.decision.durationTicks();
    }

    public Identifier characterId() {
        return this.decision.characterId();
    }

    public Identifier skinId() {
        return this.decision.skinId();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record PlayedCardsView(List<ItemStack> attacker, List<ItemStack> defender) {
        public static final PlayedCardsView EMPTY = new PlayedCardsView(List.of(), List.of());
        public static final StreamCodec<RegistryFriendlyByteBuf, PlayedCardsView> STREAM_CODEC = StreamCodec.composite(
                ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(MAXIMUM_COMBAT_CARDS)), PlayedCardsView::attacker,
                ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(MAXIMUM_COMBAT_CARDS)), PlayedCardsView::defender,
                PlayedCardsView::new);

        public PlayedCardsView {
            attacker = copyStacks(attacker);
            defender = copyStacks(defender);
        }

        private static List<ItemStack> copyStacks(List<ItemStack> stacks) {
            if (stacks == null || stacks.isEmpty()) return List.of();
            return stacks.stream().filter(stack -> stack != null && !stack.isEmpty()).map(ItemStack::copy).toList();
        }
    }

    public enum BattleRole {
        ATTACKER,
        DEFENDER,
        SPECTATOR;

        public static final StreamCodec<ByteBuf, BattleRole> STREAM_CODEC = ByteBufCodecs.idMapper(
                index -> index >= 0 && index < values().length ? values()[index] : SPECTATOR,
                BattleRole::ordinal);
    }

    public enum BattlePhase {
        SELECT,
        READY,
        ATTACKER_ROLL,
        DEFENSE_CHOICE,
        DEFENDER_ROLL,
        RESULT;

        private static final IntFunction<BattlePhase> BY_ID = ByIdMap.continuous(
                BattlePhase::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        public static final StreamCodec<ByteBuf, BattlePhase> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, BattlePhase::ordinal);
    }

    public enum DefenseMode {
        DEFEND,
        EVADE;

        private static final IntFunction<DefenseMode> BY_ID = ByIdMap.continuous(
                DefenseMode::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        public static final StreamCodec<ByteBuf, DefenseMode> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, DefenseMode::ordinal);
    }

    public record CombatCardView(int handIndex, ItemStack stack, int cost, int minimumBonus, int maximumBonus) {
        public static final StreamCodec<RegistryFriendlyByteBuf, CombatCardView> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, CombatCardView::handIndex,
                ItemStack.OPTIONAL_STREAM_CODEC, CombatCardView::stack,
                ByteBufCodecs.VAR_INT, CombatCardView::cost,
                ByteBufCodecs.VAR_INT, CombatCardView::minimumBonus,
                ByteBufCodecs.VAR_INT, CombatCardView::maximumBonus,
                CombatCardView::new);

        public CombatCardView {
            stack = stack.copy();
            cost = Math.max(0, cost);
            minimumBonus = Math.max(0, minimumBonus);
            maximumBonus = Math.max(minimumBonus, maximumBonus);
        }
    }

    public record BattleView(
            BattlePhase phase, int attackerHealth, int defenderHealth,
            int attackBase, int defenseBase, int attackMinimum, int attackMaximum,
            int defenseMinimum, int defenseMaximum, int attackerDie, int defenderDie,
            int attackBonus, int defenseBonus, int attackTotal, int defenseTotal,
            int damage, boolean evaded, boolean knockout, boolean evadeAllowed,
            boolean attackerReady, boolean defenderReady, DefenseMode defenseMode) {

        public static final StreamCodec<ByteBuf, BattleView> STREAM_CODEC = StreamCodec.ofMember(
                BattleView::encode, BattleView::new);

        private BattleView(ByteBuf buffer) {
            this(BattlePhase.STREAM_CODEC.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.BOOL.decode(buffer),
                    ByteBufCodecs.BOOL.decode(buffer), ByteBufCodecs.BOOL.decode(buffer),
                    ByteBufCodecs.BOOL.decode(buffer), ByteBufCodecs.BOOL.decode(buffer),
                    DefenseMode.STREAM_CODEC.decode(buffer));
        }

        private void encode(ByteBuf buffer) {
            BattlePhase.STREAM_CODEC.encode(buffer, this.phase);
            ByteBufCodecs.VAR_INT.encode(buffer, this.attackerHealth);
            ByteBufCodecs.VAR_INT.encode(buffer, this.defenderHealth);
            ByteBufCodecs.VAR_INT.encode(buffer, this.attackBase);
            ByteBufCodecs.VAR_INT.encode(buffer, this.defenseBase);
            ByteBufCodecs.VAR_INT.encode(buffer, this.attackMinimum);
            ByteBufCodecs.VAR_INT.encode(buffer, this.attackMaximum);
            ByteBufCodecs.VAR_INT.encode(buffer, this.defenseMinimum);
            ByteBufCodecs.VAR_INT.encode(buffer, this.defenseMaximum);
            ByteBufCodecs.VAR_INT.encode(buffer, this.attackerDie);
            ByteBufCodecs.VAR_INT.encode(buffer, this.defenderDie);
            ByteBufCodecs.VAR_INT.encode(buffer, this.attackBonus);
            ByteBufCodecs.VAR_INT.encode(buffer, this.defenseBonus);
            ByteBufCodecs.VAR_INT.encode(buffer, this.attackTotal);
            ByteBufCodecs.VAR_INT.encode(buffer, this.defenseTotal);
            ByteBufCodecs.VAR_INT.encode(buffer, this.damage);
            ByteBufCodecs.BOOL.encode(buffer, this.evaded);
            ByteBufCodecs.BOOL.encode(buffer, this.knockout);
            ByteBufCodecs.BOOL.encode(buffer, this.evadeAllowed);
            ByteBufCodecs.BOOL.encode(buffer, this.attackerReady);
            ByteBufCodecs.BOOL.encode(buffer, this.defenderReady);
            DefenseMode.STREAM_CODEC.encode(buffer, this.defenseMode);
        }

        public BattleView {
            attackerHealth = Math.max(0, attackerHealth);
            defenderHealth = Math.max(0, defenderHealth);
            damage = Math.max(0, damage);
        }

        public static BattleView empty() {
            return new BattleView(BattlePhase.SELECT, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, false, false, true, false, false, DefenseMode.DEFEND);
        }

        public boolean selecting() {
            return this.phase == BattlePhase.SELECT;
        }

        public boolean ready() {
            return this.phase == BattlePhase.READY;
        }

        public boolean attackerRolling() {
            return this.phase == BattlePhase.ATTACKER_ROLL;
        }

        public boolean scorePhase() {
            return this.selecting() || this.ready();
        }

        public boolean defenseChoice() {
            return this.phase == BattlePhase.DEFENSE_CHOICE;
        }

        public boolean defenderRolling() {
            return this.phase == BattlePhase.DEFENDER_ROLL;
        }

        public boolean result() {
            return this.phase == BattlePhase.RESULT;
        }
    }

}