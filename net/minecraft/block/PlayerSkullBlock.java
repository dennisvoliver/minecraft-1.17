package net.minecraft.block;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

public class PlayerSkullBlock extends SkullBlock {
   protected PlayerSkullBlock(AbstractBlock.Settings settings) {
      super(SkullBlock.Type.PLAYER, settings);
   }

   public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
      super.onPlaced(world, pos, state, placer, itemStack);
      BlockEntity blockEntity = world.getBlockEntity(pos);
      if (blockEntity instanceof SkullBlockEntity) {
         SkullBlockEntity skullBlockEntity = (SkullBlockEntity)blockEntity;
         GameProfile gameProfile = null;
         if (itemStack.hasTag()) {
            NbtCompound nbtCompound = itemStack.getTag();
            if (nbtCompound.contains("SkullOwner", 10)) {
               gameProfile = NbtHelper.toGameProfile(nbtCompound.getCompound("SkullOwner"));
            } else if (nbtCompound.contains("SkullOwner", 8) && !StringUtils.isBlank(nbtCompound.getString("SkullOwner"))) {
               gameProfile = new GameProfile((UUID)null, nbtCompound.getString("SkullOwner"));
            }
         }

         skullBlockEntity.setOwner(gameProfile);
      }

   }
}
