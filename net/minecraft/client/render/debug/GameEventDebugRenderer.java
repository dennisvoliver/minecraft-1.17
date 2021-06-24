package net.minecraft.client.render.debug;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.PositionSource;
import net.minecraft.world.event.listener.GameEventListener;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class GameEventDebugRenderer implements DebugRenderer.Renderer {
   private final MinecraftClient client;
   private static final int field_32899 = 32;
   private static final float field_32900 = 1.0F;
   private final List<GameEventDebugRenderer.Entry> entries = Lists.newArrayList();
   private final List<GameEventDebugRenderer.Listener> listeners = Lists.newArrayList();

   public GameEventDebugRenderer(MinecraftClient client) {
      this.client = client;
   }

   public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, double cameraX, double cameraY, double cameraZ) {
      World world = this.client.world;
      if (world == null) {
         this.entries.clear();
         this.listeners.clear();
      } else {
         BlockPos blockPos = new BlockPos(cameraX, 0.0D, cameraZ);
         this.entries.removeIf(GameEventDebugRenderer.Entry::hasExpired);
         this.listeners.removeIf((listenerx) -> {
            return listenerx.isTooFar(world, blockPos);
         });
         RenderSystem.disableTexture();
         RenderSystem.enableDepthTest();
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getLines());
         Iterator var12 = this.listeners.iterator();

         while(var12.hasNext()) {
            GameEventDebugRenderer.Listener listener = (GameEventDebugRenderer.Listener)var12.next();
            listener.getPos(world).ifPresent((pos) -> {
               int i = pos.getX() - listener.getRange();
               int j = pos.getY() - listener.getRange();
               int k = pos.getZ() - listener.getRange();
               int l = pos.getX() + listener.getRange();
               int m = pos.getY() + listener.getRange();
               int n = pos.getZ() + listener.getRange();
               Vec3f vec3f = new Vec3f(1.0F, 1.0F, 0.0F);
               WorldRenderer.method_22983(matrices, vertexConsumer, VoxelShapes.cuboid(new Box((double)i, (double)j, (double)k, (double)l, (double)m, (double)n)), -cameraX, -cameraY, -cameraZ, vec3f.getX(), vec3f.getY(), vec3f.getZ(), 0.35F);
            });
         }

         RenderSystem.setShader(GameRenderer::getPositionColorShader);
         Tessellator tessellator = Tessellator.getInstance();
         BufferBuilder bufferBuilder = tessellator.getBuffer();
         bufferBuilder.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
         Iterator var14 = this.listeners.iterator();

         GameEventDebugRenderer.Listener listener3;
         while(var14.hasNext()) {
            listener3 = (GameEventDebugRenderer.Listener)var14.next();
            listener3.getPos(world).ifPresent((blockPosx) -> {
               Vec3f vec3f = new Vec3f(1.0F, 1.0F, 0.0F);
               WorldRenderer.drawBox(bufferBuilder, (double)((float)blockPosx.getX() - 0.25F) - cameraX, (double)blockPosx.getY() - cameraY, (double)((float)blockPosx.getZ() - 0.25F) - cameraZ, (double)((float)blockPosx.getX() + 0.25F) - cameraX, (double)blockPosx.getY() - cameraY + 1.0D, (double)((float)blockPosx.getZ() + 0.25F) - cameraZ, vec3f.getX(), vec3f.getY(), vec3f.getZ(), 0.35F);
            });
         }

         tessellator.draw();
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.lineWidth(2.0F);
         RenderSystem.depthMask(false);
         var14 = this.listeners.iterator();

         while(var14.hasNext()) {
            listener3 = (GameEventDebugRenderer.Listener)var14.next();
            listener3.getPos(world).ifPresent((blockPosx) -> {
               DebugRenderer.drawString("Listener Origin", (double)blockPosx.getX(), (double)((float)blockPosx.getY() + 1.8F), (double)blockPosx.getZ(), -1, 0.025F);
               DebugRenderer.drawString((new BlockPos(blockPosx)).toString(), (double)blockPosx.getX(), (double)((float)blockPosx.getY() + 1.5F), (double)blockPosx.getZ(), -6959665, 0.025F);
            });
         }

         var14 = this.entries.iterator();

         while(var14.hasNext()) {
            GameEventDebugRenderer.Entry entry = (GameEventDebugRenderer.Entry)var14.next();
            Vec3d vec3d = entry.pos;
            double d = 0.20000000298023224D;
            double e = vec3d.x - 0.20000000298023224D;
            double f = vec3d.y - 0.20000000298023224D;
            double g = vec3d.z - 0.20000000298023224D;
            double h = vec3d.x + 0.20000000298023224D;
            double i = vec3d.y + 0.20000000298023224D + 0.5D;
            double j = vec3d.z + 0.20000000298023224D;
            method_33089(new Box(e, f, g, h, i, j), 1.0F, 1.0F, 1.0F, 0.2F);
            DebugRenderer.drawString(entry.event.getId(), vec3d.x, vec3d.y + 0.8500000238418579D, vec3d.z, -7564911, 0.0075F);
         }

         RenderSystem.depthMask(true);
         RenderSystem.enableTexture();
         RenderSystem.disableBlend();
      }
   }

   private static void method_33089(Box box, float f, float g, float h, float i) {
      Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
      if (camera.isReady()) {
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         Vec3d vec3d = camera.getPos().negate();
         DebugRenderer.drawBox(box.offset(vec3d), f, g, h, i);
      }
   }

   public void addEvent(GameEvent event, BlockPos pos) {
      this.entries.add(new GameEventDebugRenderer.Entry(Util.getMeasuringTimeMs(), event, Vec3d.ofBottomCenter(pos)));
   }

   public void addListener(PositionSource positionSource, int range) {
      this.listeners.add(new GameEventDebugRenderer.Listener(positionSource, range));
   }

   @Environment(EnvType.CLIENT)
   static class Listener implements GameEventListener {
      public final PositionSource positionSource;
      public final int range;

      public Listener(PositionSource positionSource, int range) {
         this.positionSource = positionSource;
         this.range = range;
      }

      public boolean isTooFar(World world, BlockPos pos) {
         Optional<BlockPos> optional = this.positionSource.getPos(world);
         return !optional.isPresent() || ((BlockPos)optional.get()).getSquaredDistance(pos) <= 1024.0D;
      }

      public Optional<BlockPos> getPos(World world) {
         return this.positionSource.getPos(world);
      }

      public PositionSource getPositionSource() {
         return this.positionSource;
      }

      public int getRange() {
         return this.range;
      }

      public boolean listen(World world, GameEvent event, @Nullable Entity entity, BlockPos pos) {
         return false;
      }
   }

   @Environment(EnvType.CLIENT)
   static class Entry {
      public final long startingMs;
      public final GameEvent event;
      public final Vec3d pos;

      public Entry(long startingMs, GameEvent event, Vec3d pos) {
         this.startingMs = startingMs;
         this.event = event;
         this.pos = pos;
      }

      public boolean hasExpired() {
         return Util.getMeasuringTimeMs() - this.startingMs > 3000L;
      }
   }
}
