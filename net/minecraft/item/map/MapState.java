package net.minecraft.item.map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.MapUpdateS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.BlockView;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.dimension.DimensionType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class MapState extends PersistentState {
   private static final Logger field_25019 = LogManager.getLogger();
   private static final int field_31832 = 128;
   private static final int field_31833 = 64;
   public static final int field_31831 = 4;
   public static final int field_33991 = 256;
   /**
    * The scaled center coordinate of the map state on the X axis.
    * <p>
    * Always {@code 0} for the client.
    */
   public final int centerX;
   /**
    * The scaled center coordinate of the map state on the Z axis.
    * <p>
    * Always {@code 0} for the client.
    */
   public final int centerZ;
   public final RegistryKey<World> dimension;
   private final boolean showIcons;
   private final boolean unlimitedTracking;
   public final byte scale;
   public byte[] colors = new byte[16384];
   public final boolean locked;
   private final List<MapState.PlayerUpdateTracker> updateTrackers = Lists.newArrayList();
   private final Map<PlayerEntity, MapState.PlayerUpdateTracker> updateTrackersByPlayer = Maps.newHashMap();
   /**
    * The banner markers to track in world.
    * <p>
    * Empty for the client.
    */
   private final Map<String, MapBannerMarker> banners = Maps.newHashMap();
   final Map<String, MapIcon> icons = Maps.newLinkedHashMap();
   private final Map<String, MapFrameMarker> frames = Maps.newHashMap();
   private int field_33992;

   private MapState(int centerX, int centerZ, byte scale, boolean showIcons, boolean unlimitedTracking, boolean locked, RegistryKey<World> dimension) {
      this.scale = scale;
      this.centerX = centerX;
      this.centerZ = centerZ;
      this.dimension = dimension;
      this.showIcons = showIcons;
      this.unlimitedTracking = unlimitedTracking;
      this.locked = locked;
      this.markDirty();
   }

   /**
    * Creates a new map state instance.
    * 
    * @param centerX the absolute center X-coordinate
    * @param centerZ the absolute center Z-coordinate
    */
   public static MapState of(double centerX, double centerZ, byte scale, boolean showIcons, boolean unlimitedTracking, RegistryKey<World> dimension) {
      int i = 128 * (1 << scale);
      int j = MathHelper.floor((centerX + 64.0D) / (double)i);
      int k = MathHelper.floor((centerZ + 64.0D) / (double)i);
      int l = j * i + i / 2 - 64;
      int m = k * i + i / 2 - 64;
      return new MapState(l, m, scale, showIcons, unlimitedTracking, false, dimension);
   }

   /**
    * Creates a new map state instance for the client.
    * <p>
    * The client is not aware of the coordinates of the map state so its center coordinates will always be {@code (0, 0)}.
    */
   public static MapState of(byte scale, boolean showIcons, RegistryKey<World> dimension) {
      return new MapState(0, 0, scale, false, false, showIcons, dimension);
   }

   public static MapState fromNbt(NbtCompound nbt) {
      DataResult var10000 = DimensionType.worldFromDimensionNbt(new Dynamic(NbtOps.INSTANCE, nbt.get("dimension")));
      Logger var10001 = field_25019;
      Objects.requireNonNull(var10001);
      RegistryKey<World> registryKey = (RegistryKey)var10000.resultOrPartial(var10001::error).orElseThrow(() -> {
         return new IllegalArgumentException("Invalid map dimension: " + nbt.get("dimension"));
      });
      int i = nbt.getInt("xCenter");
      int j = nbt.getInt("zCenter");
      byte b = (byte)MathHelper.clamp((int)nbt.getByte("scale"), (int)0, (int)4);
      boolean bl = !nbt.contains("trackingPosition", 1) || nbt.getBoolean("trackingPosition");
      boolean bl2 = nbt.getBoolean("unlimitedTracking");
      boolean bl3 = nbt.getBoolean("locked");
      MapState mapState = new MapState(i, j, b, bl, bl2, bl3, registryKey);
      byte[] bs = nbt.getByteArray("colors");
      if (bs.length == 16384) {
         mapState.colors = bs;
      }

      NbtList nbtList = nbt.getList("banners", 10);

      for(int k = 0; k < nbtList.size(); ++k) {
         MapBannerMarker mapBannerMarker = MapBannerMarker.fromNbt(nbtList.getCompound(k));
         mapState.banners.put(mapBannerMarker.getKey(), mapBannerMarker);
         mapState.addIcon(mapBannerMarker.getIconType(), (WorldAccess)null, mapBannerMarker.getKey(), (double)mapBannerMarker.getPos().getX(), (double)mapBannerMarker.getPos().getZ(), 180.0D, mapBannerMarker.getName());
      }

      NbtList nbtList2 = nbt.getList("frames", 10);

      for(int l = 0; l < nbtList2.size(); ++l) {
         MapFrameMarker mapFrameMarker = MapFrameMarker.fromNbt(nbtList2.getCompound(l));
         mapState.frames.put(mapFrameMarker.getKey(), mapFrameMarker);
         mapState.addIcon(MapIcon.Type.FRAME, (WorldAccess)null, "frame-" + mapFrameMarker.getEntityId(), (double)mapFrameMarker.getPos().getX(), (double)mapFrameMarker.getPos().getZ(), (double)mapFrameMarker.getRotation(), (Text)null);
      }

      return mapState;
   }

   public NbtCompound writeNbt(NbtCompound nbt) {
      DataResult var10000 = Identifier.CODEC.encodeStart(NbtOps.INSTANCE, this.dimension.getValue());
      Logger var10001 = field_25019;
      Objects.requireNonNull(var10001);
      var10000.resultOrPartial(var10001::error).ifPresent((nbtElement) -> {
         nbt.put("dimension", nbtElement);
      });
      nbt.putInt("xCenter", this.centerX);
      nbt.putInt("zCenter", this.centerZ);
      nbt.putByte("scale", this.scale);
      nbt.putByteArray("colors", this.colors);
      nbt.putBoolean("trackingPosition", this.showIcons);
      nbt.putBoolean("unlimitedTracking", this.unlimitedTracking);
      nbt.putBoolean("locked", this.locked);
      NbtList nbtList = new NbtList();
      Iterator var3 = this.banners.values().iterator();

      while(var3.hasNext()) {
         MapBannerMarker mapBannerMarker = (MapBannerMarker)var3.next();
         nbtList.add(mapBannerMarker.getNbt());
      }

      nbt.put("banners", nbtList);
      NbtList nbtList2 = new NbtList();
      Iterator var7 = this.frames.values().iterator();

      while(var7.hasNext()) {
         MapFrameMarker mapFrameMarker = (MapFrameMarker)var7.next();
         nbtList2.add(mapFrameMarker.toNbt());
      }

      nbt.put("frames", nbtList2);
      return nbt;
   }

   public MapState copy() {
      MapState mapState = new MapState(this.centerX, this.centerZ, this.scale, this.showIcons, this.unlimitedTracking, true, this.dimension);
      mapState.banners.putAll(this.banners);
      mapState.icons.putAll(this.icons);
      mapState.field_33992 = this.field_33992;
      System.arraycopy(this.colors, 0, mapState.colors, 0, this.colors.length);
      mapState.markDirty();
      return mapState;
   }

   /**
    * Creates a new map state which is a zoomed out version of the current one.
    * <p>
    * The scale of the new map state is {@code currentScale + zoomOutScale} and clamped between {@code 0} and {@code 4}.
    * <p>
    * The colors are not copied, neither are the icons.
    * 
    * @param zoomOutScale the amount to add to the scale of the map
    */
   public MapState zoomOut(int zoomOutScale) {
      return of((double)this.centerX, (double)this.centerZ, (byte)MathHelper.clamp((int)(this.scale + zoomOutScale), (int)0, (int)4), this.showIcons, this.unlimitedTracking, this.dimension);
   }

   public void update(PlayerEntity player, ItemStack stack) {
      if (!this.updateTrackersByPlayer.containsKey(player)) {
         MapState.PlayerUpdateTracker playerUpdateTracker = new MapState.PlayerUpdateTracker(player);
         this.updateTrackersByPlayer.put(player, playerUpdateTracker);
         this.updateTrackers.add(playerUpdateTracker);
      }

      if (!player.getInventory().contains(stack)) {
         this.removeIcon(player.getName().getString());
      }

      for(int i = 0; i < this.updateTrackers.size(); ++i) {
         MapState.PlayerUpdateTracker playerUpdateTracker2 = (MapState.PlayerUpdateTracker)this.updateTrackers.get(i);
         String string = playerUpdateTracker2.player.getName().getString();
         if (!playerUpdateTracker2.player.isRemoved() && (playerUpdateTracker2.player.getInventory().contains(stack) || stack.isInFrame())) {
            if (!stack.isInFrame() && playerUpdateTracker2.player.world.getRegistryKey() == this.dimension && this.showIcons) {
               this.addIcon(MapIcon.Type.PLAYER, playerUpdateTracker2.player.world, string, playerUpdateTracker2.player.getX(), playerUpdateTracker2.player.getZ(), (double)playerUpdateTracker2.player.getYaw(), (Text)null);
            }
         } else {
            this.updateTrackersByPlayer.remove(playerUpdateTracker2.player);
            this.updateTrackers.remove(playerUpdateTracker2);
            this.removeIcon(string);
         }
      }

      if (stack.isInFrame() && this.showIcons) {
         ItemFrameEntity itemFrameEntity = stack.getFrame();
         BlockPos blockPos = itemFrameEntity.getDecorationBlockPos();
         MapFrameMarker mapFrameMarker = (MapFrameMarker)this.frames.get(MapFrameMarker.getKey(blockPos));
         if (mapFrameMarker != null && itemFrameEntity.getId() != mapFrameMarker.getEntityId() && this.frames.containsKey(mapFrameMarker.getKey())) {
            this.removeIcon("frame-" + mapFrameMarker.getEntityId());
         }

         MapFrameMarker mapFrameMarker2 = new MapFrameMarker(blockPos, itemFrameEntity.getHorizontalFacing().getHorizontal() * 90, itemFrameEntity.getId());
         this.addIcon(MapIcon.Type.FRAME, player.world, "frame-" + itemFrameEntity.getId(), (double)blockPos.getX(), (double)blockPos.getZ(), (double)(itemFrameEntity.getHorizontalFacing().getHorizontal() * 90), (Text)null);
         this.frames.put(mapFrameMarker2.getKey(), mapFrameMarker2);
      }

      NbtCompound nbtCompound = stack.getTag();
      if (nbtCompound != null && nbtCompound.contains("Decorations", 9)) {
         NbtList nbtList = nbtCompound.getList("Decorations", 10);

         for(int j = 0; j < nbtList.size(); ++j) {
            NbtCompound nbtCompound2 = nbtList.getCompound(j);
            if (!this.icons.containsKey(nbtCompound2.getString("id"))) {
               this.addIcon(MapIcon.Type.byId(nbtCompound2.getByte("type")), player.world, nbtCompound2.getString("id"), nbtCompound2.getDouble("x"), nbtCompound2.getDouble("z"), nbtCompound2.getDouble("rot"), (Text)null);
            }
         }
      }

   }

   private void removeIcon(String id) {
      MapIcon mapIcon = (MapIcon)this.icons.remove(id);
      if (mapIcon != null && mapIcon.getType().method_37342()) {
         --this.field_33992;
      }

      this.markIconsDirty();
   }

   public static void addDecorationsNbt(ItemStack stack, BlockPos pos, String id, MapIcon.Type type) {
      NbtList nbtList2;
      if (stack.hasTag() && stack.getTag().contains("Decorations", 9)) {
         nbtList2 = stack.getTag().getList("Decorations", 10);
      } else {
         nbtList2 = new NbtList();
         stack.putSubTag("Decorations", nbtList2);
      }

      NbtCompound nbtCompound = new NbtCompound();
      nbtCompound.putByte("type", type.getId());
      nbtCompound.putString("id", id);
      nbtCompound.putDouble("x", (double)pos.getX());
      nbtCompound.putDouble("z", (double)pos.getZ());
      nbtCompound.putDouble("rot", 180.0D);
      nbtList2.add(nbtCompound);
      if (type.hasTintColor()) {
         NbtCompound nbtCompound2 = stack.getOrCreateSubTag("display");
         nbtCompound2.putInt("MapColor", type.getTintColor());
      }

   }

   private void addIcon(MapIcon.Type type, @Nullable WorldAccess world, String key, double x, double z, double rotation, @Nullable Text text) {
      int i = 1 << this.scale;
      float f = (float)(x - (double)this.centerX) / (float)i;
      float g = (float)(z - (double)this.centerZ) / (float)i;
      byte b = (byte)((int)((double)(f * 2.0F) + 0.5D));
      byte c = (byte)((int)((double)(g * 2.0F) + 0.5D));
      int j = true;
      byte e;
      if (f >= -63.0F && g >= -63.0F && f <= 63.0F && g <= 63.0F) {
         rotation += rotation < 0.0D ? -8.0D : 8.0D;
         e = (byte)((int)(rotation * 16.0D / 360.0D));
         if (this.dimension == World.NETHER && world != null) {
            int k = (int)(world.getLevelProperties().getTimeOfDay() / 10L);
            e = (byte)(k * k * 34187121 + k * 121 >> 15 & 15);
         }
      } else {
         if (type != MapIcon.Type.PLAYER) {
            this.removeIcon(key);
            return;
         }

         int l = true;
         if (Math.abs(f) < 320.0F && Math.abs(g) < 320.0F) {
            type = MapIcon.Type.PLAYER_OFF_MAP;
         } else {
            if (!this.unlimitedTracking) {
               this.removeIcon(key);
               return;
            }

            type = MapIcon.Type.PLAYER_OFF_LIMITS;
         }

         e = 0;
         if (f <= -63.0F) {
            b = -128;
         }

         if (g <= -63.0F) {
            c = -128;
         }

         if (f >= 63.0F) {
            b = 127;
         }

         if (g >= 63.0F) {
            c = 127;
         }
      }

      MapIcon mapIcon = new MapIcon(type, b, c, e, text);
      MapIcon mapIcon2 = (MapIcon)this.icons.put(key, mapIcon);
      if (!mapIcon.equals(mapIcon2)) {
         if (mapIcon2 != null && mapIcon2.getType().method_37342()) {
            --this.field_33992;
         }

         if (type.method_37342()) {
            ++this.field_33992;
         }

         this.markIconsDirty();
      }

   }

   @Nullable
   public Packet<?> getPlayerMarkerPacket(int id, PlayerEntity player) {
      MapState.PlayerUpdateTracker playerUpdateTracker = (MapState.PlayerUpdateTracker)this.updateTrackersByPlayer.get(player);
      return playerUpdateTracker == null ? null : playerUpdateTracker.getPacket(id);
   }

   private void markDirty(int x, int z) {
      this.markDirty();
      Iterator var3 = this.updateTrackers.iterator();

      while(var3.hasNext()) {
         MapState.PlayerUpdateTracker playerUpdateTracker = (MapState.PlayerUpdateTracker)var3.next();
         playerUpdateTracker.markDirty(x, z);
      }

   }

   private void markIconsDirty() {
      this.markDirty();
      this.updateTrackers.forEach(MapState.PlayerUpdateTracker::markIconsDirty);
   }

   public MapState.PlayerUpdateTracker getPlayerSyncData(PlayerEntity player) {
      MapState.PlayerUpdateTracker playerUpdateTracker = (MapState.PlayerUpdateTracker)this.updateTrackersByPlayer.get(player);
      if (playerUpdateTracker == null) {
         playerUpdateTracker = new MapState.PlayerUpdateTracker(player);
         this.updateTrackersByPlayer.put(player, playerUpdateTracker);
         this.updateTrackers.add(playerUpdateTracker);
      }

      return playerUpdateTracker;
   }

   public boolean addBanner(WorldAccess world, BlockPos pos) {
      double d = (double)pos.getX() + 0.5D;
      double e = (double)pos.getZ() + 0.5D;
      int i = 1 << this.scale;
      double f = (d - (double)this.centerX) / (double)i;
      double g = (e - (double)this.centerZ) / (double)i;
      int j = true;
      if (f >= -63.0D && g >= -63.0D && f <= 63.0D && g <= 63.0D) {
         MapBannerMarker mapBannerMarker = MapBannerMarker.fromWorldBlock(world, pos);
         if (mapBannerMarker == null) {
            return false;
         }

         if (this.banners.remove(mapBannerMarker.getKey(), mapBannerMarker)) {
            this.removeIcon(mapBannerMarker.getKey());
            return true;
         }

         if (!this.method_37343(256)) {
            this.banners.put(mapBannerMarker.getKey(), mapBannerMarker);
            this.addIcon(mapBannerMarker.getIconType(), world, mapBannerMarker.getKey(), d, e, 180.0D, mapBannerMarker.getName());
            return true;
         }
      }

      return false;
   }

   public void removeBanner(BlockView world, int x, int z) {
      Iterator iterator = this.banners.values().iterator();

      while(iterator.hasNext()) {
         MapBannerMarker mapBannerMarker = (MapBannerMarker)iterator.next();
         if (mapBannerMarker.getPos().getX() == x && mapBannerMarker.getPos().getZ() == z) {
            MapBannerMarker mapBannerMarker2 = MapBannerMarker.fromWorldBlock(world, mapBannerMarker.getPos());
            if (!mapBannerMarker.equals(mapBannerMarker2)) {
               iterator.remove();
               this.removeIcon(mapBannerMarker.getKey());
            }
         }
      }

   }

   public Collection<MapBannerMarker> getBanners() {
      return this.banners.values();
   }

   public void removeFrame(BlockPos pos, int id) {
      this.removeIcon("frame-" + id);
      this.frames.remove(MapFrameMarker.getKey(pos));
   }

   /**
    * Sets the color at the specified coordinates if the current color is different.
    * 
    * @return {@code true} if the color has been updated, else {@code false}
    */
   public boolean putColor(int x, int z, byte color) {
      byte b = this.colors[x + z * 128];
      if (b != color) {
         this.setColor(x, z, color);
         return true;
      } else {
         return false;
      }
   }

   public void setColor(int x, int z, byte color) {
      this.colors[x + z * 128] = color;
      this.markDirty(x, z);
   }

   public boolean hasMonumentIcon() {
      Iterator var1 = this.icons.values().iterator();

      MapIcon mapIcon;
      do {
         if (!var1.hasNext()) {
            return false;
         }

         mapIcon = (MapIcon)var1.next();
      } while(mapIcon.getType() != MapIcon.Type.MANSION && mapIcon.getType() != MapIcon.Type.MONUMENT);

      return true;
   }

   public void replaceIcons(List<MapIcon> icons) {
      this.icons.clear();
      this.field_33992 = 0;

      for(int i = 0; i < icons.size(); ++i) {
         MapIcon mapIcon = (MapIcon)icons.get(i);
         this.icons.put("icon-" + i, mapIcon);
         if (mapIcon.getType().method_37342()) {
            ++this.field_33992;
         }
      }

   }

   public Iterable<MapIcon> getIcons() {
      return this.icons.values();
   }

   public boolean method_37343(int i) {
      return this.field_33992 >= i;
   }

   public class PlayerUpdateTracker {
      public final PlayerEntity player;
      private boolean dirty = true;
      private int startX;
      private int startZ;
      private int endX = 127;
      private int endZ = 127;
      private boolean iconsDirty = true;
      private int emptyPacketsRequested;
      public int field_131;

      PlayerUpdateTracker(PlayerEntity player) {
         this.player = player;
      }

      private MapState.UpdateData getMapUpdateData() {
         int i = this.startX;
         int j = this.startZ;
         int k = this.endX + 1 - this.startX;
         int l = this.endZ + 1 - this.startZ;
         byte[] bs = new byte[k * l];

         for(int m = 0; m < k; ++m) {
            for(int n = 0; n < l; ++n) {
               bs[m + n * k] = MapState.this.colors[i + m + (j + n) * 128];
            }
         }

         return new MapState.UpdateData(i, j, k, l, bs);
      }

      @Nullable
      Packet<?> getPacket(int mapId) {
         MapState.UpdateData updateData2;
         if (this.dirty) {
            this.dirty = false;
            updateData2 = this.getMapUpdateData();
         } else {
            updateData2 = null;
         }

         Collection collection2;
         if (this.iconsDirty && this.emptyPacketsRequested++ % 5 == 0) {
            this.iconsDirty = false;
            collection2 = MapState.this.icons.values();
         } else {
            collection2 = null;
         }

         return collection2 == null && updateData2 == null ? null : new MapUpdateS2CPacket(mapId, MapState.this.scale, MapState.this.locked, collection2, updateData2);
      }

      void markDirty(int startX, int startZ) {
         if (this.dirty) {
            this.startX = Math.min(this.startX, startX);
            this.startZ = Math.min(this.startZ, startZ);
            this.endX = Math.max(this.endX, startX);
            this.endZ = Math.max(this.endZ, startZ);
         } else {
            this.dirty = true;
            this.startX = startX;
            this.startZ = startZ;
            this.endX = startX;
            this.endZ = startZ;
         }

      }

      private void markIconsDirty() {
         this.iconsDirty = true;
      }
   }

   public static class UpdateData {
      public final int startX;
      public final int startZ;
      public final int width;
      public final int height;
      public final byte[] colors;

      public UpdateData(int startX, int startZ, int width, int height, byte[] colors) {
         this.startX = startX;
         this.startZ = startZ;
         this.width = width;
         this.height = height;
         this.colors = colors;
      }

      public void setColorsTo(MapState mapState) {
         for(int i = 0; i < this.width; ++i) {
            for(int j = 0; j < this.height; ++j) {
               mapState.setColor(this.startX + i, this.startZ + j, this.colors[i + j * this.width]);
            }
         }

      }
   }
}
