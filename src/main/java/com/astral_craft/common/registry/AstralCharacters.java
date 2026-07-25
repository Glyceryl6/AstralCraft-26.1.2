package com.astral_craft.common.registry;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.character.AstralCharacter;
import com.astral_craft.common.gameplay.character.impl.InkShadowCharacter;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class AstralCharacters {

    public static final ResourceKey<Registry<AstralCharacter>> REGISTRY_KEY = ResourceKey.createRegistryKey(AstralCraft.prefix("characters"));
    public static final DeferredRegister<AstralCharacter> CHARACTERS = DeferredRegister.create(REGISTRY_KEY, AstralCraft.MOD_ID);
    public static final Registry<AstralCharacter> REGISTRY = CHARACTERS.makeRegistry(builder -> builder.sync(true));
    public static final Map<String, DeferredHolder<AstralCharacter, AstralCharacter>> BUILTINS = new LinkedHashMap<>();

    public static final DeferredHolder<AstralCharacter, AstralCharacter> PARUNAN = register("parunan", properties(1, 2, 10, 10).cooldown(2).unlockedByDefault(true));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> FANNY = register("fanny", properties(1, 2, 10, 11).cooldown(3).unlockedByDefault(true));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> ALANA = register("alana", properties(1, 1, 9, 12).cooldown(3).unlockedByDefault(true));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> KOMACHI = register("komachi", properties(1, 1, 9, 13).cooldown(3).unlockedByDefault(true));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> PADMAN = register("padman", properties(2, 2, 9, 14).cooldown(3));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> PAPARA = register("papara", properties(2, 1, 10, 15).cooldown(3));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> REN = register("ren", properties(2, 1, 8, 16).cooldown(3, 3).unlockedByDefault(true));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> MIMI = register("mimi", properties(1, 1, 9, 17).cooldown(3).unlockedByDefault(true));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> Z3000 = register("z3000", properties(1, 2, 10, 18).cooldown(3, 4));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> PANDAMAN = register("pandaman", properties(1, 0, 14, 19).cooldown(4, 3));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> LULU = register("lulu", properties(2, 2, 9, 20).cooldown(4, 3));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> FEN = register("fen", properties(1, 0, 10, 21).cooldown(3, 3));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> HAI_QING = register("hai_qing", properties(1, 1, 10, 22).cooldown(3, 3));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> MISAKI = register("misaki", properties(0, 2, 9, 23).cooldown(3, 3));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> NARDIS = register("nardis", properties(1, 1, 9, 24).cooldown(3));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> JASMINE = register("jasmine", properties(1, 0, 9, 25).cooldown(4));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> AL = register("al", properties(1, 1, 9, 26).cooldown(3, 3));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> LUKA = register("luka", properties(1, 2, 9, 27).cooldown(3, 3));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> NANCY_LU = register("nancy_lu", properties(1, 1, 9, 28).cooldown(3, 3));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> RIN = register("rin", properties(1, 1, 9, 29).cooldown(3, 3));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> MEGAS = register("megas", properties(0, 2, 9, 30).cooldown(3, 3));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> ZHAO = register("zhao", properties(1, 1, 10, 31).cooldown(3, 3));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> TERU = register("teru", properties(2, 1, 9, 32).cooldown(3, 3));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> MOSES = register("moses", properties(1, 1, 11, 33).cooldown(2, 2));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> MAMUSHI = register("mamushi", properties(2, 1, 9, 34).cooldown(3, 3));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> INK_SHADOW = register("ink_shadow", InkShadowCharacter::new, properties(2, 1, 10, 35).cooldown(3, 3));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> BONNIE = register("bonnie", properties(2, 1, 9, 36).cooldown(3, 3));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> LING_LING = register("ling_ling", properties(2, 2, 10, 37).cooldown(3, 3));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> K_ANGEL = register("k_angel", properties(0, 1, 9, 38).cooldown(3, 3).implicitBondSkin(false));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> AME = register("ame", properties(1, 4, 9, 39).cooldown(3).implicitBondSkin(false));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> JILL = register("jill", properties(1, 1, 10, 40).cooldown(3, 3).implicitBondSkin(false));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> DOROTHY = register("dorothy", properties(1, 0, 8, 41).cooldown(3, 2).implicitBondSkin(false));

    private static DeferredHolder<AstralCharacter, AstralCharacter> register(String name, AstralCharacter.Properties properties) {
        return register(name, AstralCharacter::new, properties);
    }

    private static DeferredHolder<AstralCharacter, AstralCharacter> register(String name, Function<AstralCharacter.Properties, AstralCharacter> factory, AstralCharacter.Properties properties) {
        DeferredHolder<AstralCharacter, AstralCharacter> holder = CHARACTERS.register(name, () -> factory.apply(properties));
        BUILTINS.put(name, holder);
        return holder;
    }

    private static AstralCharacter.Properties properties(int attack, int defense, int health, int sortOrder) {
        return new AstralCharacter.Properties().baseStats(attack, defense, health, 6).sortOrder(sortOrder);
    }

}