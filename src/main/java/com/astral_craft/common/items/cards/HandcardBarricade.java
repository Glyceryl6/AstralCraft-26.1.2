package com.astral_craft.common.items.cards;

import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.gameplay.board.*;
import com.astral_craft.common.gameplay.board.BoardMechanicsState.BoardTrapType;
import com.astral_craft.common.gameplay.handcard.CardTargetTypes;
import com.astral_craft.common.gameplay.handcard.CardUseService;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.network.s2c.CardRevealPayload;
import com.astral_craft.common.registry.AstralItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class HandcardBarricade extends BaseHandCard implements BoardPanelPlacementCard, BoardBotEffect {

    public static final CardDefinition DEFINITION = CardDefinition.create(CardType.EFFECT, CardTargetTypes.NONE, 10);

    public HandcardBarricade(Properties properties) {
        super(properties);
    }

    @Override
    public BoardTrapType boardTrapType() {
        return BoardTrapType.BARRICADE;
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
                BoardWorldObjectService.placeTrap(context.level(), context.session(), this.boardTrapType(), context.userSlotId(), nodeId));
        return 0;
    }

    public static void playTriggeredReveal(ServerLevel level, BoardSession session, BoardParticipant ownerParticipant, boolean enhanced) {
        List<ServerPlayer> viewers = BoardSessionManager.humanPlayers(level, session);
        if (viewers.isEmpty()) return;
        ServerPlayer owner = ownerParticipant.controllerUuid()
                .map(level.getServer().getPlayerList()::getPlayer).orElse(viewers.getFirst());
        BaseHandCard card = (BaseHandCard) (enhanced
                ? AstralItems.HANDCARD_ENHANCED_BARRICADE.get()
                : AstralItems.HANDCARD_BARRICADE.get());
        ItemStack stack = new ItemStack(card);
        for (ServerPlayer viewer : viewers) {
            CardUseService.sendReveal(viewer, stack, owner, card.definition(stack),
                    CardRevealPayload.ANIMATION_FLIP, CardUseService.CARD_REVEAL_DURATION_TICKS);
        }
    }

}
