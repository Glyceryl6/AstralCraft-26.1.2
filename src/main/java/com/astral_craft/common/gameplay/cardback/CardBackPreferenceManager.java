package com.astral_craft.common.gameplay.cardback;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.s2c.OpenCardBackSelectionPayload;
import com.astral_craft.common.registry.AstralAttachments;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public class CardBackPreferenceManager {

    public static final Identifier DEFAULT_TEXTURE = AstralCraft.prefix("textures/gui/cards/card_back.png");
    private static final String RESOURCE_PREFIX = "textures/gui/cards/back/";

    public static Identifier selectedId(ServerPlayer player) {
        Identifier selected = player.getData(AstralAttachments.CARD_BACK);
        if (selected.equals(AstralCraft.prefix("default"))) return DEFAULT_TEXTURE;
        return isSelectable(selected) ? selected : DEFAULT_TEXTURE;
    }

    public static Identifier selectedTexture(ServerPlayer player) {
        return selectedId(player);
    }

    public static void select(ServerPlayer player, Identifier id) {
        if (isSelectable(id) || DEFAULT_TEXTURE.equals(id)) player.setData(AstralAttachments.CARD_BACK, id);
    }

    public static void openSelection(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new OpenCardBackSelectionPayload(selectedId(player)));
    }

    private static boolean isSelectable(Identifier id) {
        if (id == null || !id.getPath().startsWith(RESOURCE_PREFIX)) return false;
        return id.getPath().endsWith(".png");
    }

}