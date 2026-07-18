package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import com.astral_craft.common.network.BoardDecisionProgress;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

public record OpenBoardBattlePayload(
        UUID boardId, int attackerEntityId, int defenderEntityId,
        String attackerName, String defenderName, List<CombatCardView> cards,
        BattleRole role, BoardDecisionProgress decision,
        int maximumCost, BattleView view)
        implements CustomPacketPayload {

    private static final int MAXIMUM_COMBAT_CARDS = 7;

    public static final Type<OpenBoardBattlePayload> TYPE = new Type<>(AstralCraft.prefix("open_board_battle"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenBoardBattlePayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, OpenBoardBattlePayload::boardId,
            ByteBufCodecs.VAR_INT, OpenBoardBattlePayload::attackerEntityId,
            ByteBufCodecs.VAR_INT, OpenBoardBattlePayload::defenderEntityId,
            ByteBufCodecs.STRING_UTF8, OpenBoardBattlePayload::attackerName,
            ByteBufCodecs.STRING_UTF8, OpenBoardBattlePayload::defenderName,
            CombatCardView.STREAM_CODEC.apply(ByteBufCodecs.list(MAXIMUM_COMBAT_CARDS)),
            OpenBoardBattlePayload::cards,
            BattleRole.STREAM_CODEC, OpenBoardBattlePayload::role,
            BoardDecisionProgress.STREAM_CODEC, OpenBoardBattlePayload::decision,
            ByteBufCodecs.VAR_INT, OpenBoardBattlePayload::maximumCost,
            BattleView.STREAM_CODEC, OpenBoardBattlePayload::view,
            OpenBoardBattlePayload::new);

    public OpenBoardBattlePayload {
        cards = List.copyOf(cards == null ? List.of() : cards);
        role = role == null ? BattleRole.SPECTATOR : role;
        view = view == null ? BattleView.empty() : view;
        maximumCost = Math.max(0, maximumCost);
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

    public enum BattleRole {
        ATTACKER,
        DEFENDER,
        SPECTATOR;

        public static final StreamCodec<ByteBuf, BattleRole> STREAM_CODEC = enumCodec(values(), SPECTATOR);
    }

    public enum BattlePhase {
        SELECT,
        READY,
        ATTACKER_ROLL,
        DEFENSE_CHOICE,
        DEFENDER_ROLL,
        RESULT;

        public static final StreamCodec<ByteBuf, BattlePhase> STREAM_CODEC = enumCodec(values(), SELECT);
    }

    public enum DefenseMode {
        DEFEND,
        EVADE;

        public static final StreamCodec<ByteBuf, DefenseMode> STREAM_CODEC = enumCodec(values(), DEFEND);
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
            stack = stack == null ? ItemStack.EMPTY : stack.copy();
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
            int damage, boolean evaded, boolean knockout,
            boolean attackerReady, boolean defenderReady, DefenseMode defenseMode) {

        public static final StreamCodec<ByteBuf, BattleView> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public BattleView decode(ByteBuf buffer) {
                return new BattleView(
                        BattlePhase.STREAM_CODEC.decode(buffer),
                        ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer),
                        ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer),
                        ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer),
                        ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer),
                        ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer),
                        ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer),
                        ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer),
                        ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.BOOL.decode(buffer),
                        ByteBufCodecs.BOOL.decode(buffer), ByteBufCodecs.BOOL.decode(buffer),
                        ByteBufCodecs.BOOL.decode(buffer), DefenseMode.STREAM_CODEC.decode(buffer));
            }

            @Override
            public void encode(ByteBuf buffer, BattleView value) {
                BattlePhase.STREAM_CODEC.encode(buffer, value.phase());
                ByteBufCodecs.VAR_INT.encode(buffer, value.attackerHealth());
                ByteBufCodecs.VAR_INT.encode(buffer, value.defenderHealth());
                ByteBufCodecs.VAR_INT.encode(buffer, value.attackBase());
                ByteBufCodecs.VAR_INT.encode(buffer, value.defenseBase());
                ByteBufCodecs.VAR_INT.encode(buffer, value.attackMinimum());
                ByteBufCodecs.VAR_INT.encode(buffer, value.attackMaximum());
                ByteBufCodecs.VAR_INT.encode(buffer, value.defenseMinimum());
                ByteBufCodecs.VAR_INT.encode(buffer, value.defenseMaximum());
                ByteBufCodecs.VAR_INT.encode(buffer, value.attackerDie());
                ByteBufCodecs.VAR_INT.encode(buffer, value.defenderDie());
                ByteBufCodecs.VAR_INT.encode(buffer, value.attackBonus());
                ByteBufCodecs.VAR_INT.encode(buffer, value.defenseBonus());
                ByteBufCodecs.VAR_INT.encode(buffer, value.attackTotal());
                ByteBufCodecs.VAR_INT.encode(buffer, value.defenseTotal());
                ByteBufCodecs.VAR_INT.encode(buffer, value.damage());
                ByteBufCodecs.BOOL.encode(buffer, value.evaded());
                ByteBufCodecs.BOOL.encode(buffer, value.knockout());
                ByteBufCodecs.BOOL.encode(buffer, value.attackerReady());
                ByteBufCodecs.BOOL.encode(buffer, value.defenderReady());
                DefenseMode.STREAM_CODEC.encode(buffer, value.defenseMode());
            }
        };

        public BattleView {
            phase = phase == null ? BattlePhase.SELECT : phase;
            defenseMode = defenseMode == null ? DefenseMode.DEFEND : defenseMode;
            attackerHealth = Math.max(0, attackerHealth);
            defenderHealth = Math.max(0, defenderHealth);
            damage = Math.max(0, damage);
        }

        public static BattleView empty() {
            return new BattleView(BattlePhase.SELECT, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, false, false, false, false, DefenseMode.DEFEND);
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

    private static <E extends Enum<E>> StreamCodec<ByteBuf, E> enumCodec(E[] values, E fallback) {
        return new StreamCodec<>() {
            @Override
            public E decode(ByteBuf buffer) {
                int index = ByteBufCodecs.VAR_INT.decode(buffer);
                return index >= 0 && index < values.length ? values[index] : fallback;
            }

            @Override
            public void encode(ByteBuf buffer, E value) {
                ByteBufCodecs.VAR_INT.encode(buffer, value == null ? fallback.ordinal() : value.ordinal());
            }
        };
    }

}