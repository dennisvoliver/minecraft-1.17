package net.minecraft.item;

import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.map.MapState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.tag.BlockTags;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

public class FilledMapItem extends NetworkSyncedItem {
   public static final int field_30907 = 128;
   public static final int field_30908 = 128;
   private static final int DEFAULT_MAP_COLOR = -12173266;
   private static final String MAP_KEY = "map";

   public FilledMapItem(Item.Settings settings) {
      super(settings);
   }

   public static ItemStack createMap(World world, int x, int z, byte scale, boolean showIcons, boolean unlimitedTracking) {
      ItemStack itemStack = new ItemStack(Items.FILLED_MAP);
      createMapState(itemStack, world, x, z, scale, showIcons, unlimitedTracking, world.getRegistryKey());
      return itemStack;
   }

   @Nullable
   public static MapState getMapState(@Nullable Integer id, World world) {
      return id == null ? null : world.getMapState(getMapName(id));
   }

   @Nullable
   public static MapState getOrCreateMapState(ItemStack map, World world) {
      Integer integer = getMapId(map);
      return getMapState(integer, world);
   }

   @Nullable
   public static Integer getMapId(ItemStack stack) {
      NbtCompound nbtCompound = stack.getTag();
      return nbtCompound != null && nbtCompound.contains("map", 99) ? nbtCompound.getInt("map") : null;
   }

   private static int allocateMapId(World world, int x, int z, int scale, boolean showIcons, boolean unlimitedTracking, RegistryKey<World> dimension) {
      MapState mapState = MapState.of((double)x, (double)z, (byte)scale, showIcons, unlimitedTracking, dimension);
      int i = world.getNextMapId();
      world.putMapState(getMapName(i), mapState);
      return i;
   }

   private static void setMapId(ItemStack stack, int id) {
      stack.getOrCreateTag().putInt("map", id);
   }

   private static void createMapState(ItemStack stack, World world, int x, int z, int scale, boolean showIcons, boolean unlimitedTracking, RegistryKey<World> dimension) {
      int i = allocateMapId(world, x, z, scale, showIcons, unlimitedTracking, dimension);
      setMapId(stack, i);
   }

   public static String getMapName(int mapId) {
      return "map_" + mapId;
   }

