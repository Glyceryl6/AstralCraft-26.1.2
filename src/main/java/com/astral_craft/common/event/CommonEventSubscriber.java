package com.astral_craft.common.event;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.blocks.BasePlatform;
import com.astral_craft.common.gameplay.KnockdownManager;
import com.astral_craft.common.items.cards.pvp.HandcardSoulLink;
import com.astral_craft.common.gameplay.board.*;
import com.astral_craft.common.gameplay.battle.BoardBattleService;
import com.astral_craft.common.gameplay.cardback.CardBackManager;
import com.astral_craft.common.gameplay.character.CharacterManager;
import com.astral_craft.common.gameplay.character.skill.AstralCharacterSkillService;
import com.astral_craft.common.gameplay.character.skin.CharacterSkinManager;
import com.astral_craft.common.gameplay.event.*;
import com.astral_craft.common.gameplay.handcard.PendingCardActionManager;
import com.astral_craft.common.gameplay.handcard.PendingCounterEffectManager;
import com.astral_craft.common.registry.AstralDataComponents;
import com.astral_craft.common.registry.AstralAttachments;
import com.astral_craft.common.gameplay.character.skill.effect.AstralStatusMobEffect;
import com.astral_craft.common.items.BaseHandCard;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = AstralCraft.MOD_ID)
public class CommonEventSubscriber {

