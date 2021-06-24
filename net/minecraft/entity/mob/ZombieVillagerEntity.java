package net.minecraft.entity.mob;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityInteraction;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerDataContainer;
import net.minecraft.village.VillagerProfession;
import net.minecraft.village.VillagerType;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;
import net.minecraft.world.event.GameEvent;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class ZombieVillagerEntity extends ZombieEntity implements VillagerDataContainer {
   private static final TrackedData<Boolean> CONVERTING;
   private static final TrackedData<VillagerData> VILLAGER_DATA;
   private static final int field_30523 = 3600;
   private static final int field_30520 = 6000;
   private static final int field_30521 = 14;
   private static final int field_30522 = 4;
   private int conversionTimer;
   private UUID converter;
   private NbtElement gossipData;
   private NbtCompound offerData;
   private int xp;

   public ZombieVillagerEntity(EntityType<? extends ZombieVillagerEntity> entityType, World world) {
      super(entityType, world);
      this.setVillagerData(this.getVillagerData().withProfession((VillagerProfession)Registry.VILLAGER_PROFESSION.getRandom(this.random)));
   }

   protected void initDataTracker() {
      super.initDataTracker();
      this.dataTracker.startTracking(CONVERTING, false);
      this.dataTracker.startTracking(VILLAGER_DATA, new VillagerData(VillagerType.PLAINS, VillagerProfession.NONE, 1));
   }

   public void writeCustomDataToNbt(NbtCompound nbt) {
      super.writeCustomDataToNbt(nbt);
      DataResult var10000 = VillagerData.CODEC.encodeStart(NbtOps.INSTANCE, this.getVillagerData());
      Logger var10001 = LOGGER;
      Objects.requireNonNull(var10001);
      var10000.resultOrPartial(var10001::error).ifPresent((nbtElement) -> {
         nbt.put("VillagerData", nbtElement);
      });
      if (this.offerData != null) {
         nbt.put("Offers", this.offerData);
      }

      if (this.gossipData != null) {
         nbt.put("Gossips", this.gossipData);
      }

      nbt.putInt("ConversionTime", this.isConverting() ? this.conversionTimer : -1);
      if (this.converter != null) {
         nbt.putUuid("ConversionPlayer", this.converter);
      }

      nbt.putInt("Xp", this.xp);
   }

   public void readCustomDataFromNbt(NbtCompound nbt) {
      super.readCustomDataFromNbt(nbt);
      if (nbt.contains("VillagerData", 10)) {
         DataResult<VillagerData> dataResult = VillagerData.CODEC.parse(new Dynamic(NbtOps.INSTANCE, nbt.get("VillagerData")));
         Logger var10001 = LOGGER;
         Objects.requireNonNull(var10001);
         dataResult.resultOrPartial(var10001::error).ifPresent(this::setVillagerData);
      }

      if (nbt.contains("Offers", 10)) {
         this.offerData = nbt.getCompound("Offers");
      }

      if (nbt.contains("Gossips", 10)) {
         this.gossipData = nbt.getList("Gossips", 10);
      }

      if (nbt.contains("ConversionTime", 99) && nbt.getInt("ConversionTime") > -1) {
         this.setConverting(nbt.containsUuid("ConversionPlayer") ? nbt.getUuid("ConversionPlayer") : null, nbt.getInt("ConversionTime"));
      }

      if (nbt.contains("Xp", 3)) {
         this.xp = nbt.getInt("Xp");
      }

   }

   public void tick() {
      if (!this.world.isClient && this.isAlive() && this.isConverting()) {
         int i = this.getConversionRate();
         this.conversionTimer -= i;
         if (this.conversionTimer <= 0) {
            this.finishConversion((ServerWorld)this.world);
         }
      }

      super.tick();
   }

   public ActionResult interactMob(PlayerEntity player, Hand hand) {
      ItemStack itemStack = player.getStackInHand(hand);
      if (itemStack.isOf(Items.GOLDEN_APPLE)) {
         if (this.hasStatusEffect(StatusEffects.WEAKNESS)) {
            if (!player.getAbilities().creativeMode) {
               itemStack.decrement(1);
            }

            if (!this.world.isClient) {
               this.setConverting(player.getUuid(), this.random.nextInt(2401) + 3600);
            }

            this.emitGameEvent(GameEvent.MOB_INTERACT, this.getCameraBlockPos());
            return ActionResult.SUCCESS;
         } else {
            return ActionResult.CONSUME;
         }
      } else {
         return super.interactMob(player, hand);
      }
   }

   protected boolean canConvertInWater() {
      return false;
   }

   public boolean canImmediatelyDespawn(double distanceSquared) {
      return !this.isConverting() && this.xp == 0;
   }

   public boolean isConverting() {
      return (Boolean)this.getDataTracker().get(CONVERTING);
   }

   private void setConverting(@Nullable UUID uuid, int delay) {
      this.converter = uuid;
      this.conversionTimer = delay;
      this.getDataTracker().set(CONVERTING, true);
      this.removeStatusEffect(StatusEffects.WEAKNESS);
      this.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, delay, Math.min(this.world.getDifficulty().getId() - 1, 0)));
      this.world.sendEntityStatus(this, (byte)16);
   }

   public void handleStatus(byte status) {
      if (status == 16) {
         if (!this.isSilent()) {
            this.world.playSound(this.getX(), this.getEyeY(), this.getZ(), SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE, this.getSoundCategory(), 1.0F + this.random.nextFloat(), this.random.nextFloat() * 0.7F + 0.3F, false);
         }

      } else {
         super.handleStatus(status);
      }
   }

   private void finishConversion(ServerWorld world) {
      VillagerEntity villagerEntity = (VillagerEntity)this.convertTo(EntityType.VILLAGER, false);
      EquipmentSlot[] var3 = EquipmentSlot.values();
      int var4 = var3.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         EquipmentSlot equipmentSlot = var3[var5];
         ItemStack itemStack = this.getEquippedStack(equipmentSlot);
         if (!itemStack.isEmpty()) {
            if (EnchantmentHelper.hasBindingCurse(itemStack)) {
               villagerEntity.getStackReference(equipmentSlot.getEntitySlotId() + 300).set(itemStack);
            } else {
               double d = (double)this.getDropChance(equipmentSlot);
               if (d > 1.0D) {
                  this.dropStack(itemStack);
               }
            }
         }
      }

      villagerEntity.setVillagerData(this.getVillagerData());
      if (this.gossipData != null) {
         villagerEntity.readGossipDataNbt(this.gossipData);
      }

      if (this.offerData != null) {
         villagerEntity.setOffers(new TradeOfferList(this.offerData));
      }

      villagerEntity.setExperience(this.xp);
      villagerEntity.initialize(world, world.getLocalDifficulty(villagerEntity.getBlockPos()), SpawnReason.CONVERSION, (EntityData)null, (NbtCompound)null);
      if (this.converter != null) {
         PlayerEntity playerEntity = world.getPlayerByUuid(this.converter);
         if (playerEntity instanceof ServerPlayerEntity) {
            Criteria.CURED_ZOMBIE_VILLAGER.trigger((ServerPlayerEntity)playerEntity, this, villagerEntity);
            world.handleInteraction(EntityInteraction.ZOMBIE_VILLAGER_CURED, playerEntity, villagerEntity);
         }
      }

      villagerEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 200, 0));
      if (!this.isSilent()) {
         world.syncWorldEvent((PlayerEntity)null, WorldEvents.ZOMBIE_VILLAGER_CURED, this.getBlockPos(), 0);
      }

   }

   private int getConversionRate() {
      int i = 1;
      if (this.random.nextFloat() < 0.01F) {
         int j = 0;
         BlockPos.Mutable mutable = new BlockPos.Mutable();

         for(int k = (int)this.getX() - 4; k < (int)this.getX() + 4 && j < 14; ++k) {
            for(int l = (int)this.getY() - 4; l < (int)this.getY() + 4 && j < 14; ++l) {
               for(int m = (int)this.getZ() - 4; m < (int)this.getZ() + 4 && j < 14; ++m) {
                  BlockState blockState = this.world.getBlockState(mutable.set(k, l, m));
                  if (blockState.isOf(Blocks.IRON_BARS) || blockState.getBlock() instanceof BedBlock) {
                     if (this.random.nextFloat() < 0.3F) {
                        ++i;
                     }

                     ++j;
                  }
               }
            }
         }
      }

      return i;
   }

   public float getSoundPitch() {
      return this.isBaby() ? (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 2.0F : (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F;
   }

   public SoundEvent getAmbientSound() {
      return SoundEvents.ENTITY_ZOMBIE_VILLAGER_AMBIENT;
   }

   public SoundEvent getHurtSound(DamageSource source) {
      return SoundEvents.ENTITY_ZOMBIE_VILLAGER_HURT;
   }

   public SoundEvent getDeathSound() {
      return SoundEvents.ENTITY_ZOMBIE_VILLAGER_DEATH;
   }

   public SoundEvent getStepSound() {
      return SoundEvents.ENTITY_ZOMBIE_VILLAGER_STEP;
   }

   protected ItemStack getSkull() {
      return ItemStack.EMPTY;
   }

   public void setOfferData(NbtCompound offerTag) {
      this.offerData = offerTag;
   }

   public void setGossipData(NbtElement gossipTag) {
      this.gossipData = gossipTag;
   }

   @Nullable
   public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
      this.setVillagerData(this.getVillagerData().withType(VillagerType.forBiome(world.getBiomeKey(this.getBlockPos()))));
      return super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
   }

   public void setVillagerData(VillagerData villagerData) {
      VillagerData villagerData2 = this.getVillagerData();
      if (villagerData2.getProfession() != villagerData.getProfession()) {
         this.offerData = null;
      }

      this.dataTracker.set(VILLAGER_DATA, villagerData);
   }

   public VillagerData getVillagerData() {
      return (VillagerData)this.dataTracker.get(VILLAGER_DATA);
   }

   public int getXp() {
      return this.xp;
   }

   public void setXp(int xp) {
      this.xp = xp;
   }

   static {
      CONVERTING = DataTracker.registerData(ZombieVillagerEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
      VILLAGER_DATA = DataTracker.registerData(ZombieVillagerEntity.class, TrackedDataHandlerRegistry.VILLAGER_DATA);
   }
}
