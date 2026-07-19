package com.astral_craft.common.items.cards;

import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.gameplay.BuffKinds;
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

public class HandcardBerserk extends BaseHandCard implements BoardBotEffect {

    public static final CardDefinition DEFINITION = CardDefinition.create(CardType.EFFECT, CardTargetTypes.PLAYERS_AND_MOBS, 10);

    public HandcardBerserk(Properties properties) {
        super(properties);
    }

    @Override
    public boolean allowsSelfTarget() {
        return true;
    }

    @Override
    public boolean canTarget(ServerPlayer user, LivingEntity target, ItemStack sourceStack) {
        return isSelfTarget(user, target);
    }

    @Override
    protected boolean apply(ServerPlayer user, InteractionHand hand, List<LivingEntity> targets) {
        LivingEntity target = AstralCardEffects.target(targets).orElse(null);
        if (!isSelfTarget(user, target)) return false;
        AstralCardEffects.update(target, AstralStats.getOrDefault(target)
                .addTemporary("attack", 3, 2).addBuff(BuffKinds.BERSERK, 1));
        return true;
    }

    @Override
    public List<UUID> selectBoardBotTargets(BoardBotEffectContext context) {
        return List.of(context.userSlotId());
    }

    @Override
    public int applyByBoardBot(BoardBotEffectContext context) {
        context.updateUser(stats -> stats.addTemporary("attack", 3, 2).addBuff(BuffKinds.BERSERK, 1));
        return 0;
    }

    private static boolean isSelfTarget(ServerPlayer user, LivingEntity target) {
        if (user == null || target == null) return false;
        if (target == user) return true;
        if (!(target instanceof AstralCharacterEntity character) || !character.isBoardPawn()) return false;
        BoardSession session = BoardSessionManager.findByController(user).orElse(null);
        BoardParticipant source = session == null ? null
                : session.participantByController(user.getUUID()).orElse(null);
        UUID boardId = character.boardSessionUuid().orElse(null);
        UUID slotId = character.boardParticipantUuid().orElse(null);
        return session != null && source != null && session.id().equals(boardId)
                && source.slotUuid().equals(slotId);
    }

}