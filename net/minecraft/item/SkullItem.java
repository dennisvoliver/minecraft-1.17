package net.minecraft.item;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.apache.commons.lang3.StringUtils;

public class SkullItem extends WallStandingBlockItem {
   public static final String SKULL_OWNER_KEY = "SkullOwner";

   public SkullItem(Block block, Block block2, Item.Settings settings) {
      super(block, block2, settings);
   }

   public Text getName(ItemStack stack) {
      if (stack.isOf(Items.PLAYER_HEAD) && stack.hasTag()) {
         String string = null;
         NbtCompound nbtCompound = stack.getTag();
         if (nbtCompound.contains("SkullOwner", 8)) {
            string = nbtCompound.getString("SkullOwner");
         } else if (nbtCompound.contains("SkullOwner", 10)) {
            NbtCompound nbtCompound2 = nbtCompound.getCompound("SkullOwner");
            if (nbtCompound2.contains("Name", 8)) {
               string = nbtCompound2.getString("Name");
            }
         }

         if (string != null) {
            return new TranslatableText(this.getTranslationKey() + ".named", new Object[]{string});
         }
      }

      return super.getName(stack);
   }

   public void postProcessNbt(NbtCompound nbt) {
      super.postProcessNbt(nbt);
      if (nbt.contains("SkullOwner", 8) && !StringUtils.isBlank(nbt.getString("SkullOwner"))) {
         GameProfile gameProfile = new GameProfile((UUID)null, nbt.getString("SkullOwner"));
         SkullBlockEntity.loadProperties(gameProfile, (profile) -> {
            nbt.put("SkullOwner", NbtHelper.writeGameProfile(new NbtCompound(), profile));
         });
      }

   }
}
