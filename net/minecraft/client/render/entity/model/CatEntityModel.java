package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelUtil;
import net.minecraft.entity.passive.CatEntity;

/**
 * Represents the model of a {@linkplain CatEntity}.
 * 
 * <p>Inherits the {@link OcelotEntityModel}.
 */
@Environment(EnvType.CLIENT)
public class CatEntityModel<T extends CatEntity> extends OcelotEntityModel<T> {
   private float sleepAnimation;
   private float tailCurlAnimation;
   private float headDownAnimation;

   public CatEntityModel(ModelPart modelPart) {
      super(modelPart);
   }

   public void animateModel(T catEntity, float f, float g, float h) {
      this.sleepAnimation = catEntity.getSleepAnimation(h);
      this.tailCurlAnimation = catEntity.getTailCurlAnimation(h);
      this.headDownAnimation = catEntity.getHeadDownAnimation(h);
      if (this.sleepAnimation <= 0.0F) {
         this.head.pitch = 0.0F;
         this.head.roll = 0.0F;
         this.leftFrontLeg.pitch = 0.0F;
         this.leftFrontLeg.roll = 0.0F;
         this.rightFrontLeg.pitch = 0.0F;
         this.rightFrontLeg.roll = 0.0F;
         this.rightFrontLeg.pivotX = -1.2F;
         this.leftHindLeg.pitch = 0.0F;
         this.rightHindLeg.pitch = 0.0F;
         this.rightHindLeg.roll = 0.0F;
         this.rightHindLeg.pivotX = -1.1F;
         this.rightHindLeg.pivotY = 18.0F;
      }

      super.animateModel(catEntity, f, g, h);
      if (catEntity.isInSittingPose()) {
         this.body.pitch = 0.7853982F;
         ModelPart var10000 = this.body;
         var10000.pivotY += -4.0F;
         var10000 = this.body;
         var10000.pivotZ += 5.0F;
         var10000 = this.head;
         var10000.pivotY += -3.3F;
         ++this.head.pivotZ;
         var10000 = this.upperTail;
         var10000.pivotY += 8.0F;
         var10000 = this.upperTail;
         var10000.pivotZ += -2.0F;
         var10000 = this.lowerTail;
         var10000.pivotY += 2.0F;
         var10000 = this.lowerTail;
         var10000.pivotZ += -0.8F;
         this.upperTail.pitch = 1.7278761F;
         this.lowerTail.pitch = 2.670354F;
         this.leftFrontLeg.pitch = -0.15707964F;
         this.leftFrontLeg.pivotY = 16.1F;
         this.leftFrontLeg.pivotZ = -7.0F;
         this.rightFrontLeg.pitch = -0.15707964F;
         this.rightFrontLeg.pivotY = 16.1F;
         this.rightFrontLeg.pivotZ = -7.0F;
         this.leftHindLeg.pitch = -1.5707964F;
         this.leftHindLeg.pivotY = 21.0F;
         this.leftHindLeg.pivotZ = 1.0F;
         this.rightHindLeg.pitch = -1.5707964F;
         this.rightHindLeg.pivotY = 21.0F;
         this.rightHindLeg.pivotZ = 1.0F;
         this.animationState = 3;
      }

   }

   public void setAngles(T catEntity, float f, float g, float h, float i, float j) {
      super.setAngles(catEntity, f, g, h, i, j);
      if (this.sleepAnimation > 0.0F) {
         this.head.roll = ModelUtil.interpolateAngle(this.head.roll, -1.2707963F, this.sleepAnimation);
         this.head.yaw = ModelUtil.interpolateAngle(this.head.yaw, 1.2707963F, this.sleepAnimation);
         this.leftFrontLeg.pitch = -1.2707963F;
         this.rightFrontLeg.pitch = -0.47079635F;
         this.rightFrontLeg.roll = -0.2F;
         this.rightFrontLeg.pivotX = -0.2F;
         this.leftHindLeg.pitch = -0.4F;
         this.rightHindLeg.pitch = 0.5F;
         this.rightHindLeg.roll = -0.5F;
         this.rightHindLeg.pivotX = -0.3F;
         this.rightHindLeg.pivotY = 20.0F;
         this.upperTail.pitch = ModelUtil.interpolateAngle(this.upperTail.pitch, 0.8F, this.tailCurlAnimation);
         this.lowerTail.pitch = ModelUtil.interpolateAngle(this.lowerTail.pitch, -0.4F, this.tailCurlAnimation);
      }

      if (this.headDownAnimation > 0.0F) {
         this.head.pitch = ModelUtil.interpolateAngle(this.head.pitch, -0.58177644F, this.headDownAnimation);
      }

   }
}
