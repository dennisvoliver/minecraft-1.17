package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class RavagerEntityModel extends SinglePartEntityModel<RavagerEntity> {
   private final ModelPart root;
   private final ModelPart head;
   private final ModelPart jaw;
   private final ModelPart rightHindLeg;
   private final ModelPart leftHindLeg;
   private final ModelPart rightFrontLeg;
   private final ModelPart leftFrontLeg;
   private final ModelPart neck;

   public RavagerEntityModel(ModelPart root) {
      this.root = root;
      this.neck = root.getChild(EntityModelPartNames.NECK);
      this.head = this.neck.getChild(EntityModelPartNames.HEAD);
      this.jaw = this.head.getChild(EntityModelPartNames.MOUTH);
      this.rightHindLeg = root.getChild(EntityModelPartNames.RIGHT_HIND_LEG);
      this.leftHindLeg = root.getChild(EntityModelPartNames.LEFT_HIND_LEG);
      this.rightFrontLeg = root.getChild(EntityModelPartNames.RIGHT_FRONT_LEG);
      this.leftFrontLeg = root.getChild(EntityModelPartNames.LEFT_FRONT_LEG);
   }

   public static TexturedModelData getTexturedModelData() {
      ModelData modelData = new ModelData();
      ModelPartData modelPartData = modelData.getRoot();
      int i = true;
      ModelPartData modelPartData2 = modelPartData.addChild(EntityModelPartNames.NECK, ModelPartBuilder.create().uv(68, 73).cuboid(-5.0F, -1.0F, -18.0F, 10.0F, 10.0F, 18.0F), ModelTransform.pivot(0.0F, -7.0F, 5.5F));
      ModelPartData modelPartData3 = modelPartData2.addChild(EntityModelPartNames.HEAD, ModelPartBuilder.create().uv(0, 0).cuboid(-8.0F, -20.0F, -14.0F, 16.0F, 20.0F, 16.0F).uv(0, 0).cuboid(-2.0F, -6.0F, -18.0F, 4.0F, 8.0F, 4.0F), ModelTransform.pivot(0.0F, 16.0F, -17.0F));
      modelPartData3.addChild(EntityModelPartNames.RIGHT_HORN, ModelPartBuilder.create().uv(74, 55).cuboid(0.0F, -14.0F, -2.0F, 2.0F, 14.0F, 4.0F), ModelTransform.of(-10.0F, -14.0F, -8.0F, 1.0995574F, 0.0F, 0.0F));
      modelPartData3.addChild(EntityModelPartNames.LEFT_HORN, ModelPartBuilder.create().uv(74, 55).mirrored().cuboid(0.0F, -14.0F, -2.0F, 2.0F, 14.0F, 4.0F), ModelTransform.of(8.0F, -14.0F, -8.0F, 1.0995574F, 0.0F, 0.0F));
      modelPartData3.addChild(EntityModelPartNames.MOUTH, ModelPartBuilder.create().uv(0, 36).cuboid(-8.0F, 0.0F, -16.0F, 16.0F, 3.0F, 16.0F), ModelTransform.pivot(0.0F, -2.0F, 2.0F));
      modelPartData.addChild(EntityModelPartNames.BODY, ModelPartBuilder.create().uv(0, 55).cuboid(-7.0F, -10.0F, -7.0F, 14.0F, 16.0F, 20.0F).uv(0, 91).cuboid(-6.0F, 6.0F, -7.0F, 12.0F, 13.0F, 18.0F), ModelTransform.of(0.0F, 1.0F, 2.0F, 1.5707964F, 0.0F, 0.0F));
      modelPartData.addChild(EntityModelPartNames.RIGHT_HIND_LEG, ModelPartBuilder.create().uv(96, 0).cuboid(-4.0F, 0.0F, -4.0F, 8.0F, 37.0F, 8.0F), ModelTransform.pivot(-8.0F, -13.0F, 18.0F));
      modelPartData.addChild(EntityModelPartNames.LEFT_HIND_LEG, ModelPartBuilder.create().uv(96, 0).mirrored().cuboid(-4.0F, 0.0F, -4.0F, 8.0F, 37.0F, 8.0F), ModelTransform.pivot(8.0F, -13.0F, 18.0F));
      modelPartData.addChild(EntityModelPartNames.RIGHT_FRONT_LEG, ModelPartBuilder.create().uv(64, 0).cuboid(-4.0F, 0.0F, -4.0F, 8.0F, 37.0F, 8.0F), ModelTransform.pivot(-8.0F, -13.0F, -5.0F));
      modelPartData.addChild(EntityModelPartNames.LEFT_FRONT_LEG, ModelPartBuilder.create().uv(64, 0).mirrored().cuboid(-4.0F, 0.0F, -4.0F, 8.0F, 37.0F, 8.0F), ModelTransform.pivot(8.0F, -13.0F, -5.0F));
      return TexturedModelData.of(modelData, 128, 128);
   }

   public ModelPart getPart() {
      return this.root;
   }

   public void setAngles(RavagerEntity ravagerEntity, float f, float g, float h, float i, float j) {
      this.head.pitch = j * 0.017453292F;
      this.head.yaw = i * 0.017453292F;
      float k = 0.4F * g;
      this.rightHindLeg.pitch = MathHelper.cos(f * 0.6662F) * k;
      this.leftHindLeg.pitch = MathHelper.cos(f * 0.6662F + 3.1415927F) * k;
      this.rightFrontLeg.pitch = MathHelper.cos(f * 0.6662F + 3.1415927F) * k;
      this.leftFrontLeg.pitch = MathHelper.cos(f * 0.6662F) * k;
   }

   public void animateModel(RavagerEntity ravagerEntity, float f, float g, float h) {
      super.animateModel(ravagerEntity, f, g, h);
      int i = ravagerEntity.getStunTick();
      int j = ravagerEntity.getRoarTick();
      int k = true;
      int l = ravagerEntity.getAttackTick();
      int m = true;
      float n;
      float o;
      float u;
      if (l > 0) {
         n = MathHelper.wrap((float)l - h, 10.0F);
         o = (1.0F + n) * 0.5F;
         float p = o * o * o * 12.0F;
         u = p * MathHelper.sin(this.neck.pitch);
         this.neck.pivotZ = -6.5F + p;
         this.neck.pivotY = -7.0F - u;
         float r = MathHelper.sin(((float)l - h) / 10.0F * 3.1415927F * 0.25F);
         this.jaw.pitch = 1.5707964F * r;
         if (l > 5) {
            this.jaw.pitch = MathHelper.sin(((float)(-4 + l) - h) / 4.0F) * 3.1415927F * 0.4F;
         } else {
            this.jaw.pitch = 0.15707964F * MathHelper.sin(3.1415927F * ((float)l - h) / 10.0F);
         }
      } else {
         n = -1.0F;
         o = -1.0F * MathHelper.sin(this.neck.pitch);
         this.neck.pivotX = 0.0F;
         this.neck.pivotY = -7.0F - o;
         this.neck.pivotZ = 5.5F;
         boolean bl = i > 0;
         this.neck.pitch = bl ? 0.21991149F : 0.0F;
         this.jaw.pitch = 3.1415927F * (bl ? 0.05F : 0.01F);
         if (bl) {
            double d = (double)i / 40.0D;
            this.neck.pivotX = (float)Math.sin(d * 10.0D) * 3.0F;
         } else if (j > 0) {
            u = MathHelper.sin(((float)(20 - j) - h) / 20.0F * 3.1415927F * 0.25F);
            this.jaw.pitch = 1.5707964F * u;
         }
      }

   }
}
