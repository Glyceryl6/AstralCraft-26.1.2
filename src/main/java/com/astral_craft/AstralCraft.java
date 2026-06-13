package com.astral_craft;

import com.astral_craft.common.registry.AstralAttachments;
import com.astral_craft.common.registry.AstralBlocks;
import com.astral_craft.common.registry.AstralDataComponents;
import com.astral_craft.common.registry.AstralEntities;
import com.astral_craft.common.registry.AstralTabs;
import com.astral_craft.common.gameplay.AstralBuiltinCards;
import com.astral_craft.common.gameplay.AstralBuiltinChips;
import com.astral_craft.common.gameplay.AstralPartyCards;
import com.astral_craft.common.gameplay.AstralPartyChips;
import com.astral_craft.common.gameplay.PanelTypes;
import com.astral_craft.common.registry.AstralItems;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

import java.util.Locale;

@Mod(AstralCraft.MOD_ID)
public class AstralCraft {

    public static final String MOD_ID = "astral_craft";

    public AstralCraft(IEventBus modEventBus) {
        PanelTypes.bootstrap();
        AstralBuiltinChips.bootstrap();
        AstralBuiltinCards.bootstrap();
        PanelTypes.PANEL_TYPES.register(modEventBus);
        AstralPartyChips.CHIPS.register(modEventBus);
        AstralPartyCards.CARDS.register(modEventBus);
        AstralTabs.TABS.register(modEventBus);
        AstralItems.ITEMS.register(modEventBus);
        AstralBlocks.BLOCKS.register(modEventBus);
        AstralDataComponents.DATA_COMPONENT_TYPE.register(modEventBus);
        AstralEntities.ENTITIES.register(modEventBus);
        AstralAttachments.ATTACHMENTS.register(modEventBus);
    }

    public static Identifier prefix(String name) {
        return Identifier.fromNamespaceAndPath(MOD_ID, name.toLowerCase(Locale.ROOT));
    }

}