package com.astral_craft.common.items.cards.pvp;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.gameplay.board.BoardBotEffect;
import com.astral_craft.common.gameplay.board.BoardBotEffectContext;
import com.astral_craft.common.gameplay.board.BoardParticipant;
import com.astral_craft.common.gameplay.board.BoardSession;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.gameplay.handcard.AstralCardEffects;
import com.astral_craft.common.gameplay.handcard.CardTargetTypes;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.registry.AstralBoardBuffs;
import com.astral_craft.common.stats.AstralStats;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public class HandcardAllOrNothing extends BaseHandCard implements BoardBotEffect {

    public static final CardDefinition DEFINITION = CardDefinition.create(CardType.EFFECT, CardTargetTypes.NONE, -1);
    public static final Identifier BUFF_ID = AstralCraft.prefix("all_or_nothing");

    public HandcardAllOrNothing(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean apply(ServerPlayer user, InteractionHand hand, List<LivingEntity> targets) {
        BoardSession session = BoardSessionManager.findByController(user).orElse(null);
        BoardParticipant participant = session == null ? null : session.participantByController(user.getUUID()).orElse(null);
        if (session != null && participant != null) {
            BoardSessionManager.updateParticipant(user.level(), session, participant.withStats(participant.stats().addBuff(
                    AstralBoardBuffs.instance(BUFF_ID, AstralBoardBuffs.ATTACK.get()).value(5).permanent().build())));
            return true;
        }
        AstralCardEffects.update(user, AstralStats.get(user).addBuff(
                AstralBoardBuffs.instance(BUFF_ID, AstralBoardBuffs.ATTACK.get()).value(5).permanent().build()));
        return true;
    }

    @Override
    public boolean canUseByBoardBot(BoardBotEffectContext context) {
        return !context.user().stats().hasBuff(BUFF_ID);
    }

    @Override
    public int applyByBoardBot(BoardBotEffectContext context) {
        BoardSessionManager.updateParticipant(context.level(), context.session(), context.user().withStats(
                context.user().stats().addBuff(AstralBoardBuffs.instance(BUFF_ID, AstralBoardBuffs.ATTACK.get())
                        .value(5).permanent().build())));
        return 0;
    }

}