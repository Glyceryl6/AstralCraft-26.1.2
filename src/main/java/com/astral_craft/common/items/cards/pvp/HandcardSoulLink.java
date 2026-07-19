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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

import java.util.*;

public class HandcardSoulLink extends BaseHandCard implements BoardBotEffect {

    public static final CardDefinition DEFINITION = CardDefinition.create(
            CardType.EFFECT, CardTargetTypes.PLAYERS_AND_MOBS, 32, 2, 2);
    private static final int BOARD_DURATION_ROUNDS = 3;
    private static boolean mirroringBoardDamage;

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
                    || !addBoardLink(user.level(), firstSession,
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
                .filter(participant -> !participant.knockedDown()).map(BoardParticipant::slotUuid)
                .filter(slotId -> context.session().mechanics().soulLinkFor(slotId).isEmpty()).toList());
        Collections.shuffle(candidates, new Random(context.level().getRandom().nextLong()));
        return candidates.size() < 2 ? List.of() : List.copyOf(candidates.subList(0, 2));
    }

    @Override
    public int applyByBoardBot(BoardBotEffectContext context) {
        if (context.targetSlotIds().size() < 2) return 0;
        addBoardLink(context.level(), context.session(), context.targetSlotIds().get(0), context.targetSlotIds().get(1), BOARD_DURATION_ROUNDS);
        return 0;
    }

    public static boolean addBoardLink(ServerLevel level, BoardSession session, UUID firstSlotId, UUID secondSlotId, int rounds) {
        if (session == null || !session.mechanics().addSoulLink(firstSlotId, secondSlotId, rounds)) return false;
        BoardSessionManager.markChanged(level);
        return true;
    }

    public static void tickBoardLinks(ServerLevel level, BoardSession session) {
        List<BoardMechanicsState.BoardSoulLink> expired = session.mechanics().tickSoulLinks();
        for (BoardMechanicsState.BoardSoulLink link : expired) {
            removeVisual(level, session, link);
        }
        if (!expired.isEmpty()) BoardSessionManager.markChanged(level);
    }

    public static void removeBoardLink(ServerLevel level, BoardSession session, UUID slotId) {
        BoardMechanicsState.BoardSoulLink link = session.mechanics().soulLinkFor(slotId).orElse(null);
        if (link == null) return;
        removeVisual(level, session, link);
        session.mechanics().removeSoulLink(link.id());
        BoardSessionManager.markChanged(level);
    }

    public static void mirrorBoardDamage(ServerLevel level, BoardSession session, BoardParticipant damaged, int logicalDamage) {
        if (logicalDamage <= 0) return;
        BoardMechanicsState.BoardSoulLink link = session.mechanics()
                .soulLinkFor(damaged.slotUuid()).orElse(null);
        AstralCharacterEntity entity = BoardEntityService.entity(level, damaged);
        if (link == null) {
            if (entity != null) SoulLinkManager.mirrorLogicalDamage(level, entity, logicalDamage);
            return;
        }
        if (mirroringBoardDamage) return;
        UUID otherSlotId = link.other(damaged.slotUuid()).orElse(null);
        if (otherSlotId == null) return;
        mirroringBoardDamage = true;
        try {
            BoardSessionManager.damageFromEffect(level, session, otherSlotId, logicalDamage);
        } finally {
            mirroringBoardDamage = false;
        }
    }

    public static void reconcileBoardVisuals(ServerLevel level, BoardSession session) {
        for (BoardMechanicsState.BoardSoulLink link : session.mechanics().soulLinks()) {
            BoardParticipant first = session.participant(link.firstSlotId()).orElse(null);
            BoardParticipant second = session.participant(link.secondSlotId()).orElse(null);
            AstralCharacterEntity firstEntity = first == null ? null : BoardEntityService.entity(level, first);
            AstralCharacterEntity secondEntity = second == null ? null : BoardEntityService.entity(level, second);
            if (firstEntity != null && secondEntity != null) {
                SoulLinkManager.ensureVisual(level, firstEntity, secondEntity, SoulLinkStyle.rainbow(2.2F, 0.05F));
            }
        }
    }

    public static void clearBoardVisuals(ServerLevel level, BoardSession session) {
        for (BoardMechanicsState.BoardSoulLink link : session.mechanics().soulLinks()) {
            removeVisual(level, session, link);
        }
    }

    private static void removeVisual(ServerLevel level, BoardSession session, BoardMechanicsState.BoardSoulLink link) {
        BoardParticipant first = session.participant(link.firstSlotId()).orElse(null);
        BoardParticipant second = session.participant(link.secondSlotId()).orElse(null);
        AstralCharacterEntity firstEntity = first == null ? null : BoardEntityService.entity(level, first);
        AstralCharacterEntity secondEntity = second == null ? null : BoardEntityService.entity(level, second);
        if (firstEntity != null && secondEntity != null) {
            SoulLinkManager.removeVisual(level, firstEntity, secondEntity);
        }
    }

}