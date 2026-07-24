package com.astral_craft.common.gameplay.handcard;

import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.gameplay.board.*;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.network.s2c.CardRevealControlPayload;
import com.astral_craft.common.network.s2c.CardRevealPayload;
import com.astral_craft.common.registry.AstralDataComponents;
import com.astral_craft.common.util.AstralServerTickClock;
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
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntConsumer;

/**
 * Resolves effect-card counter windows. Board cards are intercepted before their effect callback is
 * executed, allowing Barrier, Eye for an Eye, and Random Select to form a redirectable counter chain.
 * World effects use the same resolver callback instead of maintaining one offer method per visual.
 */
public class PendingCounterEffectManager {

    public static final int DEFAULT_RESPONSE_TICKS = 20 * 12;
    private static final int REDIRECT_REVEAL_DELAY_TICKS = CardUseService.CARD_REVEAL_DURATION_TICKS + 20;

    private static final Map<UUID, PendingEffect> BY_TARGET = new ConcurrentHashMap<>();
    private static final Map<UUID, PendingBoardCounter> BOARD_BY_CONTROLLER = new ConcurrentHashMap<>();
    private static final ThreadLocal<Boolean> RESOLVING_BOARD_CARD = ThreadLocal.withInitial(() -> false);

    /**
     * Offers one counterable world effect. The callback receives the final target after any reflection
     * or random redirection, so projectile and direct-damage cards share this single entry point.
     */
    public static void offer(ServerPlayer source, LivingEntity target, EffectResolver resolver) {
        if (source == null || target == null || resolver == null) return;
        offer(new PendingEffect(source, target, resolver, DEFAULT_RESPONSE_TICKS));
    }

    /**
     * Registers a board counter window immediately after target validation. Returning {@code true}
     * means the caller must hold the initial reveal until the target responds or times out.
     */
    public static boolean offerBoardCard(ServerPlayer source, ItemStack sourceStack, CardDefinition definition,
                                         List<LivingEntity> targets, UUID revealId, int effectDelayTicks,
                                         BoardCardResolver resolver) {
        if (source == null || sourceStack.isEmpty() || definition == null || resolver == null) return false;
        List<LivingEntity> capturedTargets = List.copyOf(targets == null ? List.of() : targets);
        int delay = Math.max(0, effectDelayTicks);
        if (capturedTargets.size() != 1 || !(capturedTargets.getFirst() instanceof AstralCharacterEntity target)
                || !target.isBoardPawn()) {
            scheduleBoardEffect(source, capturedTargets, resolver, delay);
            return false;
        }

        BoardSession session = BoardSessionManager.findByController(source).orElse(null);
        BoardParticipant sourceParticipant = session == null ? null
                : session.participantByController(source.getUUID()).orElse(null);
        BoardParticipant targetParticipant = participantForTarget(session, target);
        if (session == null || sourceParticipant == null || targetParticipant == null
                || sourceParticipant.slotUuid().equals(targetParticipant.slotUuid())) {
            scheduleBoardEffect(source, capturedTargets, resolver, delay);
            return false;
        }

        List<Integer> counterIndexes = counterIndexes(targetParticipant);
        if (counterIndexes.isEmpty()) {
            scheduleBoardEffect(source, capturedTargets, resolver, delay);
            return false;
        }

        PendingBoardCounter chain = new PendingBoardCounter(source.level(), session, source, source.getUUID(),
                sourceParticipant.slotUuid(), targetParticipant.slotUuid(), targetParticipant.slotUuid(),
                sourceStack.copyWithCount(1), definition, resolver, null, null, revealId, false,
                AstralServerTickClock.now(source.level()) + DEFAULT_RESPONSE_TICKS);
        presentInitialBoardTarget(chain, targetParticipant, counterIndexes);
        return true;
    }

