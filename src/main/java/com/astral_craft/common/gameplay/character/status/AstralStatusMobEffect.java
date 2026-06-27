package com.astral_craft.common.gameplay.character.status;

import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class AstralStatusMobEffect extends MobEffect {

    protected final Identifier statusId;
    protected final Identifier iconTexture;

    public AstralStatusMobEffect(MobEffectCategory category, int color, Identifier statusId, Identifier iconTexture) {
        super(category, color);
        this.statusId = statusId;
        this.iconTexture = iconTexture;
    }

    public Identifier statusId() {
        return this.statusId;
    }

    public Identifier iconTexture() {
        return this.iconTexture;
    }

}