package com.astral_craft.common.items.cards.pvp;

import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.gameplay.board.*;
import com.astral_craft.common.gameplay.handcard.AstralCardEffects;
import com.astral_craft.common.gameplay.handcard.CardTargetTypes;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.stats.AstralStats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.UUID;

public class HandcardSelfExplosion extends BaseHandCard implements BoardBotEffect {

    public static final CardDefinition DEFINITION = CardDefinition.create(CardType.EFFECT, CardTargetTypes.NONE, 5);

    public HandcardSelfExplosion(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean apply(ServerPlayer user, InteractionHand hand, List<LivingEntity> targets) {
        BoardSession session = BoardSessionManager.findByController(user).orElse(null);
        BoardParticipant source = session == null ? null : session.participantByController(user.getUUID()).orElse(null);
        if (session != null && source != null) {
            AstralCharacterEntity entity = BoardEntityService.entity(user.level(), source);
            if (entity != null) {
                BoardWorldObjectService.playExplosion(user.level(), entity.getX(), entity.getY() + 0.8D, entity.getZ());
            }

            for (BoardParticipant target : session.participants()) {
                if (target.slotUuid().equals(source.slotUuid()) || target.knockedDown()) continue;
                int distance = BoardRouteService.graphDistance(session, source.currentNodeKey(),
                        target.currentNodeKey(), DEFINITION.range());
                if (distance >= 0 && distance <= DEFINITION.range()) {
                    BoardSessionManager.damageFromEffect(user.level(), session, target.slotUuid(), 5);
                }
            }

            BoardSessionManager.damageFromEffect(user.level(), session, source.slotUuid(), 3);
            return true;
        }

        BoardWorldObjectService.playExplosion(user.level(), user.getX(), user.getY() + 0.8D, user.getZ());
        AstralCardEffects.update(user, AstralStats.get(user).damage(3));
        AstralCardEffects.areaDamage(user, 5, 5, true);
        return true;
    }

    @Override
    public List<UUID> selectBoardBotTargets(BoardBotEffectContext context) {
        return List.of();
    }

    @Override
    public int applyByBoardBot(BoardBotEffectContext context) {
        AstralCharacterEntity source = BoardEntityService.entity(context.level(), context.user());
        if (source != null) {
            BoardWorldObjectService.playExplosion(context.level(), source.getX(), source.getY() + 0.8D, source.getZ());
        }

        for (UUID targetSlotId : context.opponentSlotsInRange(DEFINITION.range())) context.damageTarget(targetSlotId, 5, false);
        context.damageUser(3);
        return 0;
    }

}
