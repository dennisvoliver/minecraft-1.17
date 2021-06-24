package net.minecraft.block;

import java.util.Random;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.EndGatewayBlockEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class EndGatewayBlock extends BlockWithEntity {
   protected EndGatewayBlock(AbstractBlock.Settings settings) {
      super(settings);
   }

   public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
      return new EndGatewayBlockEntity(pos, state);
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
      return checkType(type, BlockEntityType.END_GATEWAY, world.isClient ? EndGatewayBlockEntity::clientTick : EndGatewayBlockEntity::serverTick);
   }

   public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
      BlockEntity blockEntity = world.getBlockEntity(pos);
      if (blockEntity instanceof EndGatewayBlockEntity) {
         int i = ((EndGatewayBlockEntity)blockEntity).getDrawnSidesCount();

         for(int j = 0; j < i; ++j) {
            double d = (double)pos.getX() + random.nextDouble();
            double e = (double)pos.getY() + random.nextDouble();
            double f = (double)pos.getZ() + random.nextDouble();
            double g = (random.nextDouble() - 0.5D) * 0.5D;
            double h = (random.nextDouble() - 0.5D) * 0.5D;
            double k = (random.nextDouble() - 0.5D) * 0.5D;
            int l = random.nextInt(2) * 2 - 1;
            if (random.nextBoolean()) {
               f = (double)pos.getZ() + 0.5D + 0.25D * (double)l;
               k = (double)(random.nextFloat() * 2.0F * (float)l);
            } else {
               d = (double)pos.getX() + 0.5D + 0.25D * (double)l;
               g = (double)(random.nextFloat() * 2.0F * (float)l);
            }

            world.addParticle(ParticleTypes.PORTAL, d, e, f, g, h, k);
         }

      }
   }

   public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
      return ItemStack.EMPTY;
   }

   public boolean canBucketPlace(BlockState state, Fluid fluid) {
      return false;
   }
}
