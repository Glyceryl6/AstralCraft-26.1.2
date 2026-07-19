package com.astral_craft.common.items.cards;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.gameplay.board.BoardParticipant;
import com.astral_craft.common.gameplay.board.BoardSession;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.gameplay.handcard.CardTargetTypes;
import com.astral_craft.common.items.BaseHandCard;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public class HandcardRedirection extends BaseHandCard {

    public static final CardDefinition DEFINITION = CardDefinition.create(CardType.EFFECT, CardTargetTypes.NONE, -1);
    public static final Identifier FREE_DIRECTION_STATUS = AstralCraft.prefix("free_direction");

    public HandcardRedirection(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean apply(ServerPlayer user, InteractionHand hand, List<LivingEntity> targets) {
        BoardSession session = BoardSessionManager.findByController(user).orElse(null);
        BoardParticipant participant = session == null ? null
                : session.participantByController(user.getUUID()).orElse(null);
        if (session == null || participant == null) return false;
        BoardSessionManager.updateParticipant(user.level(), session,
                participant.withRoundStatusEffect(FREE_DIRECTION_STATUS, 1));
        return true;
    }

    public static boolean hasFreeDirection(BoardParticipant participant) {
        return participant.hasRoundStatusEffect(FREE_DIRECTION_STATUS);
    }

    public static void consumeFreeDirection(ServerLevel level, BoardSession session, BoardParticipant participant) {
        if (!hasFreeDirection(participant)) return;
        BoardParticipant updated = participant.withoutRoundStatusEffect(FREE_DIRECTION_STATUS);
        BoardSessionManager.updateParticipant(level, session, updated);
    }

}