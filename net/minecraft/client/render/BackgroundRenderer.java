package net.minecraft.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.CubicSampler;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;

@Environment(EnvType.CLIENT)
public class BackgroundRenderer {
   private static final int field_32685 = 192;
   public static final float field_32684 = 5000.0F;
   private static float red;
   private static float green;
   private static float blue;
   private static int waterFogColor = -1;
   private static int nextWaterFogColor = -1;
   private static long lastWaterFogColorUpdateTime = -1L;

   public static void render(Camera camera, float tickDelta, ClientWorld world, int i, float f) {
      CameraSubmersionType cameraSubmersionType = camera.getSubmersionType();
      Entity entity = camera.getFocusedEntity();
      int af;
      float aa;
      float ad;
      float z;
      float aj;
      float ak;
      if (cameraSubmersionType == CameraSubmersionType.WATER) {
         long l = Util.getMeasuringTimeMs();
         af = world.getBiome(new BlockPos(camera.getPos())).getWaterFogColor();
         if (lastWaterFogColorUpdateTime < 0L) {
            waterFogColor = af;
            nextWaterFogColor = af;
            lastWaterFogColorUpdateTime = l;
         }

         int k = waterFogColor >> 16 & 255;
         int m = waterFogColor >> 8 & 255;
         int n = waterFogColor & 255;
         int o = nextWaterFogColor >> 16 & 255;
         int p = nextWaterFogColor >> 8 & 255;
         int q = nextWaterFogColor & 255;
         aa = MathHelper.clamp((float)(l - lastWaterFogColorUpdateTime) / 5000.0F, 0.0F, 1.0F);
         ad = MathHelper.lerp(aa, (float)o, (float)k);
         z = MathHelper.lerp(aa, (float)p, (float)m);
         float s = MathHelper.lerp(aa, (float)q, (float)n);
         red = ad / 255.0F;
         green = z / 255.0F;
         blue = s / 255.0F;
         if (waterFogColor != af) {
            waterFogColor = af;
            nextWaterFogColor = MathHelper.floor(ad) << 16 | MathHelper.floor(z) << 8 | MathHelper.floor(s);
            lastWaterFogColorUpdateTime = l;
         }
      } else if (cameraSubmersionType == CameraSubmersionType.LAVA) {
         red = 0.6F;
         green = 0.1F;
         blue = 0.0F;
         lastWaterFogColorUpdateTime = -1L;
      } else if (cameraSubmersionType == CameraSubmersionType.POWDER_SNOW) {
         red = 0.623F;
         green = 0.734F;
         blue = 0.785F;
         lastWaterFogColorUpdateTime = -1L;
         RenderSystem.clearColor(red, green, blue, 0.0F);
      } else {
         float t = 0.25F + 0.75F * (float)i / 32.0F;
         t = 1.0F - (float)Math.pow((double)t, 0.25D);
         Vec3d vec3d = world.method_23777(camera.getPos(), tickDelta);
         aj = (float)vec3d.x;
         ak = (float)vec3d.y;
         float w = (float)vec3d.z;
         float x = MathHelper.clamp(MathHelper.cos(world.getSkyAngle(tickDelta) * 6.2831855F) * 2.0F + 0.5F, 0.0F, 1.0F);
         BiomeAccess biomeAccess = world.getBiomeAccess();
         Vec3d vec3d2 = camera.getPos().subtract(2.0D, 2.0D, 2.0D).multiply(0.25D);
         Vec3d vec3d3 = CubicSampler.sampleColor(vec3d2, (ix, j, kx) -> {
            return world.getSkyProperties().adjustFogColor(Vec3d.unpackRgb(biomeAccess.getBiomeForNoiseGen(ix, j, kx).getFogColor()), x);
         });
         red = (float)vec3d3.getX();
         green = (float)vec3d3.getY();
         blue = (float)vec3d3.getZ();
         if (i >= 4) {
            aa = MathHelper.sin(world.getSkyAngleRadians(tickDelta)) > 0.0F ? -1.0F : 1.0F;
            Vec3f vec3f = new Vec3f(aa, 0.0F, 0.0F);
            z = camera.getHorizontalPlane().dot(vec3f);
            if (z < 0.0F) {
               z = 0.0F;
            }

            if (z > 0.0F) {
               float[] fs = world.getSkyProperties().getFogColorOverride(world.getSkyAngle(tickDelta), tickDelta);
               if (fs != null) {
                  z *= fs[3];
                  red = red * (1.0F - z) + fs[0] * z;
                  green = green * (1.0F - z) + fs[1] * z;
                  blue = blue * (1.0F - z) + fs[2] * z;
               }
            }
         }

         red += (aj - red) * t;
         green += (ak - green) * t;
         blue += (w - blue) * t;
         aa = world.getRainGradient(tickDelta);
         if (aa > 0.0F) {
            ad = 1.0F - aa * 0.5F;
            z = 1.0F - aa * 0.4F;
            red *= ad;
            green *= ad;
            blue *= z;
         }

         ad = world.getThunderGradient(tickDelta);
         if (ad > 0.0F) {
            z = 1.0F - ad * 0.5F;
            red *= z;
            green *= z;
            blue *= z;
         }

         lastWaterFogColorUpdateTime = -1L;
      }

      double d = (camera.getPos().y - (double)world.getBottomY()) * world.getLevelProperties().getHorizonShadingRatio();
      if (camera.getFocusedEntity() instanceof LivingEntity && ((LivingEntity)camera.getFocusedEntity()).hasStatusEffect(StatusEffects.BLINDNESS)) {
         af = ((LivingEntity)camera.getFocusedEntity()).getStatusEffect(StatusEffects.BLINDNESS).getDuration();
         if (af < 20) {
            d *= (double)(1.0F - (float)af / 20.0F);
         } else {
            d = 0.0D;
         }
      }

      if (d < 1.0D && cameraSubmersionType != CameraSubmersionType.LAVA) {
         if (d < 0.0D) {
            d = 0.0D;
         }

         d *= d;
         red = (float)((double)red * d);
         green = (float)((double)green * d);
         blue = (float)((double)blue * d);
      }

      if (f > 0.0F) {
         red = red * (1.0F - f) + red * 0.7F * f;
         green = green * (1.0F - f) + green * 0.6F * f;
         blue = blue * (1.0F - f) + blue * 0.6F * f;
      }

      if (cameraSubmersionType == CameraSubmersionType.WATER) {
         if (entity instanceof ClientPlayerEntity) {
            aj = ((ClientPlayerEntity)entity).getUnderwaterVisibility();
         } else {
            aj = 1.0F;
         }
      } else if (entity instanceof LivingEntity && ((LivingEntity)entity).hasStatusEffect(StatusEffects.NIGHT_VISION)) {
         aj = GameRenderer.getNightVisionStrength((LivingEntity)entity, tickDelta);
      } else {
         aj = 0.0F;
      }

      if (red != 0.0F && green != 0.0F && blue != 0.0F) {
         ak = Math.min(1.0F / red, Math.min(1.0F / green, 1.0F / blue));
         red = red * (1.0F - aj) + red * ak * aj;
         green = green * (1.0F - aj) + green * ak * aj;
         blue = blue * (1.0F - aj) + blue * ak * aj;
      }

      RenderSystem.clearColor(red, green, blue, 0.0F);
   }

