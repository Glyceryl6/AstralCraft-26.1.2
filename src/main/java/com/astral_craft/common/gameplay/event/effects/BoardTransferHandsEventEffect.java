package com.astral_craft.common.gameplay.event.effects;

import com.astral_craft.common.gameplay.board.BoardEventTargets;
import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.board.BoardEventContext;
import com.astral_craft.common.gameplay.board.BoardEventTask;
import com.astral_craft.common.gameplay.board.BoardParticipant;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.gameplay.event.AstralEventEffect;
import com.mojang.serialization.MapCodec;
import net.minecraft.resources.Identifier;

import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record BoardTransferHandsEventEffect() implements BoardEventEffect {

    public static final MapCodec<BoardTransferHandsEventEffect> CODEC = MapCodec.unit(new BoardTransferHandsEventEffect());

    @Override
    public String typeId() {
        return AstralCraft.prefix("board_transfer_hands").toString();
    }

    @Override
    public MapCodec<? extends AstralEventEffect> codec() {
        return CODEC;
    }

    @Override
    public void enqueue(BoardEventContext context, Deque<BoardEventTask> tasks) {
        tasks.addLast(BoardEventTask.action(() -> {
            List<UUID> order = context.session().turnOrder().stream().filter(slotId -> context.session().participant(slotId)
                    .filter(participant -> BoardEventTargets.affected(context.session(), participant,
                            BoardEventTargets.Impact.HAND_LOSS)).isPresent()).toList();
            if (order.size() < 2) return;
            Map<UUID, List<Identifier>> hands = new LinkedHashMap<>();
            for (UUID slotId : order) context.session().participant(slotId)
                    .ifPresent(participant -> hands.put(slotId, participant.hand()));
            for (int index = 0; index < order.size(); index++) {
                UUID receiverId = order.get((index + 1) % order.size());
                BoardParticipant receiver = context.session().participant(receiverId).orElse(null);
                List<Identifier> hand = hands.get(order.get(index));
                if (receiver != null && hand != null) BoardSessionManager.updateParticipant(context.level(),
                        context.session(), receiver.withHand(hand));
            }
        }, 8));
    }
}
