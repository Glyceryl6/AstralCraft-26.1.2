package com.astral_craft.common.gameplay.battle;

import com.astral_craft.common.util.AstralServerTickClock;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.*;

/**
 * World-space combat coordinator. Use this for the future battle UI: start an engagement, allow
 * attack/defense cards to contribute numbers, then resolve without locking either entity in place.
 */
public class BattleService {

    private static final Map<UUID, BattleSession> SESSIONS = new LinkedHashMap<>();

    public static BattleSession start(ServerLevel level, LivingEntity attacker, LivingEntity defender) {
        BattleSession session = new BattleSession(UUID.randomUUID(), attacker, defender, AstralServerTickClock.now(level), 20 * 12, 12.0D);
        SESSIONS.put(session.id(), session);
        if (attacker instanceof ServerPlayer attackerPlayer) {
            attackerPlayer.sendSystemMessage(Component.translatable("message.astral_craft.battle.started_attacker", defender.getDisplayName()), true);
        }

        if (defender instanceof ServerPlayer defenderPlayer) {
            defenderPlayer.sendSystemMessage(Component.translatable("message.astral_craft.battle.started_defender", attacker.getDisplayName()), true);
        }

        return session;
    }

    public static Optional<BattleSession> get(UUID id) {
        return Optional.ofNullable(SESSIONS.get(id));
    }

    public static Optional<BattleResolution> resolve(ServerLevel level, UUID id, BattleAction defenderAction) {
        BattleSession session = SESSIONS.remove(id);
        if (session == null || !session.stillValid(level)) return Optional.empty();
        BattleResolution resolution = session.resolve(level, defenderAction);
        LivingEntity defender = level.getEntity(session.defenderId()) instanceof LivingEntity living ? living : null;
        if (defender != null && resolution.damage() > 0) {
            defender.hurtServer(level, level.damageSources().generic(), resolution.damage());
        }

        return Optional.of(resolution);
    }

    public static void tick(ServerLevel level) {
        Iterator<Map.Entry<UUID, BattleSession>> iterator = SESSIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            BattleSession session = iterator.next().getValue();
            if (session.expired(level) || !session.stillValid(level)) {
                iterator.remove();
            }
        }
    }

}