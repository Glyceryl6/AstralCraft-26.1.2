package com.astral_craft.common.event;

import com.astral_craft.AstralCraft;
import com.astral_craft.client.gui.CardRevealOverlay;
import com.astral_craft.client.gui.TargetSelectionScreen;
import com.astral_craft.client.gui.battle.BattleSceneScreen;
import com.astral_craft.client.model.LargeCuboidModelLoader;
import com.astral_craft.client.render.AstralDiceRenderer;
import com.astral_craft.client.render.SoulLinkRenderer;
import com.astral_craft.client.render.effect.ArcProjectileRenderer;
import com.astral_craft.client.render.effect.FallingBrickRenderer;
import com.astral_craft.client.render.effect.LaserStrikeRenderer;
import com.astral_craft.common.network.CardRevealPayload;
import com.astral_craft.common.network.OpenBattleScenePayload;
import com.astral_craft.common.network.OpenTargetSelectionPayload;
import com.astral_craft.common.registry.AstralEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

@EventBusSubscriber(modid = AstralCraft.MOD_ID, value = Dist.CLIENT)
public class ClientEventSubscriber {

    @SubscribeEvent
    public static void registerModelLoaders(ModelEvent.RegisterLoaders event) {
        event.register(LargeCuboidModelLoader.ID, LargeCuboidModelLoader.INSTANCE);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(CardRevealOverlay.LAYER, CardRevealOverlay::render);
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AstralEntities.ASTRAL_DICE.get(), AstralDiceRenderer::new);
        event.registerEntityRenderer(AstralEntities.SOUL_LINK.get(), SoulLinkRenderer::new);
        event.registerEntityRenderer(AstralEntities.LASER_STRIKE.get(), LaserStrikeRenderer::new);
        event.registerEntityRenderer(AstralEntities.ARC_PROJECTILE.get(), ArcProjectileRenderer::new);
        event.registerEntityRenderer(AstralEntities.FALLING_BRICK.get(), FallingBrickRenderer::new);
    }

    @SubscribeEvent
    public static void registerClientPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(CardRevealPayload.TYPE, CardRevealOverlay::show);
        event.register(OpenTargetSelectionPayload.TYPE, TargetSelectionScreen::open);
        event.register(OpenBattleScenePayload.TYPE, BattleSceneScreen::open);
    }

}