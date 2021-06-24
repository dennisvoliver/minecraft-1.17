package net.minecraft.world.event;

import com.mojang.serialization.Codec;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public interface PositionSourceType<T extends PositionSource> {
   PositionSourceType<BlockPositionSource> BLOCK = register("block", new BlockPositionSource.Type());
   PositionSourceType<EntityPositionSource> ENTITY = register("entity", new EntityPositionSource.Type());

   T readFromBuf(PacketByteBuf buf);

   void writeToBuf(PacketByteBuf buf, T positionSource);

   Codec<T> getCodec();

   static <S extends PositionSourceType<T>, T extends PositionSource> S register(String id, S positionSourceType) {
      return (PositionSourceType)Registry.register(Registry.POSITION_SOURCE_TYPE, (String)id, positionSourceType);
   }

   static PositionSource read(PacketByteBuf buf) {
      Identifier identifier = buf.readIdentifier();
      return ((PositionSourceType)Registry.POSITION_SOURCE_TYPE.getOrEmpty(identifier).orElseThrow(() -> {
         return new IllegalArgumentException("Unknown position source type " + identifier);
      })).readFromBuf(buf);
   }

   static <T extends PositionSource> void write(T positionSource, PacketByteBuf buf) {
      buf.writeIdentifier(Registry.POSITION_SOURCE_TYPE.getId(positionSource.getType()));
      positionSource.getType().writeToBuf(buf, positionSource);
   }
}
