package net.minecraft.block;

import java.util.Random;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.entity.passive.TurtleEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;
import org.jetbrains.annotations.Nullable;

public class TurtleEggBlock extends Block {
   public static final int field_31272 = 2;
   public static final int field_31273 = 1;
   public static final int field_31274 = 4;
   private static final VoxelShape SMALL_SHAPE = Block.createCuboidShape(3.0D, 0.0D, 3.0D, 12.0D, 7.0D, 12.0D);
   private static final VoxelShape LARGE_SHAPE = Block.createCuboidShape(1.0D, 0.0D, 1.0D, 15.0D, 7.0D, 15.0D);
   public static final IntProperty HATCH;
   public static final IntProperty EGGS;

   public TurtleEggBlock(AbstractBlock.Settings settings) {
      super(settings);
      this.setDefaultState((BlockState)((BlockState)((BlockState)this.stateManager.getDefaultState()).with(HATCH, 0)).with(EGGS, 1));
   }

   public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
      this.tryBreakEgg(world, state, pos, entity, 100);
      super.onSteppedOn(world, pos, state, entity);
   }

   public void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
      if (!(entity instanceof ZombieEntity)) {
         this.tryBreakEgg(world, state, pos, entity, 3);
      }

      super.onLandedUpon(world, state, pos, entity, fallDistance);
   }

   private void tryBreakEgg(World world, BlockState state, BlockPos pos, Entity entity, int inverseChance) {
      if (this.breaksEgg(world, entity)) {
         if (!world.isClient && world.random.nextInt(inverseChance) == 0 && state.isOf(Blocks.TURTLE_EGG)) {
            this.breakEgg(world, pos, state);
         }

      }
   }

   private void breakEgg(World world, BlockPos pos, BlockState state) {
      world.playSound((PlayerEntity)null, pos, SoundEvents.ENTITY_TURTLE_EGG_BREAK, SoundCategory.BLOCKS, 0.7F, 0.9F + world.random.nextFloat() * 0.2F);
      int i = (Integer)state.get(EGGS);
      if (i <= 1) {
         world.breakBlock(pos, false);
      } else {
         world.setBlockState(pos, (BlockState)state.with(EGGS, i - 1), Block.NOTIFY_LISTENERS);
         world.syncWorldEvent(WorldEvents.BLOCK_BROKEN, pos, Block.getRawIdFromState(state));
      }

   }

   public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
      if (this.shouldHatchProgress(world) && isSandBelow(world, pos)) {
         int i = (Integer)state.get(HATCH);
         if (i < 2) {
            world.playSound((PlayerEntity)null, pos, SoundEvents.ENTITY_TURTLE_EGG_CRACK, SoundCategory.BLOCKS, 0.7F, 0.9F + random.nextFloat() * 0.2F);
            world.setBlockState(pos, (BlockState)state.with(HATCH, i + 1), Block.NOTIFY_LISTENERS);
         } else {
            world.playSound((PlayerEntity)null, pos, SoundEvents.ENTITY_TURTLE_EGG_HATCH, SoundCategory.BLOCKS, 0.7F, 0.9F + random.nextFloat() * 0.2F);
            world.removeBlock(pos, false);

            for(int j = 0; j < (Integer)state.get(EGGS); ++j) {
               world.syncWorldEvent(WorldEvents.BLOCK_BROKEN, pos, Block.getRawIdFromState(state));
               TurtleEntity turtleEntity = (TurtleEntity)EntityType.TURTLE.create(world);
               turtleEntity.setBreedingAge(-24000);
               turtleEntity.setHomePos(pos);
               turtleEntity.refreshPositionAndAngles((double)pos.getX() + 0.3D + (double)j * 0.2D, (double)pos.getY(), (double)pos.getZ() + 0.3D, 0.0F, 0.0F);
               world.spawnEntity(turtleEntity);
            }
         }
      }

   }

   public static boolean isSandBelow(BlockView world, BlockPos pos) {
      return isSand(world, pos.down());
   }

   public static boolean isSand(BlockView world, BlockPos pos) {
      return world.getBlockState(pos).isIn(BlockTags.SAND);
   }

   public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
      if (isSandBelow(world, pos) && !world.isClient) {
         world.syncWorldEvent(WorldEvents.PLANT_FERTILIZED, pos, 0);
      }

   }

   private boolean shouldHatchProgress(World world) {
      float f = world.getSkyAngle(1.0F);
      if ((double)f < 0.69D && (double)f > 0.65D) {
         return true;
      } else {
         return world.random.nextInt(500) == 0;
      }
   }

   public void afterBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack stack) {
      super.afterBreak(world, player, pos, state, blockEntity, stack);
      this.breakEgg(world, pos, state);
   }

   public boolean canReplace(BlockState state, ItemPlacementContext context) {
      return !context.shouldCancelInteraction() && context.getStack().isOf(this.asItem()) && (Integer)state.get(EGGS) < 4 ? true : super.canReplace(state, context);
   }

   @Nullable
   public BlockState getPlacementState(ItemPlacementContext ctx) {
      BlockState blockState = ctx.getWorld().getBlockState(ctx.getBlockPos());
      return blockState.isOf(this) ? (BlockState)blockState.with(EGGS, Math.min(4, (Integer)blockState.get(EGGS) + 1)) : super.getPlacementState(ctx);
   }

   public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
      return (Integer)state.get(EGGS) > 1 ? LARGE_SHAPE : SMALL_SHAPE;
   }

   protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
      builder.add(HATCH, EGGS);
   }

   private boolean breaksEgg(World world, Entity entity) {
      if (!(entity instanceof TurtleEntity) && !(entity instanceof BatEntity)) {
         if (!(entity instanceof LivingEntity)) {
            return false;
         } else {
            return entity instanceof PlayerEntity || world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING);
         }
      } else {
         return false;
      }
   }

   static {
      HATCH = Properties.HATCH;
      EGGS = Properties.EGGS;
   }
}
