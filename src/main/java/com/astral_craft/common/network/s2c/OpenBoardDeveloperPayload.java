package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.character.CharacterCodecLines;
import com.astral_craft.common.gameplay.character.CharacterDefinition;
import com.astral_craft.common.network.BoardNetworkCodecs;
import com.astral_craft.common.stats.AstralPlayerStats;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.UUID;

public record OpenBoardDeveloperPayload(UUID boardId, List<CharacterDefinition> characters,
                                        List<Identifier> botSelectableCharacterIds, List<Identifier> cardIds,
                                        BoardCharacterSelectionEntry human, BattleOverrideView humanBattle,
                                        List<BotView> bots, boolean live) implements CustomPacketPayload {

    public static final Type<OpenBoardDeveloperPayload> TYPE = new Type<>(AstralCraft.prefix("open_board_developer"));
    public static final BattleOverrideView DEFAULT_BATTLE = new BattleOverrideView(0, 0, -1, -1);
    private static final StreamCodec<ByteBuf, BattleOverrideView> BATTLE_OVERRIDE_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BattleOverrideView::attackerDie,
            ByteBufCodecs.VAR_INT, BattleOverrideView::defenderDie,
            ByteBufCodecs.VAR_INT, BattleOverrideView::attackCardBonus,
            ByteBufCodecs.VAR_INT, BattleOverrideView::defenseCardBonus,
            BattleOverrideView::new);
    private static final StreamCodec<ByteBuf, BotView> BOT_VIEW_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, BotView::slotId,
            Identifier.STREAM_CODEC, BotView::characterId,
            Identifier.STREAM_CODEC, BotView::skinId,
            AstralPlayerStats.STREAM_CODEC, BotView::stats,
            Identifier.STREAM_CODEC.apply(ByteBufCodecs.list(4096)), BotView::hand,
            ByteBufCodecs.VAR_INT, BotView::skillCooldownTurns,
            ByteBufCodecs.VAR_INT, BotView::knockedDownTurns,
            ByteBufCodecs.VAR_INT, BotView::cardPlaysUsed,
            ByteBufCodecs.VAR_INT, BotView::maxHandSize,
            BATTLE_OVERRIDE_CODEC, BotView::battle,
            BotView::new);
    public static final StreamCodec<ByteBuf, OpenBoardDeveloperPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, OpenBoardDeveloperPayload::boardId,
            CharacterCodecLines.STREAM_CODEC, OpenBoardDeveloperPayload::characters,
            Identifier.STREAM_CODEC.apply(ByteBufCodecs.list(512)), OpenBoardDeveloperPayload::botSelectableCharacterIds,
            Identifier.STREAM_CODEC.apply(ByteBufCodecs.list(512)), OpenBoardDeveloperPayload::cardIds,
            BoardCharacterSelectionEntry.STREAM_CODEC, OpenBoardDeveloperPayload::human,
            BATTLE_OVERRIDE_CODEC, OpenBoardDeveloperPayload::humanBattle,
            BOT_VIEW_CODEC.apply(ByteBufCodecs.list(3)), OpenBoardDeveloperPayload::bots,
            ByteBufCodecs.BOOL, OpenBoardDeveloperPayload::live,
            OpenBoardDeveloperPayload::new);

    public OpenBoardDeveloperPayload {
        characters = List.copyOf(characters);
        botSelectableCharacterIds = List.copyOf(botSelectableCharacterIds);
        cardIds = List.copyOf(cardIds);
        humanBattle = humanBattle == null ? DEFAULT_BATTLE : humanBattle;
        bots = List.copyOf(bots);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record BattleOverrideView(int attackerDie, int defenderDie, int attackCardBonus, int defenseCardBonus) {}

    public record BotView(UUID slotId, Identifier characterId, Identifier skinId, AstralPlayerStats stats,
                          List<Identifier> hand, int skillCooldownTurns, int knockedDownTurns, int cardPlaysUsed,
                          int maxHandSize, BattleOverrideView battle) {
        public BotView {
            hand = List.copyOf(hand);
            skillCooldownTurns = Math.max(0, skillCooldownTurns);
            knockedDownTurns = Math.max(0, knockedDownTurns);
            cardPlaysUsed = Math.max(0, cardPlaysUsed);
            maxHandSize = Math.max(1, maxHandSize);
            battle = battle == null ? DEFAULT_BATTLE : battle;
        }
    }
}