    @SubscribeEvent
    public static void onAddServerReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(AstralCraft.prefix("card_backs"), CardBackManager.INSTANCE);
        event.addListener(AstralCraft.prefix("character_skins"), CharacterSkinManager.INSTANCE);
        event.addListener(AstralCraft.prefix("characters"), CharacterManager.INSTANCE);
        event.addListener(AstralCraft.prefix("astral_events"), AstralEventManager.INSTANCE);
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("astral_event")
                .then(Commands.literal("trigger")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("id", StringArgumentType.greedyString())
                                .suggests(CommonEventSubscriber::suggestAstralEventIds)
                                .executes(context -> {
                                    String rawId = StringArgumentType.getString(context, "id");
                                    Identifier id = rawId.contains(":") ? Identifier.parse(rawId) : AstralCraft.prefix(rawId);
                                    boolean triggered = AstralEventService.triggerById(context.getSource().getPlayerOrException(), id);
                                    context.getSource().sendSuccess(() -> Component.translatable(triggered
                                            ? "commands.astral_craft.event.triggered"
                                            : "commands.astral_craft.event.not_triggered", id.toString()), false);
                                    return triggered ? 1 : 0;
                                })))
                .then(Commands.literal("presentation")
                        .then(Commands.argument("mode", StringArgumentType.word())
                                .suggests((_, builder) -> SharedSuggestionProvider.suggest(List.of(
                                        AstralEventPreferences.PRESENTATION_ANIMATION,
                                        AstralEventPreferences.PRESENTATION_CHAT), builder))
                                .executes(context -> {
                                    String mode = StringArgumentType.getString(context, "mode");
                                    var player = context.getSource().getPlayerOrException();
                                    AstralEventPreferences preferences = player.getData(AstralAttachments.EVENT_PREFERENCES).withPresentation(mode);
                                    player.setData(AstralAttachments.EVENT_PREFERENCES, preferences);
                                    context.getSource().sendSuccess(() -> Component.translatable("commands.astral_craft.event.presentation", preferences.presentation()), false);
                                    return 1;
                                })))
                .then(Commands.literal("list")
                        .executes(context -> {
                            List<AstralActiveEventInstance> activeEvents = AstralEventService.activeEvents(context.getSource().getPlayerOrException());
                            if (activeEvents.isEmpty()) {
                                context.getSource().sendSuccess(() -> Component.translatable("commands.astral_craft.event.none_active"), false);
                                return 0;
                            }

                            for (AstralActiveEventInstance activeEvent : activeEvents) {
                                context.getSource().sendSuccess(() -> Component.translatable("commands.astral_craft.event.active",
                                        Component.translatable(activeEvent.nameKey()), activeEvent.secondsLeft()), false);
                            }

                            return activeEvents.size();
                        })));
    }

    protected static CompletableFuture<Suggestions> suggestAstralEventIds(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        Set<String> candidates = new LinkedHashSet<>(AstralEventManager.INSTANCE.idStrings());
        for (String candidate : candidates) {
            String lower = candidate.toLowerCase(Locale.ROOT);
            if (remaining.isBlank() || lower.startsWith(remaining) || lower.contains(remaining)) {
                builder.suggest(candidate);
            }
        }

        return builder.buildFuture();
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof FallingBlockEntity fallingBlock
                && event.getLevel() instanceof ServerLevel serverLevel
                && BoardProtectionService.protectFallingBlock(serverLevel, fallingBlock)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack itemStack = event.getItemStack();
        Item item = itemStack.getItem();
        if (item instanceof BaseHandCard handCard) {
            handCard.appendHoverText(itemStack, event.getContext(), event.getToolTip()::add, event.getFlags(), event.getEntity());
        }

        if (itemStack.has(AstralDataComponents.BOARD_SPECTATOR_BINDING.get())) {
            event.getToolTip().add(Component.translatable("tooltips.astral_craft.board_spectator.bound").withStyle(ChatFormatting.AQUA));
        }

        if (item instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            if (block instanceof BasePlatform platform) {
                event.getToolTip().add(platform.tooltip());
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        HandcardSoulLink.onDamagePre(event);
        if (event.getEntity() instanceof ServerPlayer damagedPlayer) {
            AstralEventContext context = AstralEventContext.damage(damagedPlayer, damagedPlayer, event.getSource(), event.getNewDamage());
            AstralEventService.applyActiveTrigger(damagedPlayer, context);
        }

        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            AstralEventContext context = AstralEventContext.damage(attacker, event.getEntity(), event.getSource(), event.getNewDamage());
            AstralEventService.applyActiveTrigger(attacker, context);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer killedPlayer) {
            AstralEventContext context = AstralEventContext.death(killedPlayer, killedPlayer, event.getSource());
            AstralEventService.applyActiveTrigger(killedPlayer, context);
        }

        if (event.getSource().getEntity() instanceof ServerPlayer killer) {
            AstralEventContext context = AstralEventContext.death(killer, event.getEntity(), event.getSource());
            AstralEventService.applyActiveTrigger(killer, context);
        }
    }

    @SubscribeEvent
    public static void onServerTickPost(ServerTickEvent.Post event) {
        PendingCardActionManager.serverTick();
        PendingCounterEffectManager.serverTick(event.getServer());
        KnockdownManager.serverTick();
        BoardSessionManager.serverTick(event.getServer());
        BoardSpectatorService.serverTick(event.getServer());
        BoardBattleService.serverTick(event.getServer());
        BoardHudSyncManager.serverTick(event.getServer());
        AstralEventService.serverTick(event.getServer());
        event.getServer().getPlayerList().getPlayers().forEach(AstralCharacterSkillService::serverTick);
    }

    @SubscribeEvent
    public static void onMobEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance().getEffect().value() instanceof AstralStatusMobEffect effect) {
            effect.onEffectApplicable(event);
        }
    }

    @SubscribeEvent
    public static void onMobEffectAdded(MobEffectEvent.Added event) {
        if (event.getEffectInstance().getEffect().value() instanceof AstralStatusMobEffect effect) {
            effect.onEffectAdded(event.getEntity(), event.getEffectInstance());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onMobEffectRemove(MobEffectEvent.Remove event) {
        if (event.isCanceled()) return;
        if (event.getEffect().value() instanceof AstralStatusMobEffect effect) {
            effect.onEffectRemoved(event.getEntity());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onMobEffectExpired(MobEffectEvent.Expired event) {
        MobEffectInstance instance = event.getEffectInstance();
        if (instance != null && instance.getEffect().value() instanceof AstralStatusMobEffect effect) {
            effect.onEffectExpired(event.getEntity(), instance);
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        for (MobEffectInstance instance : new ArrayList<>(event.getEntity().getActiveEffects())) {
            if (instance.getEffect().value() instanceof AstralStatusMobEffect effect) {
                effect.onAttackEntity(event);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() == null) return;
        for (MobEffectInstance instance : event.getNewAboutToBeSetTarget().getActiveEffects()) {
            if (instance.getEffect().value() instanceof AstralStatusMobEffect effect) {
                effect.onLivingChangeTarget(event);
            }
        }
    }

    @SubscribeEvent
    public static void onEntityInvulnerabilityCheck(EntityInvulnerabilityCheckEvent event) {
        if (!(event.getEntity() instanceof LivingEntity livingEntity)) return;
        for (MobEffectInstance instance : livingEntity.getActiveEffects()) {
            if (instance.getEffect().value() instanceof AstralStatusMobEffect effect) {
                effect.onInvulnerabilityCheck(event);
            }
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel serverLevel
                && BoardProtectionService.isProtected(serverLevel, event.getPos())) {
            event.setCanceled(true);
            if (event.getEntity() instanceof ServerPlayer player) {
                player.sendOverlayMessage(Component.translatable("message.astral_craft.board.protected")
                        .withStyle(ChatFormatting.YELLOW));
            }
        }
    }


    @SubscribeEvent
    public static void onBlockToolModification(BlockEvent.BlockToolModificationEvent event) {
        if (event.getLevel() instanceof ServerLevel serverLevel
                && BoardProtectionService.isProtected(serverLevel, event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onFarmlandTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (event.getLevel() instanceof ServerLevel serverLevel
                && BoardProtectionService.isProtected(serverLevel, event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onFluidPlace(BlockEvent.FluidPlaceBlockEvent event) {
        if (event.getLevel() instanceof ServerLevel serverLevel
                && BoardProtectionService.isProtected(serverLevel, event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        event.getAffectedBlocks().removeIf(pos -> BoardProtectionService.isProtected(serverLevel, pos));
    }

    @SubscribeEvent
    public static void onPistonMove(PistonEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        if (BoardProtectionService.isProtected(serverLevel, event.getPos())
                || BoardProtectionService.isProtected(serverLevel, event.getFaceOffsetPos())) {
            event.setCanceled(true);
            return;
        }

        var resolver = event.getStructureHelper();
        if (resolver == null || !resolver.resolve()) return;
        for (var blockPos : resolver.getToPush()) {
            if (BoardProtectionService.isProtected(serverLevel, blockPos)
                    || BoardProtectionService.isProtected(serverLevel, blockPos.relative(event.getDirection()))
                    || BoardProtectionService.isProtected(serverLevel, blockPos.relative(event.getDirection().getOpposite()))) {
                event.setCanceled(true);
                return;
            }
        }

        for (var blockPos : resolver.getToDestroy()) {
            if (BoardProtectionService.isProtected(serverLevel, blockPos)) {
                event.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!BoardRouteService.selectBranch(player, event.getPos())) return;
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onBlockBreak(BreakBlockEvent event) {
        if (event.getLevel() instanceof ServerLevel serverLevel && BoardProtectionService.isProtected(serverLevel, event.getPos())) {
            event.setCanceled(true);
            event.setNotifyClient(true);
            event.getPlayer().sendOverlayMessage(Component.translatable("message.astral_craft.board.protected").withStyle(ChatFormatting.YELLOW));
            return;
        }

        if (event.getPlayer() instanceof ServerPlayer serverPlayer) {
            AstralEventContext context = AstralEventContext.blockBreak(serverPlayer, event.getPos(), event.getState());
            AstralEventService.applyActiveTrigger(serverPlayer, context);
        }
    }

}