package com.astral_craft.common.gameplay;

import net.minecraft.resources.Identifier;

import java.util.List;

public record BoardNode(String id, Identifier panelTypeId, List<String> next) {

    public BoardNode {
        next = List.copyOf(next);
    }

    public PanelType panelType() {
        return PanelTypes.getOrEmpty(this.panelTypeId);
    }

    public String defaultNext() {
        return next.isEmpty() ? id : next.getFirst();
    }

}