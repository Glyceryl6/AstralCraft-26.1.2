package com.astral_craft.common.items.cards.pvp;

import com.astral_craft.common.blocks.platform.TeleportPlatform;
import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.gameplay.BoardNode;
import com.astral_craft.common.gameplay.board.*;
import com.astral_craft.common.gameplay.handcard.CardTargetTypes;
import com.astral_craft.common.items.BaseHandCard;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class HandcardRandomPortal extends BaseHandCard implements BoardBotEffect {

    public static final CardDefinition DEFINITION = CardDefinition.create(CardType.EFFECT, CardTargetTypes.NONE, -1);

    public HandcardRandomPortal(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canUse(ServerPlayer user, ItemStack sourceStack) {
        BoardSession session = BoardSessionManager.findByController(user).orElse(null);
        BoardParticipant participant = session == null ? null
                : session.participantByController(user.getUUID()).orElse(null);
        return session != null && participant != null && !portalNodes(session, participant).isEmpty();
    }

    @Override
    public boolean onRevealFinished(ServerPlayer user, InteractionHand hand, ItemStack itemStack, CardDefinition definition) {
        BoardSession session = BoardSessionManager.findByController(user).orElse(null);
        BoardParticipant participant = session == null ? null
                : session.participantByController(user.getUUID()).orElse(null);
        if (session == null || participant == null) return false;
        List<String> destinations = portalNodes(session, participant);
        if (destinations.isEmpty()) return false;
        BoardSessionManager.relocateParticipant(user.level(), session, participant,
                destinations.get(user.level().getRandom().nextInt(destinations.size())));
        return true;
    }

    @Override
    public boolean canUseByBoardBot(BoardBotEffectContext context) {
        return !portalNodes(context.session(), context.user()).isEmpty();
    }

    @Override
    public int applyByBoardBot(BoardBotEffectContext context) {
        List<String> destinations = portalNodes(context.session(), context.user());
        if (!destinations.isEmpty()) {
            BoardSessionManager.relocateParticipant(context.level(), context.session(), context.user(),
                    destinations.get(context.level().getRandom().nextInt(destinations.size())));
        }

        return 0;
    }

    private static List<String> portalNodes(BoardSession session, BoardParticipant participant) {
        return session.nodes().values().stream()
                .filter(node -> BuiltInRegistries.BLOCK.getValue(node.platformId()) instanceof TeleportPlatform)
                .map(BoardNode::id).filter(nodeId -> !nodeId.equals(participant.currentNodeKey())).toList();
    }

}
