package com.astral_craft.common.registry;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.character.AstralCharacter;
import com.astral_craft.common.gameplay.character.CharacterProgressionDefinition;
import com.astral_craft.common.gameplay.character.impl.InkShadowCharacter;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class AstralCharacters {

    public static final ResourceKey<Registry<AstralCharacter>> REGISTRY_KEY = ResourceKey.createRegistryKey(AstralCraft.prefix("characters"));
    public static final DeferredRegister<AstralCharacter> CHARACTERS = DeferredRegister.create(REGISTRY_KEY, AstralCraft.MOD_ID);
    public static final Registry<AstralCharacter> REGISTRY = CHARACTERS.makeRegistry(builder -> builder.sync(true));
    public static final Map<String, DeferredHolder<AstralCharacter, AstralCharacter>> BUILTINS = new LinkedHashMap<>();

    public static final DeferredHolder<AstralCharacter, AstralCharacter> PARUNAN = register("parunan", properties(1, 2, 10).chipWeights(60, 15, 15, 60).cooldown(2), progression(10).unlockedByDefault());
    public static final DeferredHolder<AstralCharacter, AstralCharacter> FANNY = register("fanny", properties(1, 2, 10).chipWeights(100, 15, 15, 15).cooldown(3), progression(11).unlockedByDefault());
    public static final DeferredHolder<AstralCharacter, AstralCharacter> ALANA = register("alana", properties(1, 1, 9).chipWeights(15, 15, 100, 15).cooldown(3), progression(12).unlockedByDefault());
    public static final DeferredHolder<AstralCharacter, AstralCharacter> KOMACHI = register("komachi", properties(1, 1, 9).chipWeights(15, 15, 15, 100).cooldown(3), progression(13).unlockedByDefault());
    public static final DeferredHolder<AstralCharacter, AstralCharacter> PADMAN = register("padman", properties(2, 2, 9).chipWeights(15, 15, 100, 15).cooldown(3).botSelectable(false), progression(14));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> PAPARA = register("papara", properties(2, 1, 10).chipWeights(15, 15, 100, 15).cooldown(3), progression(15));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> REN = register("ren", properties(2, 1, 8).chipWeights(60, 15, 15, 60).cooldown(3, 3), progression(16).unlockedByDefault());
    public static final DeferredHolder<AstralCharacter, AstralCharacter> MIMI = register("mimi", properties(1, 1, 9).chipWeights(80, 15, 15, 30).cooldown(3), progression(17).unlockedByDefault());
    public static final DeferredHolder<AstralCharacter, AstralCharacter> Z3000 = register("z3000", properties(1, 2, 10).chipWeights(15, 15, 100, 30).cooldown(3, 4).botSelectable(false), progression(18));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> PANDAMAN = register("pandaman", properties(1, 0, 14).chipWeights(60, 60, 15, 15).cooldown(4, 3).botSelectable(false), progression(19));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> LULU = register("lulu", properties(2, 2, 9).chipWeights(15, 100, 15, 15).cooldown(4, 3), progression(20));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> FEN = register("fen", properties(1, 0, 10).chipWeights(15, 15, 100, 15).cooldown(3, 3), progression(21));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> HAI_QING = register("hai_qing", properties(1, 1, 10).chipWeights(100, 15, 15, 15).cooldown(3, 3), progression(22));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> MISAKI = register("misaki", properties(0, 2, 9).chipWeights(15, 15, 100, 15).cooldown(3, 3), progression(23));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> NARDIS = register("nardis", properties(1, 1, 9).chipWeights(15, 15, 60, 60).cooldown(3), progression(24));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> JASMINE = register("jasmine", properties(1, 0, 9).chipWeights(15, 15, 100, 15).cooldown(4), progression(25));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> AL = register("al", properties(1, 1, 9).chipWeights(80, 15, 15, 30).cooldown(3, 3).botSelectable(false), progression(26));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> LUKA = register("luka", properties(1, 2, 9).chipWeights(15, 15, 100, 15).cooldown(3, 3), progression(27));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> NANCY_LU = register("nancy_lu", properties(1, 1, 9).chipWeights(15, 15, 100, 15).cooldown(3, 3), progression(28));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> RIN = register("rin", properties(1, 1, 9).chipWeights(30, 15, 15, 80).cooldown(3, 3), progression(29));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> MEGAS = register("megas", properties(0, 2, 9).chipWeights(60, 15, 15, 60).cooldown(3, 3), progression(30));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> ZHAO = register("zhao", properties(1, 1, 10).chipWeights(15, 100, 15, 15).cooldown(3, 3), progression(31));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> TERU = register("teru", properties(2, 1, 9).chipWeights(30, 15, 80, 15).cooldown(3, 3), progression(32));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> MOSES = register("moses", properties(1, 1, 11).chipWeights(15, 15, 100, 15).cooldown(2, 2), progression(33));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> MAMUSHI = register("mamushi", properties(2, 1, 9).chipWeights(15, 15, 100, 15).cooldown(3, 3), progression(34));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> INK_SHADOW = register("ink_shadow", InkShadowCharacter::new, properties(2, 1, 10).cooldown(3, 3).botSelectable(false), progression(35));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> BONNIE = register("bonnie", properties(2, 1, 9).chipWeights(15, 15, 100, 15).cooldown(3, 3), progression(36));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> LING_LING = register("ling_ling", properties(2, 2, 10).cooldown(3, 3), progression(37));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> SYKES = register("sykes", properties(0, 2, 9).cooldown(3, 3), progression(38));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> K_ANGEL = register("k_angel", properties(0, 1, 9).chipWeights(100, 15, 15, 15).cooldown(3, 3), progression(39).implicitBondSkin(false));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> AME = register("ame", properties(1, 4, 9).chipWeights(15, 15, 100, 15).cooldown(3), progression(40).implicitBondSkin(false));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> JILL = register("jill", properties(1, 1, 10).chipWeights(80, 30, 15, 15).cooldown(3, 3), progression(41).implicitBondSkin(false));
    public static final DeferredHolder<AstralCharacter, AstralCharacter> DOROTHY = register("dorothy", properties(1, 0, 8).chipWeights(15, 60, 60, 15).cooldown(3, 2), progression(42).implicitBondSkin(false));

    private static DeferredHolder<AstralCharacter, AstralCharacter> register(String name, AstralCharacter.Properties properties, CharacterProgressionDefinition progression) {
        return register(name, AstralCharacter::new, properties, progression);
    }

    private static DeferredHolder<AstralCharacter, AstralCharacter> register(
            String name, BiFunction<AstralCharacter.Properties, CharacterProgressionDefinition, AstralCharacter> factory,
            AstralCharacter.Properties properties, CharacterProgressionDefinition progression) {
        DeferredHolder<AstralCharacter, AstralCharacter> holder = CHARACTERS.register(name, () -> factory.apply(properties, progression));
        BUILTINS.put(name, holder);
        return holder;
    }

    private static AstralCharacter.Properties properties(int attack, int defense, int health) {
        return new AstralCharacter.Properties().baseStats(attack, defense, health, 6);
    }

    private static CharacterProgressionDefinition progression(int sortOrder) {
        return CharacterProgressionDefinition.of(sortOrder);
    }

}