    public static boolean offerBoardBotCard(ServerLevel level, BoardSession session, BoardParticipant source,
                                            ItemStack sourceStack, CardDefinition definition, List<UUID> targetSlotIds,
                                            BoardBotCardResolver resolver, IntConsumer completion) {
        if (level == null || session == null || source == null || sourceStack.isEmpty()
                || definition == null || resolver == null || completion == null) return false;
        List<UUID> targets = List.copyOf(targetSlotIds == null ? List.of() : targetSlotIds);
        if (targets.size() != 1) return false;
        BoardParticipant target = session.participant(targets.getFirst()).orElse(null);
        if (target == null || target.knockedDown() || source.slotUuid().equals(target.slotUuid())) return false;
        List<Integer> counterIndexes = counterIndexes(target);
        if (counterIndexes.isEmpty()) return false;
        long deadlineTick = AstralServerTickClock.now(level) + DEFAULT_RESPONSE_TICKS;
        PendingBoardCounter chain = new PendingBoardCounter(level, session, null, source.slotUuid(),
                source.slotUuid(), target.slotUuid(), target.slotUuid(), sourceStack.copyWithCount(1),
                definition, null, resolver, completion, null, false, deadlineTick);
        session.setActionDeadlineTick(deadlineTick);
        BoardSessionManager.markChanged(level);
        presentInitialBoardTarget(chain, target, counterIndexes);
        return true;
    }

    private static void presentInitialBoardTarget(PendingBoardCounter chain, BoardParticipant target, List<Integer> counterIndexes) {
        ServerPlayer controller = controllerFor(chain, target);
        if (controller != null) {
            BOARD_BY_CONTROLLER.put(controller.getUUID(), chain);
            openHumanCounter(chain, target, controller);
            controller.sendSystemMessage(Component.translatable("message.astral_craft.board.counter.prompt",
                    sourceDisplayName(chain)).withStyle(ChatFormatting.AQUA), true);
            return;
        }

        int selectedIndex = counterIndexes.get(chain.level().getRandom().nextInt(counterIndexes.size()));
        schedule(chain, 18, () -> respondBoard(chain, target, null, selectedIndex));
    }

    public static boolean respondBoard(ServerPlayer responder, UUID boardId, int handIndex) {
        if (responder == null || boardId == null) return false;
        PendingBoardCounter chain = BOARD_BY_CONTROLLER.get(responder.getUUID());
        if (chain == null || !chain.session().id().equals(boardId)) return false;
        BoardParticipant target = chain.session().participant(chain.currentTargetSlot()).orElse(null);
        if (target == null || !target.controlledBy(responder.getUUID())) return false;
        if (handIndex >= 0 && !isUsableCounterIndex(target, handIndex)) {
            BoardSessionManager.openCounterScreen(responder, chain.session(), target, remainingResponseTicks(chain));
            return false;
        }
        if (!BOARD_BY_CONTROLLER.remove(responder.getUUID(), chain)) return false;
        boolean handled = respondBoard(chain, target, responder, handIndex);
        if (!handled) {
            BOARD_BY_CONTROLLER.put(responder.getUUID(), chain);
            BoardSessionManager.openCounterScreen(responder, chain.session(), target, remainingResponseTicks(chain));
        }
        return handled;
    }

    public static boolean respond(ServerPlayer target, CounterCardBehavior counter) {
        if (target == null || counter == null) return false;
        PendingEffect effect = BY_TARGET.remove(target.getUUID());
        if (effect == null) {
            target.sendSystemMessage(Component.translatable("message.astral_craft.counter.none")
                    .withStyle(ChatFormatting.GRAY), true);
            return false;
        }

        WorldCounterContext context = new WorldCounterContext(effect, target);
        counter.resolveWorldCounter(context);
        return context.handled();
    }

