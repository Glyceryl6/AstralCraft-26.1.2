package com.astral_craft.common.gameplay;

import net.minecraft.resources.Identifier;

public record MoveStep(String nodeId, Identifier platformId, int remainingSteps, boolean arrival) {
}
