package com.astral_craft.common.event;

import com.astral_craft.AstralCraft;
import com.astral_craft.client.gui.*;
import com.astral_craft.client.gui.board.*;
import com.astral_craft.client.gui.appearance.AppearanceSelectionScreen;
import com.astral_craft.client.gui.components.AstralConfirmationScreen;
import com.astral_craft.client.gui.character.AstralSkinRarityManager;
import com.astral_craft.client.gui.character.CharacterSettingsScreen;
import com.astral_craft.client.gui.character.ExhibitionCharacterConfigScreen;
import com.astral_craft.client.gui.phrase.QuickPhraseSidebar;
import com.astral_craft.client.gui.phrase.QuickPhraseSidebarHost;
import com.astral_craft.client.input.AstralKeyMappings;
import com.astral_craft.client.jpgloader.JpgCacheReloadListener;
import com.astral_craft.client.model.LargeCuboidModelLoader;
import com.astral_craft.client.model.character.AstralGeoAnimationManager;
import com.astral_craft.client.model.character.AstralGeoModelManager;
import com.astral_craft.client.model.entity.FirecrackersModel;
import com.astral_craft.client.render.*;
import com.astral_craft.client.render.blockentity.PlatformBlockEntityRenderer;
import com.astral_craft.client.render.character.AstralCharacterRenderStateModifier;
import com.astral_craft.client.render.character.AstralCharacterRenderer;
import com.astral_craft.client.render.character.AstralPlayerCharacterRenderBridge;
import com.astral_craft.client.render.effect.FallingBrickRenderer;
import com.astral_craft.client.render.effect.LaserStrikeRenderer;
import com.astral_craft.client.render.projectile.FirecrackersRenderer;
import com.astral_craft.client.render.projectile.SlingshotProjectileRenderer;
import com.astral_craft.client.render.projectile.SnowballAttackProjectileRenderer;
import com.astral_craft.client.util.ClientAnimationClock;
import com.astral_craft.common.network.c2s.BoardDismantleConfirmPayload;
import com.astral_craft.common.network.c2s.RequestCardBackSelectionPayload;
import com.astral_craft.common.network.c2s.RequestCharacterSettingsPayload;
import com.astral_craft.common.network.c2s.RequestCharacterSkillPayload;
import com.astral_craft.common.network.c2s.RequestHandCardDeckPayload;
import com.astral_craft.common.network.s2c.*;
import com.astral_craft.common.registry.AstralBlockEntities;
import com.astral_craft.common.registry.AstralEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Avatar;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import org.jspecify.annotations.NullMarked;

import java.util.List;

@EventBusSubscriber(modid = AstralCraft.MOD_ID, value = Dist.CLIENT)
public class ClientEventSubscriber {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        CardRevealEntityOverlay.tick();
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

