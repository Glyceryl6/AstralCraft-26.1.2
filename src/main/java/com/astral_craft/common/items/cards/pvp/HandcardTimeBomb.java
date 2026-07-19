package com.astral_craft.common.items.cards.pvp;

import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.gameplay.board.*;
import com.astral_craft.common.gameplay.handcard.CardTargetTypes;
import com.astral_craft.common.items.BaseHandCard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class HandcardTimeBomb extends BaseHandCard implements BoardBotEffect {

    public static final CardDefinition DEFINITION = CardDefinition.create(CardType.EFFECT, CardTargetTypes.NONE, -1);

    public HandcardTimeBomb(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canUse(ServerPlayer user, ItemStack sourceStack) {
        BoardSession session = BoardSessionManager.findByController(user).orElse(null);
        BoardParticipant participant = session == null ? null
                : session.participantByController(user.getUUID()).orElse(null);
        return session != null && participant != null
                && session.mechanics().timeBombSlot().isEmpty()
                && BoardSessionManager.nextActionSlot(session, participant.slotUuid()).isPresent();
    }

    @Override
    public boolean onRevealFinished(ServerPlayer user, InteractionHand hand, ItemStack itemStack,
                                    CardDefinition definition) {
        BoardSession session = BoardSessionManager.findByController(user).orElse(null);
        BoardParticipant participant = session == null ? null
                : session.participantByController(user.getUUID()).orElse(null);
        return session != null && participant != null
                && session.mechanics().timeBombSlot().isEmpty()
                && BoardSessionManager.giveTimeBombToNext(user.level(), session, participant.slotUuid());
    }

    @Override
    public boolean canUseByBoardBot(BoardBotEffectContext context) {
        return context.session().mechanics().timeBombSlot().isEmpty()
                && BoardSessionManager.nextActionSlot(context.session(), context.userSlotId()).isPresent();
    }

    @Override
    public int applyByBoardBot(BoardBotEffectContext context) {
        BoardSessionManager.giveTimeBombToNext(context.level(), context.session(), context.userSlotId());
        return 0;
    }
}
