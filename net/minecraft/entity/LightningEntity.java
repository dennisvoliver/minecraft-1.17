package net.minecraft.entity;

import com.google.common.collect.Sets;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LightningRodBlock;
import net.minecraft.block.Oxidizable;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

public class LightningEntity extends Entity {
   private static final int field_30062 = 2;
   private static final double field_33906 = 3.0D;
   private static final double field_33907 = 15.0D;
   private int ambientTick;
   public long seed;
   private int remainingActions;
   private boolean cosmetic;
   @Nullable
   private ServerPlayerEntity channeler;
   private final Set<Entity> struckEntities = Sets.newHashSet();
   private int blocksSetOnFire;

   public LightningEntity(EntityType<? extends LightningEntity> entityType, World world) {
      super(entityType, world);
      this.ignoreCameraFrustum = true;
      this.ambientTick = 2;
      this.seed = this.random.nextLong();
      this.remainingActions = this.random.nextInt(3) + 1;
   }

   public void setCosmetic(boolean cosmetic) {
      this.cosmetic = cosmetic;
   }

   public SoundCategory getSoundCategory() {
      return SoundCategory.WEATHER;
   }

   @Nullable
   public ServerPlayerEntity getChanneler() {
      return this.channeler;
   }

   public void setChanneler(@Nullable ServerPlayerEntity channeler) {
      this.channeler = channeler;
   }

   private void powerLightningRod() {
      BlockPos blockPos = this.getAffectedBlockPos();
      BlockState blockState = this.world.getBlockState(blockPos);
      if (blockState.isOf(Blocks.LIGHTNING_ROD)) {
         ((LightningRodBlock)blockState.getBlock()).setPowered(blockState, this.world, blockPos);
      }

   }

   public void tick() {
      super.tick();
      if (this.ambientTick == 2) {
         if (this.world.isClient()) {
            this.world.playSound(this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER, 10000.0F, 0.8F + this.random.nextFloat() * 0.2F, false);
            this.world.playSound(this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.WEATHER, 2.0F, 0.5F + this.random.nextFloat() * 0.2F, false);
         } else {
            Difficulty difficulty = this.world.getDifficulty();
            if (difficulty == Difficulty.NORMAL || difficulty == Difficulty.HARD) {
               this.spawnFire(4);
            }

            this.powerLightningRod();
            cleanOxidization(this.world, this.getAffectedBlockPos());
            this.emitGameEvent(GameEvent.LIGHTNING_STRIKE);
         }
      }

      --this.ambientTick;
      Iterator var2;
      List list2;
      if (this.ambientTick < 0) {
         if (this.remainingActions == 0) {
            if (this.world instanceof ServerWorld) {
               list2 = this.world.getOtherEntities(this, new Box(this.getX() - 15.0D, this.getY() - 15.0D, this.getZ() - 15.0D, this.getX() + 15.0D, this.getY() + 6.0D + 15.0D, this.getZ() + 15.0D), (entityx) -> {
                  return entityx.isAlive() && !this.struckEntities.contains(entityx);
               });
               var2 = ((ServerWorld)this.world).getPlayers((serverPlayerEntityx) -> {
                  return serverPlayerEntityx.distanceTo(this) < 256.0F;
               }).iterator();

               while(var2.hasNext()) {
                  ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)var2.next();
                  Criteria.LIGHTNING_STRIKE.test(serverPlayerEntity, this, list2);
               }
            }

            this.discard();
         } else if (this.ambientTick < -this.random.nextInt(10)) {
            --this.remainingActions;
            this.ambientTick = 1;
            this.seed = this.random.nextLong();
            this.spawnFire(0);
         }
      }

