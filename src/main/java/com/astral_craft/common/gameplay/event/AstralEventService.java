package com.astral_craft.common.gameplay.event;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.CardUseService;
import com.astral_craft.common.gameplay.PendingCardActionManager;
import com.astral_craft.common.gameplay.cardback.CardBackPreferenceManager;
import com.astral_craft.common.network.CardRevealPayload;
import com.astral_craft.common.registry.AstralAttachments;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AstralEventService {

    public static final int DEFAULT_EVENT_REVEAL_DURATION_TICKS = CardUseService.CARD_APPROACH_REVEAL_DURATION_TICKS;

    private static final ConcurrentLinkedQueue<ActiveEvent> ACTIVE_EVENTS = new ConcurrentLinkedQueue<>();

    public static void serverTick(MinecraftServer server) {
        if (server != null) {
            server.getPlayerList().getPlayers().forEach(player -> setState(player, state(player).tickCooldowns()));
        }

        ACTIVE_EVENTS.removeIf(ActiveEvent::tick);
    }

    public static boolean trigger(ServerPlayer player, String trigger) {
        List<AstralEventDefinition> candidates = AstralEventManager.INSTANCE.matching(trigger);
        if (candidates.isEmpty()) return false;
        for (AstralEventDefinition definition : candidates) {
            if (tryTrigger(player, definition, false, trigger)) {
                return true;
            }
        }

        return false;
    }

    public static boolean triggerById(ServerPlayer player, Identifier id) {
        if (player == null || id == null || !AstralEventManager.INSTANCE.contains(id)) return false;
        return tryTrigger(player, AstralEventManager.INSTANCE.get(id), true, "manual");
    }

    public static boolean tryTrigger(ServerPlayer player, AstralEventDefinition definition) {
        return tryTrigger(player, definition, false, "manual");
    }

    public static boolean tryTrigger(ServerPlayer player, AstralEventDefinition definition, boolean force, String trigger) {
        if (player == null || definition == null) return false;
        AstralEventContext base = AstralEventContext.of(player, player, definition, trigger);
        if (!force && !definition.testConditions(base)) return false;
        double chance = Math.clamp(definition.chance(), 0.0D, 1.0D);
        if (!force && chance < 1.0D && player.getRandom().nextDouble() > chance) return false;
        List<AstralEventContext> contexts = new ArrayList<>();
        for (Entity target : definition.target().resolve(player)) {
            AstralEventContext context = AstralEventContext.of(player, target, definition, trigger);
            if ((force || definition.testConditions(context)) && canApplyToTarget(player, context, force)) {
                contexts.add(context);
            }
        }

        if (contexts.isEmpty()) return false;
        sendReveal(player, definition);
        if (definition.broadcast()) {
            contexts.stream().map(AstralEventContext::targetPlayer).filter(target -> target != null && !target.getUUID().equals(player.getUUID())).forEach(target -> sendReveal(target, definition));
        }

        PendingCardActionManager.schedule(player, DEFAULT_EVENT_REVEAL_DURATION_TICKS, () -> beginEvent(contexts));
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

    public static void applyActiveTrigger(ServerPlayer player, String trigger) {
        if (player == null || trigger == null || trigger.isBlank()) return;
        AstralEventState eventState = state(player);
        for (Map.Entry<String, AstralActiveEventInstance> entry : eventState.activeEvents().entrySet()) {
            Identifier id = Identifier.parse(entry.getValue().eventId());
            if (!AstralEventManager.INSTANCE.contains(id)) continue;
            AstralEventDefinition definition = AstralEventManager.INSTANCE.get(id);
            if (!definition.canApplyDuring(trigger)) continue;
            AstralEventContext context = AstralEventContext.of(player, player, definition, trigger);
            if (definition.testConditions(context)) {
                applyEffects(context, definition.activeEffects());
            }
        }
    }

    public static List<AstralActiveEventInstance> activeEvents(ServerPlayer player) {
        if (player == null) return List.of();
        return new ArrayList<>(state(player).activeEvents().values());
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
                setState(context.target(), next);
                ACTIVE_EVENTS.add(new ActiveEvent(context, key, instance));
            } else {
                setState(context.target(), next);
            }

            applyEffects(context, definition.effects());
        }
    }

    protected static void finishEvent(AstralEventContext context, String key) {
        applyEffects(context, context.definition().endEffects());
        setState(context.target(), state(context.target()).withoutActive(key));
    }

    protected static void applyIntervalEffects(AstralEventContext context) {
        applyEffects(context, context.definition().safeIntervalEffects());
    }

    protected static void applyEffects(AstralEventContext context, List<AstralEventEffect> effects) {
        for (AstralEventEffect effect : effects) {
            if (effect != null) {
                effect.apply(context);
            }
        }
    }

    protected static void sendReveal(ServerPlayer viewer, AstralEventDefinition definition) {
        PacketDistributor.sendToPlayer(viewer, new CardRevealPayload(definition.id().toString(),
                AstralCraft.MOD_ID + ":event_card",
                "event",
                definition.nameKey(),
                definition.descriptionKey(),
                definition.texture().toString(),
                CardBackPreferenceManager.selectedTexture(viewer).toString(),
                CardRevealPayload.ANIMATION_APPROACH,
                DEFAULT_EVENT_REVEAL_DURATION_TICKS));
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

    private static final class ActiveEvent {
        private final AstralEventContext context;
        private final String key;
        private AstralActiveEventInstance instance;

        private ActiveEvent(AstralEventContext context, String key, AstralActiveEventInstance instance) {
            this.context = context;
            this.key = key;
            this.instance = instance;
        }

        private boolean tick() {
            if (this.context.target() == null || this.context.target().isRemoved()) {
                return true;
            }
            AstralEventState state = AstralEventService.state(this.context.target());
            if (!state.active(this.key)) {
                return true;
            }
            this.instance = this.instance.tick();
            if (this.instance.intervalLeft() <= 0 && this.context.definition().safeIntervalTicks() > 0) {
                applyIntervalEffects(this.context);
                this.instance = this.instance.resetInterval();
            }
            if (this.instance.ticksLeft() <= 0) {
                finishEvent(this.context, this.key);
                return true;
            }
            setState(this.context.target(), state.updateActive(this.key, this.instance));
            return false;
        }
    }

}