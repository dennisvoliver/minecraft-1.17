package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleTypes;

@Environment(EnvType.CLIENT)
public class LavaEmberParticle extends SpriteBillboardParticle {
   LavaEmberParticle(ClientWorld clientWorld, double d, double e, double f) {
      super(clientWorld, d, e, f, 0.0D, 0.0D, 0.0D);
      this.gravityStrength = 0.75F;
      this.field_28786 = 0.999F;
      this.velocityX *= 0.800000011920929D;
      this.velocityY *= 0.800000011920929D;
      this.velocityZ *= 0.800000011920929D;
      this.velocityY = (double)(this.random.nextFloat() * 0.4F + 0.05F);
      this.scale *= this.random.nextFloat() * 2.0F + 0.2F;
      this.maxAge = (int)(16.0D / (Math.random() * 0.8D + 0.2D));
   }

   public ParticleTextureSheet getType() {
      return ParticleTextureSheet.PARTICLE_SHEET_OPAQUE;
   }

   public int getBrightness(float tint) {
      int i = super.getBrightness(tint);
      int j = true;
      int k = i >> 16 & 255;
      return 240 | k << 16;
   }

   public float getSize(float tickDelta) {
      float f = ((float)this.age + tickDelta) / (float)this.maxAge;
      return this.scale * (1.0F - f * f);
   }

   public void tick() {
      super.tick();
      if (!this.dead) {
         float f = (float)this.age / (float)this.maxAge;
         if (this.random.nextFloat() > f) {
            this.world.addParticle(ParticleTypes.SMOKE, this.x, this.y, this.z, this.velocityX, this.velocityY, this.velocityZ);
         }
      }

   }

   @Environment(EnvType.CLIENT)
   public static class Factory implements ParticleFactory<DefaultParticleType> {
      private final SpriteProvider spriteProvider;

      public Factory(SpriteProvider spriteProvider) {
         this.spriteProvider = spriteProvider;
      }

      public Particle createParticle(DefaultParticleType defaultParticleType, ClientWorld clientWorld, double d, double e, double f, double g, double h, double i) {
         LavaEmberParticle lavaEmberParticle = new LavaEmberParticle(clientWorld, d, e, f);
         lavaEmberParticle.setSprite(this.spriteProvider);
         return lavaEmberParticle;
      }
   }
}
