package com.astral_craft.common.gameplay.board;

import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.gameplay.event.AstralEventEffect;
import com.astral_craft.common.gameplay.fortune.BoardFortuneDefinition;
import com.astral_craft.common.gameplay.fortune.BoardFortuneManager;
import com.astral_craft.common.network.s2c.CloseBoardPresentationPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BoardFortuneService {

    public static @Nullable BoardFortuneDefinition randomDefinition(ServerLevel level) {
        List<BoardFortuneDefinition> values = BoardFortuneManager.INSTANCE.values();
        if (values.isEmpty()) return null;
        int total = values.stream().mapToInt(BoardFortuneDefinition::weight).sum();
        int roll = level.getRandom().nextInt(Math.max(1, total));
        for (BoardFortuneDefinition definition : values) {
            roll -= definition.weight();
            if (roll < 0) return definition;
        }
        return values.getLast();
    }

    public static List<BoardFortuneDefinition> randomDefinitions(ServerLevel level, int count) {
        List<BoardFortuneDefinition> pool = new ArrayList<>(BoardFortuneManager.INSTANCE.values());
        List<BoardFortuneDefinition> result = new ArrayList<>();
        while (!pool.isEmpty() && result.size() < count) {
            int total = pool.stream().mapToInt(BoardFortuneDefinition::weight).sum();
            int roll = level.getRandom().nextInt(Math.max(1, total));
            BoardFortuneDefinition selected = pool.getLast();
            for (BoardFortuneDefinition definition : pool) {
                roll -= definition.weight();
                if (roll < 0) {
                    selected = definition;
                    break;
                }
            }
            result.add(selected);
            pool.remove(selected);
        }
        return List.copyOf(result);
    }

    public static void apply(ServerLevel level, BoardSession session, BoardParticipant source,
                             BoardFortuneDefinition definition, List<UUID> targetSlots) {
        if (level == null || session == null || source == null || definition == null) return;
        ServerPlayer triggerPlayer = source.controllerUuid().map(level.getServer().getPlayerList()::getPlayer).orElse(null);
        for (UUID targetId : targetSlots) {
            BoardParticipant target = session.participant(targetId).orElse(null);
            AstralCharacterEntity pawn = target == null ? null : BoardEntityService.entity(level, target);
            if (target == null || pawn == null || target.knockedDown()) continue;
            AstralEventContext context = new AstralEventContext(triggerPlayer, pawn, level, null,
                    pawn.blockPosition(), null, null, null, 0.0F, false);
            for (AstralEventEffect effect : definition.effects()) {
                if (effect != null) effect.apply(context);
            }
        }
        BoardSessionManager.syncBoardSnapshot(level, session);
    }

    public static void closePresentation(ServerLevel level, BoardSession session) {
        if (level == null || session == null) return;
        for (ServerPlayer viewer : BoardSpectatorService.presentationViewers(level, session)) {
            PacketDistributor.sendToPlayer(viewer, new CloseBoardPresentationPayload(session.id()));
        }
    }
}
