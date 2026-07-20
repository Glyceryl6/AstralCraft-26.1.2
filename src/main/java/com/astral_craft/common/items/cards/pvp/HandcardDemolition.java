package com.astral_craft.common.items.cards.pvp;

import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.gameplay.board.*;
import com.astral_craft.common.gameplay.board.BoardMechanicsState.BoardTrapType;
import com.astral_craft.common.gameplay.handcard.CardTargetTypes;
import com.astral_craft.common.items.BaseHandCard;
import net.minecraft.server.level.ServerLevel;

public class HandcardDemolition extends BaseHandCard implements BoardPanelPlacementCard, BoardBotEffect {

    public static final CardDefinition DEFINITION = CardDefinition.create(CardType.EFFECT, CardTargetTypes.NONE, 3);

    public HandcardDemolition(Properties properties) {
        super(properties);
    }

    @Override
    public BoardTrapType boardTrapType() {
        return BoardTrapType.DEMOLITION;
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
                BoardWorldObjectService.placeTrap(context.level(), context.session(), this.boardTrapType(), context.userSlotId(), nodeId));
        return 0;
    }

    public static void trigger(ServerLevel level, BoardSession session, BoardParticipant target) {
        var entity = BoardEntityService.entity(level, target);
        if (entity != null) {
            BoardWorldObjectService.playExplosion(level, entity.getX(),
                    entity.getY() + 0.7D, entity.getZ());
        }
        BoardSessionManager.damageFromEffect(level, session, target.slotUuid(), 3);
    }

}