        while (AstralKeyMappings.HAND_CARD_DECK.get().consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                ClientPacketDistributor.sendToServer(new RequestHandCardDeckPayload());
            }
        }

        while (AstralKeyMappings.CHARACTER_SKILL.get().consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                ClientPacketDistributor.sendToServer(new RequestCharacterSkillPayload());
            }
        }
    }

    @SubscribeEvent
    public static void onRenderFrame(RenderFrameEvent.Pre event) {
        ClientAnimationClock.updateFrame(event.getPartialTick());
    }

    @SubscribeEvent
    public static void onClientPauseChanged(ClientPauseChangeEvent.Post event) {
        ClientAnimationClock.setPaused(event.isPaused());
    }

    @SubscribeEvent
    public static void addClientReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(AstralCraft.prefix("character_models"), AstralGeoModelManager.INSTANCE);
        event.addListener(AstralCraft.prefix("character_animations"), AstralGeoAnimationManager.INSTANCE);
        event.addListener(AstralCraft.prefix("skin_rarities"), AstralSkinRarityManager.INSTANCE);
        event.addListener(AstralCraft.prefix("jpg_texture_cache"), JpgCacheReloadListener.INSTANCE);
    }

    @SubscribeEvent
    public static void registerSpecialModelRenderers(RegisterSpecialModelRendererEvent event) {
        event.register(AstralCraft.prefix("dice"), AstralDiceSpecialRenderer.Unbaked.MAP_CODEC);
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
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        AstralStatusMobEffectClientExtensions.register(event);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(BoardHudOverlay.LAYER, BoardHudOverlay::render);
        event.registerAboveAll(BoardAnnouncementOverlay.LAYER, BoardAnnouncementOverlay::render);
        event.registerAboveAll(BoardRouteDecisionOverlay.LAYER, BoardRouteDecisionOverlay::render);
        event.registerAboveAll(CardRevealOverlay.LAYER, CardRevealOverlay::render);
        event.registerAboveAll(AstralHandCardHudOverlay.LAYER, AstralHandCardHudOverlay::render);
        event.registerAboveAll(CharacterSkillCutinOverlay.LAYER, CharacterSkillCutinOverlay::render);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(FirecrackersModel.LAYER, FirecrackersModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void submitWorldGeometry(SubmitCustomGeometryEvent event) {
        CardRevealWorldRenderer.submit(event);
        PlatformTooltipWorldRenderer.submit(event);
        ExhibitionSpeechBubbleRenderer.submit(event);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            try (Gizmos.TemporaryCollection ignored = minecraft.levelRenderer.collectPerFrameGizmos()) {
                BoardRouteWorldRenderer.submit();
                BoardTemplatePreviewRenderer.submit();
                CustomPaintingPreviewRenderer.submit();
            }
        }

        BoardProtectionWorldRenderer.submit(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void renderFirstPersonArm(RenderArmEvent event) {
        AstralPlayerCharacterRenderBridge.renderFirstPersonArm(event);
    }

    @NullMarked
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static<T extends Avatar & ClientAvatarEntity> void beforePlayerRender(RenderPlayerEvent.Pre<T> event) {
        AstralPlayerCharacterRenderBridge.beforeRender(event);
    }

    @NullMarked
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static<T extends Avatar & ClientAvatarEntity> void afterPlayerRender(RenderPlayerEvent.Post<T> event) {
        AstralPlayerCharacterRenderBridge.afterRender(event);
    }

    @SubscribeEvent
    public static void registerRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerAvatarEntityModifier(new AstralCharacterRenderStateModifier());
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(AstralBlockEntities.PLATFORM.get(), context -> new PlatformBlockEntityRenderer());
        event.registerEntityRenderer(AstralEntities.ASTRAL_DICE.get(), AstralDiceRenderer::new);
        event.registerEntityRenderer(AstralEntities.SOUL_LINK.get(), SoulLinkRenderer::new);
        event.registerEntityRenderer(AstralEntities.BOARD_WORLD_OBJECT.get(), BoardWorldObjectRenderer::new);
        event.registerEntityRenderer(AstralEntities.STAR_COIN.get(), StarCoinRenderer::new);
        event.registerEntityRenderer(AstralEntities.LASER_STRIKE.get(), LaserStrikeRenderer::new);
        event.registerEntityRenderer(AstralEntities.FALLING_BRICK.get(), FallingBrickRenderer::new);
        event.registerEntityRenderer(AstralEntities.ASTRAL_CHARACTER.get(), AstralCharacterRenderer::new);
        event.registerEntityRenderer(AstralEntities.EXHIBITION_CHARACTER.get(), AstralCharacterRenderer::new);
        event.registerEntityRenderer(AstralEntities.FIRECRACKERS_PROJECTILE.get(), FirecrackersRenderer::new);
        event.registerEntityRenderer(AstralEntities.SLINGSHOT_PROJECTILE.get(), SlingshotProjectileRenderer::new);
        event.registerEntityRenderer(AstralEntities.SNOWBALL_ATTACK_PROJECTILE.get(), SnowballAttackProjectileRenderer::new);
        event.registerEntityRenderer(AstralEntities.CUSTOM_PAINTING.get(), CustomPaintingRenderer::new);
    }

    @SubscribeEvent
    public static void registerClientPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(CardRevealPayload.TYPE, CardRevealOverlay::show);
        event.register(CardRevealControlPayload.TYPE, CardRevealOverlay::control);
        event.register(CharacterSkillCutinPayload.TYPE, CharacterSkillCutinOverlay::show);
        event.register(CardRevealEntityPayload.TYPE, CardRevealEntityOverlay::show);
        event.register(OpenTargetSelectionPayload.TYPE, TargetSelectionScreen::open);
        event.register(OpenCardNumberSelectionPayload.TYPE, CardNumberSelectionScreen::open);
        event.register(OpenChipSelectionPayload.TYPE, ChipSelectionScreen::open);
        event.register(BoardHudSnapshotPayload.TYPE, BoardHudOverlay::acceptSnapshot);
        event.register(BoardAnnouncementPayload.TYPE, BoardAnnouncementOverlay::show);
        event.register(OpenBoardCharacterSelectionPayload.TYPE, BoardCharacterSelectionScreen::open);
        event.register(OpenBoardMatchmakingModeSelectionPayload.TYPE, BoardMatchmakingModeSelectionScreen::open);
        event.register(OpenBoardDeveloperPayload.TYPE, BoardDeveloperCharacterScreen::openDeveloper);
        event.register(OpenBoardProjectorConfirmPayload.TYPE, BoardProjectorConfirmScreen::open);
        event.register(OpenBoardModeSelectionPayload.TYPE, BoardModeSelectionScreen::open);
        event.register(OpenBoardDivinationPayload.TYPE, BoardDivinationScreen::open);
        event.register(ResolveBoardDivinationPayload.TYPE, BoardDivinationScreen::resolve);
        event.register(OpenBoardDismantleConfirmPayload.TYPE, (payload, context) -> context.enqueueWork(() ->
                Minecraft.getInstance().setScreen(new AstralConfirmationScreen(
                        Component.translatable("gui.astral_craft.board_dismantle.confirm.title"),
                        List.of(Component.translatable("gui.astral_craft.board_dismantle.confirm.warning"),
                                Component.translatable("gui.astral_craft.board_dismantle.confirm.details", payload.panelCount())),
                        Component.translatable("gui.astral_craft.board_dismantle.confirm.remove_all"),
                        Component.translatable("gui.astral_craft.board_dismantle.confirm.remove_data_only"),
                        Component.translatable("gui.astral_craft.confirm.cancel"),
                        () -> ClientPacketDistributor.sendToServer(new BoardDismantleConfirmPayload(payload.boardId(),
                                BoardDismantleConfirmPayload.Action.REMOVE_DATA_AND_PANELS)),
                        () -> ClientPacketDistributor.sendToServer(new BoardDismantleConfirmPayload(payload.boardId(),
                                BoardDismantleConfirmPayload.Action.REMOVE_DATA_ONLY))))));
        event.register(OpenBoardTurnPayload.TYPE, BoardTurnScreen::open);
        event.register(OpenBoardDiscardPayload.TYPE, BoardDiscardScreen::open);
        event.register(OpenBoardEncounterPayload.TYPE, BoardEncounterScreen::open);
        event.register(CloseBoardEncounterPayload.TYPE, BoardEncounterScreen::close);
        event.register(CloseBoardPresentationPayload.TYPE, (payload, context) -> context.enqueueWork(() -> {
            BoardEncounterScreen.closePresentation(payload.boardId());
            BoardBattleScreen.closePresentation(payload.boardId());
            BoardGambleScreen.closePresentation(payload.boardId());
            BoardLotteryDrawScreen.closePresentation(payload.boardId());
            BoardHospitalScreen.closePresentation(payload.boardId());
            BoardRelicShopScreen.closePresentation(payload.boardId());
            BoardDivinationScreen.closePresentation(payload.boardId());
            ChipSelectionScreen.closePresentation(payload.boardId());
            TargetSelectionScreen.closePresentation(payload.boardId());
            BoardLotteryNumberScreen.closePresentation(payload.boardId());
            BoardHudOverlay.clear(payload.boardId());
            BoardTutorialGuide.clear(payload.boardId());
            BoardRouteWorldRenderer.clear(payload.boardId());
            CardRevealOverlay.clear();
        }));
        event.register(OpenBoardBattlePayload.TYPE, BoardBattleScreen::open);
        event.register(OpenBoardStartChoicePayload.TYPE, BoardStartChoiceScreen::open);
        event.register(OpenBoardLotteryNumberPayload.TYPE, BoardLotteryNumberScreen::open);
        event.register(CloseBoardLotteryNumberPayload.TYPE, BoardLotteryNumberScreen::close);
        event.register(OpenBoardGamblePayload.TYPE, BoardGambleScreen::open);
        event.register(CloseBoardGamblePayload.TYPE, BoardGambleScreen::close);
        event.register(OpenBoardLotteryDrawPayload.TYPE, BoardLotteryDrawScreen::open);
        event.register(OpenBoardHospitalPayload.TYPE, BoardHospitalScreen::open);
        event.register(CloseBoardHospitalPayload.TYPE, BoardHospitalScreen::close);
        event.register(OpenBoardPlatformTargetPayload.TYPE, TargetSelectionScreen::open);
        event.register(CloseBoardPlatformTargetPayload.TYPE, TargetSelectionScreen::close);
        event.register(CloseBoardLotteryDrawPayload.TYPE, BoardLotteryDrawScreen::close);
        event.register(OpenBoardShopPayload.TYPE, BoardShopScreen::open);
        event.register(OpenBoardRelicShopPayload.TYPE, BoardRelicShopScreen::open);
        event.register(OpenBoardPanelSelectionPayload.TYPE, BoardPanelSelectionScreen::open);
        event.register(BoardRouteStatePayload.TYPE, BoardRouteWorldRenderer::accept);
        event.register(OpenCardBackSelectionPayload.TYPE, AppearanceSelectionScreen::open);
        event.register(OpenCharacterSettingsPayload.TYPE, CharacterSettingsScreen::open);
        event.register(OpenHandCardDeckPayload.TYPE, HandCardDeckScreen::open);
        event.register(OpenCustomPaintingConfigPayload.TYPE, CustomPaintingConfigScreen::open);
        event.register(OpenExhibitionCharacterConfigPayload.TYPE, ExhibitionCharacterConfigScreen::open);
    }

    @SubscribeEvent
    public static void onMouseClicked(ScreenEvent.MouseButtonPressed.Pre event) {
        Screen screen = event.getScreen();
        QuickPhraseSidebar sidebar = sidebar(screen);
        if (sidebar != null && sidebar.mouseClicked(
                event.getMouseButtonEvent(),
                event.isDoubleClick(),
                screen.width, screen.height)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        QuickPhraseSidebar sidebar = sidebar(event.getScreen());
        if (sidebar != null && sidebar.mouseReleased(event.getMouseButtonEvent())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        QuickPhraseSidebar sidebar = sidebar(event.getScreen());
        if (sidebar != null && sidebar.mouseDragged(
                event.getMouseButtonEvent(),
                event.getScreen().height)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        Screen screen = event.getScreen();
        QuickPhraseSidebar sidebar = sidebar(screen);
        if (sidebar != null && sidebar.mouseScrolled(
                event.getMouseX(), event.getMouseY(),
                event.getScrollDeltaY(),
                screen.width, screen.height)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        QuickPhraseSidebar sidebar = sidebar(event.getScreen());
        if (sidebar != null && sidebar.keyPressed(event.getKeyEvent())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onCharacterTyped(ScreenEvent.CharacterTyped.Pre event) {
        QuickPhraseSidebar sidebar = sidebar(event.getScreen());
        if (sidebar != null && sidebar.charTyped(event.getCodePoint())) {
            event.setCanceled(true);
        }
    }

    private static QuickPhraseSidebar sidebar(Screen screen) {
        if (screen instanceof ChatScreen && screen instanceof QuickPhraseSidebarHost host) {
            return host.astralCraft$getQuickPhraseSidebar();
        }

        return null;
    }

}
