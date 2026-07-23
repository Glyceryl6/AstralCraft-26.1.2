package com.astral_craft.common.gameplay.cardback;

import com.astral_craft.common.network.s2c.OpenCardBackSelectionPayload;
import com.astral_craft.common.registry.AstralAttachments;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public class CardBackPreferenceManager {

    private static final String RESOURCE_PREFIX = "textures/gui/cards/back/";

    public static Identifier selectedId(ServerPlayer player) {
        Identifier selected = player.getData(AstralAttachments.CARD_BACK);
        return isSelectable(selected) ? selected : CardBackManager.INSTANCE.defaultBack().id();
    }

    public static Identifier selectedTexture(ServerPlayer player) {
        Identifier selected = selectedId(player);
        return CardBackManager.INSTANCE.contains(selected)
                ? CardBackManager.INSTANCE.get(selected).texture() : selected;
    }

    public static void select(ServerPlayer player, Identifier id) {
        if (isSelectable(id)) player.setData(AstralAttachments.CARD_BACK, id);
    }

    public static void openSelection(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new OpenCardBackSelectionPayload(CardBackManager.INSTANCE.values(), selectedId(player)));
    }

    private static boolean isSelectable(Identifier id) {
        return id != null && (CardBackManager.INSTANCE.contains(id) || id.getPath().startsWith(RESOURCE_PREFIX) && id.getPath().endsWith(".jpg"));
    }

}