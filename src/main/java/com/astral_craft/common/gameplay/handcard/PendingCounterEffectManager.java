package com.astral_craft.common.gameplay.handcard;

import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.entity.projectile.CardProjectileSettings;
import com.astral_craft.common.entity.projectile.FirecrackersProjectileEntity;
import com.astral_craft.common.entity.projectile.SlingshotProjectileEntity;
import com.astral_craft.common.entity.projectile.SnowballAttackProjectileEntity;
import com.astral_craft.common.entity.visual.FallingBrickEntity;
import com.astral_craft.common.entity.visual.LaserStrikeEntity;
import com.astral_craft.common.gameplay.board.BoardEntityService;
import com.astral_craft.common.gameplay.board.BoardParticipant;
import com.astral_craft.common.gameplay.board.BoardSession;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.items.cards.pvp.HandcardBarrier;
import com.astral_craft.common.items.cards.pvp.HandcardEyeForAnEye;
import com.astral_craft.common.items.cards.pvp.HandcardRandomSelect;
import com.astral_craft.common.network.s2c.CardRevealControlPayload;
import com.astral_craft.common.network.s2c.CardRevealPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves effect-card counter windows. Board cards are intercepted before their effect callback is
 * executed, allowing Barrier, Eye for an Eye, and Random Select to form a redirectable counter chain.
 * The older world-player damage window is retained for cards used outside a board session.
 */
public class PendingCounterEffectManager {

    public static final int DEFAULT_RESPONSE_TICKS = 20 * 12;
    private static final int REDIRECT_REVEAL_DELAY_TICKS = CardUseService.CARD_REVEAL_DURATION_TICKS + 20;

    private static final Map<UUID, PendingEffect> BY_TARGET = new ConcurrentHashMap<>();
    private static final Map<UUID, PendingBoardCounter> BOARD_BY_CONTROLLER = new ConcurrentHashMap<>();
    private static final ThreadLocal<Boolean> RESOLVING_BOARD_CARD = ThreadLocal.withInitial(() -> false);

    public static boolean hasBoardCounter(ServerPlayer source, List<LivingEntity> targets) {
        if (source == null || targets == null || targets.size() != 1
                || !(targets.getFirst() instanceof AstralCharacterEntity target) || !target.isBoardPawn()) return false;
        BoardSession session = BoardSessionManager.findByController(source).orElse(null);
        BoardParticipant sourceParticipant = session == null ? null
                : session.participantByController(source.getUUID()).orElse(null);
        BoardParticipant targetParticipant = session == null ? null
                : session.participantByEntity(target.getUUID()).orElse(null);
        return sourceParticipant != null && targetParticipant != null
                && !sourceParticipant.slotUuid().equals(targetParticipant.slotUuid())
                && !counterIndexes(targetParticipant).isEmpty();
    }

    public static void offerBoardCard(ServerPlayer source, ItemStack sourceStack, CardDefinition definition,
                                      List<LivingEntity> targets, BoardCardResolver resolver) {
        offerBoardCard(source, sourceStack, definition, targets, null, resolver);
    }

    public static void offerBoardCard(ServerPlayer source, ItemStack sourceStack, CardDefinition definition,
                                      List<LivingEntity> targets, UUID heldRevealId, BoardCardResolver resolver) {
        if (source == null || sourceStack.isEmpty() || definition == null || resolver == null) return;
        List<LivingEntity> capturedTargets = List.copyOf(targets == null ? List.of() : targets);
        if (capturedTargets.size() != 1 || !(capturedTargets.getFirst() instanceof AstralCharacterEntity target)
                || !target.isBoardPawn()) {
            finishUninterceptedBoardCard(source, capturedTargets, resolver);
            return;
        }

        BoardSession session = BoardSessionManager.findByController(source).orElse(null);
        BoardParticipant sourceParticipant = session == null ? null
                : session.participantByController(source.getUUID()).orElse(null);
        BoardParticipant targetParticipant = session == null ? null
                : session.participantByEntity(target.getUUID()).orElse(null);
        if (session == null || sourceParticipant == null || targetParticipant == null
                || sourceParticipant.slotUuid().equals(targetParticipant.slotUuid())) {
            if (heldRevealId == null) {
                finishUninterceptedBoardCard(source, capturedTargets, resolver);
            } else {
                releaseReveal(source, session, heldRevealId);
                PendingCardActionManager.schedule(source, 20,
                        () -> finishUninterceptedBoardCard(source, capturedTargets, resolver));
            }
            return;
        }

        PendingBoardCounter chain = new PendingBoardCounter(session, source, sourceParticipant.slotUuid(),
                targetParticipant.slotUuid(), targetParticipant.slotUuid(), sourceStack.copyWithCount(1),
                definition, resolver, heldRevealId, false, 0L);
        presentBoardTarget(chain, false);
    }