   public static void method_23792() {
      RenderSystem.setShaderFogStart(Float.MAX_VALUE);
   }

   public static void applyFog(Camera camera, BackgroundRenderer.FogType fogType, float viewDistance, boolean thickFog) {
      CameraSubmersionType cameraSubmersionType = camera.getSubmersionType();
      Entity entity = camera.getFocusedEntity();
      float y;
      if (cameraSubmersionType == CameraSubmersionType.WATER) {
         y = 192.0F;
         if (entity instanceof ClientPlayerEntity) {
            ClientPlayerEntity clientPlayerEntity = (ClientPlayerEntity)entity;
            y *= Math.max(0.25F, clientPlayerEntity.getUnderwaterVisibility());
            Biome biome = clientPlayerEntity.world.getBiome(clientPlayerEntity.getBlockPos());
            if (biome.getCategory() == Biome.Category.SWAMP) {
               y *= 0.85F;
            }
         }

         RenderSystem.setShaderFogStart(-8.0F);
         RenderSystem.setShaderFogEnd(y * 0.5F);
      } else {
         float ab;
         if (cameraSubmersionType == CameraSubmersionType.LAVA) {
            if (entity.isSpectator()) {
               y = -8.0F;
               ab = viewDistance * 0.5F;
            } else if (entity instanceof LivingEntity && ((LivingEntity)entity).hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) {
               y = 0.0F;
               ab = 3.0F;
            } else {
               y = 0.25F;
               ab = 1.0F;
            }
         } else if (entity instanceof LivingEntity && ((LivingEntity)entity).hasStatusEffect(StatusEffects.BLINDNESS)) {
            int m = ((LivingEntity)entity).getStatusEffect(StatusEffects.BLINDNESS).getDuration();
            float n = MathHelper.lerp(Math.min(1.0F, (float)m / 20.0F), viewDistance, 5.0F);
            if (fogType == BackgroundRenderer.FogType.FOG_SKY) {
               y = 0.0F;
               ab = n * 0.8F;
            } else {
               y = n * 0.25F;
               ab = n;
            }
         } else if (cameraSubmersionType == CameraSubmersionType.POWDER_SNOW) {
            if (entity.isSpectator()) {
               y = -8.0F;
               ab = viewDistance * 0.5F;
            } else {
               y = 0.0F;
               ab = 2.0F;
            }
         } else if (thickFog) {
            y = viewDistance * 0.05F;
            ab = Math.min(viewDistance, 192.0F) * 0.5F;
         } else if (fogType == BackgroundRenderer.FogType.FOG_SKY) {
            y = 0.0F;
            ab = viewDistance;
         } else {
            y = viewDistance * 0.75F;
            ab = viewDistance;
         }

         RenderSystem.setShaderFogStart(y);
         RenderSystem.setShaderFogEnd(ab);
      }

   }

   public static void setFogBlack() {
      RenderSystem.setShaderFogColor(red, green, blue);
   }

   @Environment(EnvType.CLIENT)
   public static enum FogType {
      FOG_SKY,
      FOG_TERRAIN;
   }
}
