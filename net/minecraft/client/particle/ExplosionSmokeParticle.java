package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;

@Environment(EnvType.CLIENT)
public class ExplosionSmokeParticle extends SpriteBillboardParticle {
   private final SpriteProvider spriteProvider;

   protected ExplosionSmokeParticle(ClientWorld world, double x, double y, double z, double d, double e, double f, SpriteProvider spriteProvider) {
      super(world, x, y, z);
      this.gravityStrength = -0.1F;
      this.field_28786 = 0.9F;
      this.spriteProvider = spriteProvider;
      this.velocityX = d + (Math.random() * 2.0D - 1.0D) * 0.05000000074505806D;
      this.velocityY = e + (Math.random() * 2.0D - 1.0D) * 0.05000000074505806D;
      this.velocityZ = f + (Math.random() * 2.0D - 1.0D) * 0.05000000074505806D;
      float g = this.random.nextFloat() * 0.3F + 0.7F;
      this.colorRed = g;
      this.colorGreen = g;
      this.colorBlue = g;
      this.scale = 0.1F * (this.random.nextFloat() * this.random.nextFloat() * 6.0F + 1.0F);
      this.maxAge = (int)(16.0D / ((double)this.random.nextFloat() * 0.8D + 0.2D)) + 2;
      this.setSpriteForAge(spriteProvider);
   }

   public ParticleTextureSheet getType() {
      return ParticleTextureSheet.PARTICLE_SHEET_OPAQUE;
   }

   public void tick() {
      super.tick();
      this.setSpriteForAge(this.spriteProvider);
   }

   @Environment(EnvType.CLIENT)
   public static class Factory implements ParticleFactory<DefaultParticleType> {
      private final SpriteProvider spriteProvider;

      public Factory(SpriteProvider spriteProvider) {
         this.spriteProvider = spriteProvider;
      }

      public Particle createParticle(DefaultParticleType defaultParticleType, ClientWorld clientWorld, double d, double e, double f, double g, double h, double i) {
         return new ExplosionSmokeParticle(clientWorld, d, e, f, g, h, i, this.spriteProvider);
      }
   }
}
