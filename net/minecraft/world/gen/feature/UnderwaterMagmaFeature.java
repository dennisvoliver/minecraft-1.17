package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import java.util.Iterator;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;
import java.util.function.Predicate;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.gen.feature.util.CaveSurface;
import net.minecraft.world.gen.feature.util.FeatureContext;

public class UnderwaterMagmaFeature extends Feature<UnderwaterMagmaFeatureConfig> {
   public UnderwaterMagmaFeature(Codec<UnderwaterMagmaFeatureConfig> codec) {
      super(codec);
   }

   public boolean generate(FeatureContext<UnderwaterMagmaFeatureConfig> context) {
      StructureWorldAccess structureWorldAccess = context.getWorld();
      BlockPos blockPos = context.getOrigin();
      UnderwaterMagmaFeatureConfig underwaterMagmaFeatureConfig = (UnderwaterMagmaFeatureConfig)context.getConfig();
      Random random = context.getRandom();
      OptionalInt optionalInt = getFloorHeight(structureWorldAccess, blockPos, underwaterMagmaFeatureConfig);
      if (!optionalInt.isPresent()) {
         return false;
      } else {
         BlockPos blockPos2 = blockPos.withY(optionalInt.getAsInt());
         Vec3i vec3i = new Vec3i(underwaterMagmaFeatureConfig.placementRadiusAroundFloor, underwaterMagmaFeatureConfig.placementRadiusAroundFloor, underwaterMagmaFeatureConfig.placementRadiusAroundFloor);
         Box box = new Box(blockPos2.subtract(vec3i), blockPos2.add(vec3i));
         return BlockPos.stream(box).filter((pos) -> {
            return random.nextFloat() < underwaterMagmaFeatureConfig.placementProbabilityPerValidPosition;
         }).filter((pos) -> {
            return this.isValidPosition(structureWorldAccess, pos);
         }).mapToInt((pos) -> {
            structureWorldAccess.setBlockState(pos, Blocks.MAGMA_BLOCK.getDefaultState(), Block.NOTIFY_LISTENERS);
            return 1;
         }).sum() > 0;
      }
   }

   private static OptionalInt getFloorHeight(StructureWorldAccess world, BlockPos pos, UnderwaterMagmaFeatureConfig config) {
      Predicate<BlockState> predicate = (state) -> {
         return state.isOf(Blocks.WATER);
      };
      Predicate<BlockState> predicate2 = (state) -> {
         return !state.isOf(Blocks.WATER);
      };
      Optional<CaveSurface> optional = CaveSurface.create(world, pos, config.floorSearchRange, predicate, predicate2);
      return (OptionalInt)optional.map(CaveSurface::getFloorHeight).orElseGet(OptionalInt::empty);
   }

   private boolean isValidPosition(StructureWorldAccess world, BlockPos pos) {
      if (!this.isWaterOrAir(world, pos) && !this.isWaterOrAir(world, pos.down())) {
         Iterator var3 = Direction.Type.HORIZONTAL.iterator();

         Direction direction;
         do {
            if (!var3.hasNext()) {
               return true;
            }

            direction = (Direction)var3.next();
         } while(!this.isWaterOrAir(world, pos.offset(direction)));

         return false;
      } else {
         return false;
      }
   }

   private boolean isWaterOrAir(WorldAccess world, BlockPos pos) {
      BlockState blockState = world.getBlockState(pos);
      return blockState.isOf(Blocks.WATER) || blockState.isAir();
   }
}
