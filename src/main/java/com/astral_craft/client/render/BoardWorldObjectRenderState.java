package com.astral_craft.client.render;

import com.astral_craft.common.entity.BoardWorldObjectEntity;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class BoardWorldObjectRenderState extends EntityRenderState {

    public final MovingBlockRenderState movingBlockRenderState = new MovingBlockRenderState();
    public BoardWorldObjectEntity.Kind kind = BoardWorldObjectEntity.Kind.ENTRAPMENT;
    public float age;
    public int stackIndex;
    public int stackCount = 1;
    public int amount = 1;

}