package com.astral_craft.client.gui;

import com.astral_craft.AstralCraft;
import com.astral_craft.client.gameplay.character.ClientCharacterDefinitionCache;
import com.astral_craft.common.gameplay.character.ActiveCharacterState;
import com.astral_craft.common.gameplay.character.CharacterDefinition;
import com.astral_craft.common.gameplay.character.skin.CharacterSkinDefinition;
import com.astral_craft.common.registry.AstralAttachments;
import com.astral_craft.common.registry.AstralStatusEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class AstralStatusIconRenderer {

    public static final int MOB_EFFECT_ICON_SIZE = 18;
    public static final int SKILL_STATUS_ICON_SIZE = 22;

    public static void renderMobEffectIcon(GuiGraphicsExtractor graphics, Identifier statusId, int x, int y, int size, int alpha) {
        Identifier configuredIcon = AstralStatusEffects.defaultIcon(statusId).orElse(null);
        if (configuredIcon != null) {
            renderTextureIcon(graphics, normalizeTexture(configuredIcon), x, y, size, alpha);
            return;
        }

        ActiveCharacterState activeCharacter = activeCharacter();
        if (activeCharacter.active() && renderCharacterSkinHead(graphics, activeCharacter.characterId(), activeCharacter.skinId(), x, y, size, alpha)) {
            return;
        }

        thisCannotHappenFallback(graphics, statusId, x, y, size, alpha);
    }

    public static void renderTextureIcon(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int size, int alpha) {
        int argb = (Mth.clamp(alpha, 0, 255) << 24) | 0xFFFFFF;
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F, size, size, size, size, size, size, argb);
    }

    public static boolean renderCharacterSkinHead(GuiGraphicsExtractor graphics, Identifier characterId, String skinId, int x, int y, int size, int alpha) {
        return renderCharacterSkinHead(graphics, characterId, skinId, x, y, size, alpha, false);
    }

    public static boolean renderCharacterSkinHead(GuiGraphicsExtractor graphics, Identifier characterId, String skinId,
                                                   int x, int y, int size, int alpha, boolean grayscale) {
        Identifier texture = characterSkinTexture(characterId, skinId);
        if (texture == null) return false;
        int safeAlpha = Mth.clamp(alpha, 0, 255);
        int tint = grayscale ? 0xB0B0B0 : 0xFFFFFF;
        int argb = safeAlpha << 24 | tint;
        int pad = Math.max(1, Math.round(size / 18.0F));
        int headSize = Math.max(1, size - pad * 2);
        graphics.fill(x, y, x + size, y + size, safeAlpha << 24 | 0x11131F);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x + pad, y + pad, 8.0F, 8.0F, headSize, headSize, 8, 8, 64, 64, argb);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x + pad, y + pad, 40.0F, 8.0F, headSize, headSize, 8, 8, 64, 64, argb);
        if (grayscale) graphics.fill(x, y, x + size, y + size, 0xAA707070);
        return true;
    }

    public static Identifier characterSkinTexture(Identifier characterId, String skinId) {
        Identifier safeCharacterId = characterId == null ? AstralCraft.prefix("mimi") : characterId;
        String safeSkinId = skinId == null || skinId.isBlank() ? "default" : skinId;
        if (ClientCharacterDefinitionCache.INSTANCE.contains(safeCharacterId)) {
            CharacterDefinition definition = ClientCharacterDefinitionCache.INSTANCE.getOrFallback(safeCharacterId);
            CharacterSkinDefinition skin = definition.skinOrDefault(safeSkinId);
            if (skin.texture() != null) {
                return normalizeTexture(skin.texture());
            }
        }

        return AstralCraft.prefix("textures/entity/character/skin_" + safeCharacterId.getPath() + "_" + safeSkinId + ".png");
    }

    public static Identifier normalizeTexture(Identifier raw) {
        if (raw == null) return AstralCraft.prefix("textures/mob_effect/shadow_cloak.png");
        String path = raw.getPath();
        if (path.startsWith("textures/") && path.endsWith(".png")) {
            return raw;
        }

        if (path.startsWith("textures/")) {
            return Identifier.fromNamespaceAndPath(raw.getNamespace(), path + ".png");
        }

        if (path.endsWith(".png")) {
            return Identifier.fromNamespaceAndPath(raw.getNamespace(), "textures/" + path);
        }

        return Identifier.fromNamespaceAndPath(raw.getNamespace(), "textures/" + path + ".png");
    }

    protected static void thisCannotHappenFallback(GuiGraphicsExtractor graphics, Identifier statusId, int x, int y, int size, int alpha) {
        int a = Mth.clamp(alpha, 0, 255);
        int background = (a << 24) | 0x31364D;
        int inner = (a << 24) | 0x171A28;
        graphics.fill(x, y, x + size, y + size, background);
        graphics.fill(x + 2, y + 2, x + size - 2, y + size - 2, inner);
        Font font = Minecraft.getInstance().font;
        String letter = statusId == null || statusId.getPath().isBlank() ? "?" : statusId.getPath().substring(0, 1).toUpperCase();
        Component text = Component.literal(letter);
        graphics.text(font, text, x + size / 2 - font.width(text) / 2, y + size / 2 - 4, 0xFFFFFFFF, true);
    }

    protected static ActiveCharacterState activeCharacter() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return ActiveCharacterState.NONE;
        return minecraft.player.getData(AstralAttachments.ACTIVE_CHARACTER);
    }

}
