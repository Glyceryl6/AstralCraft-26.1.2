package com.astral_craft.common.gameplay.character.skill.status;

import com.astral_craft.AstralCraft;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;

import java.util.Optional;
import java.util.function.Supplier;

public class AstralStatusEffectType {

    protected final Identifier id;
    protected final Supplier<? extends Holder<MobEffect>> mobEffect;
    protected final Identifier defaultIcon;

    public AstralStatusEffectType(Identifier id, Supplier<? extends Holder<MobEffect>> mobEffect, Identifier defaultIcon) {
        this.id = id == null ? AstralCraft.prefix("generic_status") : id;
        this.mobEffect = mobEffect;
        this.defaultIcon = defaultIcon;
    }

    public Identifier id() {
        return this.id;
    }

    public Optional<Holder<MobEffect>> mobEffect() {
        if (this.mobEffect == null) return Optional.empty();
        Holder<MobEffect> holder = this.mobEffect.get();
        return holder == null ? Optional.empty() : Optional.of(holder);
    }

    public Optional<Identifier> defaultIcon() {
        return Optional.ofNullable(this.defaultIcon);
    }

}