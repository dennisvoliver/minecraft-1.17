package net.minecraft.client.render.entity.model;

import java.util.Arrays;
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

/**
 * Represents the model of a blaze-like entity.
 * This model is not tied to a specific entity.
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
 *   <td>{@code part0}</td><td>{@linkplain #root Root part}</td><td>{@link #rods rods[0]}</td>
 * </tr>
 * <tr>
 *   <td>{@code part1}</td><td>{@linkplain #root Root part}</td><td>{@link #rods rods[1]}</td>
 * </tr>
 * <tr>
 *   <td>{@code part2}</td><td>{@linkplain #root Root part}</td><td>{@link #rods rods[2]}</td>
 * </tr>
 * <tr>
 *   <td>{@code part3}</td><td>{@linkplain #root Root part}</td><td>{@link #rods rods[3]}</td>
 * </tr>
 * <tr>
 *   <td>{@code part4}</td><td>{@linkplain #root Root part}</td><td>{@link #rods rods[4]}</td>
 * </tr>
 * <tr>
 *   <td>{@code part5}</td><td>{@linkplain #root Root part}</td><td>{@link #rods rods[5]}</td>
 * </tr>
 * <tr>
 *   <td>{@code part6}</td><td>{@linkplain #root Root part}</td><td>{@link #rods rods[6]}</td>
 * </tr>
 * <tr>
 *   <td>{@code part7}</td><td>{@linkplain #root Root part}</td><td>{@link #rods rods[7]}</td>
 * </tr>
 * <tr>
 *   <td>{@code part8}</td><td>{@linkplain #root Root part}</td><td>{@link #rods rods[8]}</td>
 * </tr>
 * <tr>
 *   <td>{@code part9}</td><td>{@linkplain #root Root part}</td><td>{@link #rods rods[9]}</td>
 * </tr>
 * <tr>
 *   <td>{@code part10}</td><td>{@linkplain #root Root part}</td><td>{@link #rods rods[10]}</td>
 * </tr>
 * <tr>
 *   <td>{@code part11}</td><td>{@linkplain #root Root part}</td><td>{@link #rods rods[11]}</td>
 * </tr>
 * </table>
 * </div>
 */
@Environment(EnvType.CLIENT)
public class BlazeEntityModel<T extends Entity> extends SinglePartEntityModel<T> {
   private final ModelPart root;
   private final ModelPart[] rods;
   private final ModelPart head;

   public BlazeEntityModel(ModelPart root) {
      this.root = root;
      this.head = root.getChild(EntityModelPartNames.HEAD);
      this.rods = new ModelPart[12];
      Arrays.setAll(this.rods, (index) -> {
         return root.getChild(getRodName(index));
      });
   }

   private static String getRodName(int index) {
      return "part" + index;
   }

   public static TexturedModelData getTexturedModelData() {
      ModelData modelData = new ModelData();
      ModelPartData modelPartData = modelData.getRoot();
      modelPartData.addChild(EntityModelPartNames.HEAD, ModelPartBuilder.create().uv(0, 0).cuboid(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F), ModelTransform.NONE);
      float f = 0.0F;
      ModelPartBuilder modelPartBuilder = ModelPartBuilder.create().uv(0, 16).cuboid(0.0F, 0.0F, 0.0F, 2.0F, 8.0F, 2.0F);

      int o;
      float p;
      float q;
      float r;
      for(o = 0; o < 4; ++o) {
         p = MathHelper.cos(f) * 9.0F;
         q = -2.0F + MathHelper.cos((float)(o * 2) * 0.25F);
         r = MathHelper.sin(f) * 9.0F;
         modelPartData.addChild(getRodName(o), modelPartBuilder, ModelTransform.pivot(p, q, r));
         ++f;
      }

      f = 0.7853982F;

      for(o = 4; o < 8; ++o) {
         p = MathHelper.cos(f) * 7.0F;
         q = 2.0F + MathHelper.cos((float)(o * 2) * 0.25F);
         r = MathHelper.sin(f) * 7.0F;
         modelPartData.addChild(getRodName(o), modelPartBuilder, ModelTransform.pivot(p, q, r));
         ++f;
      }

      f = 0.47123894F;

      for(o = 8; o < 12; ++o) {
         p = MathHelper.cos(f) * 5.0F;
         q = 11.0F + MathHelper.cos((float)o * 1.5F * 0.5F);
         r = MathHelper.sin(f) * 5.0F;
         modelPartData.addChild(getRodName(o), modelPartBuilder, ModelTransform.pivot(p, q, r));
         ++f;
      }

      return TexturedModelData.of(modelData, 64, 32);
   }

   public ModelPart getPart() {
      return this.root;
   }

   public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
      float f = animationProgress * 3.1415927F * -0.1F;

      int k;
      for(k = 0; k < 4; ++k) {
         this.rods[k].pivotY = -2.0F + MathHelper.cos(((float)(k * 2) + animationProgress) * 0.25F);
         this.rods[k].pivotX = MathHelper.cos(f) * 9.0F;
         this.rods[k].pivotZ = MathHelper.sin(f) * 9.0F;
         ++f;
      }

      f = 0.7853982F + animationProgress * 3.1415927F * 0.03F;

      for(k = 4; k < 8; ++k) {
         this.rods[k].pivotY = 2.0F + MathHelper.cos(((float)(k * 2) + animationProgress) * 0.25F);
         this.rods[k].pivotX = MathHelper.cos(f) * 7.0F;
         this.rods[k].pivotZ = MathHelper.sin(f) * 7.0F;
         ++f;
      }

      f = 0.47123894F + animationProgress * 3.1415927F * -0.05F;

      for(k = 8; k < 12; ++k) {
         this.rods[k].pivotY = 11.0F + MathHelper.cos(((float)k * 1.5F + animationProgress) * 0.5F);
         this.rods[k].pivotX = MathHelper.cos(f) * 5.0F;
         this.rods[k].pivotZ = MathHelper.sin(f) * 5.0F;
         ++f;
      }

      this.head.yaw = headYaw * 0.017453292F;
      this.head.pitch = headPitch * 0.017453292F;
   }
}
