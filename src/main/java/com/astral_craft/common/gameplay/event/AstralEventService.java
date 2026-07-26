package com.astral_craft.common.gameplay.event;

import com.astral_craft.common.config.AstralGameplayConfig;
import com.astral_craft.common.gameplay.board.BoardEventService;
import com.astral_craft.common.gameplay.cardback.CardBackPreferenceManager;
import com.astral_craft.common.gameplay.handcard.CardUseService;
import com.astral_craft.common.gameplay.handcard.PendingCardActionManager;
import com.astral_craft.common.network.s2c.CardRevealPayload;
import com.astral_craft.common.registry.AstralAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AstralEventService {

    public static final int DEFAULT_EVENT_REVEAL_DURATION_TICKS = CardUseService.CARD_APPROACH_REVEAL_DURATION_TICKS;

    public static void serverTick(MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            tickState(player);
            trigger(player);
        }
    }

    public static boolean trigger(ServerPlayer player) {
        if (player == null) return false;
        List<AstralEventDefinition> candidates = AstralEventManager.INSTANCE.automaticEvents();
        if (candidates.isEmpty()) return false;
        for (AstralEventDefinition definition : candidates) {
            if (tryTrigger(player, definition, false)) {
                return true;
            }
        }

        return false;
    }

    public static boolean triggerById(ServerPlayer player, Identifier id) {
        if (player == null || id == null || !AstralEventManager.INSTANCE.contains(id)) return false;
        if (BoardEventService.isBoardEvent(id)) return BoardEventService.triggerById(player, id);
        return tryTrigger(player, AstralEventManager.INSTANCE.get(id), true);
    }

    public static boolean tryTrigger(ServerPlayer player, AstralEventDefinition definition) {
        return tryTrigger(player, definition, false);
    }

    public static boolean tryTrigger(ServerPlayer player, AstralEventDefinition definition, boolean force) {
        if (player == null || definition == null || BoardEventService.isBoardEvent(definition.id())) return false;
        if (!force && !definition.canAutoTrigger()) return false;
        if (!definition.canTriggerInDifficulty(player.level().getDifficulty())) return false;
        if (PendingCardActionManager.isExclusiveBusy(player)) return false;
        AstralEventContext base = AstralEventContext.of(player, player, definition);
        if (!force && !definition.testConditions(base)) return false;
        double chance = Math.clamp(definition.chance(), 0.0D, 1.0D);
        if (!force && chance < 1.0D && player.getRandom().nextDouble() > chance) return false;
        List<AstralEventContext> contexts = new ArrayList<>();
        for (Entity target : definition.target().resolve(player)) {
            AstralEventContext context = AstralEventContext.of(player, target, definition);
            if ((force || definition.testConditions(context)) && canApplyToTarget(player, context, force)) {
                contexts.add(context);
            }
        }

        if (contexts.isEmpty()) return false;
        int revealDelay = revealDelay(player);
        if (revealDelay > 0 && !PendingCardActionManager.scheduleExclusive(player, revealDelay, () -> beginEvent(contexts))) return false;
        sendRevealOrMessage(player, definition);
        if (definition.broadcast()) {
            contexts.stream().map(AstralEventContext::targetPlayer)
                    .filter(target -> target != null && !target.getUUID().equals(player.getUUID()))
                    .forEach(target -> sendRevealOrMessage(target, definition));
        }

        if (revealDelay > 0) {
            CardUseService.sendEntityRevealAround(
                    player, definition.id().toString(),
                    ItemStack.EMPTY, "event",
                    Component.translatable(definition.nameKey()),
                    Component.translatable(definition.descriptionKey()),
                    definition.texture(),
                    CardBackPreferenceManager.selectedTexture(player),
                    CardRevealPayload.ANIMATION_APPROACH,
                    Math.max(DEFAULT_EVENT_REVEAL_DURATION_TICKS, AstralGameplayConfig.eventRevealLockTicks()));
        } else {
            beginEvent(contexts);
        }

        return true;
    }

    private static boolean canApplyToTarget(ServerPlayer triggerPlayer, AstralEventContext context, boolean force) {
        if (force) return true;
        AstralEventDefinition definition = context.definition();
        AstralEventTriggerSettings settings = definition.triggerSettings();
        String key = settings.key(definition);
        AstralEventState targetState = state(context.target());
        AstralEventState triggerState = state(triggerPlayer);
        if (settings.preventWhileActive() && definition.durationBased() && targetState.active(key)) return false;
        if (settings.whileInactiveOnly() && targetState.active(key)) return false;
        if (settings.oncePerTarget() && targetState.triggerCount(key) > 0) return false;
        if (settings.oncePerPlayer() && triggerState.triggerCount(key) > 0) return false;
        if (settings.maxTriggers() > 0 && targetState.triggerCount(key) >= settings.maxTriggers()) return false;
        return settings.always() || targetState.cooldownLeft(key) <= 0;
    }

    public static void applyActiveTrigger(ServerPlayer player, AstralEventContext triggerContext) {
        if (player == null || triggerContext == null) return;
        AstralEventState eventState = state(player);
        for (Map.Entry<String, AstralActiveEventInstance> entry : eventState.activeEvents().entrySet()) {
            Identifier id = Identifier.parse(entry.getValue().eventId());
            if (!AstralEventManager.INSTANCE.contains(id)) continue;
            AstralEventDefinition definition = AstralEventManager.INSTANCE.get(id);
            if (!definition.canTriggerInDifficulty(player.level().getDifficulty())) continue;
            AstralEventContext context = triggerContext.withDefinition(definition);
            if (definition.testActiveConditions(context)) {
                applyEffects(context, definition.activeEffects());
            }
        }
    }

    public static List<AstralActiveEventInstance> activeEvents(ServerPlayer player) {
        if (player == null) return List.of();
        return new ArrayList<>(state(player).activeEvents().values());
    }

    protected static void tickState(ServerPlayer player) {
        AstralEventState next = state(player).tickCooldowns();
        for (Map.Entry<String, AstralActiveEventInstance> entry : new ArrayList<>(next.activeEvents().entrySet())) {
            Identifier id = Identifier.parse(entry.getValue().eventId());
            if (!AstralEventManager.INSTANCE.contains(id)) {
                next = next.withoutActive(entry.getKey());
                continue;
            }

            AstralEventDefinition definition = AstralEventManager.INSTANCE.get(id);
            AstralEventContext context = AstralEventContext.of(player, player, definition);
            AstralActiveEventInstance instance = entry.getValue().tick();
            if (instance.intervalLeft() <= 0 && definition.safeIntervalTicks() > 0) {
                applyIntervalEffects(context);
                instance = instance.resetInterval();
            }

            if (instance.ticksLeft() <= 0) {
                applyEffects(context, definition.endEffects());
                next = next.withoutActive(entry.getKey());
            } else {
                next = next.updateActive(entry.getKey(), instance);
            }
        }

        setState(player, next);
    }

    protected static void beginEvent(List<AstralEventContext> contexts) {
        for (AstralEventContext context : contexts) {
            AstralEventDefinition definition = context.definition();
            String key = definition.triggerSettings().key(definition);
            AstralEventState next = state(context.target()).markTriggered(key);
            int cooldown = definition.triggerSettings().safeRetriggerDelayTicks(definition);
            next = next.withCooldown(key, cooldown);
            if (definition.durationBased()) {
                AstralActiveEventInstance instance = AstralActiveEventInstance.create(definition);
                next = next.withActive(key, instance);
            }

            setState(context.target(), next);
            applyEffects(context, definition.effects());
        }
    }

    protected static void applyIntervalEffects(AstralEventContext context) {
        applyEffects(context, context.definition().safeIntervalEffects());
    }

    protected static void applyEffects(AstralEventContext context, List<AstralEventEffect> effects) {
        for (AstralEventEffect effect : effects) {
            if (effect != null) effect.apply(context);
        }
    }

    protected static int revealDelay(ServerPlayer viewer) {
        if (viewer.getData(AstralAttachments.EVENT_PREFERENCES).prefersChat()) return 0;
        return Math.max(DEFAULT_EVENT_REVEAL_DURATION_TICKS, AstralGameplayConfig.eventRevealLockTicks());
    }

    protected static void sendRevealOrMessage(ServerPlayer viewer, AstralEventDefinition definition) {
        if (viewer.getData(AstralAttachments.EVENT_PREFERENCES).prefersChat()) {
            viewer.sendSystemMessage(Component.translatable("message.astral_craft.event.triggered_chat", Component.translatable(definition.nameKey())));
            return;
        }

        PacketDistributor.sendToPlayer(viewer, new CardRevealPayload(definition.id().toString(),
                ItemStack.EMPTY, "event",
                Component.translatable(definition.nameKey()),
                Component.translatable(definition.descriptionKey()),
                definition.texture(),
                CardBackPreferenceManager.selectedTexture(viewer),
                CardRevealPayload.ANIMATION_APPROACH,
                Math.max(DEFAULT_EVENT_REVEAL_DURATION_TICKS, AstralGameplayConfig.eventRevealLockTicks()),
                -1, List.of(), false));
    }

    protected static AstralEventState state(Entity entity) {
        if (entity == null) return AstralEventState.empty();
        return entity.getData(AstralAttachments.EVENT_STATE);
    }

    protected static void setState(Entity entity, AstralEventState state) {
        if (entity != null && state != null) {
            entity.setData(AstralAttachments.EVENT_STATE, state);
        }
    }

}