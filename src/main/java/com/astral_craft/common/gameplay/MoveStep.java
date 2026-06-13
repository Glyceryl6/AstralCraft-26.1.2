package com.astral_craft.common.gameplay;

public record MoveStep(String nodeId, PanelType panelType, int remainingSteps, boolean arrival) {
}