      if (this.ambientTick >= 0) {
         if (!(this.world instanceof ServerWorld)) {
            this.world.setLightningTicksLeft(2);
         } else if (!this.cosmetic) {
            list2 = this.world.getOtherEntities(this, new Box(this.getX() - 3.0D, this.getY() - 3.0D, this.getZ() - 3.0D, this.getX() + 3.0D, this.getY() + 6.0D + 3.0D, this.getZ() + 3.0D), Entity::isAlive);
            var2 = list2.iterator();

            while(var2.hasNext()) {
               Entity entity = (Entity)var2.next();
               entity.onStruckByLightning((ServerWorld)this.world, this);
            }

            this.struckEntities.addAll(list2);
            if (this.channeler != null) {
               Criteria.CHANNELED_LIGHTNING.trigger(this.channeler, list2);
            }
         }
      }

   }

   private BlockPos getAffectedBlockPos() {
      Vec3d vec3d = this.getPos();
      return new BlockPos(vec3d.x, vec3d.y - 1.0E-6D, vec3d.z);
   }

   private void spawnFire(int spreadAttempts) {
      if (!this.cosmetic && !this.world.isClient && this.world.getGameRules().getBoolean(GameRules.DO_FIRE_TICK)) {
         BlockPos blockPos = this.getBlockPos();
         BlockState blockState = AbstractFireBlock.getState(this.world, blockPos);
         if (this.world.getBlockState(blockPos).isAir() && blockState.canPlaceAt(this.world, blockPos)) {
            this.world.setBlockState(blockPos, blockState);
            ++this.blocksSetOnFire;
         }

         for(int i = 0; i < spreadAttempts; ++i) {
            BlockPos blockPos2 = blockPos.add(this.random.nextInt(3) - 1, this.random.nextInt(3) - 1, this.random.nextInt(3) - 1);
            blockState = AbstractFireBlock.getState(this.world, blockPos2);
            if (this.world.getBlockState(blockPos2).isAir() && blockState.canPlaceAt(this.world, blockPos2)) {
               this.world.setBlockState(blockPos2, blockState);
               ++this.blocksSetOnFire;
            }
         }

      }
   }

   private static void cleanOxidization(World world, BlockPos pos) {
      BlockState blockState = world.getBlockState(pos);
      BlockPos blockPos2;
      BlockState blockState3;
      if (blockState.isOf(Blocks.LIGHTNING_ROD)) {
         blockPos2 = pos.offset(((Direction)blockState.get(LightningRodBlock.FACING)).getOpposite());
         blockState3 = world.getBlockState(blockPos2);
      } else {
         blockPos2 = pos;
         blockState3 = blockState;
      }

      if (blockState3.getBlock() instanceof Oxidizable) {
         world.setBlockState(blockPos2, Oxidizable.getUnaffectedOxidationState(world.getBlockState(blockPos2)));
         BlockPos.Mutable mutable = pos.mutableCopy();
         int i = world.random.nextInt(3) + 3;

         for(int j = 0; j < i; ++j) {
            int k = world.random.nextInt(8) + 1;
            cleanOxidizationAround(world, blockPos2, mutable, k);
         }

      }
   }

   private static void cleanOxidizationAround(World world, BlockPos pos, BlockPos.Mutable mutablePos, int count) {
      mutablePos.set(pos);

      for(int i = 0; i < count; ++i) {
         Optional<BlockPos> optional = cleanOxidizationAround(world, mutablePos);
         if (!optional.isPresent()) {
            break;
         }

         mutablePos.set((Vec3i)optional.get());
      }

   }

   private static Optional<BlockPos> cleanOxidizationAround(World world, BlockPos pos) {
      Iterator var2 = BlockPos.iterateRandomly(world.random, 10, pos, 1).iterator();

      BlockPos blockPos;
      BlockState blockState;
      do {
         if (!var2.hasNext()) {
            return Optional.empty();
         }

         blockPos = (BlockPos)var2.next();
         blockState = world.getBlockState(blockPos);
      } while(!(blockState.getBlock() instanceof Oxidizable));

      Oxidizable.getDecreasedOxidationState(blockState).ifPresent((state) -> {
         world.setBlockState(blockPos, state);
      });
      world.syncWorldEvent(WorldEvents.ELECTRICITY_SPARKS, blockPos, -1);
      return Optional.of(blockPos);
   }

   public boolean shouldRender(double distance) {
      double d = 64.0D * getRenderDistanceMultiplier();
      return distance < d * d;
   }

   protected void initDataTracker() {
   }

   protected void readCustomDataFromNbt(NbtCompound nbt) {
   }

   protected void writeCustomDataToNbt(NbtCompound nbt) {
   }

   public Packet<?> createSpawnPacket() {
      return new EntitySpawnS2CPacket(this);
   }

   public int getBlocksSetOnFire() {
      return this.blocksSetOnFire;
   }

   public Stream<Entity> getStruckEntities() {
      return this.struckEntities.stream().filter(Entity::isAlive);
   }
}
