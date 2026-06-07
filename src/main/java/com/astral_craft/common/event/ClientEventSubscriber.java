package com.astral_craft.common.event;

import com.astral_craft.AstralCraft;
import com.astral_craft.client.model.LargeCuboidModelLoader;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

@EventBusSubscriber(modid = AstralCraft.MOD_ID, value = Dist.CLIENT)
public class ClientEventSubscriber {

    @SubscribeEvent
    public static void registerModelLoaders(ModelEvent.RegisterLoaders event) {
        event.register(LargeCuboidModelLoader.ID, LargeCuboidModelLoader.INSTANCE);
    }

}