package net.minecraft.client.particle;

import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class GlowParticle extends SpriteBillboardParticle {
   static final Random RANDOM = new Random();
   private final SpriteProvider spriteProvider;

   GlowParticle(ClientWorld clientWorld, double d, double e, double f, double g, double h, double i, SpriteProvider spriteProvider) {
      super(clientWorld, d, e, f, g, h, i);
      this.field_28786 = 0.96F;
      this.field_28787 = true;
      this.spriteProvider = spriteProvider;
      this.scale *= 0.75F;
      this.collidesWithWorld = false;
      this.setSpriteForAge(spriteProvider);
   }

   public ParticleTextureSheet getType() {
      return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
   }

   public int getBrightness(float tint) {
      float f = ((float)this.age + tint) / (float)this.maxAge;
      f = MathHelper.clamp(f, 0.0F, 1.0F);
      int i = super.getBrightness(tint);
      int j = i & 255;
      int k = i >> 16 & 255;
      j += (int)(f * 15.0F * 16.0F);
      if (j > 240) {
         j = 240;
      }

      return j | k << 16;
   }

   public void tick() {
      super.tick();
      this.setSpriteForAge(this.spriteProvider);
   }

   @Environment(EnvType.CLIENT)
   public static class ScrapeFactory implements ParticleFactory<DefaultParticleType> {
      private final double field_29573 = 0.01D;
      private final SpriteProvider spriteProvider;

      public ScrapeFactory(SpriteProvider spriteProvider) {
         this.spriteProvider = spriteProvider;
      }

      public Particle createParticle(DefaultParticleType defaultParticleType, ClientWorld clientWorld, double d, double e, double f, double g, double h, double i) {
         GlowParticle glowParticle = new GlowParticle(clientWorld, d, e, f, 0.0D, 0.0D, 0.0D, this.spriteProvider);
         if (clientWorld.random.nextBoolean()) {
            glowParticle.setColor(0.29F, 0.58F, 0.51F);
         } else {
            glowParticle.setColor(0.43F, 0.77F, 0.62F);
         }

         glowParticle.setVelocity(g * 0.01D, h * 0.01D, i * 0.01D);
         int j = true;
         int k = true;
         glowParticle.setMaxAge(clientWorld.random.nextInt(30) + 10);
         return glowParticle;
      }
   }

   @Environment(EnvType.CLIENT)
   public static class ElectricSparkFactory implements ParticleFactory<DefaultParticleType> {
      private final double field_29570 = 0.25D;
      private final SpriteProvider spriteProvider;

      public ElectricSparkFactory(SpriteProvider spriteProvider) {
         this.spriteProvider = spriteProvider;
      }

      public Particle createParticle(DefaultParticleType defaultParticleType, ClientWorld clientWorld, double d, double e, double f, double g, double h, double i) {
         GlowParticle glowParticle = new GlowParticle(clientWorld, d, e, f, 0.0D, 0.0D, 0.0D, this.spriteProvider);
         glowParticle.setColor(1.0F, 0.9F, 1.0F);
         glowParticle.setVelocity(g * 0.25D, h * 0.25D, i * 0.25D);
         int j = true;
         int k = true;
         glowParticle.setMaxAge(clientWorld.random.nextInt(2) + 2);
         return glowParticle;
      }
   }

   @Environment(EnvType.CLIENT)
   public static class WaxOffFactory implements ParticleFactory<DefaultParticleType> {
      private final double field_29575 = 0.01D;
      private final SpriteProvider spriteProvider;

      public WaxOffFactory(SpriteProvider spriteProvider) {
         this.spriteProvider = spriteProvider;
      }

      public Particle createParticle(DefaultParticleType defaultParticleType, ClientWorld clientWorld, double d, double e, double f, double g, double h, double i) {
         GlowParticle glowParticle = new GlowParticle(clientWorld, d, e, f, 0.0D, 0.0D, 0.0D, this.spriteProvider);
         glowParticle.setColor(1.0F, 0.9F, 1.0F);
         glowParticle.setVelocity(g * 0.01D / 2.0D, h * 0.01D, i * 0.01D / 2.0D);
         int j = true;
         int k = true;
         glowParticle.setMaxAge(clientWorld.random.nextInt(30) + 10);
         return glowParticle;
      }
   }

   @Environment(EnvType.CLIENT)
   public static class WaxOnFactory implements ParticleFactory<DefaultParticleType> {
      private final double field_29577 = 0.01D;
      private final SpriteProvider spriteProvider;

      public WaxOnFactory(SpriteProvider spriteProvider) {
         this.spriteProvider = spriteProvider;
      }

      public Particle createParticle(DefaultParticleType defaultParticleType, ClientWorld clientWorld, double d, double e, double f, double g, double h, double i) {
         GlowParticle glowParticle = new GlowParticle(clientWorld, d, e, f, 0.0D, 0.0D, 0.0D, this.spriteProvider);
         glowParticle.setColor(0.91F, 0.55F, 0.08F);
         glowParticle.setVelocity(g * 0.01D / 2.0D, h * 0.01D, i * 0.01D / 2.0D);
         int j = true;
         int k = true;
         glowParticle.setMaxAge(clientWorld.random.nextInt(30) + 10);
         return glowParticle;
      }
   }

   @Environment(EnvType.CLIENT)
   public static class GlowFactory implements ParticleFactory<DefaultParticleType> {
      private final SpriteProvider spriteProvider;

      public GlowFactory(SpriteProvider spriteProvider) {
         this.spriteProvider = spriteProvider;
      }

      public Particle createParticle(DefaultParticleType defaultParticleType, ClientWorld clientWorld, double d, double e, double f, double g, double h, double i) {
         GlowParticle glowParticle = new GlowParticle(clientWorld, d, e, f, 0.5D - GlowParticle.RANDOM.nextDouble(), h, 0.5D - GlowParticle.RANDOM.nextDouble(), this.spriteProvider);
         if (clientWorld.random.nextBoolean()) {
            glowParticle.setColor(0.6F, 1.0F, 0.8F);
         } else {
            glowParticle.setColor(0.08F, 0.4F, 0.4F);
         }

         glowParticle.velocityY *= 0.20000000298023224D;
         if (g == 0.0D && i == 0.0D) {
            glowParticle.velocityX *= 0.10000000149011612D;
            glowParticle.velocityZ *= 0.10000000149011612D;
         }

         glowParticle.setMaxAge((int)(8.0D / (clientWorld.random.nextDouble() * 0.8D + 0.2D)));
         return glowParticle;
      }
   }
}
