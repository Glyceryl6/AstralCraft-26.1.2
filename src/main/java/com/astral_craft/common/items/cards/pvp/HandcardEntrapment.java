package com.astral_craft.common.items.cards.pvp;

import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.gameplay.board.*;
import com.astral_craft.common.gameplay.board.BoardMechanicsState.BoardTrap;
import com.astral_craft.common.gameplay.board.BoardMechanicsState.BoardTrapType;
import com.astral_craft.common.gameplay.handcard.CardTargetTypes;
import com.astral_craft.common.items.BaseHandCard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public class HandcardEntrapment extends BaseHandCard implements BoardPanelPlacementCard, BoardBotEffect {

    public static final CardDefinition DEFINITION = CardDefinition.create(CardType.EFFECT, CardTargetTypes.NONE, 3);

    public HandcardEntrapment(Properties properties) {
        super(properties);
    }

    @Override
    public BoardTrapType boardTrapType() {
        return BoardTrapType.ENTRAPMENT;
    }

    @Override
    public int boardPlacementRange() {
        return DEFINITION.range();
    }

    @Override
    public boolean canUseByBoardBot(BoardBotEffectContext context) {
        return this.randomValidNode(context.session(), context.user(), context.level().getRandom()).isPresent();
    }

    @Override
    public int applyByBoardBot(BoardBotEffectContext context) {
        this.randomValidNode(context.session(), context.user(), context.level().getRandom()).ifPresent(nodeId ->
                BoardWorldObjectService.placeTrap(context.level(), context.session(), this.boardTrapType(),
                        context.userSlotId(), nodeId));
        return 0;
    }

    public static void trigger(ServerLevel level, BoardSession session,
                               BoardTrap trap, BoardParticipant target) {
        BoardParticipant owner = session.participant(trap.ownerSlotId()).orElse(null);
        if (owner == null || owner.slotUuid().equals(target.slotUuid())) return;
        int amount = Math.min(5, target.stats().starCoins());
        if (amount <= 0) return;
        BoardSessionManager.updateParticipant(level, session,
                target.withStats(target.stats().spendCoins(amount)));
        BoardWorldObjectService.awardCoins(level, session, owner.slotUuid(), amount);
        var targetEntity = BoardEntityService.entity(level, target);
        if (targetEntity != null) {
            level.playSound(null, targetEntity.blockPosition(), SoundEvents.ITEM_PICKUP,
                    SoundSource.PLAYERS, 0.8F, 0.9F);
        }
    }

}
