package net.minecraft.block.entity;

import com.google.common.collect.Iterables;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.properties.Property;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.ChatUtil;
import net.minecraft.util.UserCache;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class SkullBlockEntity extends BlockEntity {
   public static final String SKULL_OWNER_KEY = "SkullOwner";
   @Nullable
   private static UserCache userCache;
   @Nullable
   private static MinecraftSessionService sessionService;
   @Nullable
   private GameProfile owner;
   private int ticksPowered;
   private boolean powered;

   public SkullBlockEntity(BlockPos pos, BlockState state) {
      super(BlockEntityType.SKULL, pos, state);
   }

   public static void setUserCache(UserCache value) {
      userCache = value;
   }

   public static void setSessionService(MinecraftSessionService value) {
      sessionService = value;
   }

   public NbtCompound writeNbt(NbtCompound nbt) {
      super.writeNbt(nbt);
      if (this.owner != null) {
         NbtCompound nbtCompound = new NbtCompound();
         NbtHelper.writeGameProfile(nbtCompound, this.owner);
         nbt.put("SkullOwner", nbtCompound);
      }

      return nbt;
   }

   public void readNbt(NbtCompound nbt) {
      super.readNbt(nbt);
      if (nbt.contains("SkullOwner", 10)) {
         this.setOwner(NbtHelper.toGameProfile(nbt.getCompound("SkullOwner")));
      } else if (nbt.contains("ExtraType", 8)) {
         String string = nbt.getString("ExtraType");
         if (!ChatUtil.isEmpty(string)) {
            this.setOwner(new GameProfile((UUID)null, string));
         }
      }

   }

   public static void tick(World world, BlockPos pos, BlockState state, SkullBlockEntity blockEntity) {
      if (world.isReceivingRedstonePower(pos)) {
         blockEntity.powered = true;
         ++blockEntity.ticksPowered;
      } else {
         blockEntity.powered = false;
      }

   }

   public float getTicksPowered(float tickDelta) {
      return this.powered ? (float)this.ticksPowered + tickDelta : (float)this.ticksPowered;
   }

   @Nullable
   public GameProfile getOwner() {
      return this.owner;
   }

   @Nullable
   public BlockEntityUpdateS2CPacket toUpdatePacket() {
      return new BlockEntityUpdateS2CPacket(this.pos, BlockEntityUpdateS2CPacket.SKULL, this.toInitialChunkDataNbt());
   }

   public NbtCompound toInitialChunkDataNbt() {
      return this.writeNbt(new NbtCompound());
   }

   public void setOwner(@Nullable GameProfile owner) {
      synchronized(this) {
         this.owner = owner;
      }

      this.loadOwnerProperties();
   }

   private void loadOwnerProperties() {
      loadProperties(this.owner, (owner) -> {
         this.owner = owner;
         this.markDirty();
      });
   }

   public static void loadProperties(@Nullable GameProfile owner, Consumer<GameProfile> callback) {
      if (owner != null && !ChatUtil.isEmpty(owner.getName()) && (!owner.isComplete() || !owner.getProperties().containsKey("textures")) && userCache != null && sessionService != null) {
         userCache.method_37156(owner.getName(), (gameProfile) -> {
            Property property = (Property)Iterables.getFirst(gameProfile.getProperties().get("textures"), (Object)null);
            if (property == null) {
               gameProfile = sessionService.fillProfileProperties(gameProfile, true);
            }

            userCache.add(gameProfile);
            callback.accept(gameProfile);
         });
      } else {
         callback.accept(owner);
      }
   }
}
