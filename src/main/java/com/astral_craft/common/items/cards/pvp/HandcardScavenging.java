package com.astral_craft.common.items.cards.pvp;

import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.gameplay.board.*;
import com.astral_craft.common.gameplay.handcard.CardTargetTypes;
import com.astral_craft.common.items.BaseHandCard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNullableByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HandcardScavenging extends BaseHandCard implements BoardBotEffect {

    public static final CardDefinition DEFINITION = CardDefinition.create(CardType.EFFECT, CardTargetTypes.NONE, 3);

    public HandcardScavenging(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canUse(ServerPlayer user, ItemStack sourceStack) {
        BoardSession session = BoardSessionManager.findByController(user).orElse(null);
        BoardParticipant participant = session == null ? null : session.participantByController(user.getUUID()).orElse(null);
        return participant != null && hasDroppedCoinsInRange(session, participant, DEFINITION.range());
    }

    @Override
    public boolean onRevealFinished(ServerPlayer user, InteractionHand hand, ItemStack itemStack,
                                    CardDefinition definition) {
        BoardSession session = BoardSessionManager.findByController(user).orElse(null);
        BoardParticipant participant = session == null ? null
                : session.participantByController(user.getUUID()).orElse(null);
        return session != null && participant != null
                && collectNearbyCoins(user.level(), session, participant, definition.range()) > 0;
    }

    @Override
    public boolean canUseByBoardBot(BoardBotEffectContext context) {
        return hasDroppedCoinsInRange(context.session(), context.user(), context.definition().range());
    }

    @Override
    public int applyByBoardBot(BoardBotEffectContext context) {
        collectNearbyCoins(context.level(), context.session(),
                context.user(), context.definition().range());
        return 0;
    }

    @ParametersAreNullableByDefault
    private static boolean hasDroppedCoinsInRange(BoardSession session, BoardParticipant collector, int range) {
        if (session == null || collector == null) return false;
        int maximum = Math.max(0, range);
        return session.mechanics().droppedCoins().entrySet().stream()
                .anyMatch(entry -> entry.getValue() > 0
                        && BoardRouteService.graphDistance(session, collector.currentNodeKey(),
                        entry.getKey(), maximum) >= 0);
    }

    private static int collectNearbyCoins(ServerLevel level, BoardSession session, BoardParticipant collector, int range) {
        int total = 0;
        List<String> collectedNodes = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : session.mechanics().droppedCoins().entrySet()) {
            int distance = BoardRouteService.graphDistance(session, collector.currentNodeKey(),
                    entry.getKey(), range);
            if (distance < 0 || distance > range || entry.getValue() <= 0) continue;
            total += entry.getValue();
            collectedNodes.add(entry.getKey());
            BoardWorldObjectService.spawnPickup(level, session, entry.getKey(), collector, entry.getValue());
        }

        for (String nodeId : collectedNodes) session.mechanics().removeDroppedCoins(nodeId);
        if (total > 0) {
            BoardWorldObjectService.awardCoins(level, session, collector.slotUuid(), total);
            BoardSessionManager.markChanged(level);
        }

        return total;
    }

}