    public static boolean respondBoard(ServerPlayer responder, UUID boardId, int handIndex) {
        if (responder == null || boardId == null) return false;
        PendingBoardCounter chain = BOARD_BY_CONTROLLER.get(responder.getUUID());
        if (chain == null || !chain.session().id().equals(boardId)) return false;
        BoardParticipant target = chain.session().participant(chain.currentTargetSlot()).orElse(null);
        if (target == null || !target.controlledBy(responder.getUUID())) return false;
        if (handIndex >= 0 && !isUsableCounterIndex(target, handIndex)) {
            BoardSessionManager.openCounterScreen(responder, chain.session(), target,
                    remainingResponseTicks(chain));
            return false;
        }
        if (!BOARD_BY_CONTROLLER.remove(responder.getUUID(), chain)) return false;
        boolean handled = respondBoard(chain, target, responder, handIndex);
        if (!handled) {
            BOARD_BY_CONTROLLER.put(responder.getUUID(), chain);
            BoardSessionManager.openCounterScreen(responder, chain.session(), target,
                    remainingResponseTicks(chain));
        }
        return handled;
    }

    public static void offerDirectDamage(ServerPlayer source, LivingEntity target, int damage) {
        offer(PendingEffect.direct(source, BoardEntityService.effectSourceEntity(source), target, damage));
    }

    public static void offerLaser(ServerPlayer source, LivingEntity target, int damage, int argb, float radius) {
        offer(PendingEffect.laser(source, BoardEntityService.effectSourceEntity(source), target, damage, argb, radius));
    }

    public static void offerRailgun(ServerPlayer source, LivingEntity target, int damage, int argb, float radius) {
        offer(PendingEffect.laser(source, BoardEntityService.effectSourceEntity(source), target, damage, argb, radius));
    }

    public static void offerFirecracker(ServerPlayer source, LivingEntity target, int damage) {
        offerFirecracker(source, target, damage, CardProjectileSettings.firecrackers());
    }

    public static void offerFirecracker(ServerPlayer source, LivingEntity target, int damage, CardProjectileSettings settings) {
        offer(PendingEffect.projectile(source, BoardEntityService.effectSourceEntity(source), target, damage, VisualKind.FIRECRACKERS, settings));
    }

    public static void offerSlingshot(ServerPlayer source, LivingEntity target, int damage) {
        offerSlingshot(source, target, damage, CardProjectileSettings.slingshot());
    }

    public static void offerSlingshot(ServerPlayer source, LivingEntity target, int damage, CardProjectileSettings settings) {
        offer(PendingEffect.projectile(source, BoardEntityService.effectSourceEntity(source), target, damage,
                VisualKind.SLINGSHOT, settings));
    }

    public static void offerSnowballAttack(ServerPlayer source, LivingEntity target, int damage) {
        offerSnowballAttack(source, target, damage, CardProjectileSettings.snowballAttack());
    }

    public static void offerSnowballAttack(ServerPlayer source, LivingEntity target, int damage, CardProjectileSettings settings) {
        offer(PendingEffect.projectile(source, BoardEntityService.effectSourceEntity(source), target, damage,
                VisualKind.SNOWBALL_ATTACK, settings));
    }

    public static void offerFallingBrick(ServerPlayer source, LivingEntity target, int damage) {
        offer(PendingEffect.projectile(source, BoardEntityService.effectSourceEntity(source), target, damage,
                VisualKind.FALLING_BRICK, CardProjectileSettings.slingshot()));
    }

    private static void offer(PendingEffect effect) {
        if (RESOLVING_BOARD_CARD.get() || !(effect.target() instanceof ServerPlayer player)) {
            resolve(effect, effect.target());
            return;
        }

        BY_TARGET.put(player.getUUID(), effect.withTicksLeft(DEFAULT_RESPONSE_TICKS));
        player.sendSystemMessage(Component.translatable("message.astral_craft.counter.prompt",
                effect.source().getDisplayName()).withStyle(ChatFormatting.AQUA), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS, 0.7F, 1.35F);
    }

