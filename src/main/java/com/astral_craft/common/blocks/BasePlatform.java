package com.astral_craft.common.blocks;

import com.astral_craft.common.gameplay.board.BoardPanelContext;
import com.astral_craft.common.gameplay.board.BoardSession;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class BasePlatform extends Block {

    public enum Trigger { PASS, LANDING, BOTH }

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    private static final Map<UUID, BasePlatform> ACTIVE_BOARD_EFFECTS = new HashMap<>();
    private final Trigger trigger;

    public BasePlatform(Properties properties) {
        this(properties, Trigger.LANDING);
    }

    public BasePlatform(Properties properties, Trigger trigger) {
        super(properties.instabreak().sound(SoundType.WOOL).noOcclusion());
        this.trigger = trigger;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Block.column(16.0F, 0.0F, 1.0F);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return !level.isEmptyBlock(pos.below());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    public Trigger boardTrigger() {
        return this.trigger;
    }

    public boolean triggers(boolean landing) {
        return this.trigger == Trigger.BOTH || landing && this.trigger == Trigger.LANDING || !landing && this.trigger == Trigger.PASS;
    }

    public void applyBoardEffect(BoardPanelContext context) {}

    protected final void activateBoardEffect(BoardSession session) {
        ACTIVE_BOARD_EFFECTS.put(session.id(), this);
    }

    protected final void deactivateBoardEffect(UUID boardId) {
        ACTIVE_BOARD_EFFECTS.remove(boardId, this);
    }

    public static Optional<BasePlatform> activeBoardEffect(UUID boardId) {
        return Optional.ofNullable(ACTIVE_BOARD_EFFECTS.get(boardId));
    }

    public static boolean hasActiveBoardEffect(UUID boardId) {
        return ACTIVE_BOARD_EFFECTS.containsKey(boardId);
    }

    public static boolean tickActiveBoardEffect(ServerLevel level, BoardSession session) {
        BasePlatform platform = ACTIVE_BOARD_EFFECTS.get(session.id());
        if (platform == null) return false;
        platform.tickPendingBoardEffect(level, session);
        return true;
    }

    public static void activeParticipantBecameAutomated(ServerLevel level, BoardSession session, UUID slotId) {
        BasePlatform platform = ACTIVE_BOARD_EFFECTS.get(session.id());
        if (platform != null) platform.pendingParticipantBecameAutomated(level, session, slotId);
    }

    public static void clearActiveBoardEffect(UUID boardId) {
        BasePlatform platform = ACTIVE_BOARD_EFFECTS.remove(boardId);
        if (platform != null) platform.discardPendingBoardEffect(boardId);
    }

    protected void tickPendingBoardEffect(ServerLevel level, BoardSession session) {}

    protected void pendingParticipantBecameAutomated(ServerLevel level, BoardSession session, UUID slotId) {}

    protected void discardPendingBoardEffect(UUID boardId) {}

    public Component boardActionPrompt(Component actorName) {
        return Component.empty();
    }

    public void handleBoardTargetSelection(ServerPlayer player, BoardSession session, int entityId) {}

    public void handleBoardChipSelection(ServerPlayer player, BoardSession session, Identifier chipId) {}

    public boolean protectsBoardParticipant() {
        return false;
    }

    public boolean characterStart() {
        return false;
    }

    public Component tooltip() {
        Identifier id = BuiltInRegistries.BLOCK.getKey(this);
        return Component.translatable("tooltips." + id.getNamespace() + "." + id.getPath())
                .withStyle(ChatFormatting.YELLOW);
    }

}
