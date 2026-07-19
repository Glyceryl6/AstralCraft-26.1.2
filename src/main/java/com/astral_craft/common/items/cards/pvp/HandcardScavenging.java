package com.astral_craft.common.items.cards.pvp;

import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.gameplay.board.*;
import com.astral_craft.common.gameplay.handcard.CardTargetTypes;
import com.astral_craft.common.items.BaseHandCard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class HandcardScavenging extends BaseHandCard implements BoardBotEffect {

    public static final CardDefinition DEFINITION = CardDefinition.create(CardType.EFFECT, CardTargetTypes.NONE, 3);

    public HandcardScavenging(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canUse(ServerPlayer user, ItemStack sourceStack) {
        BoardSession session = BoardSessionManager.findByController(user).orElse(null);
        BoardParticipant participant = session == null ? null
                : session.participantByController(user.getUUID()).orElse(null);
        return session != null && participant != null
                && BoardWorldObjectService.hasDroppedCoinsInRange(session, participant, DEFINITION.range());
    }

    @Override
    public boolean onRevealFinished(ServerPlayer user, InteractionHand hand, ItemStack itemStack,
                                    CardDefinition definition) {
        BoardSession session = BoardSessionManager.findByController(user).orElse(null);
        BoardParticipant participant = session == null ? null
                : session.participantByController(user.getUUID()).orElse(null);
        return session != null && participant != null
                && BoardWorldObjectService.collectNearbyCoins(user.level(), session, participant, definition.range()) > 0;
    }

    @Override
    public boolean canUseByBoardBot(BoardBotEffectContext context) {
        return BoardWorldObjectService.hasDroppedCoinsInRange(
                context.session(), context.user(), context.definition().range());
    }

    @Override
    public int applyByBoardBot(BoardBotEffectContext context) {
        BoardWorldObjectService.collectNearbyCoins(context.level(), context.session(),
                context.user(), context.definition().range());
        return 0;
    }
}
