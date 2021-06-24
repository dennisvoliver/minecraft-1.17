package net.minecraft.world.gen.feature;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.GlowLichenBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;

public class GlowLichenFeature extends Feature<GlowLichenFeatureConfig> {
   public GlowLichenFeature(Codec<GlowLichenFeatureConfig> codec) {
      super(codec);
   }

   public boolean generate(FeatureContext<GlowLichenFeatureConfig> context) {
      StructureWorldAccess structureWorldAccess = context.getWorld();
      BlockPos blockPos = context.getOrigin();
      Random random = context.getRandom();
      GlowLichenFeatureConfig glowLichenFeatureConfig = (GlowLichenFeatureConfig)context.getConfig();
      if (!isAirOrWater(structureWorldAccess.getBlockState(blockPos))) {
         return false;
      } else {
         List<Direction> list = shuffleDirections(glowLichenFeatureConfig, random);
         if (generate(structureWorldAccess, blockPos, structureWorldAccess.getBlockState(blockPos), glowLichenFeatureConfig, random, list)) {
            return true;
         } else {
            BlockPos.Mutable mutable = blockPos.mutableCopy();
            Iterator var8 = list.iterator();

            while(var8.hasNext()) {
               Direction direction = (Direction)var8.next();
               mutable.set(blockPos);
               List<Direction> list2 = shuffleDirections(glowLichenFeatureConfig, random, direction.getOpposite());

               for(int i = 0; i < glowLichenFeatureConfig.searchRange; ++i) {
                  mutable.set(blockPos, (Direction)direction);
                  BlockState blockState = structureWorldAccess.getBlockState(mutable);
                  if (!isAirOrWater(blockState) && !blockState.isOf(Blocks.GLOW_LICHEN)) {
                     break;
                  }

                  if (generate(structureWorldAccess, mutable, blockState, glowLichenFeatureConfig, random, list2)) {
                     return true;
                  }
               }
            }

            return false;
         }
      }
   }

   public static boolean generate(StructureWorldAccess world, BlockPos pos, BlockState state, GlowLichenFeatureConfig config, Random random, List<Direction> directions) {
      BlockPos.Mutable mutable = pos.mutableCopy();
      Iterator var7 = directions.iterator();

      Direction direction;
      BlockState blockState;
      do {
         if (!var7.hasNext()) {
            return false;
         }

         direction = (Direction)var7.next();
         blockState = world.getBlockState(mutable.set(pos, (Direction)direction));
      } while(!config.canGrowOn(blockState.getBlock()));

      GlowLichenBlock glowLichenBlock = (GlowLichenBlock)Blocks.GLOW_LICHEN;
      BlockState blockState2 = glowLichenBlock.withDirection(state, world, pos, direction);
      if (blockState2 == null) {
         return false;
      } else {
         world.setBlockState(pos, blockState2, Block.NOTIFY_ALL);
         world.getChunk(pos).markBlockForPostProcessing(pos);
         if (random.nextFloat() < config.spreadChance) {
            glowLichenBlock.trySpreadRandomly(blockState2, world, pos, direction, random, true);
         }

         return true;
      }
   }

   public static List<Direction> shuffleDirections(GlowLichenFeatureConfig config, Random random) {
      List<Direction> list = Lists.newArrayList((Iterable)config.directions);
      Collections.shuffle(list, random);
      return list;
   }

   public static List<Direction> shuffleDirections(GlowLichenFeatureConfig config, Random random, Direction excluded) {
      List<Direction> list = (List)config.directions.stream().filter((direction) -> {
         return direction != excluded;
      }).collect(Collectors.toList());
      Collections.shuffle(list, random);
      return list;
   }

   private static boolean isAirOrWater(BlockState state) {
      return state.isAir() || state.isOf(Blocks.WATER);
   }
}
