package co.uk.loopylucy.kinkstuff.client.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.LivingEntity;

/**
 * Custom model for the Mittens item.
 * This class defines the geometry for the mittens that cover the player's hands.
 * It attaches the mittens directly to the arm parts to ensure they move with the arms.
 */
public class MittensModel extends HumanoidModel<LivingEntity> {
    /** The sub-part for the left mitten. */
    public final ModelPart leftMitten;
    /** The sub-part for the right mitten. */
    public final ModelPart rightMitten;

    public MittensModel(ModelPart root) {
        super(root);
        // Extract the mitten sub-parts from the baked arm parts
        this.leftMitten = root.getChild("left_arm").getChild("left_mitten");
        this.rightMitten = root.getChild("right_arm").getChild("right_mitten");
    }

    /**
     * Defines the layout and geometry of the mittens model.
     * Consists of two boxes slightly larger than the player's hands, 
     * parented to the arm parts for animation synchronization.
     */
    public static LayerDefinition createLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // 1. REGISTER STANDARD HUMANOID PARTS (Required for the model tree)
        partdefinition.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.ZERO);

        // Define arm parts to act as parents for the mittens
        PartDefinition left_arm = partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition right_arm = partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.ZERO);

        // 2. DEFINE LEFT MITTEN
        // Offset and sized to encapsulate the standard player hand area
        left_arm.addOrReplaceChild("left_mitten", CubeListBuilder.create()
                .texOffs(0, 11)
                .addBox(-1.5F, 6.0F, -2.5F, 5.0F, 6.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));

        // 3. DEFINE RIGHT MITTEN
        right_arm.addOrReplaceChild("right_mitten", CubeListBuilder.create()
                .texOffs(0,0)
                .addBox(-3.5F, 6.0F, -2.5F, 5.0F, 6.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 32, 32);
    }
}