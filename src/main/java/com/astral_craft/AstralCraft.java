package com.astral_craft;

import com.astral_craft.common.gameplay.AstralBuiltinCards;
import com.astral_craft.common.gameplay.AstralBuiltinChips;
import com.astral_craft.common.gameplay.AstralPartyChips;
import com.astral_craft.common.gameplay.PanelTypes;
import com.astral_craft.common.registry.AstralEventEffectTypes;
import com.astral_craft.common.registry.*;
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
        AstralTabs.TABS.register(modEventBus);
        AstralItems.ITEMS.register(modEventBus);
        AstralBlocks.BLOCKS.register(modEventBus);
        AstralEntities.ENTITIES.register(modEventBus);
        AstralAttachments.ATTACHMENTS.register(modEventBus);
        AstralEventEffectTypes.EFFECT_TYPES.register(modEventBus);
        AstralEventConditionTypes.CONDITION_TYPES.register(modEventBus);
        AstralDataComponents.DATA_COMPONENT_TYPE.register(modEventBus);
    }

    public static Identifier prefix(String name) {
        return Identifier.fromNamespaceAndPath(MOD_ID, name.toLowerCase(Locale.ROOT));
    }

}