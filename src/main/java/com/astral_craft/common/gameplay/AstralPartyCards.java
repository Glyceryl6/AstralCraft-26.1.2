package com.astral_craft.common.gameplay;

import com.astral_craft.AstralCraft;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collection;
import java.util.Optional;

/**
 * NeoForge-style registry for Astral Party card metadata.
 *
 * <p>Concrete {@code BaseHandCard} subclasses own their {@link CardDefinition} and call
 * {@link #register(CardDefinition)} from their static definition field. This class is only the
 * DeferredRegister/custom-registry boundary; it no longer contains a hard-coded card list.</p>
 */
public class AstralPartyCards {

    public static final ResourceKey<Registry<CardDefinition>> REGISTRY_KEY = ResourceKey.createRegistryKey(AstralCraft.prefix("astral_party_cards"));
    public static final DeferredRegister<CardDefinition> CARDS = DeferredRegister.create(REGISTRY_KEY, AstralCraft.MOD_ID);
    public static final Registry<CardDefinition> REGISTRY = CARDS.makeRegistry(_ -> {});

    public static CardDefinition register(CardDefinition definition) {
        CARDS.register(definition.registryPath(), () -> definition);
        return definition;
    }

    public static DeferredRegister<CardDefinition> createRegister(String modId) {
        return DeferredRegister.create(REGISTRY_KEY, modId);
    }

    public static Optional<CardDefinition> get(String id) {
        CardDefinition definition = REGISTRY.getValue(AstralCraft.prefix(id));
        return Optional.ofNullable(definition);
    }

    public static Collection<DeferredHolder<CardDefinition, ? extends CardDefinition>> allHolders() {
        return CARDS.getEntries();
    }

}