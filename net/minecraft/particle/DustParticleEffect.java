package net.minecraft.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.BiFunction;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;

public class DustParticleEffect extends AbstractDustParticleEffect {
   public static final Vec3f RED = new Vec3f(Vec3d.unpackRgb(16711680));
   public static final DustParticleEffect DEFAULT;
   public static final Codec<DustParticleEffect> CODEC;
   public static final ParticleEffect.Factory<DustParticleEffect> PARAMETERS_FACTORY;

   public DustParticleEffect(Vec3f vec3f, float f) {
      super(vec3f, f);
   }

   public ParticleType<DustParticleEffect> getType() {
      return ParticleTypes.DUST;
   }

   static {
      DEFAULT = new DustParticleEffect(RED, 1.0F);
      CODEC = RecordCodecBuilder.create((instance) -> {
         return instance.group(Vec3f.CODEC.fieldOf("color").forGetter((dustParticleEffect) -> {
            return dustParticleEffect.color;
         }), Codec.FLOAT.fieldOf("scale").forGetter((dustParticleEffect) -> {
            return dustParticleEffect.scale;
         })).apply(instance, (BiFunction)(DustParticleEffect::new));
      });
      PARAMETERS_FACTORY = new ParticleEffect.Factory<DustParticleEffect>() {
         public DustParticleEffect read(ParticleType<DustParticleEffect> particleType, StringReader stringReader) throws CommandSyntaxException {
            Vec3f vec3f = AbstractDustParticleEffect.readColor(stringReader);
            stringReader.expect(' ');
            float f = stringReader.readFloat();
            return new DustParticleEffect(vec3f, f);
         }

         public DustParticleEffect read(ParticleType<DustParticleEffect> particleType, PacketByteBuf packetByteBuf) {
            return new DustParticleEffect(AbstractDustParticleEffect.readColor(packetByteBuf), packetByteBuf.readFloat());
         }
      };
   }
}
