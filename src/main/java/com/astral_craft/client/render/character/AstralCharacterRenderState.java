package com.astral_craft.client.render.character;

import com.astral_craft.AstralCraft;
import com.astral_craft.client.model.character.AstralGeoPose;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;

public class AstralCharacterRenderState extends AvatarRenderState {

    public Identifier characterId = AstralCraft.prefix("mimi");
    public String skinId = "default";
    public Identifier texture = AstralCraft.prefix("textures/entity/character/default.png");
    public Identifier modelKey = AstralCraft.prefix("humanoid");
    public Identifier animationSetKey = AstralCraft.prefix("humanoid");
    public String animationAction = "idle";
    public float animationTimeSeconds;
    public AstralGeoPose rootPose = AstralGeoPose.IDENTITY;

}