    public static void serverTick(MinecraftServer server) {
        for (UUID key : List.copyOf(BY_TARGET.keySet())) {
            PendingEffect current = BY_TARGET.get(key);
            if (current == null) continue;
            PendingEffect next = current.tickDown();
            if (next.ticksLeft() > 0) {
                BY_TARGET.put(key, next);
                continue;
            }
            if (!BY_TARGET.remove(key, current)) continue;
            Entity target = next.source().level().getEntity(key);
            if (target instanceof LivingEntity living && living.isAlive()) resolve(next, living);
        }

        for (Map.Entry<UUID, PendingBoardCounter> entry : List.copyOf(BOARD_BY_CONTROLLER.entrySet())) {
            PendingBoardCounter chain = entry.getValue();
            if (AstralServerTickClock.now(chain.level()) < chain.deadlineTick()) continue;
            if (!BOARD_BY_CONTROLLER.remove(entry.getKey(), chain)) continue;
            releaseReveal(chain);
            BoardParticipant target = chain.session().participant(chain.currentTargetSlot()).orElse(null);
            LivingEntity entity = target == null ? null : BoardEntityService.entity(chain.level(), target);
            if (entity == null) completeBoardCard(chain);
            else schedule(chain, 20, () -> finishBoardEffect(chain, entity));
        }
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

    private static void presentBoardTarget(PendingBoardCounter chain, boolean redirected) {
        BoardParticipant target = chain.session().participant(chain.currentTargetSlot()).orElse(null);
        AstralCharacterEntity targetEntity = target == null ? null
                : BoardEntityService.entity(chain.level(), target);
        if (target == null || targetEntity == null || target.knockedDown()) {
            releaseReveal(chain);
            completeBoardCard(chain);
            return;
        }

        List<Integer> counterIndexes = counterIndexes(target);
        if (counterIndexes.isEmpty()) {
            if (!redirected) {
                finishBoardEffect(chain, targetEntity);
                return;
            }
            UUID revealId = UUID.randomUUID();
            broadcastEffectReveal(chain, targetEntity, revealId, false);
            schedule(chain, REDIRECT_REVEAL_DELAY_TICKS, () -> finishBoardEffect(chain, targetEntity));
            return;
        }

        UUID revealId = UUID.randomUUID();
        PendingBoardCounter waiting = chain.withReveal(revealId,
                AstralServerTickClock.now(chain.level()) + DEFAULT_RESPONSE_TICKS);
        broadcastEffectReveal(waiting, targetEntity, revealId, true);
        ServerPlayer controller = controllerFor(waiting, target);
        if (controller != null) {
            BOARD_BY_CONTROLLER.put(controller.getUUID(), waiting);
            openHumanCounter(waiting, target, controller);
            controller.sendSystemMessage(Component.translatable("message.astral_craft.board.counter.prompt",
                    sourceDisplayName(chain)).withStyle(ChatFormatting.AQUA), true);
            return;
        }

        int selectedIndex = counterIndexes.get(chain.level().getRandom().nextInt(counterIndexes.size()));
        schedule(chain, 18, () -> respondBoard(waiting, target, null, selectedIndex));
    }

    private static void openHumanCounter(PendingBoardCounter chain, BoardParticipant target, ServerPlayer controller) {
        BoardSessionManager.openCounterScreen(controller, chain.session(), target, remainingResponseTicks(chain));
        schedule(chain, 2, () -> {
            PendingBoardCounter current = BOARD_BY_CONTROLLER.get(controller.getUUID());
            if (chain.equals(current) && !controller.isRemoved()) {
                BoardSessionManager.openCounterScreen(controller, chain.session(), target, remainingResponseTicks(chain));
            }
        });
    }

    private static boolean respondBoard(PendingBoardCounter chain, BoardParticipant target,
                                        ServerPlayer responder, int handIndex) {
        AstralCharacterEntity responderEntity = BoardEntityService.entity(chain.level(), target);
        if (responderEntity == null) {
            releaseReveal(chain);
            completeBoardCard(chain);
            return true;
        }

        if (handIndex < 0) {
            releaseReveal(chain);
            schedule(chain, 20, () -> finishBoardEffect(chain, responderEntity));
            return true;
        }
        if (!isUsableCounterIndex(target, handIndex)) return false;
        Item item = BuiltInRegistries.ITEM.getValue(target.hand().get(handIndex));
        if (!(item instanceof BaseHandCard card) || !(item instanceof CounterCardBehavior counter)) return false;
        ItemStack counterStack = new ItemStack(item);
        CardDefinition counterDefinition = card.definition(counterStack);
        boolean consumed = responder != null
                ? BoardSessionManager.consumeCounterCard(responder, chain.session().id(), handIndex)
                : BoardSessionManager.consumeCounterCard(chain.level(), chain.session(), target, handIndex);
        if (!consumed) return false;
        releaseReveal(chain);
        BoardCounterContext context = new BoardCounterContext(chain, target, responder, responderEntity,
                counterStack, counterDefinition);
        counter.resolveBoardCounter(context);
        return context.handled();
    }

    private static void scheduleBoardEffect(ServerPlayer source, List<LivingEntity> targets, BoardCardResolver resolver, int delayTicks) {
        PendingCardActionManager.schedule(source, Math.max(0, delayTicks),
                () -> finishUninterceptedBoardCard(source, targets, resolver));
    }

    private static void finishUninterceptedBoardCard(ServerPlayer source, List<LivingEntity> targets, BoardCardResolver resolver) {
        boolean applied = resolveBoardCard(source, targets, resolver);
        if (!applied || !PendingCardActionManager.waitsForBoardDamage(source)) {
            PendingCardActionManager.completeBoardCardUi(source);
        }
    }

    private static void finishBoardEffect(PendingBoardCounter chain, LivingEntity target) {
        if (target == null || !target.isAlive()) {
            completeBoardCard(chain);
            return;
        }
        if (chain.source() != null && chain.resolver() != null) {
            boolean applied = resolveBoardCard(chain.source(), List.of(target), chain.resolver());
            if (!applied || !PendingCardActionManager.waitsForBoardDamage(chain.source())) completeBoardCard(chain);
            return;
        }
        if (!(target instanceof AstralCharacterEntity character) || chain.botResolver() == null) {
            completeBoardCard(chain);
            return;
        }
        BoardParticipant participant = chain.session().participantFor(character).orElse(null);
        if (participant == null) {
            completeBoardCard(chain);
            return;
        }
        int followUpDelay = Math.max(0, chain.botResolver().apply(List.of(participant.slotUuid())));
        if (chain.botCompletion() != null) chain.botCompletion().accept(followUpDelay);
    }

    private static void completeBoardCard(PendingBoardCounter chain) {
        if (chain.source() != null) PendingCardActionManager.completeBoardCardUi(chain.source());
        else if (chain.botCompletion() != null) chain.botCompletion().accept(0);
    }

    private static void schedule(PendingBoardCounter chain, int delayTicks, Runnable action) {
        PendingCardActionManager.schedule(chain.schedulerOwner(), Math.max(0, delayTicks), action);
    }

    private static boolean resolveBoardCard(ServerPlayer source, List<LivingEntity> targets, BoardCardResolver resolver) {
        boolean previous = RESOLVING_BOARD_CARD.get();
        RESOLVING_BOARD_CARD.set(true);
        try {
            return resolver.apply(targets);
        } finally {
            RESOLVING_BOARD_CARD.set(previous);
        }
    }

    private static BoardParticipant participantForTarget(BoardSession session, AstralCharacterEntity target) {
        return session == null ? null : session.participantFor(target).orElse(null);
    }

    private static ServerPlayer controllerFor(PendingBoardCounter chain, BoardParticipant participant) {
        return participant.controllerUuid()
                .map(controllerId -> chain.level().getServer().getPlayerList().getPlayer(controllerId))
                .orElse(null);
    }

    private static boolean isUsableCounterIndex(BoardParticipant participant, int handIndex) {
        if (participant == null || handIndex < 0 || handIndex >= participant.hand().size()) return false;
        Item item = BuiltInRegistries.ITEM.getValue(participant.hand().get(handIndex));
        return item instanceof BaseHandCard card && item instanceof CounterCardBehavior
                && card.definition(new ItemStack(item)).type() == CardType.COUNTER;
    }

    private static int remainingResponseTicks(PendingBoardCounter chain) {
        return (int) Math.clamp(chain.deadlineTick() - AstralServerTickClock.now(chain.level()),
                1L, DEFAULT_RESPONSE_TICKS);
    }

    private static List<Integer> counterIndexes(BoardParticipant participant) {
        List<Integer> result = new ArrayList<>();
        for (int index = 0; index < participant.hand().size(); index++) {
            Item item = BuiltInRegistries.ITEM.getValue(participant.hand().get(index));
            if (item instanceof BaseHandCard card && item instanceof CounterCardBehavior
                    && card.definition(new ItemStack(item)).type() == CardType.COUNTER) result.add(index);
        }

        return result;
    }

    private static BoardParticipant randomOtherBoardTarget(PendingBoardCounter chain, BoardParticipant currentTarget) {
        List<BoardParticipant> candidates = chain.session().participants().stream()
                .filter(participant -> !participant.knockedDown())
                .filter(participant -> !participant.slotUuid().equals(chain.sourceSlot()))
                .filter(participant -> !participant.slotUuid().equals(chain.originalTargetSlot()))
                .filter(participant -> !participant.slotUuid().equals(currentTarget.slotUuid()))
                .filter(participant -> BoardEntityService.entity(chain.level(), participant) != null).toList();
        return candidates.isEmpty() ? null : candidates.get(chain.level().getRandom().nextInt(candidates.size()));
    }

    private static void broadcastEffectReveal(PendingBoardCounter chain, LivingEntity target, UUID revealId, boolean held) {
        AstralCharacterEntity sourceEntity = chain.session().participant(chain.sourceSlot())
                .map(participant -> BoardEntityService.entity(chain.level(), participant)).orElse(null);
        CardRevealPayload payload = boardRevealPayload(chain.source(), sourceEntity, chain.sourceStack(),
                chain.definition(), List.of(target), revealId);
        for (ServerPlayer viewer : BoardSpectatorService.presentationViewers(chain.level(), chain.session())) {
            PacketDistributor.sendToPlayer(viewer, payload);
            if (held) PacketDistributor.sendToPlayer(viewer,
                    new CardRevealControlPayload(revealId, CardRevealControlPayload.Action.HOLD));
        }
    }

    private static void broadcastCounterReveal(PendingBoardCounter chain, ServerPlayer responder,
                                               AstralCharacterEntity responderEntity, ItemStack stack,
                                               CardDefinition definition, LivingEntity redirectedTarget) {
        List<LivingEntity> targets = redirectedTarget == null ? List.of(responderEntity) : List.of(redirectedTarget);
        CardRevealPayload payload = boardRevealPayload(responder, responderEntity, stack, definition,
                targets, UUID.randomUUID());
        for (ServerPlayer viewer : BoardSpectatorService.presentationViewers(chain.level(), chain.session())) {
            PacketDistributor.sendToPlayer(viewer, payload);
        }
    }

    private static CardRevealPayload boardRevealPayload(@Nullable ServerPlayer owner, @Nullable LivingEntity source,
                                                        ItemStack stack, CardDefinition definition,
                                                        List<? extends LivingEntity> targets, UUID revealId) {
        List<Integer> targetIds = targets.stream().map(LivingEntity::getId).distinct().limit(8).toList();
        if (owner != null) {
            CardRevealPayload base = CardUseService.cardRevealPayload(owner, stack, definition, targets,
                    CardRevealPayload.ANIMATION_FLIP, CardUseService.CARD_REVEAL_DURATION_TICKS, revealId);
            return new CardRevealPayload(base.cardId(), base.stack(), base.cardType(), base.title(), base.body(),
                    base.largeFrontTexture(), base.largeBackTexture(), base.animation(), base.durationTicks(),
                    source == null ? base.sourceEntityId() : source.getId(), targetIds, base.revealId());
        }
        CardType cardType = stack.getOrDefault(AstralDataComponents.CARD_TYPE, definition.type());
        return new CardRevealPayload(definition.itemId(stack).toString(), stack.copyWithCount(1),
                cardType.getSerializedName(), definition.displayName(stack),
                definition.effectText(stack, definition.range()), definition.largeFrontTexture(stack),
                definition.largeBackTexture(), CardRevealPayload.ANIMATION_FLIP,
                CardUseService.CARD_REVEAL_DURATION_TICKS, source == null ? -1 : source.getId(), targetIds, revealId);
    }

    private static void releaseReveal(PendingBoardCounter chain) {
        if (chain.revealId() == null) return;
        CardRevealControlPayload payload = new CardRevealControlPayload(chain.revealId(),
                CardRevealControlPayload.Action.RELEASE);
        for (ServerPlayer viewer : BoardSpectatorService.presentationViewers(chain.level(), chain.session())) {
            PacketDistributor.sendToPlayer(viewer, payload);
        }
    }

    private static Component sourceDisplayName(PendingBoardCounter chain) {
        if (chain.source() != null) return chain.source().getDisplayName();
        BoardParticipant source = chain.session().participant(chain.sourceSlot()).orElse(null);
        AstralCharacterEntity entity = source == null ? null : BoardEntityService.entity(chain.level(), source);
        return entity == null ? chain.definition().displayName(chain.sourceStack()) : entity.getDisplayName();
    }

    private static LivingEntity randomOtherPlayer(PendingEffect effect, ServerLevel level, ServerPlayer originalTarget) {
        List<ServerPlayer> candidates = level.players().stream().filter(ServerPlayer::isAlive)
                .filter(player -> player != originalTarget && player != effect.source()).toList();
        return candidates.isEmpty() ? null : candidates.get(level.getRandom().nextInt(candidates.size()));
    }

    private static void resolve(PendingEffect effect, LivingEntity target) {
        if (target == null || !target.isAlive() || !effect.source().isAlive()) return;
        effect.resolver().apply(target);
    }

    public static class WorldCounterContext {

        private final PendingEffect effect;
        private final ServerPlayer responder;
        private boolean handled;

        private WorldCounterContext(PendingEffect effect, ServerPlayer responder) {
            this.effect = effect;
            this.responder = responder;
        }

        public ServerPlayer responder() {
            return this.responder;
        }

        public LivingEntity source() {
            return this.effect.source();
        }

        public LivingEntity randomTarget() {
            return randomOtherPlayer(this.effect, this.responder.level(), this.responder);
        }

        public void block() {
            this.handled = true;
        }

        public void redirect(LivingEntity target) {
            if (target != null) offer(this.effect.withTarget(target).withTicksLeft(DEFAULT_RESPONSE_TICKS));
            this.handled = true;
        }

        private boolean handled() {
            return this.handled;
        }
    }

    public static class BoardCounterContext {

        private final PendingBoardCounter chain;
        private final BoardParticipant responder;
        private final ServerPlayer controller;
        private final AstralCharacterEntity responderEntity;
        private final ItemStack stack;
        private final CardDefinition definition;
        private boolean handled;

        private BoardCounterContext(PendingBoardCounter chain, BoardParticipant responder, ServerPlayer controller,
                                    AstralCharacterEntity responderEntity, ItemStack stack, CardDefinition definition) {
            this.chain = chain;
            this.responder = responder;
            this.controller = controller;
            this.responderEntity = responderEntity;
            this.stack = stack;
            this.definition = definition;
        }

        public ServerLevel level() {
            return this.chain.level();
        }

        public AstralCharacterEntity responderEntity() {
            return this.responderEntity;
        }

        public void block() {
            this.finish(null);
        }

        public void redirectToSource() {
            this.finish(this.chain.session().participant(this.chain.sourceSlot()).orElse(null));
        }

        public void redirectRandomly() {
            this.finish(randomOtherBoardTarget(this.chain, this.responder));
        }

        private void finish(BoardParticipant redirectedTarget) {
            AstralCharacterEntity redirectedEntity = redirectedTarget == null ? null
                    : BoardEntityService.entity(this.chain.level(), redirectedTarget);
            broadcastCounterReveal(this.chain, this.controller, this.responderEntity, this.stack,
                    this.definition, redirectedEntity);
            if (redirectedTarget == null || redirectedEntity == null) {
                schedule(this.chain, CardUseService.CARD_REVEAL_DURATION_TICKS, () -> completeBoardCard(this.chain));
            } else {
                PendingBoardCounter redirected = this.chain.withTarget(redirectedTarget.slotUuid(), true);
                schedule(this.chain, CardUseService.CARD_REVEAL_DURATION_TICKS, () -> presentBoardTarget(redirected, true));
            }
            this.handled = true;
        }

        private boolean handled() {
            return this.handled;
        }
    }

    @FunctionalInterface
    public interface EffectResolver {
        void apply(LivingEntity target);
    }

    @FunctionalInterface
    public interface BoardCardResolver {
        boolean apply(List<LivingEntity> targets);
    }

    @FunctionalInterface
    public interface BoardBotCardResolver {
        int apply(List<UUID> targetSlotIds);
    }

    private record PendingBoardCounter(ServerLevel level, BoardSession session,
                                       @Nullable ServerPlayer source, UUID schedulerOwner, UUID sourceSlot,
                                       UUID originalTargetSlot, UUID currentTargetSlot,
                                       ItemStack sourceStack, CardDefinition definition,
                                       @Nullable BoardCardResolver resolver,
                                       @Nullable BoardBotCardResolver botResolver,
                                       @Nullable IntConsumer botCompletion, @Nullable UUID revealId,
                                       boolean redirected, long deadlineTick) {
        private PendingBoardCounter withReveal(UUID revealId, long deadlineTick) {
            return new PendingBoardCounter(this.level, this.session, this.source, this.schedulerOwner,
                    this.sourceSlot, this.originalTargetSlot, this.currentTargetSlot, this.sourceStack,
                    this.definition, this.resolver, this.botResolver, this.botCompletion,
                    revealId, this.redirected, deadlineTick);
        }

        private PendingBoardCounter withTarget(UUID targetSlot, boolean redirected) {
            return new PendingBoardCounter(this.level, this.session, this.source, this.schedulerOwner,
                    this.sourceSlot, this.originalTargetSlot, targetSlot, this.sourceStack,
                    this.definition, this.resolver, this.botResolver, this.botCompletion,
                    null, redirected, 0L);
        }
    }

    private record PendingEffect(ServerPlayer source, LivingEntity target, EffectResolver resolver, int ticksLeft) {

        private PendingEffect withTarget(LivingEntity target) {
            return new PendingEffect(this.source, target, this.resolver, this.ticksLeft);
        }

        private PendingEffect withTicksLeft(int ticks) {
            return new PendingEffect(this.source, this.target, this.resolver, ticks);
        }

        private PendingEffect tickDown() {
            return this.withTicksLeft(this.ticksLeft - 1);
        }

    }

}