package com.astral_craft.common.gameplay.cardback;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.dice.DiceSkinPreferenceManager;
import com.astral_craft.common.network.s2c.OpenCardBackSelectionPayload;
import com.astral_craft.common.registry.AstralAttachments;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public class CardBackPreferenceManager {

    public static final Identifier DEFAULT_TEXTURE = AstralCraft.prefix("textures/gui/cards/back/card_back_0.jpg");
    private static final String RESOURCE_PREFIX = "textures/gui/cards/back/";

    public static Identifier selectedId(ServerPlayer player) {
        Identifier selected = player.getData(AstralAttachments.CARD_BACK);
        if (isLegacyDefault(selected)) return DEFAULT_TEXTURE;
        return isSelectable(selected) ? selected : DEFAULT_TEXTURE;
    }

    public static Identifier selectedTexture(ServerPlayer player) {
        return selectedId(player);
    }

    public static void select(ServerPlayer player, Identifier cardBackId, Identifier diceSkinId) {
        if (isSelectable(cardBackId) || DEFAULT_TEXTURE.equals(cardBackId)) player.setData(AstralAttachments.CARD_BACK, cardBackId);
        DiceSkinPreferenceManager.select(player, diceSkinId);
    }

    public static void openSelection(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new OpenCardBackSelectionPayload(
                selectedId(player), DiceSkinPreferenceManager.selectedTexture(player)));
    }

    private static boolean isLegacyDefault(Identifier id) {
        if (id == null) return true;
        if (id.equals(AstralCraft.prefix("default"))) return true;
        if (!AstralCraft.MOD_ID.equals(id.getNamespace())) return false;
        String path = id.getPath();
        int dot = path.lastIndexOf('.');
        String stem = dot >= 0 ? path.substring(0, dot) : path;
        return stem.equals(RESOURCE_PREFIX + "card_back_0");
    }

    private static boolean isSelectable(Identifier id) {
        return id != null && id.getPath().startsWith(RESOURCE_PREFIX);
    }

}