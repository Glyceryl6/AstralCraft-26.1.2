package com.astral_craft.client.render.character;

import com.astral_craft.AstralCraft;
import com.astral_craft.client.gameplay.character.ClientCharacterDefinitionCache;
import com.astral_craft.client.model.character.AstralGeoAnimationManager;
import com.astral_craft.client.model.character.AstralGeoModelManager;
import com.astral_craft.client.model.character.AstralGeoPose;
import com.astral_craft.common.gameplay.character.ActiveCharacterState;
import com.astral_craft.common.gameplay.character.CharacterDefinition;
import com.astral_craft.common.gameplay.character.skin.CharacterSkinDefinition;
import com.astral_craft.common.registry.AstralAttachments;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.jspecify.annotations.NullMarked;

/**
 * Compatibility-first adapter between the character resource framework and the vanilla player renderer.
 * The vanilla renderer, equipment layers and ordinary player animation state stay intact; character animation
 * is applied as an additional root transform only while an active character render state is present.
 */
public class AstralPlayerCharacterRenderBridge {

    public static final Identifier PLAYER_RENDERER = AstralCraft.prefix("player");
    public static final ContextKey<PlayerCharacterRenderData> CHARACTER_RENDER_DATA =
            new ContextKey<>(AstralCraft.prefix("player_character_render"));

    private static final ThreadLocal<PoseStack> ACTIVE_POSE_STACK = new ThreadLocal<>();

    @NullMarked
    public static<T extends Avatar & ClientAvatarEntity> void beforeRender(RenderPlayerEvent.Pre<T> event) {
        clearStalePose();
        PlayerCharacterRenderData data = event.getRenderState().getRenderData(CHARACTER_RENDER_DATA);
        if (data == null || !PLAYER_RENDERER.equals(data.rendererKey())) return;
        if (AstralGeoModelManager.INSTANCE.get(data.modelKey()) == null) return;
        AstralGeoPose pose = AstralGeoAnimationManager.INSTANCE.sample(
                data.animationSetKey(), data.animationAction(), "root",
                data.animationTimeSeconds() + event.getPartialTick() / 20.0F);
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        ACTIVE_POSE_STACK.set(poseStack);
        poseStack.translate(pose.position().x() / 16.0F, -pose.position().y() / 16.0F, pose.position().z() / 16.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(pose.rotation().x()));
        poseStack.mulPose(Axis.YP.rotationDegrees(pose.rotation().y()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pose.rotation().z()));
        poseStack.scale(safeScale(pose.scale().x()), safeScale(pose.scale().y()), safeScale(pose.scale().z()));
    }

    public static void renderFirstPersonArm(RenderArmEvent event) {
        AbstractClientPlayer player = event.getPlayer();
        ActiveCharacterState state = player.getData(AstralAttachments.ACTIVE_CHARACTER);
        if (!state.active() || !ClientCharacterDefinitionCache.INSTANCE.contains(state.characterId())) return;
        CharacterDefinition definition = ClientCharacterDefinitionCache.INSTANCE.getOrFallback(state.characterId());
        CharacterSkinDefinition skin = definition.skinOrDefault(state.skinId());
        PlayerModel model = Minecraft.getInstance().getEntityRenderDispatcher().getPlayerRenderer(player).getModel();
        ModelPart arm = event.getArm() == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        arm.resetPose();
        arm.visible = true;
        model.leftArm.zRot = -0.1F;
        model.rightArm.zRot = 0.1F;
        model.leftSleeve.visible = player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE);
        model.rightSleeve.visible = player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE);
        Identifier identifier = skin.texture().withPrefix("textures/").withSuffix(".png");
        event.getSubmitNodeCollector().submitModelPart(arm, event.getPoseStack(),
                RenderTypes.entityTranslucent(identifier), event.getPackedLight(),
                OverlayTexture.NO_OVERLAY, null);
        event.setCanceled(true);
    }

    @NullMarked
    public static<T extends Avatar & ClientAvatarEntity> void afterRender(RenderPlayerEvent.Post<T> event) {
        PoseStack activePoseStack = ACTIVE_POSE_STACK.get();
        if (activePoseStack == null) return;
        activePoseStack.popPose();
        ACTIVE_POSE_STACK.remove();
    }

    private static void clearStalePose() {
        PoseStack stalePoseStack = ACTIVE_POSE_STACK.get();
        if (stalePoseStack == null) return;
        stalePoseStack.popPose();
        ACTIVE_POSE_STACK.remove();
    }

    private static float safeScale(float value) {
        return Math.max(0.01F, value);
    }

    public record PlayerCharacterRenderData(
            Identifier modelKey, Identifier rendererKey, Identifier animationSetKey,
            String animationAction, float animationTimeSeconds) {
    }

}