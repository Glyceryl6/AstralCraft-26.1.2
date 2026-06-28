package com.astral_craft.common.gameplay;

import com.astral_craft.AstralCraft;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;

public class PanelTypes {

    public static final ResourceKey<Registry<PanelType>> REGISTRY_KEY = ResourceKey.createRegistryKey(AstralCraft.prefix("panel_types"));
    public static final DeferredRegister<PanelType> PANEL_TYPES = DeferredRegister.create(REGISTRY_KEY, AstralCraft.MOD_ID);
    public static final Registry<PanelType> REGISTRY = PANEL_TYPES.makeRegistry(_ -> {});

    public static final DeferredHolder<PanelType, PanelType> START = registerBuiltin("start", PanelTrigger.LANDING);
    public static final DeferredHolder<PanelType, PanelType> CHECK_POINT = registerBuiltin("check_point", PanelTrigger.LANDING);
    public static final DeferredHolder<PanelType, PanelType> PORTAL = registerBuiltin("portal", PanelTrigger.LANDING);
    public static final DeferredHolder<PanelType, PanelType> ASSAULT_GATE = registerBuiltin("assault_gate", PanelTrigger.LANDING);
    public static final DeferredHolder<PanelType, PanelType> DIVINATION = registerBuiltin("divination", PanelTrigger.LANDING);
    public static final DeferredHolder<PanelType, PanelType> SHOP = registerBuiltin("shop", PanelTrigger.BOTH);
    public static final DeferredHolder<PanelType, PanelType> EVENT = registerBuiltin("event", PanelTrigger.LANDING);
    public static final DeferredHolder<PanelType, PanelType> GUESSING = registerBuiltin("guessing", PanelTrigger.LANDING);
    public static final DeferredHolder<PanelType, PanelType> LOTTERY = registerBuiltin("lottery", PanelTrigger.LANDING);
    public static final DeferredHolder<PanelType, PanelType> CANNON = registerBuiltin("cannon", PanelTrigger.LANDING);
    public static final DeferredHolder<PanelType, PanelType> HASTE = registerBuiltin("haste", PanelTrigger.LANDING);
    public static final DeferredHolder<PanelType, PanelType> HOSPITAL = registerBuiltin("hospital", PanelTrigger.LANDING);
    public static final DeferredHolder<PanelType, PanelType> CARD_REWARD = registerBuiltin("card_reward", PanelTrigger.LANDING);
    public static final DeferredHolder<PanelType, PanelType> WINDFALL = registerBuiltin("windfall", PanelTrigger.LANDING);
    public static final DeferredHolder<PanelType, PanelType> FORTUNE = registerBuiltin("fortune", PanelTrigger.LANDING);
    public static final DeferredHolder<PanelType, PanelType> CALAMITY = registerBuiltin("calamity", PanelTrigger.LANDING);
    public static final DeferredHolder<PanelType, PanelType> JUMP = registerBuiltin("jump", PanelTrigger.LANDING);
    public static final DeferredHolder<PanelType, PanelType> MONSTER = registerBuiltin("monster", PanelTrigger.LANDING);
    public static final DeferredHolder<PanelType, PanelType> RECOVER = registerBuiltin("recover", PanelTrigger.LANDING);
    public static final DeferredHolder<PanelType, PanelType> MONSTER_ASSAULT_GATE = registerBuiltin("monster_assault_gate", PanelTrigger.LANDING);
    public static final DeferredHolder<PanelType, PanelType> GIFT = registerBuiltin("gift", PanelTrigger.BOTH);
    public static final DeferredHolder<PanelType, PanelType> CHIP_SHOP = registerBuiltin("chip_shop", PanelTrigger.BOTH);
    public static final DeferredHolder<PanelType, PanelType> GIMMICK = registerBuiltin("gimmick", PanelTrigger.LANDING);
    public static final DeferredHolder<PanelType, PanelType> EMPTY = registerBuiltin("empty", PanelTrigger.LANDING);

    public static DeferredHolder<PanelType, PanelType> register(String path, PanelTrigger trigger, String nameKey, String descriptionKey) {
        Identifier id = AstralCraft.prefix(path);
        return PANEL_TYPES.register(path, () -> new PanelType(id, trigger, nameKey, descriptionKey));
    }

    public static DeferredRegister<PanelType> createRegister(String modId) {
        return DeferredRegister.create(REGISTRY_KEY, modId);
    }

    public static Optional<PanelType> get(Identifier id) {
        return Optional.ofNullable(REGISTRY.getValue(id));
    }

    public static PanelType getOrEmpty(Identifier id) {
        return get(id).orElseGet(EMPTY);
    }

    private static DeferredHolder<PanelType, PanelType> registerBuiltin(String path, PanelTrigger trigger) {
        return register(path, trigger, "panel." + AstralCraft.MOD_ID + "." + path, "panel." + AstralCraft.MOD_ID + "." + path + ".desc");
    }

}