package com.astral_craft.client.render;

import com.astral_craft.common.gameplay.dice.DiceSkinPreferenceManager;
import net.minecraft.resources.Identifier;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class AstralDiceRenderState extends EntityRenderState {

    public String text = "?";
    public float xSpin;
    public float ySpin;
    public float zSpin;
    public float mergeOffsetX;
    public float mergeOffsetZ;
    public float scale = 1.0F;
    public boolean flatNumber;
    public Identifier texture = DiceSkinPreferenceManager.DEFAULT_TEXTURE;

}
