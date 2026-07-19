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
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

public class HandcardSnatch extends BaseHandCard implements BoardBotEffect {

    public static final CardDefinition DEFINITION = CardDefinition.create(CardType.EFFECT, CardTargetTypes.PLAYERS, 32);

    public HandcardSnatch(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canTarget(ServerPlayer user, LivingEntity target, ItemStack sourceStack) {
        if (target instanceof AstralCharacterEntity character) {
            return BoardSessionManager.participantForEntity(character)
                    .filter(participant -> !participant.knockedDown() && participant.stats().starCoins() > 0)
                    .isPresent();
        }
        return AstralStats.getOrDefault(target).starCoins() > 0;
    }

    @Override
    protected boolean apply(ServerPlayer user, InteractionHand hand, List<LivingEntity> targets) {
        LivingEntity target = targets.isEmpty() ? null : targets.getFirst();
        if (target == null || !this.canTarget(user, target, user.getItemInHand(hand))) return false;
        if (target instanceof AstralCharacterEntity character) {
            BoardSession session = BoardSessionManager.findByController(user).orElse(null);
            BoardParticipant source = session == null ? null : session.participantByController(user.getUUID()).orElse(null);
            BoardParticipant selected = session == null ? null : session.participantByEntity(character.getUUID()).orElse(null);
            if (session == null || source == null || selected == null || selected.knockedDown()) return false;
            int taken = Math.min(5, selected.stats().starCoins());
            if (taken <= 0) return false;
            BoardSessionManager.updateParticipant(user.level(), session,
                    selected.withStats(selected.stats().spendCoins(taken)));
            BoardWorldObjectService.awardCoins(user.level(), session, source.slotUuid(), taken);
            return true;
        }
        AstralCardEffects.snatchCoins(user, target, 5);
        return true;
    }

    @Override
    public List<UUID> selectBoardBotTargets(BoardBotEffectContext context) {
        return context.opponentSlotsInRange(DEFINITION.range()).stream()
                .filter(slotId -> context.target(slotId).map(target -> target.stats().starCoins() > 0).orElse(false))
                .toList();
    }

    @Override
    public boolean canUseByBoardBot(BoardBotEffectContext context) {
        return !this.selectBoardBotTargets(context).isEmpty();
    }

    @Override
    public int applyByBoardBot(BoardBotEffectContext context) {
        if (!context.targetSlotIds().isEmpty()) context.snatch(context.targetSlotIds().getFirst(), 5);
        return 0;
    }
}
