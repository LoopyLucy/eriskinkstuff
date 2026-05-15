package co.uk.loopylucy.kinkstuff.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.LivingEntity;

/**
 * Custom model for the Collar item.
 * This class defines the geometry and animations for the collar and its pendant.
 * It extends HumanoidModel to inherit standard player animations.
 */
public class CollarModel extends HumanoidModel<LivingEntity> {
    /** The parts of the collar that can be dyed. */
    public final ModelPart dyeableParts;
    /** The static parts of the collar (the pendant). */
    public final ModelPart staticParts;

    public CollarModel(ModelPart root) {
        super(root);
        // Extract the sub-parts from the baked model tree
        this.dyeableParts = root.getChild("body").getChild("collar_ring");
        this.staticParts = root.getChild("body").getChild("pendant");
    }

    /**
     * Defines the layout and geometry of the collar model.
     * This uses a custom mesh definition to add the collar ring and pendant 
     * as children of the standard player body part.
     */
    public static LayerDefinition createLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // 1. REGISTER STANDARD HUMANOID PARTS (Required for the model tree)
        partdefinition.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.ZERO);

        // 2. DEFINE THE COLLAR RING
        // Consists of 4 thin boxes forming a square around the neck
        body.addOrReplaceChild("collar_ring", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.5F, -0.1F, -2.5F, 5.0F, 1.0F, 1.0F) // Front
                .texOffs(0, 2).addBox(-2.5F, -0.1F, 1.5F, 5.0F, 1.0F, 1.0F)  // Back
                .texOffs(0, 10).addBox(-3.5F, -0.1F, -2.5F, 1.0F, 1.0F, 5.0F) // Left
                .texOffs(4, 4).addBox(2.5F, -0.1F, -2.5F, 1.0F, 1.0F, 5.0F), // Right
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));

        // 3. DEFINE THE PENDANT
        // A flat vertical plane hanging from the front of the collar
        body.addOrReplaceChild("pendant", CubeListBuilder.create()
                        .texOffs(0,0)
                        .addBox(-2.0F, 1.9F, -2.4F, 4.0F, 4.0F, 0.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 16, 16);
    }
}