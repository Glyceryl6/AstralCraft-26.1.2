package com.astral_craft.common.items.cards.pvp;

import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.entity.SoulLinkEntity;
import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.gameplay.BuffKinds;
import com.astral_craft.common.gameplay.board.*;
import com.astral_craft.common.gameplay.handcard.AstralCardEffects;
import com.astral_craft.common.gameplay.handcard.CardTargetTypes;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.stats.AstralStats;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.*;

public class HandcardSoulLink extends BaseHandCard implements BoardBotEffect {

    public static final CardDefinition DEFINITION = CardDefinition.create(
            CardType.EFFECT, CardTargetTypes.PLAYERS_AND_MOBS, 32, 2, 2);
    private static final int BOARD_DURATION_ROUNDS = 3;
    private static final Map<UUID, Link> LINKS = new HashMap<>();
    private static boolean mirroringDamage;
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

            ensureVisual(user.level(), firstPawn, secondPawn, SoulLinkEntity.VisualStyle.rainbow(2.2F, 0.05F));
            return true;
        }

        if (!link(first, second, user.level().getGameTime() + 20L * 60L,
                SoulLinkEntity.VisualStyle.rainbow(2.2F, 0.05F))) {
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

    public static boolean isLinked(LivingEntity entity) {
        Link link = LINKS.get(entity.getUUID());
        if (link == null) return false;
        if (entity.level() instanceof ServerLevel level && level.getGameTime() > link.untilGameTime()) {
            remove(link);
            return false;
        }
        return true;
    }

    public static boolean link(LivingEntity first, LivingEntity second, long untilGameTime) {
        return link(first, second, untilGameTime, SoulLinkEntity.VisualStyle.DEFAULT);
    }

    public static boolean link(LivingEntity first, LivingEntity second, long untilGameTime, SoulLinkEntity.VisualStyle style) {
        if (!(first.level() instanceof ServerLevel level) || first.level() != second.level()) return false;
        if (isLinked(first) || isLinked(second)) return false;
        int lifetime = Math.max(1, (int) (untilGameTime - level.getGameTime()));
        SoulLinkEntity visual = new SoulLinkEntity(level, first, second, lifetime, style);
        level.addFreshEntity(visual);
        Link link = new Link(first.getUUID(), second.getUUID(), untilGameTime);
        LINKS.put(first.getUUID(), link);
        LINKS.put(second.getUUID(), link);
        return true;
    }

    public static void onDamagePre(LivingDamageEvent.Pre event) {
        if (mirroringDamage || event.getNewDamage() <= 0.0F || !(event.getEntity().level() instanceof ServerLevel level)) return;
        Link link = LINKS.get(event.getEntity().getUUID());
        if (link == null) return;
        if (level.getGameTime() > link.untilGameTime()) {
            remove(link);
            return;
        }
        Entity other = level.getEntity(link.other(event.getEntity().getUUID()));
        if (!(other instanceof LivingEntity living) || !living.isAlive()) {
            remove(link);
            return;
        }
        mirroringDamage = true;
        try {
            living.hurtServer(level, event.getSource(), event.getNewDamage());
        } finally {
            mirroringDamage = false;
        }
    }

    public static void ensureVisual(ServerLevel level, LivingEntity first, LivingEntity second, SoulLinkEntity.VisualStyle style) {
        if (level == null || first == null || second == null || first.level() != level || second.level() != level) return;
        boolean present = !level.getEntitiesOfClass(SoulLinkEntity.class, visualBounds(first, second), visual ->
                (visual.firstId() == first.getId() && visual.secondId() == second.getId())
                        || (visual.firstId() == second.getId() && visual.secondId() == first.getId())).isEmpty();
        if (present) return;
        SoulLinkEntity visual = new SoulLinkEntity(level, first, second, Integer.MAX_VALUE,
                style == null ? SoulLinkEntity.VisualStyle.DEFAULT : style);
        level.addFreshEntity(visual);
    }

    public static void removeVisual(ServerLevel level, LivingEntity first, LivingEntity second) {
        if (level == null || first == null || second == null) return;
        for (SoulLinkEntity visual : level.getEntitiesOfClass(SoulLinkEntity.class, visualBounds(first, second), candidate ->
                (candidate.firstId() == first.getId() && candidate.secondId() == second.getId())
                        || (candidate.firstId() == second.getId() && candidate.secondId() == first.getId()))) {
            visual.discard();
        }
    }

    private static AABB visualBounds(LivingEntity first, LivingEntity second) {
        double minX = Math.min(first.getX(), second.getX());
        double minY = Math.min(first.getY(), second.getY());
        double minZ = Math.min(first.getZ(), second.getZ());
        double maxX = Math.max(first.getX(), second.getX());
        double maxY = Math.max(first.getY() + first.getBbHeight(), second.getY() + second.getBbHeight());
        double maxZ = Math.max(first.getZ(), second.getZ());
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ).inflate(8.0D);
    }

    public static void mirrorLogicalDamage(ServerLevel level, LivingEntity damaged, int amount) {
        if (mirroringDamage || level == null || damaged == null || amount <= 0) return;
        Link link = LINKS.get(damaged.getUUID());
        if (link == null) return;
        if (level.getGameTime() > link.untilGameTime()) {
            remove(link);
            return;
        }
        Entity other = level.getEntity(link.other(damaged.getUUID()));
        if (!(other instanceof LivingEntity living) || !living.isAlive()) {
            remove(link);
            return;
        }
        mirroringDamage = true;
        try {
            living.hurtServer(level, level.damageSources().generic(), amount);
        } finally {
            mirroringDamage = false;
        }
    }

    public static void remove(UUID entityId) {
        Link link = LINKS.get(entityId);
        if (link != null) remove(link);
    }

    private static void remove(Link link) {
        LINKS.remove(link.first());
        LINKS.remove(link.second());
    }

    public static boolean addBoardLink(ServerLevel level, BoardSession session, UUID firstSlotId, UUID secondSlotId, int rounds) {
        if (session == null || !session.mechanics().addSoulLink(firstSlotId, secondSlotId, rounds)) return false;
        BoardSessionManager.markChanged(level);
        return true;
    }

    public static void tickBoardLinks(ServerLevel level, BoardSession session) {
        List<BoardMechanicsState.BoardSoulLink> expired = session.mechanics().tickSoulLinks();
        for (BoardMechanicsState.BoardSoulLink link : expired) {
            removeBoardVisual(level, session, link);
        }
        if (!expired.isEmpty()) BoardSessionManager.markChanged(level);
    }

    public static void removeBoardLink(ServerLevel level, BoardSession session, UUID slotId) {
        BoardMechanicsState.BoardSoulLink link = session.mechanics().soulLinkFor(slotId).orElse(null);
        if (link == null) return;
        removeBoardVisual(level, session, link);
        session.mechanics().removeSoulLink(link.id());
        BoardSessionManager.markChanged(level);
    }

    public static void mirrorBoardDamage(ServerLevel level, BoardSession session, BoardParticipant damaged, int logicalDamage) {
        if (logicalDamage <= 0) return;
        BoardMechanicsState.BoardSoulLink link = session.mechanics()
                .soulLinkFor(damaged.slotUuid()).orElse(null);
        AstralCharacterEntity entity = BoardEntityService.entity(level, damaged);
        if (link == null) {
            if (entity != null) mirrorLogicalDamage(level, entity, logicalDamage);
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
                ensureVisual(level, firstEntity, secondEntity, SoulLinkEntity.VisualStyle.rainbow(2.2F, 0.05F));
            }
        }
    }

    public static void clearBoardVisuals(ServerLevel level, BoardSession session) {
        for (BoardMechanicsState.BoardSoulLink link : session.mechanics().soulLinks()) {
            removeBoardVisual(level, session, link);
        }
    }

    private static void removeBoardVisual(ServerLevel level, BoardSession session, BoardMechanicsState.BoardSoulLink link) {
        BoardParticipant first = session.participant(link.firstSlotId()).orElse(null);
        BoardParticipant second = session.participant(link.secondSlotId()).orElse(null);
        AstralCharacterEntity firstEntity = first == null ? null : BoardEntityService.entity(level, first);
        AstralCharacterEntity secondEntity = second == null ? null : BoardEntityService.entity(level, second);
        if (firstEntity != null && secondEntity != null) {
            removeVisual(level, firstEntity, secondEntity);
        }
    }

    private record Link(UUID first, UUID second, long untilGameTime) {
        private UUID other(UUID source) {
            return source.equals(this.first) ? this.second : this.first;
        }
    }

}