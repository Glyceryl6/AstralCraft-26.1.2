package com.astral_craft.common.gameplay.event;

import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface AstralEventHandler {
    void apply(ServerPlayer player, AstralEventDefinition definition);
}
