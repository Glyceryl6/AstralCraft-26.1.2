package com.astral_craft.client.gui;

import com.astral_craft.AstralCraft;
import com.astral_craft.client.model.character.AstralGeoAnimationManager;
import com.astral_craft.client.model.character.AstralGeoPose;
import com.astral_craft.client.render.character.AstralCharacterRenderState;
import com.astral_craft.client.util.ClientAnimationClock;
import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.gameplay.character.CharacterDefinition;
import com.astral_craft.common.gameplay.character.CharacterManager;
import com.astral_craft.common.network.CharacterSkillCutinPayload;
import com.astral_craft.common.registry.AstralEntities;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

public class CharacterSkillCutinOverlay {

    public static final Identifier LAYER = AstralCraft.prefix("character_skill_cutin");

    protected static Cutin active;
    protected static AstralCharacterEntity previewEntity;
    protected static Identifier previewEntityKey;
    protected static String previewSkinId;

    public static void show(CharacterSkillCutinPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> show(payload));
    }

    public static void show(CharacterSkillCutinPayload payload) {
        active = new Cutin(
                payload.characterId(),
                payload.skinId().isBlank() ? "default" : payload.skinId(),
                payload.skillId().isBlank() ? "active" : payload.skillId(),
                payload.animation().getPath(),
                ClientAnimationClock.nowTicks(),
                Math.max(20, payload.durationTicks()));
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (active == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            active = null;
            return;
        }

        float age = ClientAnimationClock.elapsedTicks(active.startedAtTick());
        if (age >= active.durationTicks()) {
            active = null;
            return;
        }

        CharacterDefinition definition = CharacterManager.INSTANCE.get(active.characterId());
        LivingEntity entity = configuredEntity(minecraft, definition, active.skinId(), safeAnimationAction(definition, active.animationAction()), Math.max(0, Math.round(age)));
        if (entity == null) return;

        float intro = smooth(Mth.clamp(age / 12.0F, 0.0F, 1.0F));
        float outro = smooth(Mth.clamp((active.durationTicks() - age) / 12.0F, 0.0F, 1.0F));
        float shown = Math.min(intro, outro);
        float hop = Mth.sin(intro * (float) Math.PI) * 26.0F;
        int boxW = Math.clamp(graphics.guiWidth() / 4, 130, 230);
        int boxH = Math.clamp(graphics.guiHeight() / 3, 150, 270);
        int x1 = graphics.guiWidth() - 6 + Math.round((1.0F - shown) * (boxW + 46.0F));
        int y1 = graphics.guiHeight() - 10 + Math.round((1.0F - shown) * 78.0F) - Math.round(hop);
        int x0 = x1 - boxW;
        int y0 = y1 - boxH;
        int shadowAlpha = (int) (shown * 108.0F) << 24;
        graphics.fill(x0 + boxW / 4, y1 - 12, x1 - boxW / 8, y1 - 5, shadowAlpha);
        renderEntityModel(graphics, entity, x0, y0, x1, y1, -218.0F, -9.0F, -4.0F, 1.0F + 0.08F * Mth.sin(age * 0.18F));
    }

    protected static float smooth(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    protected static AstralCharacterEntity configuredEntity(Minecraft minecraft, CharacterDefinition definition, String skinId, String animationAction, int tickCount) {
        if (minecraft.level == null || definition == null) return null;
        if (previewEntity == null || !definition.id().equals(previewEntityKey) || !skinId.equals(previewSkinId)) {
            previewEntity = new AstralCharacterEntity(AstralEntities.ASTRAL_CHARACTER.get(), minecraft.level);
            previewEntityKey = definition.id();
            previewSkinId = skinId;
        }

        previewEntity.setCharacterId(definition.id());
        previewEntity.setSkinId(skinId);
        previewEntity.setAnimationAction(animationAction);
        previewEntity.setCharacterLevel(1);
        previewEntity.setFriendship(1);
        previewEntity.tickCount = tickCount;
        return previewEntity;
    }

    protected static String safeAnimationAction(CharacterDefinition definition, String preferred) {
        if (definition == null) return "idle";
        List<String> actions = AstralGeoAnimationManager.INSTANCE.animationNames(definition.animationSetKey());
        if (actions.isEmpty()) return preferred == null || preferred.isBlank() ? "idle" : preferred;
        if (preferred != null && actions.contains(preferred)) return preferred;
        String preview = definition.previewAction();
        if (preview != null && actions.contains(preview)) return preview;
        return actions.getFirst();
    }

    public static void renderEntityModel(GuiGraphicsExtractor graphics, LivingEntity entity, int x0, int y0, int x1, int y1, float yaw, float pitch, float roll, float scaleMultiplier) {
        EntityRenderState renderState = extractEntityRenderState(entity);
        if (renderState instanceof LivingEntityRenderState livingState) {
            livingState.bodyRot = 0.0F;
            livingState.yRot = 0.0F;
            livingState.xRot = 0.0F;
            livingState.scale = 1.0F;
        }

        float boxWidth = Math.max(0.35F, renderState.boundingBoxWidth);
        float boxHeight = Math.max(1.2F, renderState.boundingBoxHeight);
        float viewWidth = Math.max(1.0F, x1 - x0);
        float viewHeight = Math.max(1.0F, y1 - y0);
        float scale = Math.min(viewWidth / (boxWidth * 1.35F), viewHeight / (boxHeight * 1.03F)) * scaleMultiplier;
        scale = Mth.clamp(scale, 14.0F, 126.0F);
        AstralGeoPose pose = renderState instanceof AstralCharacterRenderState astralState ? astralState.rootPose : AstralGeoPose.IDENTITY;
        Quaternionf rotation = new Quaternionf()
                .rotateZ((float) Math.toRadians(180.0F))
                .rotateX((float) Math.toRadians(pitch + pose.rotation().x()))
                .rotateY((float) Math.toRadians(yaw + pose.rotation().y()))
                .rotateZ((float) Math.toRadians(roll + pose.rotation().z()));
        Vector3f translation = new Vector3f(pose.position().x() / 16.0F, boxHeight * 0.48F - pose.position().y() / 16.0F, pose.position().z() / 16.0F);
        graphics.entity(renderState, scale, translation, rotation, null, x0, y0, x1, y1);
    }

    protected static EntityRenderState extractEntityRenderState(LivingEntity entity) {
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        EntityRenderer<? super LivingEntity, ?> renderer = dispatcher.getRenderer(entity);
        EntityRenderState renderState = renderer.createRenderState(entity, 1.0F);
        renderState.shadowPieces.clear();
        renderState.outlineColor = 0;
        return renderState;
    }

    protected record Cutin(Identifier characterId, String skinId, String skillId, String animationAction, long startedAtTick, int durationTicks) {}

}
