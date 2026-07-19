package com.astral_craft.common.items.cards.pvp;

import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.gameplay.board.*;
import com.astral_craft.common.gameplay.board.BoardMechanicsState.BoardTrapType;
import com.astral_craft.common.gameplay.handcard.CardTargetTypes;
import com.astral_craft.common.items.BaseHandCard;

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
    public boolean revealWhenPlaced() {
        return true;
    }

    @Override
    public boolean canUseByBoardBot(BoardBotEffectContext context) {
        return BoardPanelSelectionService.randomValidNode(context.session(), context.user(),
                this.boardPlacementRange(), context.level().getRandom()).isPresent();
    }

    @Override
    public int applyByBoardBot(BoardBotEffectContext context) {
        BoardPanelSelectionService.randomValidNode(context.session(), context.user(),
                this.boardPlacementRange(), context.level().getRandom()).ifPresent(nodeId ->
                BoardWorldObjectService.placeTrap(context.level(), context.session(), this.boardTrapType(),
                        context.userSlotId(), nodeId));
        return 0;
    }
}
