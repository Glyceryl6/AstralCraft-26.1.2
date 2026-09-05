package com.astral_craft.common.gameplay.event.effects;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.board.BoardEventContext;
import com.astral_craft.common.gameplay.board.BoardEventTask;
import com.astral_craft.common.gameplay.board.BoardMatchmakingService;
import com.astral_craft.common.gameplay.board.BoardParticipant;
import com.astral_craft.common.gameplay.board.BoardWorldObjectService;
import com.astral_craft.common.gameplay.event.AstralEventEffect;
import com.mojang.serialization.MapCodec;

import java.util.Comparator;
import java.util.Deque;

public record BoardBalanceCoinsEventEffect() implements BoardEventEffect {

    public static final MapCodec<BoardBalanceCoinsEventEffect> CODEC = MapCodec.unit(new BoardBalanceCoinsEventEffect());

    @Override
    public String typeId() {
        return AstralCraft.prefix("board_balance_coins").toString();
    }

    @Override
    public MapCodec<? extends AstralEventEffect> codec() {
        return CODEC;
    }

    @Override
    public void enqueue(BoardEventContext context, Deque<BoardEventTask> tasks) {
        tasks.addLast(BoardEventTask.action(() -> {
            BoardParticipant highest = context.session().partyParticipants().stream()
                    .filter(participant -> !BoardMatchmakingService.tutorialProtected(context.session(), participant))
                    .max(Comparator.comparingInt(value -> value.stats().starCoins())).orElse(null);
            BoardParticipant lowest = context.session().partyParticipants().stream()
                    .filter(participant -> !BoardMatchmakingService.tutorialProtected(context.session(), participant))
                    .min(Comparator.comparingInt(value -> value.stats().starCoins())).orElse(null);
            if (highest == null || lowest == null || highest.slotUuid().equals(lowest.slotUuid())
                    || highest.stats().starCoins() == lowest.stats().starCoins()) return;
            int total = highest.stats().starCoins() + lowest.stats().starCoins();
            int lowerShare = total / 2;
            int upperShare = total - lowerShare;
            int highestDelta = upperShare - highest.stats().starCoins();
            int lowestDelta = lowerShare - lowest.stats().starCoins();
            if (highestDelta < 0) BoardWorldObjectService.changeCoins(context.level(), context.session(), highest.slotUuid(), highestDelta);
            if (lowestDelta < 0) BoardWorldObjectService.changeCoins(context.level(), context.session(), lowest.slotUuid(), lowestDelta);
            if (highestDelta > 0) BoardWorldObjectService.changeCoins(context.level(), context.session(), highest.slotUuid(), highestDelta);
            if (lowestDelta > 0) BoardWorldObjectService.changeCoins(context.level(), context.session(), lowest.slotUuid(), lowestDelta);
        }, 20));
    }
}
