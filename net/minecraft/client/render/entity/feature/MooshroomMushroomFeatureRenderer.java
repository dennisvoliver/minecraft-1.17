package net.minecraft.client.render.entity.feature;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.CowEntityModel;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.passive.MooshroomEntity;
import net.minecraft.util.math.Vec3f;

@Environment(EnvType.CLIENT)
public class MooshroomMushroomFeatureRenderer<T extends MooshroomEntity> extends FeatureRenderer<T, CowEntityModel<T>> {
   public MooshroomMushroomFeatureRenderer(FeatureRendererContext<T, CowEntityModel<T>> featureRendererContext) {
      super(featureRendererContext);
   }

   public void render(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, T mooshroomEntity, float f, float g, float h, float j, float k, float l) {
      if (!mooshroomEntity.isBaby()) {
         MinecraftClient minecraftClient = MinecraftClient.getInstance();
         boolean bl = minecraftClient.hasOutline(mooshroomEntity) && mooshroomEntity.isInvisible();
         if (!mooshroomEntity.isInvisible() || bl) {
            BlockRenderManager blockRenderManager = minecraftClient.getBlockRenderManager();
            BlockState blockState = mooshroomEntity.getMooshroomType().getMushroomState();
            int m = LivingEntityRenderer.getOverlay(mooshroomEntity, 0.0F);
            BakedModel bakedModel = blockRenderManager.getModel(blockState);
            matrixStack.push();
            matrixStack.translate(0.20000000298023224D, -0.3499999940395355D, 0.5D);
            matrixStack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-48.0F));
            matrixStack.scale(-1.0F, -1.0F, 1.0F);
            matrixStack.translate(-0.5D, -0.5D, -0.5D);
            this.method_37314(matrixStack, vertexConsumerProvider, i, bl, blockRenderManager, blockState, m, bakedModel);
            matrixStack.pop();
            matrixStack.push();
            matrixStack.translate(0.20000000298023224D, -0.3499999940395355D, 0.5D);
            matrixStack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(42.0F));
            matrixStack.translate(0.10000000149011612D, 0.0D, -0.6000000238418579D);
            matrixStack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-48.0F));
            matrixStack.scale(-1.0F, -1.0F, 1.0F);
            matrixStack.translate(-0.5D, -0.5D, -0.5D);
            this.method_37314(matrixStack, vertexConsumerProvider, i, bl, blockRenderManager, blockState, m, bakedModel);
            matrixStack.pop();
            matrixStack.push();
            ((CowEntityModel)this.getContextModel()).getHead().rotate(matrixStack);
            matrixStack.translate(0.0D, -0.699999988079071D, -0.20000000298023224D);
            matrixStack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-78.0F));
            matrixStack.scale(-1.0F, -1.0F, 1.0F);
            matrixStack.translate(-0.5D, -0.5D, -0.5D);
            this.method_37314(matrixStack, vertexConsumerProvider, i, bl, blockRenderManager, blockState, m, bakedModel);
            matrixStack.pop();
         }
      }
   }

   private void method_37314(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, boolean bl, BlockRenderManager blockRenderManager, BlockState blockState, int j, BakedModel bakedModel) {
      if (bl) {
         blockRenderManager.getModelRenderer().render(matrixStack.peek(), vertexConsumerProvider.getBuffer(RenderLayer.getOutline(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)), blockState, bakedModel, 0.0F, 0.0F, 0.0F, i, j);
      } else {
         blockRenderManager.renderBlockAsEntity(blockState, matrixStack, vertexConsumerProvider, i, j);
      }

   }
}
