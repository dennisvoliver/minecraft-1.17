package net.minecraft.client.render.debug;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
public class GoalSelectorDebugRenderer implements DebugRenderer.Renderer {
   private static final int field_32902 = 160;
   private final MinecraftClient client;
   private final Map<Integer, List<GoalSelectorDebugRenderer.GoalSelector>> goalSelectors = Maps.newHashMap();

   public void clear() {
      this.goalSelectors.clear();
   }

   public void setGoalSelectorList(int index, List<GoalSelectorDebugRenderer.GoalSelector> selectors) {
      this.goalSelectors.put(index, selectors);
   }

   public void removeGoalSelectorList(int index) {
      this.goalSelectors.remove(index);
   }

   public GoalSelectorDebugRenderer(MinecraftClient client) {
      this.client = client;
   }

   public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, double cameraX, double cameraY, double cameraZ) {
      Camera camera = this.client.gameRenderer.getCamera();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableTexture();
      BlockPos blockPos = new BlockPos(camera.getPos().x, 0.0D, camera.getPos().z);
      this.goalSelectors.forEach((integer, list) -> {
         for(int i = 0; i < list.size(); ++i) {
            GoalSelectorDebugRenderer.GoalSelector goalSelector = (GoalSelectorDebugRenderer.GoalSelector)list.get(i);
            if (blockPos.isWithinDistance(goalSelector.pos, 160.0D)) {
               double d = (double)goalSelector.pos.getX() + 0.5D;
               double e = (double)goalSelector.pos.getY() + 2.0D + (double)i * 0.25D;
               double f = (double)goalSelector.pos.getZ() + 0.5D;
               int j = goalSelector.field_18785 ? -16711936 : -3355444;
               DebugRenderer.drawString(goalSelector.name, d, e, f, j);
            }
         }

      });
      RenderSystem.enableDepthTest();
      RenderSystem.enableTexture();
   }

   @Environment(EnvType.CLIENT)
   public static class GoalSelector {
      public final BlockPos pos;
      public final int field_18783;
      public final String name;
      public final boolean field_18785;

      public GoalSelector(BlockPos pos, int i, String name, boolean bl) {
         this.pos = pos;
         this.field_18783 = i;
         this.name = name;
         this.field_18785 = bl;
      }
   }
}
