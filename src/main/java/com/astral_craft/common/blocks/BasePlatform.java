package com.astral_craft.common.blocks;

import com.astral_craft.common.gameplay.board.BoardPanelContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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

public class BasePlatform extends Block {

    public enum Trigger { PASS, LANDING, BOTH }

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    private final Trigger trigger;

    public BasePlatform(Properties properties) {
        this(properties, Trigger.LANDING);
    }

    public BasePlatform(Properties properties, Trigger trigger) {
        super(properties.instabreak().sound(SoundType.WOOL).noOcclusion());
        this.trigger = trigger == null ? Trigger.LANDING : trigger;
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
        return this.trigger == Trigger.BOTH || landing && this.trigger == Trigger.LANDING
                || !landing && this.trigger == Trigger.PASS;
    }

    public boolean isStartPoint() {
        return false;
    }

    public boolean isPortal() {
        return false;
    }

    public void applyBoardEffect(BoardPanelContext context) {}

    public Component tooltip() {
        Identifier id = BuiltInRegistries.BLOCK.getKey(this);
        return Component.translatable("tooltips." + id.getNamespace() + "." + id.getPath())
                .withStyle(ChatFormatting.YELLOW);
    }

}