package com.astral_craft.common.items.cards.pvp;

import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.gameplay.BuffKinds;
import com.astral_craft.common.gameplay.SoulLinkManager;
import com.astral_craft.common.gameplay.SoulLinkStyle;
import com.astral_craft.common.gameplay.board.*;
import com.astral_craft.common.gameplay.handcard.AstralCardEffects;
import com.astral_craft.common.gameplay.handcard.CardTargetTypes;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.stats.AstralStats;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

import java.util.*;

public class HandcardSoulLink extends BaseHandCard implements BoardBotEffect {

    public static final CardDefinition DEFINITION = CardDefinition.create(
            CardType.EFFECT, CardTargetTypes.PLAYERS_AND_MOBS, 32, 2, 2);
    private static final int BOARD_DURATION_ROUNDS = 3;

    public HandcardSoulLink(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean apply(ServerPlayer user, InteractionHand hand, List<LivingEntity> targets) {
        if (targets.size() != 2 || targets.get(0) == targets.get(1)) return false;
        LivingEntity first = targets.get(0);
        LivingEntity second = targets.get(1);
        if (first instanceof AstralCharacterEntity firstPawn && second instanceof AstralCharacterEntity secondPawn) {
            BoardSession firstSession = BoardSessionManager.findByEntity(firstPawn).orElse(null);
            BoardSession secondSession = BoardSessionManager.findByEntity(secondPawn).orElse(null);
            BoardParticipant firstParticipant = firstSession == null ? null
                    : firstSession.participantByEntity(firstPawn.getUUID()).orElse(null);
            BoardParticipant secondParticipant = secondSession == null ? null
                    : secondSession.participantByEntity(secondPawn.getUUID()).orElse(null);
            if (firstSession == null || firstSession != secondSession
                    || firstParticipant == null || secondParticipant == null
                    || !BoardSessionManager.addBoardSoulLink(user.level(), firstSession,
                    firstParticipant.slotUuid(), secondParticipant.slotUuid(), BOARD_DURATION_ROUNDS)) {
                user.sendSystemMessage(Component.translatable("message.astral_craft.soul_link.already_linked"), true);
                return false;
            }
            SoulLinkManager.ensureVisual(user.level(), firstPawn, secondPawn, SoulLinkStyle.rainbow(2.2F, 0.05F));
            return true;
        }

        if (!SoulLinkManager.link(first, second, user.level().getGameTime() + 20L * 60L,
                SoulLinkStyle.rainbow(2.2F, 0.05F))) {
            user.sendSystemMessage(Component.translatable("message.astral_craft.soul_link.already_linked"), true);
            return false;
        }
        AstralCardEffects.update(first, AstralStats.getOrDefault(first).addBuff(BuffKinds.CUSTOM, 1));
        AstralCardEffects.update(second, AstralStats.getOrDefault(second).addBuff(BuffKinds.CUSTOM, 1));
        return true;
    }

    @Override
    public boolean canUseByBoardBot(BoardBotEffectContext context) {
        return context.session().participants().stream()
                .filter(participant -> !participant.knockedDown())
                .filter(participant -> context.session().mechanics().soulLinkFor(participant.slotUuid()).isEmpty())
                .count() >= 2;
    }

    @Override
    public List<UUID> selectBoardBotTargets(BoardBotEffectContext context) {
        List<UUID> candidates = new ArrayList<>(context.session().participants().stream()
                .filter(participant -> !participant.knockedDown())
                .filter(participant -> context.session().mechanics().soulLinkFor(participant.slotUuid()).isEmpty())
                .map(BoardParticipant::slotUuid).toList());
        Collections.shuffle(candidates, new Random(context.level().getRandom().nextLong()));
        return candidates.size() < 2 ? List.of() : List.copyOf(candidates.subList(0, 2));
    }

    @Override
    public int applyByBoardBot(BoardBotEffectContext context) {
        if (context.targetSlotIds().size() < 2) return 0;
        BoardSessionManager.addBoardSoulLink(context.level(), context.session(),
                context.targetSlotIds().get(0), context.targetSlotIds().get(1), BOARD_DURATION_ROUNDS);
        return 0;
    }
}
