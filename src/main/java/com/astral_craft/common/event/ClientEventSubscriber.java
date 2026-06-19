package com.astral_craft.common.event;

import com.astral_craft.AstralCraft;
import com.astral_craft.client.gui.CardRevealOverlay;
import com.astral_craft.client.gui.ChipSelectionScreen;
import com.astral_craft.client.gui.TargetSelectionScreen;
import com.astral_craft.client.gui.battle.BattleSceneScreen;
import com.astral_craft.client.gui.board.BoardHudOverlay;
import com.astral_craft.client.gui.cardback.CardBackSelectionScreen;
import com.astral_craft.client.gui.character.CharacterSettingsScreen;
import com.astral_craft.client.input.AstralKeyMappings;
import com.astral_craft.client.model.LargeCuboidModelLoader;
import com.astral_craft.client.model.character.AstralGeoAnimationManager;
import com.astral_craft.client.model.character.AstralGeoModelManager;
import com.astral_craft.client.model.entity.FirecrackersModel;
import com.astral_craft.client.render.AstralDiceRenderer;
import com.astral_craft.client.render.character.AstralCharacterRenderer;
import com.astral_craft.client.render.SoulLinkRenderer;
import com.astral_craft.client.render.effect.FallingBrickRenderer;
import com.astral_craft.client.render.effect.LaserStrikeRenderer;
import com.astral_craft.client.render.projectile.FirecrackersRenderer;
import com.astral_craft.client.render.projectile.SlingshotProjectileRenderer;
import com.astral_craft.client.render.projectile.SnowballAttackProjectileRenderer;
import com.astral_craft.common.network.*;
import com.astral_craft.common.registry.AstralEntities;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

@EventBusSubscriber(modid = AstralCraft.MOD_ID, value = Dist.CLIENT)
public class ClientEventSubscriber {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (AstralKeyMappings.CARD_BACK_SELECTION.get().consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                ClientPacketDistributor.sendToServer(new RequestCardBackSelectionPayload());
            }
        }

        while (AstralKeyMappings.CHARACTER_SETTINGS.get().consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                ClientPacketDistributor.sendToServer(new RequestCharacterSettingsPayload());
            }
        }
    }


    @SubscribeEvent
    public static void addClientReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(AstralCraft.prefix("character_models"), AstralGeoModelManager.INSTANCE);
        event.addListener(AstralCraft.prefix("character_animations"), AstralGeoAnimationManager.INSTANCE);
    }

    @SubscribeEvent
    public static void registerModelLoaders(ModelEvent.RegisterLoaders event) {
        event.register(LargeCuboidModelLoader.ID, LargeCuboidModelLoader.INSTANCE);
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        AstralKeyMappings.register(event);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(BoardHudOverlay.LAYER, BoardHudOverlay::render);
        event.registerAboveAll(CardRevealOverlay.LAYER, CardRevealOverlay::render);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(FirecrackersModel.LAYER, FirecrackersModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AstralEntities.ASTRAL_DICE.get(), AstralDiceRenderer::new);
        event.registerEntityRenderer(AstralEntities.SOUL_LINK.get(), SoulLinkRenderer::new);
        event.registerEntityRenderer(AstralEntities.LASER_STRIKE.get(), LaserStrikeRenderer::new);
        event.registerEntityRenderer(AstralEntities.FIRECRACKERS_PROJECTILE.get(), FirecrackersRenderer::new);
        event.registerEntityRenderer(AstralEntities.SLINGSHOT_PROJECTILE.get(), SlingshotProjectileRenderer::new);
        event.registerEntityRenderer(AstralEntities.SNOWBALL_ATTACK_PROJECTILE.get(), SnowballAttackProjectileRenderer::new);
        event.registerEntityRenderer(AstralEntities.FALLING_BRICK.get(), FallingBrickRenderer::new);
        event.registerEntityRenderer(AstralEntities.ASTRAL_CHARACTER.get(), AstralCharacterRenderer::new);
    }

    @SubscribeEvent
    public static void registerClientPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(CardRevealPayload.TYPE, CardRevealOverlay::show);
        event.register(OpenTargetSelectionPayload.TYPE, TargetSelectionScreen::open);
        event.register(OpenBattleScenePayload.TYPE, BattleSceneScreen::open);
        event.register(OpenChipSelectionPayload.TYPE, ChipSelectionScreen::open);
        event.register(BoardHudSnapshotPayload.TYPE, BoardHudOverlay::acceptSnapshot);
        event.register(OpenCardBackSelectionPayload.TYPE, CardBackSelectionScreen::open);
        event.register(OpenCharacterSettingsPayload.TYPE, CharacterSettingsScreen::open);
    }

}