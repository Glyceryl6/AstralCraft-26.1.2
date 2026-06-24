package com.astral_craft.common.gameplay.character;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.registry.AstralAttachments;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class AstralPlayerCharacterApplier {

    protected static final Identifier MAX_HEALTH_ID = AstralCraft.prefix("active_character_max_health");
    protected static final Identifier ATTACK_DAMAGE_ID = AstralCraft.prefix("active_character_attack_damage");
    protected static final Identifier ARMOR_ID = AstralCraft.prefix("active_character_defense");

    public static void apply(ServerPlayer player) {
        ActiveCharacterState state = player.getData(AstralAttachments.ACTIVE_CHARACTER);
        if (!state.active()) {
            remove(player);
            return;
        }

        applyModifier(player.getAttribute(Attributes.MAX_HEALTH), MAX_HEALTH_ID, state.health() * 2.0D - 20.0D);
        applyModifier(player.getAttribute(Attributes.ATTACK_DAMAGE), ATTACK_DAMAGE_ID, Math.max(0.0D, state.attack() - 1.0D));
        applyModifier(player.getAttribute(Attributes.ARMOR), ARMOR_ID, Math.max(0.0D, state.defense()));
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null && player.getHealth() > maxHealth.getValue()) {
            player.setHealth((float) maxHealth.getValue());
        }
    }

    public static void remove(ServerPlayer player) {
        removeModifier(player.getAttribute(Attributes.MAX_HEALTH), MAX_HEALTH_ID);
        removeModifier(player.getAttribute(Attributes.ATTACK_DAMAGE), ATTACK_DAMAGE_ID);
        removeModifier(player.getAttribute(Attributes.ARMOR), ARMOR_ID);
    }

    protected static void applyModifier(AttributeInstance instance, Identifier id, double amount) {
        if (instance == null) return;
        instance.removeModifier(id);
        if (Math.abs(amount) > 0.0001D) {
            instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    protected static void removeModifier(AttributeInstance instance, Identifier id) {
        if (instance == null) return;
        instance.removeModifier(id);
    }

}