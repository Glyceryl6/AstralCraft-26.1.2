package com.astral_craft.common.gameplay.event;

import com.mojang.serialization.MapCodec;

public interface AstralEventGeneralCondition extends AstralEventTriggerCondition, AstralActiveEventCondition {

    MapCodec<? extends AstralEventGeneralCondition> codec();

    @Override
    default MapCodec<? extends AstralEventTriggerCondition> triggerCodec() {
        return this.codec();
    }

    @Override
    default MapCodec<? extends AstralActiveEventCondition> activeCodec() {
        return this.codec();
    }

}