    public static boolean respond(ServerPlayer target, CounterAction action) {
        PendingEffect effect = BY_TARGET.remove(target.getUUID());
        if (effect == null) {
            target.sendSystemMessage(Component.translatable("message.astral_craft.counter.none")
                    .withStyle(ChatFormatting.GRAY), true);
            return false;
        }

        switch (action) {
            case BARRIER -> {
                target.sendSystemMessage(Component.translatable("message.astral_craft.counter.barrier")
                        .withStyle(ChatFormatting.GREEN), true);
                target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.95F, 1.2F);
            }
            case EYE_FOR_AN_EYE -> {
                target.sendSystemMessage(Component.translatable("message.astral_craft.counter.reflect")
                        .withStyle(ChatFormatting.GOLD), true);
                target.level().playSound(null, target.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
                        SoundSource.PLAYERS, 0.9F, 1.55F);
                offer(effect.withTarget(effect.source()).withTicksLeft(DEFAULT_RESPONSE_TICKS));
            }
            case RANDOM_SELECT -> {
                LivingEntity redirected = randomOtherPlayer(effect, target.level(), target);
                if (redirected == null) {
                    target.sendSystemMessage(Component.translatable("message.astral_craft.counter.random_failed")
                            .withStyle(ChatFormatting.YELLOW), true);
                    return true;
                }
                target.sendSystemMessage(Component.translatable("message.astral_craft.counter.random",
                        redirected.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
                offer(effect.withTarget(redirected).withTicksLeft(DEFAULT_RESPONSE_TICKS));
            }
        }
        return true;
    }

    public static void serverTick(MinecraftServer server) {
        List<UUID> keys = new ArrayList<>(BY_TARGET.keySet());
        for (UUID key : keys) {
            PendingEffect current = BY_TARGET.get(key);
            if (current == null) continue;
            PendingEffect next = current.tickDown();
            if (next.ticksLeft() > 0) {
                BY_TARGET.put(key, next);
                continue;
            }
            BY_TARGET.remove(key, current);
            Entity target = next.source().level().getEntity(key);
            if (target instanceof LivingEntity living && living.isAlive()) resolve(next, living);
        }

        for (Map.Entry<UUID, PendingBoardCounter> entry : List.copyOf(BOARD_BY_CONTROLLER.entrySet())) {
            PendingBoardCounter chain = entry.getValue();
            if (chain.source().level().getGameTime() < chain.deadlineTick()) continue;
            if (!BOARD_BY_CONTROLLER.remove(entry.getKey(), chain)) continue;
            releaseReveal(chain);
            BoardParticipant target = chain.session().participant(chain.currentTargetSlot()).orElse(null);
            LivingEntity entity = target == null ? null : BoardEntityService.entity(chain.source().level(), target);
            if (entity == null) {
                PendingCardActionManager.completeBoardCardUi(chain.source());
            } else {
                PendingCardActionManager.schedule(chain.source(), 20, () -> finishBoardEffect(chain, entity));
            }
        }
    }

    private static void presentBoardTarget(PendingBoardCounter chain, boolean redirected) {
        BoardParticipant target = chain.session().participant(chain.currentTargetSlot()).orElse(null);
        AstralCharacterEntity targetEntity = target == null ? null
                : BoardEntityService.entity(chain.source().level(), target);
        if (target == null || targetEntity == null || target.knockedDown()) {
            releaseReveal(chain);
            PendingCardActionManager.completeBoardCardUi(chain.source());
            return;
        }

        List<Integer> counterIndexes = counterIndexes(target);
        if (counterIndexes.isEmpty()) {
            if (!redirected) {
                if (chain.revealId() == null) {
                    finishBoardEffect(chain, targetEntity);
                } else {
                    releaseReveal(chain);
                    PendingCardActionManager.schedule(chain.source(), 20,
                            () -> finishBoardEffect(chain, targetEntity));
                }
                return;
            }
            UUID revealId = UUID.randomUUID();
            broadcastEffectReveal(chain, targetEntity, revealId, false);
            PendingCardActionManager.schedule(chain.source(), REDIRECT_REVEAL_DELAY_TICKS,
                    () -> finishBoardEffect(chain, targetEntity));
            return;
        }

        UUID revealId = chain.revealId() == null ? UUID.randomUUID() : chain.revealId();
        PendingBoardCounter waiting = chain.withReveal(revealId,
                chain.source().level().getGameTime() + DEFAULT_RESPONSE_TICKS);
        if (chain.revealId() == null || redirected) {
            broadcastEffectReveal(waiting, targetEntity, revealId, true);
        }
        ServerPlayer controller = target.controllerUuid()
                .map(chain.source().server.getPlayerList()::getPlayer).orElse(null);
        if (controller != null) {
            BOARD_BY_CONTROLLER.put(controller.getUUID(), waiting);
            BoardSessionManager.openCounterScreen(controller, chain.session(), target, DEFAULT_RESPONSE_TICKS);
            controller.sendSystemMessage(Component.translatable("message.astral_craft.board.counter.prompt",
                    chain.source().getDisplayName()).withStyle(ChatFormatting.AQUA), true);
            return;
        }

        int selectedIndex = counterIndexes.get(chain.source().level().getRandom().nextInt(counterIndexes.size()));
        PendingCardActionManager.schedule(chain.source(), 18,
                () -> respondBoard(waiting, target, null, selectedIndex));
    }

    private static boolean respondBoard(PendingBoardCounter chain, BoardParticipant target,
                                        ServerPlayer responder, int handIndex) {
        AstralCharacterEntity responderEntity = BoardEntityService.entity(chain.source().level(), target);
        if (responderEntity == null) {
            releaseReveal(chain);
            PendingCardActionManager.completeBoardCardUi(chain.source());
            return true;
        }

        if (handIndex < 0) {
            releaseReveal(chain);
            PendingCardActionManager.schedule(chain.source(), 20,
                    () -> finishBoardEffect(chain, responderEntity));
            return true;
        }
        if (!isUsableCounterIndex(target, handIndex)) return false;
        Item item = BuiltInRegistries.ITEM.getValue(target.hand().get(handIndex));
        CounterAction action = counterAction(item);
        ItemStack counterStack = new ItemStack(item);
        CardDefinition counterDefinition = ((BaseHandCard) item).definition(counterStack);
        boolean consumed = responder != null
                ? BoardSessionManager.consumeCounterCard(responder, chain.session().id(), handIndex)
                : BoardSessionManager.consumeCounterCard(chain.source().level(), chain.session(), target, handIndex);
        if (!consumed) return false;
        releaseReveal(chain);

        BoardParticipant redirectedTarget = switch (action) {
            case BARRIER -> null;
            case EYE_FOR_AN_EYE -> chain.session().participant(chain.sourceSlot()).orElse(null);
            case RANDOM_SELECT -> randomOtherBoardTarget(chain, target);
        };
        AstralCharacterEntity redirectedEntity = redirectedTarget == null ? null
                : BoardEntityService.entity(chain.source().level(), redirectedTarget);
        broadcastCounterReveal(chain, responder, responderEntity, counterStack, counterDefinition, redirectedEntity);

        switch (action) {
            case BARRIER -> {
                chain.source().level().playSound(null, responderEntity.blockPosition(), SoundEvents.SHIELD_BLOCK.value(), SoundSource.PLAYERS, 0.95F, 1.2F);
                PendingCardActionManager.schedule(chain.source(), CardUseService.CARD_REVEAL_DURATION_TICKS, () -> PendingCardActionManager.completeBoardCardUi(chain.source()));
            }

            case EYE_FOR_AN_EYE, RANDOM_SELECT -> {
                if (redirectedTarget == null || redirectedEntity == null) {
                    PendingCardActionManager.schedule(chain.source(), CardUseService.CARD_REVEAL_DURATION_TICKS,
                            () -> PendingCardActionManager.completeBoardCardUi(chain.source()));
                } else {
                    PendingBoardCounter redirected = chain.withTarget(redirectedTarget.slotUuid(), true);
                    PendingCardActionManager.schedule(chain.source(), CardUseService.CARD_REVEAL_DURATION_TICKS,
                            () -> presentBoardTarget(redirected, true));
                }
            }
        }

        return true;
    }

    private static void finishUninterceptedBoardCard(ServerPlayer source, List<LivingEntity> targets,
                                                     BoardCardResolver resolver) {
        boolean applied = resolveBoardCard(source, targets, resolver);
        if (!applied || !PendingCardActionManager.waitsForBoardDamage(source)) {
            PendingCardActionManager.completeBoardCardUi(source);
        }
    }

    private static void finishBoardEffect(PendingBoardCounter chain, LivingEntity target) {
        if (target == null || !target.isAlive()) {
            PendingCardActionManager.completeBoardCardUi(chain.source());
            return;
        }
        boolean applied = resolveBoardCard(chain.source(), List.of(target), chain.resolver());
        if (!applied || !PendingCardActionManager.waitsForBoardDamage(chain.source())) {
            PendingCardActionManager.completeBoardCardUi(chain.source());
        }
    }

    private static boolean resolveBoardCard(ServerPlayer source, List<LivingEntity> targets,
                                            BoardCardResolver resolver) {
        boolean previous = RESOLVING_BOARD_CARD.get();
        RESOLVING_BOARD_CARD.set(true);
        try {
            return resolver.apply(targets);
        } finally {
            RESOLVING_BOARD_CARD.set(previous);
        }
    }

    private static boolean isUsableCounterIndex(BoardParticipant participant, int handIndex) {
        if (participant == null || handIndex < 0 || handIndex >= participant.hand().size()) return false;
        Item item = BuiltInRegistries.ITEM.getValue(participant.hand().get(handIndex));
        return item instanceof BaseHandCard card
                && card.definition(new ItemStack(item)).type() == CardType.COUNTER
                && counterAction(item) != null;
    }

    private static int remainingResponseTicks(PendingBoardCounter chain) {
        return (int) Math.clamp(chain.deadlineTick() - chain.source().level().getGameTime(),
                1L, DEFAULT_RESPONSE_TICKS);
    }

    private static List<Integer> counterIndexes(BoardParticipant participant) {
        List<Integer> result = new ArrayList<>();
        for (int index = 0; index < participant.hand().size(); index++) {
            Item item = BuiltInRegistries.ITEM.getValue(participant.hand().get(index));
            if (item instanceof BaseHandCard card
                    && card.definition(new ItemStack(item)).type() == CardType.COUNTER
                    && counterAction(item) != null) result.add(index);
        }
        return result;
    }

    private static CounterAction counterAction(Item item) {
        if (item instanceof HandcardBarrier) return CounterAction.BARRIER;
        if (item instanceof HandcardEyeForAnEye) return CounterAction.EYE_FOR_AN_EYE;
        if (item instanceof HandcardRandomSelect) return CounterAction.RANDOM_SELECT;
        return null;
    }

    private static BoardParticipant randomOtherBoardTarget(PendingBoardCounter chain, BoardParticipant currentTarget) {
        List<BoardParticipant> candidates = chain.session().participants().stream()
                .filter(participant -> !participant.knockedDown())
                .filter(participant -> !participant.slotUuid().equals(chain.sourceSlot()))
                .filter(participant -> !participant.slotUuid().equals(chain.originalTargetSlot()))
                .filter(participant -> !participant.slotUuid().equals(currentTarget.slotUuid()))
                .filter(participant -> BoardEntityService.entity(chain.source().level(), participant) != null)
                .toList();
        return candidates.isEmpty() ? null
                : candidates.get(chain.source().level().getRandom().nextInt(candidates.size()));
    }

    private static void broadcastEffectReveal(PendingBoardCounter chain, LivingEntity target,
                                              UUID revealId, boolean held) {
        CardRevealPayload payload = CardUseService.cardRevealPayload(chain.source(), chain.sourceStack(),
                chain.definition(), List.of(target), CardRevealPayload.ANIMATION_FLIP,
                CardUseService.CARD_REVEAL_DURATION_TICKS, revealId);
        for (ServerPlayer viewer : BoardSessionManager.humanPlayers(chain.source().level(), chain.session())) {
            PacketDistributor.sendToPlayer(viewer, payload);
            if (held) PacketDistributor.sendToPlayer(viewer,
                    new CardRevealControlPayload(revealId, CardRevealControlPayload.Action.HOLD));
        }
    }

    private static void broadcastCounterReveal(PendingBoardCounter chain, ServerPlayer responder,
                                               AstralCharacterEntity responderEntity, ItemStack stack,
                                               CardDefinition definition, LivingEntity redirectedTarget) {
        ServerPlayer revealOwner = responder == null ? chain.source() : responder;
        List<LivingEntity> targets = redirectedTarget == null ? List.of(responderEntity) : List.of(redirectedTarget);
        CardRevealPayload base = CardUseService.cardRevealPayload(revealOwner, stack, definition, targets,
                CardRevealPayload.ANIMATION_FLIP, CardUseService.CARD_REVEAL_DURATION_TICKS, UUID.randomUUID());
        CardRevealPayload payload = new CardRevealPayload(base.cardId(), base.stack(), base.cardType(), base.title(),
                base.body(), base.largeFrontTexture(), base.largeBackTexture(), base.animation(), base.durationTicks(),
                responderEntity.getId(), base.targetEntityIds(), base.revealId());
        for (ServerPlayer viewer : BoardSessionManager.humanPlayers(chain.source().level(), chain.session())) {
            PacketDistributor.sendToPlayer(viewer, payload);
        }
    }

    private static void releaseReveal(ServerPlayer source, BoardSession session, UUID revealId) {
        if (source == null || revealId == null) return;
        CardRevealControlPayload payload = new CardRevealControlPayload(revealId,
                CardRevealControlPayload.Action.RELEASE);
        if (session == null) {
            PacketDistributor.sendToPlayer(source, payload);
            return;
        }
        for (ServerPlayer viewer : BoardSessionManager.humanPlayers(source.level(), session)) {
            PacketDistributor.sendToPlayer(viewer, payload);
        }
    }

    private static void releaseReveal(PendingBoardCounter chain) {
        releaseReveal(chain.source(), chain.session(), chain.revealId());
    }

    private static LivingEntity randomOtherPlayer(PendingEffect effect, ServerLevel level, ServerPlayer originalTarget) {
        List<ServerPlayer> candidates = level.players().stream()
                .filter(ServerPlayer::isAlive)
                .filter(player -> player != originalTarget && player != effect.source())
                .toList();
        return candidates.isEmpty() ? null : candidates.get(level.getRandom().nextInt(candidates.size()));
    }

    private static void resolve(PendingEffect effect, LivingEntity target) {
        if (!target.isAlive() || !effect.source().isAlive()) return;
        LivingEntity visualSource = effect.visualSource().isAlive() ? effect.visualSource() : effect.source();
        switch (effect.kind()) {
            case DIRECT -> AstralCardEffects.damageNow(visualSource, target, effect.damage());
            case LASER -> spawnLaser(visualSource, target, effect.damage(), effect.argb(), effect.radius());
            case FIRECRACKERS -> spawnFirecrackers(visualSource, target, effect.damage(), effect.projectileSettings());
            case SLINGSHOT -> spawnSlingshot(visualSource, target, effect.damage(), effect.projectileSettings());
            case SNOWBALL_ATTACK -> spawnSnowball(visualSource, target, effect.damage(), effect.projectileSettings());
            case FALLING_BRICK -> spawnBrick(visualSource, target, effect.damage());
        }
    }

    private static void spawnLaser(LivingEntity source, LivingEntity target, int damage, int argb, float radius) {
        playSourceAttack(source);
        LaserStrikeEntity entity = new LaserStrikeEntity(source.level(), source, target, damage, argb, radius);
        source.level().playSound(null, target.blockPosition(), SoundEvents.BEACON_ACTIVATE,
                SoundSource.PLAYERS, 0.9F, 1.35F);
        source.level().addFreshEntity(entity);
    }

    private static void spawnFirecrackers(LivingEntity source, LivingEntity target, int damage,
                                          CardProjectileSettings settings) {
        playSourceAttack(source);
        FirecrackersProjectileEntity entity = new FirecrackersProjectileEntity(source.level(), source, target,
                damage, settings);
        source.level().playSound(null, source.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH,
                SoundSource.PLAYERS, 0.9F, 1.15F);
        source.level().addFreshEntity(entity);
    }

    private static void spawnSlingshot(LivingEntity source, LivingEntity target, int damage,
                                       CardProjectileSettings settings) {
        playSourceAttack(source);
        SlingshotProjectileEntity entity = new SlingshotProjectileEntity(source.level(), source, target,
                damage, settings);
        source.level().playSound(null, source.blockPosition(), SoundEvents.ARROW_SHOOT,
                SoundSource.PLAYERS, 0.9F, 1.8F);
        source.level().addFreshEntity(entity);
    }

    private static void spawnSnowball(LivingEntity source, LivingEntity target, int damage,
                                      CardProjectileSettings settings) {
        playSourceAttack(source);
        SnowballAttackProjectileEntity entity = new SnowballAttackProjectileEntity(source.level(), source, target,
                damage, settings);
        source.level().playSound(null, source.blockPosition(), SoundEvents.SNOWBALL_THROW,
                SoundSource.PLAYERS, 0.8F, 1.2F);
        source.level().addFreshEntity(entity);
    }

    private static void spawnBrick(LivingEntity source, LivingEntity target, int damage) {
        playSourceAttack(source);
        FallingBrickEntity entity = new FallingBrickEntity(source.level(), source, target, damage, 10);
        source.level().playSound(null, target.blockPosition(), SoundEvents.ANVIL_LAND,
                SoundSource.PLAYERS, 0.55F, 1.55F);
        source.level().addFreshEntity(entity);
    }

    private static void playSourceAttack(LivingEntity source) {
        if (source instanceof AstralCharacterEntity character) character.playBoardAttackAnimation(12);
    }

    @FunctionalInterface
    public interface BoardCardResolver {
        boolean apply(List<LivingEntity> targets);
    }

    public enum CounterAction {
        BARRIER,
        RANDOM_SELECT,
        EYE_FOR_AN_EYE
    }

    private enum VisualKind {
        DIRECT,
        LASER,
        FIRECRACKERS,
        SLINGSHOT,
        SNOWBALL_ATTACK,
        FALLING_BRICK
    }

    private record PendingBoardCounter(BoardSession session, ServerPlayer source, UUID sourceSlot,
                                       UUID originalTargetSlot, UUID currentTargetSlot,
                                       ItemStack sourceStack, CardDefinition definition,
                                       BoardCardResolver resolver, UUID revealId,
                                       boolean redirected, long deadlineTick) {
        private PendingBoardCounter withReveal(UUID revealId, long deadlineTick) {
            return new PendingBoardCounter(this.session, this.source, this.sourceSlot, this.originalTargetSlot,
                    this.currentTargetSlot, this.sourceStack, this.definition, this.resolver,
                    revealId, this.redirected, deadlineTick);
        }

        private PendingBoardCounter withTarget(UUID targetSlot, boolean redirected) {
            return new PendingBoardCounter(this.session, this.source, this.sourceSlot, this.originalTargetSlot,
                    targetSlot, this.sourceStack, this.definition, this.resolver,
                    null, redirected, 0L);
        }
    }

    private record PendingEffect(ServerPlayer source, LivingEntity visualSource, LivingEntity target,
                                 int damage, VisualKind kind, int argb, float radius,
                                 CardProjectileSettings projectileSettings, int ticksLeft) {
        static PendingEffect direct(ServerPlayer source, LivingEntity visualSource, LivingEntity target, int damage) {
            return new PendingEffect(source, visualSource, target, damage, VisualKind.DIRECT,
                    0xFFFFFFFF, 0.08F, CardProjectileSettings.slingshot(), DEFAULT_RESPONSE_TICKS);
        }

        static PendingEffect laser(ServerPlayer source, LivingEntity visualSource, LivingEntity target,
                                   int damage, int argb, float radius) {
            return new PendingEffect(source, visualSource, target, damage, VisualKind.LASER,
                    argb, radius, CardProjectileSettings.slingshot(), DEFAULT_RESPONSE_TICKS);
        }

        static PendingEffect projectile(ServerPlayer source, LivingEntity visualSource, LivingEntity target,
                                        int damage, VisualKind kind, CardProjectileSettings settings) {
            return new PendingEffect(source, visualSource, target, damage, kind,
                    0xFFFFFFFF, 0.08F, settings, DEFAULT_RESPONSE_TICKS);
        }

        PendingEffect withTarget(LivingEntity target) {
            return new PendingEffect(this.source, this.visualSource, target, this.damage, this.kind,
                    this.argb, this.radius, this.projectileSettings, this.ticksLeft);
        }

        PendingEffect withTicksLeft(int ticks) {
            return new PendingEffect(this.source, this.visualSource, this.target, this.damage, this.kind,
                    this.argb, this.radius, this.projectileSettings, ticks);
        }

        PendingEffect tickDown() {
            return this.withTicksLeft(this.ticksLeft - 1);
        }
    }
}
