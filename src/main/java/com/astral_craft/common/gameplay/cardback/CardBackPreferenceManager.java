package com.astral_craft.common.gameplay.cardback;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.OpenCardBackSelectionPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CardBackPreferenceManager {

    protected static final Map<UUID, Identifier> SELECTED = new HashMap<>();

    public static Identifier selectedId(ServerPlayer player) {
        return SELECTED.getOrDefault(player.getUUID(), CardBackManager.INSTANCE.defaultBack().id());
    }

    public static Identifier selectedTexture(ServerPlayer player) {
        return CardBackManager.INSTANCE.get(selectedId(player)).texture();
    }

    public static void select(ServerPlayer player, Identifier id) {
        if (CardBackManager.INSTANCE.contains(id)) {
            SELECTED.put(player.getUUID(), id);
        }
    }

    public static void openSelection(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new OpenCardBackSelectionPayload(CardBackManager.INSTANCE.encodeList(), selectedId(player).toString()));
    }

    public static Identifier safeParse(String raw) {
        try {
            return Identifier.parse(raw);
        } catch (Exception ignored) {
            return AstralCraft.prefix("default");
        }
    }

}