package com.astral_craft.common.items.cards.pvp;

import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.gameplay.board.*;
import com.astral_craft.common.gameplay.handcard.AstralCardEffects;
import com.astral_craft.common.gameplay.handcard.CardTargetTypes;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.stats.AstralStats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public class HandcardAllOrNothing extends BaseHandCard implements BoardBotEffect {

    public static final CardDefinition DEFINITION = CardDefinition.create(CardType.EFFECT, CardTargetTypes.NONE, -1);

    public HandcardAllOrNothing(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean apply(ServerPlayer user, InteractionHand hand, List<LivingEntity> targets) {
        BoardSession session = BoardSessionManager.findByController(user).orElse(null);
        BoardParticipant participant = session == null ? null
                : session.participantByController(user.getUUID()).orElse(null);
        if (session != null && participant != null) {
            return BoardSessionManager.activateAllOrNothing(user.level(), session, participant.slotUuid());
        }
        AstralCardEffects.update(user, AstralStats.get(user).addTemporary("attack", 5, 1));
        return true;
    }

    @Override
    public boolean canUseByBoardBot(BoardBotEffectContext context) {
        return !BoardSessionManager.hasAllOrNothing(context.user());
    }

    @Override
    public int applyByBoardBot(BoardBotEffectContext context) {
        BoardSessionManager.activateAllOrNothing(context.level(), context.session(), context.userSlotId());
        return 0;
    }
}
