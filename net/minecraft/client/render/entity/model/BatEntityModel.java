package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.util.math.MathHelper;

/**
 * Represents the model of a {@linkplain BatEntity}.
 * 
 * <div class="fabric">
 * <table border=1>
 * <caption>Model parts of this model</caption>
 * <tr>
 *   <th>Part Name</th><th>Parent</th><th>Corresponding Field</th>
 * </tr>
 * <tr>
 *   <td>{@value EntityModelPartNames#HEAD}</td><td>{@linkplain #root Root part}</td><td>{@link #head}</td>
 * </tr>
 * <tr>
 *   <td>{@value EntityModelPartNames#RIGHT_EAR}</td><td>{@value EntityModelPartNames#HEAD}</td><td></td>
 * </tr>
 * <tr>
 *   <td>{@value EntityModelPartNames#LEFT_EAR}</td><td>{@value EntityModelPartNames#HEAD}</td><td></td>
 * </tr>
 * <tr>
 *   <td>{@value EntityModelPartNames#BODY}</td><td>{@linkplain #root Root part}</td><td>{@link #body}</td>
 * </tr>
 * <tr>
 *   <td>{@value EntityModelPartNames#RIGHT_WING}</td><td>{@value EntityModelPartNames#BODY}</td><td>{@link #rightWing}</td>
 * </tr>
 * <tr>
 *   <td>{@value EntityModelPartNames#RIGHT_WING_TIP}</td><td>{@value EntityModelPartNames#RIGHT_WING}</td><td>{@link #rightWingTip}</td>
 * </tr>
 * <tr>
 *   <td>{@value EntityModelPartNames#LEFT_WING}</td><td>{@value EntityModelPartNames#BODY}</td><td>{@link #leftWing}</td>
 * </tr>
 * <tr>
 *   <td>{@value EntityModelPartNames#LEFT_WING_TIP}</td><td>{@value EntityModelPartNames#LEFT_WING}</td><td>{@link #leftWingTip}</td>
 * </tr>
 * </table>
 * </div>
 */
@Environment(EnvType.CLIENT)
public class BatEntityModel extends SinglePartEntityModel<BatEntity> {
   private final ModelPart root;
   private final ModelPart head;
   private final ModelPart body;
   private final ModelPart rightWing;
   private final ModelPart leftWing;
   private final ModelPart rightWingTip;
   private final ModelPart leftWingTip;

   public BatEntityModel(ModelPart root) {
      this.root = root;
      this.head = root.getChild(EntityModelPartNames.HEAD);
      this.body = root.getChild(EntityModelPartNames.BODY);
      this.rightWing = this.body.getChild(EntityModelPartNames.RIGHT_WING);
      this.rightWingTip = this.rightWing.getChild(EntityModelPartNames.RIGHT_WING_TIP);
      this.leftWing = this.body.getChild(EntityModelPartNames.LEFT_WING);
      this.leftWingTip = this.leftWing.getChild(EntityModelPartNames.LEFT_WING_TIP);
   }

   public static TexturedModelData getTexturedModelData() {
      ModelData modelData = new ModelData();
      ModelPartData modelPartData = modelData.getRoot();
      ModelPartData modelPartData2 = modelPartData.addChild(EntityModelPartNames.HEAD, ModelPartBuilder.create().uv(0, 0).cuboid(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F), ModelTransform.NONE);
      modelPartData2.addChild(EntityModelPartNames.RIGHT_EAR, ModelPartBuilder.create().uv(24, 0).cuboid(-4.0F, -6.0F, -2.0F, 3.0F, 4.0F, 1.0F), ModelTransform.NONE);
      modelPartData2.addChild(EntityModelPartNames.LEFT_EAR, ModelPartBuilder.create().uv(24, 0).mirrored().cuboid(1.0F, -6.0F, -2.0F, 3.0F, 4.0F, 1.0F), ModelTransform.NONE);
      ModelPartData modelPartData3 = modelPartData.addChild(EntityModelPartNames.BODY, ModelPartBuilder.create().uv(0, 16).cuboid(-3.0F, 4.0F, -3.0F, 6.0F, 12.0F, 6.0F).uv(0, 34).cuboid(-5.0F, 16.0F, 0.0F, 10.0F, 6.0F, 1.0F), ModelTransform.NONE);
      ModelPartData modelPartData4 = modelPartData3.addChild(EntityModelPartNames.RIGHT_WING, ModelPartBuilder.create().uv(42, 0).cuboid(-12.0F, 1.0F, 1.5F, 10.0F, 16.0F, 1.0F), ModelTransform.NONE);
      modelPartData4.addChild(EntityModelPartNames.RIGHT_WING_TIP, ModelPartBuilder.create().uv(24, 16).cuboid(-8.0F, 1.0F, 0.0F, 8.0F, 12.0F, 1.0F), ModelTransform.pivot(-12.0F, 1.0F, 1.5F));
      ModelPartData modelPartData5 = modelPartData3.addChild(EntityModelPartNames.LEFT_WING, ModelPartBuilder.create().uv(42, 0).mirrored().cuboid(2.0F, 1.0F, 1.5F, 10.0F, 16.0F, 1.0F), ModelTransform.NONE);
      modelPartData5.addChild(EntityModelPartNames.LEFT_WING_TIP, ModelPartBuilder.create().uv(24, 16).mirrored().cuboid(0.0F, 1.0F, 0.0F, 8.0F, 12.0F, 1.0F), ModelTransform.pivot(12.0F, 1.0F, 1.5F));
      return TexturedModelData.of(modelData, 64, 64);
   }

   public ModelPart getPart() {
      return this.root;
   }

   public void setAngles(BatEntity batEntity, float f, float g, float h, float i, float j) {
      if (batEntity.isRoosting()) {
         this.head.pitch = j * 0.017453292F;
         this.head.yaw = 3.1415927F - i * 0.017453292F;
         this.head.roll = 3.1415927F;
         this.head.setPivot(0.0F, -2.0F, 0.0F);
         this.rightWing.setPivot(-3.0F, 0.0F, 3.0F);
         this.leftWing.setPivot(3.0F, 0.0F, 3.0F);
         this.body.pitch = 3.1415927F;
         this.rightWing.pitch = -0.15707964F;
         this.rightWing.yaw = -1.2566371F;
         this.rightWingTip.yaw = -1.7278761F;
         this.leftWing.pitch = this.rightWing.pitch;
         this.leftWing.yaw = -this.rightWing.yaw;
         this.leftWingTip.yaw = -this.rightWingTip.yaw;
      } else {
         this.head.pitch = j * 0.017453292F;
         this.head.yaw = i * 0.017453292F;
         this.head.roll = 0.0F;
         this.head.setPivot(0.0F, 0.0F, 0.0F);
         this.rightWing.setPivot(0.0F, 0.0F, 0.0F);
         this.leftWing.setPivot(0.0F, 0.0F, 0.0F);
         this.body.pitch = 0.7853982F + MathHelper.cos(h * 0.1F) * 0.15F;
         this.body.yaw = 0.0F;
         this.rightWing.yaw = MathHelper.cos(h * 74.48451F * 0.017453292F) * 3.1415927F * 0.25F;
         this.leftWing.yaw = -this.rightWing.yaw;
         this.rightWingTip.yaw = this.rightWing.yaw * 0.5F;
         this.leftWingTip.yaw = -this.rightWing.yaw * 0.5F;
      }

   }
}
