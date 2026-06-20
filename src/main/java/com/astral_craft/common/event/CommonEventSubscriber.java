package com.astral_craft.common.event;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.blocks.BasePlatform;
import com.astral_craft.common.gameplay.KnockdownManager;
import com.astral_craft.common.gameplay.PendingCardActionManager;
import com.astral_craft.common.gameplay.PendingCounterEffectManager;
import com.astral_craft.common.gameplay.SoulLinkManager;
import com.astral_craft.common.gameplay.board.BoardHudSyncManager;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.gameplay.cardback.CardBackManager;
import com.astral_craft.common.gameplay.character.CharacterManager;
import com.astral_craft.common.gameplay.character.CharacterSkinManager;
import com.astral_craft.common.gameplay.event.AstralEventManager;
import com.astral_craft.common.gameplay.event.AstralActiveEventInstance;
import com.astral_craft.common.gameplay.event.AstralEventPreferences;
import com.astral_craft.common.gameplay.event.AstralEventService;
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
import com.astral_craft.common.registry.AstralAttachments;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
                                .suggests((_, builder) -> SharedSuggestionProvider.suggest(List.of(AstralEventPreferences.PRESENTATION_ANIMATION, AstralEventPreferences.PRESENTATION_CHAT), builder))
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
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack itemStack = event.getItemStack();
        Item item = itemStack.getItem();
        if (item instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            if (block instanceof BasePlatform) {
                List<Component> tooltip = event.getToolTip();
                String descriptionId = item.getDescriptionId();
                descriptionId = descriptionId.replaceFirst("block", "tooltips");
                tooltip.add(Component.translatable(descriptionId).withStyle(ChatFormatting.YELLOW));
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        SoulLinkManager.onDamagePre(event);
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer damagedPlayer) {
            AstralEventService.applyActiveTrigger(damagedPlayer, "player_hurt");
            AstralEventService.trigger(damagedPlayer, "player_hurt");
            AstralEventService.applyActiveTrigger(damagedPlayer, "entity_hurt_player");
            AstralEventService.trigger(damagedPlayer, "entity_hurt_player");
        }

        if (event.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer attacker) {
            AstralEventService.applyActiveTrigger(attacker, "player_hurt_entity");
            AstralEventService.trigger(attacker, "player_hurt_entity");
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer killedPlayer) {
            AstralEventService.applyActiveTrigger(killedPlayer, "player_killed");
            AstralEventService.trigger(killedPlayer, "player_killed");
        }

        if (event.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer killer) {
            AstralEventService.applyActiveTrigger(killer, "player_killed_entity");
            AstralEventService.trigger(killer, "player_killed_entity");
        }
    }

    @SubscribeEvent
    public static void onServerTickPost(ServerTickEvent.Post event) {
        PendingCardActionManager.serverTick();
        PendingCounterEffectManager.serverTick(event.getServer());
        KnockdownManager.serverTick();
        BoardSessionManager.serverTick();
        BoardHudSyncManager.serverTick();
        AstralEventService.serverTick(event.getServer());
        event.getServer().getPlayerList().getPlayers().forEach(player -> AstralEventService.trigger(player, "tick"));
    }

    @SubscribeEvent
    public static void onBlockBreak(BreakBlockEvent event) {
        if (event.getLevel() instanceof ServerLevel serverLevel && BoardSessionManager.isProtected(serverLevel, event.getPos())) {
            event.setCanceled(true);
            event.getPlayer().sendOverlayMessage(Component.translatable("message.astral_craft.board.protected").withStyle(ChatFormatting.YELLOW));
            return;
        }

        if (event.getPlayer() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            AstralEventService.applyActiveTrigger(serverPlayer, "block_break");
            AstralEventService.trigger(serverPlayer, "block_break");
        }
    }

}