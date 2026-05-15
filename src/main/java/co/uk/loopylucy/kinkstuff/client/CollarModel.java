package co.uk.loopylucy.kinkstuff.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.LivingEntity;

public class CollarModel extends HumanoidModel<LivingEntity> {
    public final ModelPart dyeableParts;
    public final ModelPart staticParts;

    public CollarModel(ModelPart root) {
        super(root);
        this.dyeableParts = root.getChild("body").getChild("collar_ring");
        this.staticParts = root.getChild("body").getChild("pendant");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        partdefinition.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.ZERO);

        body.addOrReplaceChild("collar_ring", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.5F, -0.1F, -2.5F, 5.0F, 1.0F, 1.0F) // Front
                .texOffs(0, 2).addBox(-2.5F, -0.1F, 1.5F, 5.0F, 1.0F, 1.0F)  // Back
                .texOffs(0, 10).addBox(-3.5F, -0.1F, -2.5F, 1.0F, 1.0F, 5.0F) // Left
                .texOffs(4, 4).addBox(2.5F, -0.1F, -2.5F, 1.0F, 1.0F, 5.0F), // Right
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));

        body.addOrReplaceChild("pendant", CubeListBuilder.create()
                        .texOffs(0,0)
                        .addBox(-2.0F, 1.9F, -2.4F, 4.0F, 4.0F, 0.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 16, 16);
    }
}