package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class SpiderEntityModel<T extends Entity> extends SinglePartEntityModel<T> {
   /**
    * The key of the first model part of the body, whose value is {@value}.
    */
   private static final String BODY0 = "body0";
   /**
    * The key of the second model part of the body, whose value is {@value}.
    */
   private static final String BODY1 = "body1";
   /**
    * The key of the right middle front leg model part, whose value is {@value}.
    */
   private static final String RIGHT_MIDDLE_FRONT_LEG = "right_middle_front_leg";
   /**
    * The key of the left middle front leg model part, whose value is {@value}.
    */
   private static final String LEFT_MIDDLE_FRONT_LEG = "left_middle_front_leg";
   /**
    * The key of the right middle hind leg model part, whose value is {@value}.
    */
   private static final String RIGHT_MIDDLE_HIND_LEG = "right_middle_hind_leg";
   /**
    * The key of the left middle hind leg model part, whose value is {@value}.
    */
   private static final String LEFT_MIDDLE_HIND_LEG = "left_middle_hind_leg";
   private final ModelPart root;
   private final ModelPart head;
   private final ModelPart rightHindLeg;
   private final ModelPart leftHindLeg;
   private final ModelPart rightMiddleLeg;
   private final ModelPart leftMiddleLeg;
   private final ModelPart rightMiddleFrontLeg;
   private final ModelPart leftMiddleFrontLeg;
   private final ModelPart rightFrontLeg;
   private final ModelPart leftFrontLeg;

   public SpiderEntityModel(ModelPart root) {
      this.root = root;
      this.head = root.getChild(EntityModelPartNames.HEAD);
      this.rightHindLeg = root.getChild(EntityModelPartNames.RIGHT_HIND_LEG);
      this.leftHindLeg = root.getChild(EntityModelPartNames.LEFT_HIND_LEG);
      this.rightMiddleLeg = root.getChild("right_middle_hind_leg");
      this.leftMiddleLeg = root.getChild("left_middle_hind_leg");
      this.rightMiddleFrontLeg = root.getChild("right_middle_front_leg");
      this.leftMiddleFrontLeg = root.getChild("left_middle_front_leg");
      this.rightFrontLeg = root.getChild(EntityModelPartNames.RIGHT_FRONT_LEG);
      this.leftFrontLeg = root.getChild(EntityModelPartNames.LEFT_FRONT_LEG);
   }

   public static TexturedModelData getTexturedModelData() {
      ModelData modelData = new ModelData();
      ModelPartData modelPartData = modelData.getRoot();
      int i = true;
      modelPartData.addChild(EntityModelPartNames.HEAD, ModelPartBuilder.create().uv(32, 4).cuboid(-4.0F, -4.0F, -8.0F, 8.0F, 8.0F, 8.0F), ModelTransform.pivot(0.0F, 15.0F, -3.0F));
      modelPartData.addChild("body0", ModelPartBuilder.create().uv(0, 0).cuboid(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F), ModelTransform.pivot(0.0F, 15.0F, 0.0F));
      modelPartData.addChild("body1", ModelPartBuilder.create().uv(0, 12).cuboid(-5.0F, -4.0F, -6.0F, 10.0F, 8.0F, 12.0F), ModelTransform.pivot(0.0F, 15.0F, 9.0F));
      ModelPartBuilder modelPartBuilder = ModelPartBuilder.create().uv(18, 0).cuboid(-15.0F, -1.0F, -1.0F, 16.0F, 2.0F, 2.0F);
      ModelPartBuilder modelPartBuilder2 = ModelPartBuilder.create().uv(18, 0).cuboid(-1.0F, -1.0F, -1.0F, 16.0F, 2.0F, 2.0F);
      modelPartData.addChild(EntityModelPartNames.RIGHT_HIND_LEG, modelPartBuilder, ModelTransform.pivot(-4.0F, 15.0F, 2.0F));
      modelPartData.addChild(EntityModelPartNames.LEFT_HIND_LEG, modelPartBuilder2, ModelTransform.pivot(4.0F, 15.0F, 2.0F));
      modelPartData.addChild("right_middle_hind_leg", modelPartBuilder, ModelTransform.pivot(-4.0F, 15.0F, 1.0F));
      modelPartData.addChild("left_middle_hind_leg", modelPartBuilder2, ModelTransform.pivot(4.0F, 15.0F, 1.0F));
      modelPartData.addChild("right_middle_front_leg", modelPartBuilder, ModelTransform.pivot(-4.0F, 15.0F, 0.0F));
      modelPartData.addChild("left_middle_front_leg", modelPartBuilder2, ModelTransform.pivot(4.0F, 15.0F, 0.0F));
      modelPartData.addChild(EntityModelPartNames.RIGHT_FRONT_LEG, modelPartBuilder, ModelTransform.pivot(-4.0F, 15.0F, -1.0F));
      modelPartData.addChild(EntityModelPartNames.LEFT_FRONT_LEG, modelPartBuilder2, ModelTransform.pivot(4.0F, 15.0F, -1.0F));
      return TexturedModelData.of(modelData, 64, 32);
   }

   public ModelPart getPart() {
      return this.root;
   }

   public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
      this.head.yaw = headYaw * 0.017453292F;
      this.head.pitch = headPitch * 0.017453292F;
      float f = 0.7853982F;
      this.rightHindLeg.roll = -0.7853982F;
      this.leftHindLeg.roll = 0.7853982F;
      this.rightMiddleLeg.roll = -0.58119464F;
      this.leftMiddleLeg.roll = 0.58119464F;
      this.rightMiddleFrontLeg.roll = -0.58119464F;
      this.leftMiddleFrontLeg.roll = 0.58119464F;
      this.rightFrontLeg.roll = -0.7853982F;
      this.leftFrontLeg.roll = 0.7853982F;
      float g = -0.0F;
      float h = 0.3926991F;
      this.rightHindLeg.yaw = 0.7853982F;
      this.leftHindLeg.yaw = -0.7853982F;
      this.rightMiddleLeg.yaw = 0.3926991F;
      this.leftMiddleLeg.yaw = -0.3926991F;
      this.rightMiddleFrontLeg.yaw = -0.3926991F;
      this.leftMiddleFrontLeg.yaw = 0.3926991F;
      this.rightFrontLeg.yaw = -0.7853982F;
      this.leftFrontLeg.yaw = 0.7853982F;
      float i = -(MathHelper.cos(limbAngle * 0.6662F * 2.0F + 0.0F) * 0.4F) * limbDistance;
      float j = -(MathHelper.cos(limbAngle * 0.6662F * 2.0F + 3.1415927F) * 0.4F) * limbDistance;
      float k = -(MathHelper.cos(limbAngle * 0.6662F * 2.0F + 1.5707964F) * 0.4F) * limbDistance;
      float l = -(MathHelper.cos(limbAngle * 0.6662F * 2.0F + 4.712389F) * 0.4F) * limbDistance;
      float m = Math.abs(MathHelper.sin(limbAngle * 0.6662F + 0.0F) * 0.4F) * limbDistance;
      float n = Math.abs(MathHelper.sin(limbAngle * 0.6662F + 3.1415927F) * 0.4F) * limbDistance;
      float o = Math.abs(MathHelper.sin(limbAngle * 0.6662F + 1.5707964F) * 0.4F) * limbDistance;
      float p = Math.abs(MathHelper.sin(limbAngle * 0.6662F + 4.712389F) * 0.4F) * limbDistance;
      ModelPart var10000 = this.rightHindLeg;
      var10000.yaw += i;
      var10000 = this.leftHindLeg;
      var10000.yaw += -i;
      var10000 = this.rightMiddleLeg;
      var10000.yaw += j;
      var10000 = this.leftMiddleLeg;
      var10000.yaw += -j;
      var10000 = this.rightMiddleFrontLeg;
      var10000.yaw += k;
      var10000 = this.leftMiddleFrontLeg;
      var10000.yaw += -k;
      var10000 = this.rightFrontLeg;
      var10000.yaw += l;
      var10000 = this.leftFrontLeg;
      var10000.yaw += -l;
      var10000 = this.rightHindLeg;
      var10000.roll += m;
      var10000 = this.leftHindLeg;
      var10000.roll += -m;
      var10000 = this.rightMiddleLeg;
      var10000.roll += n;
      var10000 = this.leftMiddleLeg;
      var10000.roll += -n;
      var10000 = this.rightMiddleFrontLeg;
      var10000.roll += o;
      var10000 = this.leftMiddleFrontLeg;
      var10000.roll += -o;
      var10000 = this.rightFrontLeg;
      var10000.roll += p;
      var10000 = this.leftFrontLeg;
      var10000.roll += -p;
   }
}
