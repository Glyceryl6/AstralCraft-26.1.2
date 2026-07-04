package com.astral_craft.common.gameplay.event.type;

import com.astral_craft.AstralCraft;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.resources.Identifier;

public class AstralEventIdentifiers {

    public static final Codec<Identifier> CODEC = Codec.STRING.comapFlatMap(raw -> {
        Identifier id = parse(raw, null);
        return id == null ? DataResult.error(() -> "error.astral_craft.event.identifier.invalid:" + raw) : DataResult.success(id);
    }, Identifier::toString);

    public static Identifier parse(String raw, Identifier fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            if ("*".equals(raw)) return AstralCraft.prefix("any");
            return raw.contains(":") ? Identifier.parse(raw) : AstralCraft.prefix(raw);
        } catch (Exception exception) {
            return fallback;
        }
    }

    public static boolean equals(Identifier first, Identifier second) {
        return first != null && first.equals(second);
    }

}
