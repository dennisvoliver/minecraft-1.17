package net.minecraft.predicate;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.Iterator;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.tag.ServerTagManagerHolder;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.Nullable;

public class BlockPredicate {
   public static final BlockPredicate ANY;
   @Nullable
   private final Tag<Block> tag;
   @Nullable
   private final Set<Block> blocks;
   private final StatePredicate state;
   private final NbtPredicate nbt;

   public BlockPredicate(@Nullable Tag<Block> tag, @Nullable Set<Block> blocks, StatePredicate state, NbtPredicate nbt) {
      this.tag = tag;
      this.blocks = blocks;
      this.state = state;
      this.nbt = nbt;
   }

   public boolean test(ServerWorld world, BlockPos pos) {
      if (this == ANY) {
         return true;
      } else if (!world.canSetBlock(pos)) {
         return false;
      } else {
         BlockState blockState = world.getBlockState(pos);
         if (this.tag != null && !blockState.isIn(this.tag)) {
            return false;
         } else if (this.blocks != null && !this.blocks.contains(blockState.getBlock())) {
            return false;
         } else if (!this.state.test(blockState)) {
            return false;
         } else {
            if (this.nbt != NbtPredicate.ANY) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               if (blockEntity == null || !this.nbt.test((NbtElement)blockEntity.writeNbt(new NbtCompound()))) {
                  return false;
               }
            }

            return true;
         }
      }
   }

   public static BlockPredicate fromJson(@Nullable JsonElement json) {
      if (json != null && !json.isJsonNull()) {
         JsonObject jsonObject = JsonHelper.asObject(json, "block");
         NbtPredicate nbtPredicate = NbtPredicate.fromJson(jsonObject.get("nbt"));
         Set<Block> set = null;
         JsonArray jsonArray = JsonHelper.getArray(jsonObject, "blocks", (JsonArray)null);
         if (jsonArray != null) {
            com.google.common.collect.ImmutableSet.Builder<Block> builder = ImmutableSet.builder();
            Iterator var6 = jsonArray.iterator();

            while(var6.hasNext()) {
               JsonElement jsonElement = (JsonElement)var6.next();
               Identifier identifier = new Identifier(JsonHelper.asString(jsonElement, "block"));
               builder.add((Object)((Block)Registry.BLOCK.getOrEmpty(identifier).orElseThrow(() -> {
                  return new JsonSyntaxException("Unknown block id '" + identifier + "'");
               })));
            }

            set = builder.build();
         }

         Tag<Block> tag = null;
         if (jsonObject.has("tag")) {
            Identifier identifier2 = new Identifier(JsonHelper.getString(jsonObject, "tag"));
            tag = ServerTagManagerHolder.getTagManager().getTag(Registry.BLOCK_KEY, identifier2, (identifierx) -> {
               return new JsonSyntaxException("Unknown block tag '" + identifierx + "'");
            });
         }

         StatePredicate statePredicate = StatePredicate.fromJson(jsonObject.get("state"));
         return new BlockPredicate(tag, set, statePredicate, nbtPredicate);
      } else {
         return ANY;
      }
   }

   public JsonElement toJson() {
      if (this == ANY) {
         return JsonNull.INSTANCE;
      } else {
         JsonObject jsonObject = new JsonObject();
         if (this.blocks != null) {
            JsonArray jsonArray = new JsonArray();
            Iterator var3 = this.blocks.iterator();

            while(var3.hasNext()) {
               Block block = (Block)var3.next();
               jsonArray.add(Registry.BLOCK.getId(block).toString());
            }

            jsonObject.add("blocks", jsonArray);
         }

         if (this.tag != null) {
            jsonObject.addProperty("tag", ServerTagManagerHolder.getTagManager().getTagId(Registry.BLOCK_KEY, this.tag, () -> {
               return new IllegalStateException("Unknown block tag");
            }).toString());
         }

         jsonObject.add("nbt", this.nbt.toJson());
         jsonObject.add("state", this.state.toJson());
         return jsonObject;
      }
   }

   static {
      ANY = new BlockPredicate((Tag)null, (Set)null, StatePredicate.ANY, NbtPredicate.ANY);
   }

   public static class Builder {
      @Nullable
      private Set<Block> blocks;
      @Nullable
      private Tag<Block> tag;
      private StatePredicate state;
      private NbtPredicate nbt;

      private Builder() {
         this.state = StatePredicate.ANY;
         this.nbt = NbtPredicate.ANY;
      }

      public static BlockPredicate.Builder create() {
         return new BlockPredicate.Builder();
      }

      public BlockPredicate.Builder blocks(Block... blocks) {
         this.blocks = ImmutableSet.copyOf((Object[])blocks);
         return this;
      }

      public BlockPredicate.Builder blocks(Iterable<Block> blocks) {
         this.blocks = ImmutableSet.copyOf(blocks);
         return this;
      }

      public BlockPredicate.Builder tag(Tag<Block> tag) {
         this.tag = tag;
         return this;
      }

      public BlockPredicate.Builder nbt(NbtCompound nbt) {
         this.nbt = new NbtPredicate(nbt);
         return this;
      }

      public BlockPredicate.Builder state(StatePredicate state) {
         this.state = state;
         return this;
      }

      public BlockPredicate build() {
         return new BlockPredicate(this.tag, this.blocks, this.state, this.nbt);
      }
   }
}
