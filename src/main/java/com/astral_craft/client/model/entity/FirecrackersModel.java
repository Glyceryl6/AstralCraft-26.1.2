package com.astral_craft.client.model.entity;

import com.astral_craft.AstralCraft;
import com.astral_craft.client.render.projectile.FirecrackersRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.rendertype.RenderTypes;

public class FirecrackersModel extends EntityModel<FirecrackersRenderState> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(AstralCraft.prefix("firecrackers"), "main");

    public FirecrackersModel(ModelPart root) {
        super(root, RenderTypes::entityCutoutCull);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild("bone", CubeListBuilder.create().texOffs(1, 3).addBox(-2.0F, -8.0F, -1.0F, 3.0F, 8.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(1, 15).addBox(-3.0F, -10.0F, -2.0F, 5.0F, 2.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(1, 23).addBox(-2.0F, -11.0F, -1.0F, 3.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(14, 25).addBox(-1.0F, -12.0F, 0.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));
        return LayerDefinition.create(meshdefinition, 32, 32);
    }

}