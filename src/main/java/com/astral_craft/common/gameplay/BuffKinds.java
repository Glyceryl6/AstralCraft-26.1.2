package com.astral_craft.common.gameplay;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Simple runtime registry for built-in and addon-defined buff identifiers. */
public final class BuffKinds {
    private static final Map<String, BuffKind> BY_NAME = new LinkedHashMap<>();

    public static final BuffKind HEAL = register("heal");
    public static final BuffKind STARLIGHT = register("starlight");
    public static final BuffKind MARK = register("mark");
    public static final BuffKind BERSERK = register("berserk");
    public static final BuffKind POISON = register("poison");
    public static final BuffKind CURSE = register("curse");
    public static final BuffKind STUN = register("stun");
    public static final BuffKind COUNTER = register("counter");
    public static final BuffKind OVERCLOCK = register("overclock");
    public static final BuffKind PROBLEM_STUDENT = register("problem_student");
    public static final BuffKind AWAKENING = register("awakening");
    public static final BuffKind CUSTOM = register("custom");

    private BuffKinds() {}

    public static synchronized BuffKind register(String name) {
        String key = BuffKind.normalize(name);
        BuffKind existing = BY_NAME.get(key);
        if (existing != null) {
            return existing;
        }
        BuffKind kind = new BuffKind(key);
        BY_NAME.put(key, kind);
        return kind;
    }

    public static synchronized BuffKind getOrCreate(String name) {
        return register(name);
    }

    public static synchronized Collection<BuffKind> values() {
        return List.copyOf(BY_NAME.values());
    }
}
