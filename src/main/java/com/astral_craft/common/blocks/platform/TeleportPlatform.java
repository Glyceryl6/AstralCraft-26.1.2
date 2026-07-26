package com.astral_craft.common.blocks.platform;

import com.astral_craft.common.blocks.BasePlatform;
import com.astral_craft.common.gameplay.BoardNode;
import com.astral_craft.common.gameplay.board.BoardPanelContext;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class TeleportPlatform extends BasePlatform {

    public TeleportPlatform(Block.Properties properties) {
        super(properties, Trigger.LANDING);
    }

    @Override
    public void applyBoardEffect(BoardPanelContext context) {
        Identifier platformId = context.node().platformId();
        List<String> destinations = context.session().nodes().values().stream()
                .filter(node -> node.platformId().equals(platformId)).map(BoardNode::id)
                .filter(nodeId -> !nodeId.equals(context.participant().currentNodeKey())).toList();
        if (destinations.isEmpty()) return;
        String destination = destinations.get(context.level().getRandom().nextInt(destinations.size()));
        context.level().playSound(null, context.session().positions().get(context.participant().currentNodeKey()),
                SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS, 0.75F, 1.0F);
        BoardSessionManager.relocateParticipant(context.level(), context.session(), context.participant(), destination);
        context.level().playSound(null, context.session().positions().get(destination),
                SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS, 0.75F, 1.1F);
    }

}