   public void updateColors(World world, Entity entity, MapState state) {
      if (world.getRegistryKey() == state.dimension && entity instanceof PlayerEntity) {
         int i = 1 << state.scale;
         int j = state.centerX;
         int k = state.centerZ;
         int l = MathHelper.floor(entity.getX() - (double)j) / i + 64;
         int m = MathHelper.floor(entity.getZ() - (double)k) / i + 64;
         int n = 128 / i;
         if (world.getDimension().hasCeiling()) {
            n /= 2;
         }

         MapState.PlayerUpdateTracker playerUpdateTracker = state.getPlayerSyncData((PlayerEntity)entity);
         ++playerUpdateTracker.field_131;
         boolean bl = false;

         for(int o = l - n + 1; o < l + n; ++o) {
            if ((o & 15) == (playerUpdateTracker.field_131 & 15) || bl) {
               bl = false;
               double d = 0.0D;

               for(int p = m - n - 1; p < m + n; ++p) {
                  if (o >= 0 && p >= -1 && o < 128 && p < 128) {
                     int q = o - l;
                     int r = p - m;
                     boolean bl2 = q * q + r * r > (n - 2) * (n - 2);
                     int s = (j / i + o - 64) * i;
                     int t = (k / i + p - 64) * i;
                     Multiset<MapColor> multiset = LinkedHashMultiset.create();
                     WorldChunk worldChunk = world.getWorldChunk(new BlockPos(s, 0, t));
                     if (!worldChunk.isEmpty()) {
                        ChunkPos chunkPos = worldChunk.getPos();
                        int u = s & 15;
                        int v = t & 15;
                        int w = 0;
                        double e = 0.0D;
                        if (world.getDimension().hasCeiling()) {
                           int x = s + t * 231871;
                           x = x * x * 31287121 + x * 11;
                           if ((x >> 20 & 1) == 0) {
                              multiset.add(Blocks.DIRT.getDefaultState().getMapColor(world, BlockPos.ORIGIN), 10);
                           } else {
                              multiset.add(Blocks.STONE.getDefaultState().getMapColor(world, BlockPos.ORIGIN), 100);
                           }

                           e = 100.0D;
                        } else {
                           BlockPos.Mutable mutable = new BlockPos.Mutable();
                           BlockPos.Mutable mutable2 = new BlockPos.Mutable();

                           for(int y = 0; y < i; ++y) {
                              for(int z = 0; z < i; ++z) {
                                 int aa = worldChunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, y + u, z + v) + 1;
                                 BlockState blockState;
                                 if (aa <= world.getBottomY() + 1) {
                                    blockState = Blocks.BEDROCK.getDefaultState();
                                 } else {
                                    do {
                                       --aa;
                                       mutable.set(chunkPos.getStartX() + y + u, aa, chunkPos.getStartZ() + z + v);
                                       blockState = worldChunk.getBlockState(mutable);
                                    } while(blockState.getMapColor(world, mutable) == MapColor.CLEAR && aa > world.getBottomY());

                                    if (aa > world.getBottomY() && !blockState.getFluidState().isEmpty()) {
                                       int ab = aa - 1;
                                       mutable2.set(mutable);

                                       BlockState blockState2;
                                       do {
                                          mutable2.setY(ab--);
                                          blockState2 = worldChunk.getBlockState(mutable2);
                                          ++w;
                                       } while(ab > world.getBottomY() && !blockState2.getFluidState().isEmpty());

                                       blockState = this.getFluidStateIfVisible(world, blockState, mutable);
                                    }
                                 }

                                 state.removeBanner(world, chunkPos.getStartX() + y + u, chunkPos.getStartZ() + z + v);
                                 e += (double)aa / (double)(i * i);
                                 multiset.add(blockState.getMapColor(world, mutable));
                              }
                           }
                        }

                        w /= i * i;
                        double f = (e - d) * 4.0D / (double)(i + 4) + ((double)(o + p & 1) - 0.5D) * 0.4D;
                        int ac = 1;
                        if (f > 0.6D) {
                           ac = 2;
                        }

                        if (f < -0.6D) {
                           ac = 0;
                        }

                        MapColor mapColor = (MapColor)Iterables.getFirst(Multisets.copyHighestCountFirst(multiset), MapColor.CLEAR);
                        if (mapColor == MapColor.WATER_BLUE) {
                           f = (double)w * 0.1D + (double)(o + p & 1) * 0.2D;
                           ac = 1;
                           if (f < 0.5D) {
                              ac = 2;
                           }

                           if (f > 0.9D) {
                              ac = 0;
                           }
                        }

                        d = e;
                        if (p >= 0 && q * q + r * r < n * n && (!bl2 || (o + p & 1) != 0)) {
                           bl |= state.putColor(o, p, (byte)(mapColor.id * 4 + ac));
                        }
                     }
                  }
               }
            }
         }

      }
   }

   private BlockState getFluidStateIfVisible(World world, BlockState state, BlockPos pos) {
      FluidState fluidState = state.getFluidState();
      return !fluidState.isEmpty() && !state.isSideSolidFullSquare(world, pos, Direction.UP) ? fluidState.getBlockState() : state;
   }

   private static boolean hasPositiveDepth(Biome[] biomes, int scale, int x, int z) {
      return biomes[x * scale + z * scale * 128 * scale].getDepth() >= 0.0F;
   }

   public static void fillExplorationMap(ServerWorld world, ItemStack map) {
      MapState mapState = getOrCreateMapState(map, world);
      if (mapState != null) {
         if (world.getRegistryKey() == mapState.dimension) {
            int i = 1 << mapState.scale;
            int j = mapState.centerX;
            int k = mapState.centerZ;
            Biome[] biomes = new Biome[128 * i * 128 * i];

            int n;
            int o;
            for(n = 0; n < 128 * i; ++n) {
               for(o = 0; o < 128 * i; ++o) {
                  biomes[n * 128 * i + o] = world.getBiome(new BlockPos((j / i - 64) * i + o, 0, (k / i - 64) * i + n));
               }
            }

            for(n = 0; n < 128; ++n) {
               for(o = 0; o < 128; ++o) {
                  if (n > 0 && o > 0 && n < 127 && o < 127) {
                     Biome biome = biomes[n * i + o * i * 128 * i];
                     int p = 8;
                     if (hasPositiveDepth(biomes, i, n - 1, o - 1)) {
                        --p;
                     }

                     if (hasPositiveDepth(biomes, i, n - 1, o + 1)) {
                        --p;
                     }

                     if (hasPositiveDepth(biomes, i, n - 1, o)) {
                        --p;
                     }

                     if (hasPositiveDepth(biomes, i, n + 1, o - 1)) {
                        --p;
                     }

                     if (hasPositiveDepth(biomes, i, n + 1, o + 1)) {
                        --p;
                     }

                     if (hasPositiveDepth(biomes, i, n + 1, o)) {
                        --p;
                     }

                     if (hasPositiveDepth(biomes, i, n, o - 1)) {
                        --p;
                     }

                     if (hasPositiveDepth(biomes, i, n, o + 1)) {
                        --p;
                     }

                     int q = 3;
                     MapColor mapColor = MapColor.CLEAR;
                     if (biome.getDepth() < 0.0F) {
                        mapColor = MapColor.ORANGE;
                        if (p > 7 && o % 2 == 0) {
                           q = (n + (int)(MathHelper.sin((float)o + 0.0F) * 7.0F)) / 8 % 5;
                           if (q == 3) {
                              q = 1;
                           } else if (q == 4) {
                              q = 0;
                           }
                        } else if (p > 7) {
                           mapColor = MapColor.CLEAR;
                        } else if (p > 5) {
                           q = 1;
                        } else if (p > 3) {
                           q = 0;
                        } else if (p > 1) {
                           q = 0;
                        }
                     } else if (p > 0) {
                        mapColor = MapColor.BROWN;
                        if (p > 3) {
                           q = 1;
                        } else {
                           q = 3;
                        }
                     }

                     if (mapColor != MapColor.CLEAR) {
                        mapState.setColor(n, o, (byte)(mapColor.id * 4 + q));
                     }
                  }
               }
            }

         }
      }
   }

   public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
      if (!world.isClient) {
         MapState mapState = getOrCreateMapState(stack, world);
         if (mapState != null) {
            if (entity instanceof PlayerEntity) {
               PlayerEntity playerEntity = (PlayerEntity)entity;
               mapState.update(playerEntity, stack);
            }

            if (!mapState.locked && (selected || entity instanceof PlayerEntity && ((PlayerEntity)entity).getOffHandStack() == stack)) {
               this.updateColors(world, entity, mapState);
            }

         }
      }
   }

   @Nullable
   public Packet<?> createSyncPacket(ItemStack stack, World world, PlayerEntity player) {
      Integer integer = getMapId(stack);
      MapState mapState = getMapState(integer, world);
      return mapState != null ? mapState.getPlayerMarkerPacket(integer, player) : null;
   }

   public void onCraft(ItemStack stack, World world, PlayerEntity player) {
      NbtCompound nbtCompound = stack.getTag();
      if (nbtCompound != null && nbtCompound.contains("map_scale_direction", 99)) {
         scale(stack, world, nbtCompound.getInt("map_scale_direction"));
         nbtCompound.remove("map_scale_direction");
      } else if (nbtCompound != null && nbtCompound.contains("map_to_lock", 1) && nbtCompound.getBoolean("map_to_lock")) {
         copyMap(world, stack);
         nbtCompound.remove("map_to_lock");
      }

   }

   private static void scale(ItemStack map, World world, int amount) {
      MapState mapState = getOrCreateMapState(map, world);
      if (mapState != null) {
         int i = world.getNextMapId();
         world.putMapState(getMapName(i), mapState.zoomOut(amount));
         setMapId(map, i);
      }

   }

   public static void copyMap(World world, ItemStack stack) {
      MapState mapState = getOrCreateMapState(stack, world);
      if (mapState != null) {
         int i = world.getNextMapId();
         String string = getMapName(i);
         MapState mapState2 = mapState.copy();
         world.putMapState(string, mapState2);
         setMapId(stack, i);
      }

   }

   public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
      Integer integer = getMapId(stack);
      MapState mapState = world == null ? null : getMapState(integer, world);
      if (mapState != null && mapState.locked) {
         tooltip.add((new TranslatableText("filled_map.locked", new Object[]{integer})).formatted(Formatting.GRAY));
      }

      if (context.isAdvanced()) {
         if (mapState != null) {
            tooltip.add((new TranslatableText("filled_map.id", new Object[]{integer})).formatted(Formatting.GRAY));
            tooltip.add((new TranslatableText("filled_map.scale", new Object[]{1 << mapState.scale})).formatted(Formatting.GRAY));
            tooltip.add((new TranslatableText("filled_map.level", new Object[]{mapState.scale, 4})).formatted(Formatting.GRAY));
         } else {
            tooltip.add((new TranslatableText("filled_map.unknown")).formatted(Formatting.GRAY));
         }
      }

   }

   public static int getMapColor(ItemStack stack) {
      NbtCompound nbtCompound = stack.getSubTag("display");
      if (nbtCompound != null && nbtCompound.contains("MapColor", 99)) {
         int i = nbtCompound.getInt("MapColor");
         return -16777216 | i & 16777215;
      } else {
         return -12173266;
      }
   }

   public ActionResult useOnBlock(ItemUsageContext context) {
      BlockState blockState = context.getWorld().getBlockState(context.getBlockPos());
      if (blockState.isIn(BlockTags.BANNERS)) {
         if (!context.getWorld().isClient) {
            MapState mapState = getOrCreateMapState(context.getStack(), context.getWorld());
            if (mapState != null && !mapState.addBanner(context.getWorld(), context.getBlockPos())) {
               return ActionResult.FAIL;
            }
         }

         return ActionResult.success(context.getWorld().isClient);
      } else {
         return super.useOnBlock(context);
      }
   }
}
