package net.minecraft.entity;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.world.World;

public class MarkerEntity extends Entity {
   /**
    * The name of the compound tag that stores the marker's custom data.
    */
   private static final String DATA_KEY = "data";
   private NbtCompound data = new NbtCompound();

   public MarkerEntity(EntityType<?> entityType, World world) {
      super(entityType, world);
      this.noClip = true;
   }

   public void tick() {
   }

   protected void initDataTracker() {
   }

   protected void readCustomDataFromNbt(NbtCompound nbt) {
      this.data = nbt.getCompound("data");
   }

   protected void writeCustomDataToNbt(NbtCompound nbt) {
      nbt.put("data", this.data.copy());
   }

   public Packet<?> createSpawnPacket() {
      throw new IllegalStateException("Markers should never be sent");
   }

   protected void addPassenger(Entity passenger) {
      passenger.stopRiding();
   }
}
