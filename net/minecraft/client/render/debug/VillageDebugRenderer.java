package net.minecraft.client.render.debug;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Position;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class VillageDebugRenderer implements DebugRenderer.Renderer {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final boolean field_32874 = true;
   private static final boolean field_32875 = false;
   private static final boolean field_32876 = false;
   private static final boolean field_32877 = false;
   private static final boolean field_32878 = false;
   private static final boolean field_32879 = false;
   private static final boolean field_32880 = false;
   private static final boolean field_32881 = false;
   private static final boolean field_32882 = true;
   private static final boolean field_32883 = true;
   private static final boolean field_32884 = true;
   private static final boolean field_32885 = true;
   private static final boolean field_32886 = true;
   private static final boolean field_32887 = true;
   private static final boolean field_32888 = true;
   private static final boolean field_32889 = true;
   private static final boolean field_32890 = true;
   private static final boolean field_32891 = true;
   private static final boolean field_32892 = true;
   private static final boolean field_32893 = true;
   private static final int field_32894 = 30;
   private static final int field_32895 = 30;
   private static final int field_32896 = 8;
   private static final float field_32897 = 0.02F;
   private static final int field_32898 = -1;
   private static final int field_32867 = -256;
   private static final int field_32868 = -16711681;
   private static final int field_32869 = -16711936;
   private static final int field_32870 = -3355444;
   private static final int field_32871 = -98404;
   private static final int field_32872 = -65536;
   private static final int field_32873 = -23296;
   private final MinecraftClient client;
   private final Map<BlockPos, VillageDebugRenderer.PointOfInterest> pointsOfInterest = Maps.newHashMap();
   private final Map<UUID, VillageDebugRenderer.Brain> brains = Maps.newHashMap();
   @Nullable
   private UUID targetedEntity;

   public VillageDebugRenderer(MinecraftClient client) {
      this.client = client;
   }

   public void clear() {
      this.pointsOfInterest.clear();
      this.brains.clear();
      this.targetedEntity = null;
   }

   public void addPointOfInterest(VillageDebugRenderer.PointOfInterest poi) {
      this.pointsOfInterest.put(poi.pos, poi);
   }

   public void removePointOfInterest(BlockPos pos) {
      this.pointsOfInterest.remove(pos);
   }

   public void setFreeTicketCount(BlockPos pos, int freeTicketCount) {
      VillageDebugRenderer.PointOfInterest pointOfInterest = (VillageDebugRenderer.PointOfInterest)this.pointsOfInterest.get(pos);
      if (pointOfInterest == null) {
         LOGGER.warn((String)"Strange, setFreeTicketCount was called for an unknown POI: {}", (Object)pos);
      } else {
         pointOfInterest.freeTicketCount = freeTicketCount;
      }
   }

   public void addBrain(VillageDebugRenderer.Brain brain) {
      this.brains.put(brain.uuid, brain);
   }

   public void removeBrain(int entityId) {
      this.brains.values().removeIf((brain) -> {
         return brain.entityId == entityId;
      });
   }

   public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, double cameraX, double cameraY, double cameraZ) {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableTexture();
      this.removeRemovedBrains();
      this.draw(cameraX, cameraY, cameraZ);
      RenderSystem.enableTexture();
      RenderSystem.disableBlend();
      if (!this.client.player.isSpectator()) {
         this.updateTargetedEntity();
      }

   }

   private void removeRemovedBrains() {
      this.brains.entrySet().removeIf((entry) -> {
         Entity entity = this.client.world.getEntityById(((VillageDebugRenderer.Brain)entry.getValue()).entityId);
         return entity == null || entity.isRemoved();
      });
   }

   private void draw(double x, double y, double z) {
      BlockPos blockPos = new BlockPos(x, y, z);
      this.brains.values().forEach((brain) -> {
         if (this.isClose(brain)) {
            this.drawBrain(brain, x, y, z);
         }

      });
      Iterator var8 = this.pointsOfInterest.keySet().iterator();

      while(var8.hasNext()) {
         BlockPos blockPos2 = (BlockPos)var8.next();
         if (blockPos.isWithinDistance(blockPos2, 30.0D)) {
            drawPointOfInterest(blockPos2);
         }
      }

      this.pointsOfInterest.values().forEach((poi) -> {
         if (blockPos.isWithinDistance(poi.pos, 30.0D)) {
            this.drawPointOfInterestInfo(poi);
         }

      });
      this.getGhostPointsOfInterest().forEach((pos, brains) -> {
         if (blockPos.isWithinDistance(pos, 30.0D)) {
            this.drawGhostPointOfInterest(pos, brains);
         }

      });
   }

   private static void drawPointOfInterest(BlockPos pos) {
      float f = 0.05F;
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      DebugRenderer.drawBox(pos, 0.05F, 0.2F, 0.2F, 1.0F, 0.3F);
   }

   private void drawGhostPointOfInterest(BlockPos pos, List<String> brains) {
      float f = 0.05F;
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      DebugRenderer.drawBox(pos, 0.05F, 0.2F, 0.2F, 1.0F, 0.3F);
      drawString(brains.makeConcatWithConstants<invokedynamic>(brains), (BlockPos)pos, 0, -256);
      drawString("Ghost POI", (BlockPos)pos, 1, -65536);
   }

   private void drawPointOfInterestInfo(VillageDebugRenderer.PointOfInterest pointOfInterest) {
      int i = 0;
      Set<String> set = this.getVillagerNames(pointOfInterest);
      if (set.size() < 4) {
         drawString("Owners: " + set, (VillageDebugRenderer.PointOfInterest)pointOfInterest, i, -256);
      } else {
         drawString(set.size() + " ticket holders", (VillageDebugRenderer.PointOfInterest)pointOfInterest, i, -256);
      }

      int i = i + 1;
      Set<String> set2 = this.method_29385(pointOfInterest);
      if (set2.size() < 4) {
         drawString("Candidates: " + set2, (VillageDebugRenderer.PointOfInterest)pointOfInterest, i, -23296);
      } else {
         drawString(set2.size() + " potential owners", (VillageDebugRenderer.PointOfInterest)pointOfInterest, i, -23296);
      }

      ++i;
      drawString("Free tickets: " + pointOfInterest.freeTicketCount, (VillageDebugRenderer.PointOfInterest)pointOfInterest, i, -256);
      ++i;
      drawString(pointOfInterest.field_18932, (VillageDebugRenderer.PointOfInterest)pointOfInterest, i, -1);
   }

   private void drawPath(VillageDebugRenderer.Brain brain, double cameraX, double cameraY, double cameraZ) {
      if (brain.path != null) {
         PathfindingDebugRenderer.drawPath(brain.path, 0.5F, false, false, cameraX, cameraY, cameraZ);
      }

   }

   private void drawBrain(VillageDebugRenderer.Brain brain, double cameraX, double cameraY, double cameraZ) {
      boolean bl = this.isTargeted(brain);
      int i = 0;
      drawString(brain.pos, i, brain.field_19328, -1, 0.03F);
      int i = i + 1;
      if (bl) {
         drawString(brain.pos, i, brain.profession + " " + brain.xp + " xp", -1, 0.02F);
         ++i;
      }

      if (bl) {
         int j = brain.health < brain.maxHealth ? -23296 : -1;
         Position var10000 = brain.pos;
         String var10002 = String.format("%.1f", brain.health);
         drawString(var10000, i, "health: " + var10002 + " / " + String.format("%.1f", brain.maxHealth), j, 0.02F);
         ++i;
      }

      if (bl && !brain.field_19372.equals("")) {
         drawString(brain.pos, i, brain.field_19372, -98404, 0.02F);
         ++i;
      }

      String string4;
      Iterator var13;
      if (bl) {
         for(var13 = brain.field_18928.iterator(); var13.hasNext(); ++i) {
            string4 = (String)var13.next();
            drawString(brain.pos, i, string4, -16711681, 0.02F);
         }
      }

      if (bl) {
         for(var13 = brain.field_18927.iterator(); var13.hasNext(); ++i) {
            string4 = (String)var13.next();
            drawString(brain.pos, i, string4, -16711936, 0.02F);
         }
      }

      if (brain.wantsGolem) {
         drawString(brain.pos, i, "Wants Golem", -23296, 0.02F);
         ++i;
      }

      if (bl) {
         for(var13 = brain.field_19375.iterator(); var13.hasNext(); ++i) {
            string4 = (String)var13.next();
            if (string4.startsWith(brain.field_19328)) {
               drawString(brain.pos, i, string4, -1, 0.02F);
            } else {
               drawString(brain.pos, i, string4, -23296, 0.02F);
            }
         }
      }

      if (bl) {
         for(var13 = Lists.reverse(brain.field_19374).iterator(); var13.hasNext(); ++i) {
            string4 = (String)var13.next();
            drawString(brain.pos, i, string4, -3355444, 0.02F);
         }
      }

      if (bl) {
         this.drawPath(brain, cameraX, cameraY, cameraZ);
      }

   }

   private static void drawString(String string, VillageDebugRenderer.PointOfInterest pointOfInterest, int offsetY, int color) {
      BlockPos blockPos = pointOfInterest.pos;
      drawString(string, blockPos, offsetY, color);
   }

   private static void drawString(String string, BlockPos pos, int offsetY, int color) {
      double d = 1.3D;
      double e = 0.2D;
      double f = (double)pos.getX() + 0.5D;
      double g = (double)pos.getY() + 1.3D + (double)offsetY * 0.2D;
      double h = (double)pos.getZ() + 0.5D;
      DebugRenderer.drawString(string, f, g, h, color, 0.02F, true, 0.0F, true);
   }

   private static void drawString(Position pos, int offsetY, String string, int color, float size) {
      double d = 2.4D;
      double e = 0.25D;
      BlockPos blockPos = new BlockPos(pos);
      double f = (double)blockPos.getX() + 0.5D;
      double g = pos.getY() + 2.4D + (double)offsetY * 0.25D;
      double h = (double)blockPos.getZ() + 0.5D;
      float i = 0.5F;
      DebugRenderer.drawString(string, f, g, h, color, size, false, 0.5F, true);
   }

   private Set<String> getVillagerNames(VillageDebugRenderer.PointOfInterest pointOfInterest) {
      return (Set)this.getBrains(pointOfInterest.pos).stream().map(NameGenerator::name).collect(Collectors.toSet());
   }

   private Set<String> method_29385(VillageDebugRenderer.PointOfInterest pointOfInterest) {
      return (Set)this.method_29386(pointOfInterest.pos).stream().map(NameGenerator::name).collect(Collectors.toSet());
   }

   private boolean isTargeted(VillageDebugRenderer.Brain brain) {
      return Objects.equals(this.targetedEntity, brain.uuid);
   }

   private boolean isClose(VillageDebugRenderer.Brain brain) {
      PlayerEntity playerEntity = this.client.player;
      BlockPos blockPos = new BlockPos(playerEntity.getX(), brain.pos.getY(), playerEntity.getZ());
      BlockPos blockPos2 = new BlockPos(brain.pos);
      return blockPos.isWithinDistance(blockPos2, 30.0D);
   }

   private Collection<UUID> getBrains(BlockPos pointOfInterest) {
      return (Collection)this.brains.values().stream().filter((brain) -> {
         return brain.isPointOfInterest(pointOfInterest);
      }).map(VillageDebugRenderer.Brain::getUuid).collect(Collectors.toSet());
   }

   private Collection<UUID> method_29386(BlockPos blockPos) {
      return (Collection)this.brains.values().stream().filter((brain) -> {
         return brain.method_29388(blockPos);
      }).map(VillageDebugRenderer.Brain::getUuid).collect(Collectors.toSet());
   }

   private Map<BlockPos, List<String>> getGhostPointsOfInterest() {
      Map<BlockPos, List<String>> map = Maps.newHashMap();
      Iterator var2 = this.brains.values().iterator();

      while(var2.hasNext()) {
         VillageDebugRenderer.Brain brain = (VillageDebugRenderer.Brain)var2.next();
         Iterator var4 = Iterables.concat(brain.pointsOfInterest, brain.field_25287).iterator();

         while(var4.hasNext()) {
            BlockPos blockPos = (BlockPos)var4.next();
            if (!this.pointsOfInterest.containsKey(blockPos)) {
               ((List)map.computeIfAbsent(blockPos, (blockPosx) -> {
                  return Lists.newArrayList();
               })).add(brain.field_19328);
            }
         }
      }

      return map;
   }

   private void updateTargetedEntity() {
      DebugRenderer.getTargetedEntity(this.client.getCameraEntity(), 8).ifPresent((entity) -> {
         this.targetedEntity = entity.getUuid();
      });
   }

   @Environment(EnvType.CLIENT)
   public static class PointOfInterest {
      public final BlockPos pos;
      public String field_18932;
      public int freeTicketCount;

      public PointOfInterest(BlockPos pos, String string, int freeTicketCount) {
         this.pos = pos;
         this.field_18932 = string;
         this.freeTicketCount = freeTicketCount;
      }
   }

   @Environment(EnvType.CLIENT)
   public static class Brain {
      public final UUID uuid;
      public final int entityId;
      public final String field_19328;
      public final String profession;
      public final int xp;
      public final float health;
      public final float maxHealth;
      public final Position pos;
      public final String field_19372;
      public final Path path;
      public final boolean wantsGolem;
      public final List<String> field_18927 = Lists.newArrayList();
      public final List<String> field_18928 = Lists.newArrayList();
      public final List<String> field_19374 = Lists.newArrayList();
      public final List<String> field_19375 = Lists.newArrayList();
      public final Set<BlockPos> pointsOfInterest = Sets.newHashSet();
      public final Set<BlockPos> field_25287 = Sets.newHashSet();

      public Brain(UUID uuid, int entityId, String string, String profession, int xp, float health, float maxHealth, Position pos, String string2, @Nullable Path path, boolean wantsGolem) {
         this.uuid = uuid;
         this.entityId = entityId;
         this.field_19328 = string;
         this.profession = profession;
         this.xp = xp;
         this.health = health;
         this.maxHealth = maxHealth;
         this.pos = pos;
         this.field_19372 = string2;
         this.path = path;
         this.wantsGolem = wantsGolem;
      }

      boolean isPointOfInterest(BlockPos pos) {
         Stream var10000 = this.pointsOfInterest.stream();
         Objects.requireNonNull(pos);
         return var10000.anyMatch(pos::equals);
      }

      boolean method_29388(BlockPos blockPos) {
         return this.field_25287.contains(blockPos);
      }

      public UUID getUuid() {
         return this.uuid;
      }
   }
}
