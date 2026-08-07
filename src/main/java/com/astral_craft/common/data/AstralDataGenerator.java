package com.astral_craft.common.data;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.data.provider.AstralBlockTagsProvider;
import com.astral_craft.common.data.provider.AstralDatapackBuiltinEntriesProvider;
import com.astral_craft.common.data.provider.AstralLanguageProvider;
import com.astral_craft.common.data.provider.AstralModelProvider;
import com.astral_craft.common.data.provider.AstralRecipeProvider;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = AstralCraft.MOD_ID)
public class AstralDataGenerator {

    @SubscribeEvent
    public static void onGatherClientData(GatherDataEvent.Client event) {
        event.createProvider(AstralModelProvider::new);
        event.createProvider(AstralBlockTagsProvider::new);
        event.createProvider(AstralDatapackBuiltinEntriesProvider::new);
        event.createProvider(AstralRecipeProvider.Runner::new);
        event.createProvider((output, _) -> new AstralLanguageProvider(output, "en_us"));
        event.createProvider((output, _) -> new AstralLanguageProvider(output, "zh_cn"));
    }

}
