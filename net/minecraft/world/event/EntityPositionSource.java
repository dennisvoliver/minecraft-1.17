package net.minecraft.world.event;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class EntityPositionSource implements PositionSource {
   public static final Codec<EntityPositionSource> CODEC = RecordCodecBuilder.create((instance) -> {
      return instance.group(Codec.INT.fieldOf("source_entity_id").forGetter((entityPositionSource) -> {
         return entityPositionSource.entityId;
      })).apply(instance, (Function)(EntityPositionSource::new));
   });
   final int entityId;
   private Optional<Entity> entity = Optional.empty();

   public EntityPositionSource(int entityId) {
      this.entityId = entityId;
   }

   public Optional<BlockPos> getPos(World world) {
      if (!this.entity.isPresent()) {
         this.entity = Optional.ofNullable(world.getEntityById(this.entityId));
      }

      return this.entity.map(Entity::getBlockPos);
   }

   public PositionSourceType<?> getType() {
      return PositionSourceType.ENTITY;
   }

   public static class Type implements PositionSourceType<EntityPositionSource> {
      public EntityPositionSource readFromBuf(PacketByteBuf packetByteBuf) {
         return new EntityPositionSource(packetByteBuf.readVarInt());
      }

      public void writeToBuf(PacketByteBuf packetByteBuf, EntityPositionSource entityPositionSource) {
         packetByteBuf.writeVarInt(entityPositionSource.entityId);
      }

      public Codec<EntityPositionSource> getCodec() {
         return EntityPositionSource.CODEC;
      }
   }
}
