package com.astral_craft.common.gameplay.dice;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.entity.AstralDiceEntity;
import com.astral_craft.common.stats.AstralPlayerStats;
import com.astral_craft.common.stats.AstralStats;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AstralDiceRollService {

    public static final Identifier WORLD_ENTITY_PRESENTATION = AstralCraft.prefix("world_entity");
    public static final int DEFAULT_ROLL_TICKS = 60;
    public static final int DEFAULT_MERGE_TICKS = 10;
    public static final float DEFAULT_SPIN_SPEED = 20.0F;

    private static final DicePresentation WORLD_ENTITY_PRESENTER = AstralDiceRollService::presentWorldEntities;
    private static final Map<Identifier, DicePresentation> PRESENTATIONS = new ConcurrentHashMap<>();

    static {
        registerPresentation(WORLD_ENTITY_PRESENTATION, WORLD_ENTITY_PRESENTER);
    }

    public static void registerPresentation(Identifier id, DicePresentation presentation) {
        PRESENTATIONS.put(Objects.requireNonNull(id), Objects.requireNonNull(presentation));
    }

    public static DiceRollResult rollNextMove(ServerPlayer player, Vec3 origin) {
        AstralPlayerStats stats = AstralStats.get(player);
        DiceRollResult result = rollNextMove(player, origin, stats);
        AstralStats.set(player, stats.clearNextMoveDiceEffects());
        return result;
    }

    public static DiceRollResult rollNextMove(ServerPlayer player, Vec3 origin, AstralPlayerStats stats) {
        return rollNextMove(player, origin, stats, null);
    }

    public static DiceRollResult rollNextMove(ServerPlayer player, Vec3 origin, AstralPlayerStats stats, UUID boardId) {
        AstralPlayerStats safeStats = stats == null ? AstralPlayerStats.DEFAULT : stats;
        int fixed = safeStats.nextMoveFixed();
        int diceCount = fixed > 0 ? 1 : Math.clamp(1 + safeStats.moveDiceBonus(), 1, 8);
        DiceRollRequest request = new DiceRollRequest(fixed > 0 ? fixed : 1, fixed > 0 ? fixed : 10,
                diceCount, DEFAULT_ROLL_TICKS, diceCount > 1 ? DEFAULT_MERGE_TICKS : 0,
                DEFAULT_SPIN_SPEED, WORLD_ENTITY_PRESENTATION, boardId);
        return roll(player, origin, request, safeStats.speed());
    }

    public static DiceRollResult roll(ServerPlayer player, Vec3 origin, DiceRollRequest request) {
        return roll(player, origin, request, 0);
    }

    public static DiceRollResult roll(ServerPlayer player, Vec3 origin, DiceRollRequest request, int flatBonus) {
        ServerLevel level = player.level();
        List<Integer> values = new ArrayList<>(request.diceCount());
        for (int i = 0; i < request.diceCount(); i++) {
            values.add(Mth.nextInt(level.getRandom(), request.minValue(), request.maxValue()));
        }

        int total = Math.max(0, values.stream().mapToInt(Integer::intValue).sum() + flatBonus);
        DiceRollResult result = new DiceRollResult(List.copyOf(values), total);
        DicePresentation presentation = PRESENTATIONS.getOrDefault(request.presentation(), WORLD_ENTITY_PRESENTER);
        presentation.present(player, origin, request, result);
        return result;
    }

    private static void presentWorldEntities(
            ServerPlayer player, Vec3 origin,
            DiceRollRequest request, DiceRollResult result) {
        ServerLevel level = player.level();
        Vec3 look = player.getLookAngle();
        Vec3 side = new Vec3(-look.z, 0.0D, look.x);
        if (side.lengthSqr() < 1.0E-6D) side = new Vec3(1.0D, 0.0D, 0.0D);
        side = side.normalize();
        int count = result.values().size();
        float spacing = count > 1 ? 0.72F : 0.0F;
        double center = (count - 1) * 0.5D;
        for (int i = 0; i < count; i++) {
            double offset = (i - center) * spacing;
            Vec3 spawn = origin.add(side.scale(offset));
            AstralDiceEntity dice = new AstralDiceEntity(level, spawn.x, spawn.y, spawn.z);
            dice.setBoardSessionId(request.boardId());
            dice.startRoll(request.minValue(), request.maxValue(), request.rollTicks(), request.spinSpeed(),
                    result.values().get(i), result.total(), request.mergeTicks(), i == 0,
                    (float) (-side.x * offset), (float) (-side.z * offset));
            level.addFreshEntity(dice);
        }
    }

    @FunctionalInterface
    public interface DicePresentation {
        void present(ServerPlayer player, Vec3 origin, DiceRollRequest request, DiceRollResult result);
    }

    public record DiceRollRequest(int minValue, int maxValue, int diceCount, int rollTicks,
                                  int mergeTicks, float spinSpeed, Identifier presentation, UUID boardId) {
        public DiceRollRequest(int minValue, int maxValue, int diceCount, int rollTicks,
                               int mergeTicks, float spinSpeed, Identifier presentation) {
            this(minValue, maxValue, diceCount, rollTicks, mergeTicks, spinSpeed, presentation, null);
        }

        public DiceRollRequest {
            int safeMin = Math.min(minValue, maxValue);
            int safeMax = Math.max(minValue, maxValue);
            minValue = safeMin;
            maxValue = safeMax;
            diceCount = Math.clamp(diceCount, 1, 8);
            rollTicks = Math.clamp(rollTicks, 1, 20 * 30);
            mergeTicks = Math.clamp(mergeTicks, 0, 20 * 10);
            spinSpeed = Math.clamp(spinSpeed, 1.0F, 180.0F);
            presentation = presentation == null ? WORLD_ENTITY_PRESENTATION : presentation;
        }
    }

    public record DiceRollResult(List<Integer> values, int total) {
        public DiceRollResult {
            values = List.copyOf(values);
            total = Math.max(0, total);
        }
    }
}
