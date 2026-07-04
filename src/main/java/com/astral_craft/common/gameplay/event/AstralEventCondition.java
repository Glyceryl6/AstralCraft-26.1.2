package com.astral_craft.common.gameplay.event;

public interface AstralEventCondition {

    String typeId();

    boolean test(AstralEventContext context);

}
