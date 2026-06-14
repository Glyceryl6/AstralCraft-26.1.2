package com.astral_craft.common.event;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = AstralCraft.MOD_ID)
public class ModBusEventSubscriber {

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(CardRevealPayload.TYPE, CardRevealPayload.STREAM_CODEC);
        registrar.playToClient(OpenTargetSelectionPayload.TYPE, OpenTargetSelectionPayload.STREAM_CODEC);
        registrar.playToClient(OpenBattleScenePayload.TYPE, OpenBattleScenePayload.STREAM_CODEC);
        registrar.playToClient(OpenChipSelectionPayload.TYPE, OpenChipSelectionPayload.STREAM_CODEC);
        registrar.playToClient(BoardHudSnapshotPayload.TYPE, BoardHudSnapshotPayload.STREAM_CODEC);
        registrar.playToServer(CardTargetSelectionPayload.TYPE, CardTargetSelectionPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleCardTargets);
        registrar.playToServer(ChipSelectionPayload.TYPE, ChipSelectionPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleChipSelection);
    }

}