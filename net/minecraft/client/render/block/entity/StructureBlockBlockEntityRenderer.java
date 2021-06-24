package net.minecraft.client.render.block.entity;

import java.util.Iterator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.StructureBlockBlockEntity;
import net.minecraft.block.enums.StructureBlockMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.BlockView;

@Environment(EnvType.CLIENT)
public class StructureBlockBlockEntityRenderer implements BlockEntityRenderer<StructureBlockBlockEntity> {
   public StructureBlockBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
   }

   public void render(StructureBlockBlockEntity structureBlockBlockEntity, float f, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, int j) {
      if (MinecraftClient.getInstance().player.isCreativeLevelTwoOp() || MinecraftClient.getInstance().player.isSpectator()) {
         BlockPos blockPos = structureBlockBlockEntity.getOffset();
         Vec3i vec3i = structureBlockBlockEntity.getSize();
         if (vec3i.getX() >= 1 && vec3i.getY() >= 1 && vec3i.getZ() >= 1) {
            if (structureBlockBlockEntity.getMode() == StructureBlockMode.SAVE || structureBlockBlockEntity.getMode() == StructureBlockMode.LOAD) {
               double d = (double)blockPos.getX();
               double e = (double)blockPos.getZ();
               double g = (double)blockPos.getY();
               double h = g + (double)vec3i.getY();
               double o;
               double p;
               switch(structureBlockBlockEntity.getMirror()) {
               case LEFT_RIGHT:
                  o = (double)vec3i.getX();
                  p = (double)(-vec3i.getZ());
                  break;
               case FRONT_BACK:
                  o = (double)(-vec3i.getX());
                  p = (double)vec3i.getZ();
                  break;
               default:
                  o = (double)vec3i.getX();
                  p = (double)vec3i.getZ();
               }

               double ac;
               double ad;
               double ae;
               double af;
               switch(structureBlockBlockEntity.getRotation()) {
               case CLOCKWISE_90:
                  ac = p < 0.0D ? d : d + 1.0D;
                  ad = o < 0.0D ? e + 1.0D : e;
                  ae = ac - p;
                  af = ad + o;
                  break;
               case CLOCKWISE_180:
                  ac = o < 0.0D ? d : d + 1.0D;
                  ad = p < 0.0D ? e : e + 1.0D;
                  ae = ac - o;
                  af = ad - p;
                  break;
               case COUNTERCLOCKWISE_90:
                  ac = p < 0.0D ? d + 1.0D : d;
                  ad = o < 0.0D ? e : e + 1.0D;
                  ae = ac + p;
                  af = ad - o;
                  break;
               default:
                  ac = o < 0.0D ? d + 1.0D : d;
                  ad = p < 0.0D ? e + 1.0D : e;
                  ae = ac + o;
                  af = ad + p;
               }

               float ag = 1.0F;
               float ah = 0.9F;
               float ai = 0.5F;
               VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(RenderLayer.getLines());
               if (structureBlockBlockEntity.getMode() == StructureBlockMode.SAVE || structureBlockBlockEntity.shouldShowBoundingBox()) {
                  WorldRenderer.drawBox(matrixStack, vertexConsumer, ac, g, ad, ae, h, af, 0.9F, 0.9F, 0.9F, 1.0F, 0.5F, 0.5F, 0.5F);
               }

               if (structureBlockBlockEntity.getMode() == StructureBlockMode.SAVE && structureBlockBlockEntity.shouldShowAir()) {
                  this.renderInvisibleBlocks(structureBlockBlockEntity, vertexConsumer, blockPos, matrixStack);
               }

            }
         }
      }
   }

   private void renderInvisibleBlocks(StructureBlockBlockEntity entity, VertexConsumer vertices, BlockPos pos, MatrixStack matrixStack) {
      BlockView blockView = entity.getWorld();
      BlockPos blockPos = entity.getPos();
      BlockPos blockPos2 = blockPos.add(pos);
      Iterator var8 = BlockPos.iterate(blockPos2, blockPos2.add(entity.getSize()).add(-1, -1, -1)).iterator();

      while(true) {
         BlockPos blockPos3;
         boolean bl;
         boolean bl2;
         boolean bl3;
         boolean bl4;
         boolean bl5;
         do {
            if (!var8.hasNext()) {
               return;
            }

            blockPos3 = (BlockPos)var8.next();
            BlockState blockState = blockView.getBlockState(blockPos3);
            bl = blockState.isAir();
            bl2 = blockState.isOf(Blocks.STRUCTURE_VOID);
            bl3 = blockState.isOf(Blocks.BARRIER);
            bl4 = blockState.isOf(Blocks.LIGHT);
            bl5 = bl2 || bl3 || bl4;
         } while(!bl && !bl5);

         float f = bl ? 0.05F : 0.0F;
         double d = (double)((float)(blockPos3.getX() - blockPos.getX()) + 0.45F - f);
         double e = (double)((float)(blockPos3.getY() - blockPos.getY()) + 0.45F - f);
         double g = (double)((float)(blockPos3.getZ() - blockPos.getZ()) + 0.45F - f);
         double h = (double)((float)(blockPos3.getX() - blockPos.getX()) + 0.55F + f);
         double i = (double)((float)(blockPos3.getY() - blockPos.getY()) + 0.55F + f);
         double j = (double)((float)(blockPos3.getZ() - blockPos.getZ()) + 0.55F + f);
         if (bl) {
            WorldRenderer.drawBox(matrixStack, vertices, d, e, g, h, i, j, 0.5F, 0.5F, 1.0F, 1.0F, 0.5F, 0.5F, 1.0F);
         } else if (bl2) {
            WorldRenderer.drawBox(matrixStack, vertices, d, e, g, h, i, j, 1.0F, 0.75F, 0.75F, 1.0F, 1.0F, 0.75F, 0.75F);
         } else if (bl3) {
            WorldRenderer.drawBox(matrixStack, vertices, d, e, g, h, i, j, 1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F);
         } else if (bl4) {
            WorldRenderer.drawBox(matrixStack, vertices, d, e, g, h, i, j, 1.0F, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F, 0.0F);
         }
      }
   }

   public boolean rendersOutsideBoundingBox(StructureBlockBlockEntity structureBlockBlockEntity) {
      return true;
   }

   public int getRenderDistance() {
      return 96;
   }
}
