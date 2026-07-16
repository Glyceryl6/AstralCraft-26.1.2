package com.astral_craft.common.blocks.platform;

import com.astral_craft.common.blocks.BasePlatform;
import com.astral_craft.common.gameplay.board.BoardPanelContext;
import com.astral_craft.common.gameplay.board.BoardParticipant;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.registry.AstralItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

public class CardPlatform extends BasePlatform {

    private static final int DRAW_COUNT = 2;

    public CardPlatform(Block.Properties properties) {
        super(properties, Trigger.LANDING);
    }

    @Override
    public void applyBoardEffect(BoardPanelContext context) {
        List<Identifier> candidates = new ArrayList<>();
        for (AstralItems.ModelledCardItem entry : AstralItems.MODELLED_CARD_ITEMS) {
            Item item = entry.item().get();
            if (!(item instanceof BaseHandCard card) || !BoardSessionManager.validPvpCard(card)) continue;
            Package itemPackage = item.getClass().getPackage();
            if (itemPackage != null && itemPackage.getName().contains(".cards.pve")) continue;
            candidates.add(BuiltInRegistries.ITEM.getKey(item));
        }
        if (candidates.isEmpty()) return;

        BoardParticipant updated = context.participant();
        for (int index = 0; index < DRAW_COUNT; index++) {
            updated = updated.addCard(candidates.get(context.level().getRandom().nextInt(candidates.size())));
        }
        BoardSessionManager.updateParticipant(context.level(), context.session(), updated);
    }
}
