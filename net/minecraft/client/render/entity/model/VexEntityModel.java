package net.minecraft.client.render.entity.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.VexEntity;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;

/**
 * Represents the model of a {@linkplain VexEntity}.
 * 
 * <div class="fabric">
 * <table border=1>
 * <caption>Model parts of this model</caption>
 * <tr>
 *   <th>Part Name</th><th>Parent</th><th>Corresponding Field</th>
 * </tr>
 * <tr>
 *   <td>{@value EntityModelPartNames#HAT}</td><td>Root part</td><td>{@link #hat}</td>
 * </tr>
 * <tr>
 *   <td>{@value EntityModelPartNames#HEAD}</td><td>Root part</td><td>{@link #head}</td>
 * </tr>
 * <tr>
 *   <td>{@value EntityModelPartNames#BODY}</td><td>Root part</td><td>{@link #body}</td>
 * </tr>
 * <tr>
 *   <td>{@value EntityModelPartNames#RIGHT_ARM}</td><td>Root part</td><td>{@link #rightArm}</td>
 * </tr>
 * <tr>
 *   <td>{@value EntityModelPartNames#LEFT_ARM}</td><td>Root part</td><td>{@link #leftArm}</td>
 * </tr>
 * <tr>
 *   <td>{@value EntityModelPartNames#RIGHT_LEG}</td><td>Root part</td><td>{@link #rightLeg}</td>
 * </tr>
 * <tr>
 *   <td>{@value EntityModelPartNames#LEFT_LEG}</td><td>Root part</td><td>{@link #leftLeg}</td>
 * </tr>
 * <tr>
 *   <td>{@value EntityModelPartNames#RIGHT_WING}</td><td>Root part</td><td>{@link #rightWing}</td>
 * </tr>
 * <tr>
 *   <td>{@value EntityModelPartNames#LEFT_WING}</td><td>Root part</td><td>{@link #leftWing}</td>
 * </tr>
 * </table>
 * </div>
 */
@Environment(EnvType.CLIENT)
public class VexEntityModel extends BipedEntityModel<VexEntity> {
   private final ModelPart leftWing;
   private final ModelPart rightWing;

   public VexEntityModel(ModelPart modelPart) {
      super(modelPart);
      this.leftLeg.visible = false;
      this.hat.visible = false;
      this.rightWing = modelPart.getChild(EntityModelPartNames.RIGHT_WING);
      this.leftWing = modelPart.getChild(EntityModelPartNames.LEFT_WING);
   }

   public static TexturedModelData getTexturedModelData() {
      ModelData modelData = BipedEntityModel.getModelData(Dilation.NONE, 0.0F);
      ModelPartData modelPartData = modelData.getRoot();
      modelPartData.addChild(EntityModelPartNames.RIGHT_LEG, ModelPartBuilder.create().uv(32, 0).cuboid(-1.0F, -1.0F, -2.0F, 6.0F, 10.0F, 4.0F), ModelTransform.pivot(-1.9F, 12.0F, 0.0F));
      modelPartData.addChild(EntityModelPartNames.RIGHT_WING, ModelPartBuilder.create().uv(0, 32).cuboid(-20.0F, 0.0F, 0.0F, 20.0F, 12.0F, 1.0F), ModelTransform.NONE);
      modelPartData.addChild(EntityModelPartNames.LEFT_WING, ModelPartBuilder.create().uv(0, 32).mirrored().cuboid(0.0F, 0.0F, 0.0F, 20.0F, 12.0F, 1.0F), ModelTransform.NONE);
      return TexturedModelData.of(modelData, 64, 64);
   }

   protected Iterable<ModelPart> getBodyParts() {
      return Iterables.concat(super.getBodyParts(), ImmutableList.of(this.rightWing, this.leftWing));
   }

   public void setAngles(VexEntity vexEntity, float f, float g, float h, float i, float j) {
      super.setAngles((LivingEntity)vexEntity, f, g, h, i, j);
      if (vexEntity.isCharging()) {
         if (vexEntity.getMainHandStack().isEmpty()) {
            this.rightArm.pitch = 4.712389F;
            this.leftArm.pitch = 4.712389F;
         } else if (vexEntity.getMainArm() == Arm.RIGHT) {
            this.rightArm.pitch = 3.7699115F;
         } else {
            this.leftArm.pitch = 3.7699115F;
         }
      }

      ModelPart var10000 = this.rightLeg;
      var10000.pitch += 0.62831855F;
      this.rightWing.pivotZ = 2.0F;
      this.leftWing.pivotZ = 2.0F;
      this.rightWing.pivotY = 1.0F;
      this.leftWing.pivotY = 1.0F;
      this.rightWing.yaw = 0.47123894F + MathHelper.cos(h * 45.836624F * 0.017453292F) * 3.1415927F * 0.05F;
      this.leftWing.yaw = -this.rightWing.yaw;
      this.leftWing.roll = -0.47123894F;
      this.leftWing.pitch = 0.47123894F;
      this.rightWing.pitch = 0.47123894F;
      this.rightWing.roll = 0.47123894F;
   }
}
