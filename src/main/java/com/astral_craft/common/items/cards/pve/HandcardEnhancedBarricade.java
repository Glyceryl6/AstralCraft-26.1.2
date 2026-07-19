package com.astral_craft.common.items.cards.pve;

import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.gameplay.board.BoardBotEffect;
import com.astral_craft.common.gameplay.board.BoardBotEffectContext;
import com.astral_craft.common.gameplay.board.BoardMechanicsState.BoardTrapType;
import com.astral_craft.common.gameplay.board.BoardPanelPlacementCard;
import com.astral_craft.common.gameplay.board.BoardWorldObjectService;
import com.astral_craft.common.gameplay.handcard.CardTargetTypes;
import com.astral_craft.common.items.BaseHandCard;

public class HandcardEnhancedBarricade extends BaseHandCard implements BoardPanelPlacementCard, BoardBotEffect {
    public static final CardDefinition DEFINITION = CardDefinition.create(CardType.EFFECT, CardTargetTypes.NONE, 10);

    public HandcardEnhancedBarricade(Properties properties) {
        super(properties);
    }

    @Override
    public BoardTrapType boardTrapType() {
        return BoardTrapType.ENHANCED_BARRICADE;
    }

    @Override
    public int boardPlacementRange() {
        return DEFINITION.range();
    }

    @Override
    public boolean revealWhenPlaced() {
        return false;
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
}
