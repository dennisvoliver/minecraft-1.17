package net.minecraft.client.render.block.entity;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SignBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.OrderedText;
import net.minecraft.util.DyeColor;
import net.minecraft.util.SignType;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;

@Environment(EnvType.CLIENT)
public class SignBlockEntityRenderer implements BlockEntityRenderer<SignBlockEntity> {
   public static final int field_32828 = 90;
   private static final int field_32829 = 10;
   private static final String STICK = "stick";
   private static final int GLOWING_BLACK_COLOR = -988212;
   private static final int RENDER_DISTANCE = MathHelper.square(16);
   private final Map<SignType, SignBlockEntityRenderer.SignModel> typeToModel;
   private final TextRenderer textRenderer;

   public SignBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
      this.typeToModel = (Map)SignType.stream().collect(ImmutableMap.toImmutableMap((signType) -> {
         return signType;
      }, (signType) -> {
         return new SignBlockEntityRenderer.SignModel(ctx.getLayerModelPart(EntityModelLayers.createSign(signType)));
      }));
      this.textRenderer = ctx.getTextRenderer();
   }

   public void render(SignBlockEntity signBlockEntity, float f, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, int j) {
      BlockState blockState = signBlockEntity.getCachedState();
      matrixStack.push();
      float g = 0.6666667F;
      SignType signType = getSignType(blockState.getBlock());
      SignBlockEntityRenderer.SignModel signModel = (SignBlockEntityRenderer.SignModel)this.typeToModel.get(signType);
      float h;
      if (blockState.getBlock() instanceof SignBlock) {
         matrixStack.translate(0.5D, 0.5D, 0.5D);
         h = -((float)((Integer)blockState.get(SignBlock.ROTATION) * 360) / 16.0F);
         matrixStack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(h));
         signModel.stick.visible = true;
      } else {
         matrixStack.translate(0.5D, 0.5D, 0.5D);
         h = -((Direction)blockState.get(WallSignBlock.FACING)).asRotation();
         matrixStack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(h));
         matrixStack.translate(0.0D, -0.3125D, -0.4375D);
         signModel.stick.visible = false;
      }

      matrixStack.push();
      matrixStack.scale(0.6666667F, -0.6666667F, -0.6666667F);
      SpriteIdentifier spriteIdentifier = TexturedRenderLayers.getSignTextureId(signType);
      Objects.requireNonNull(signModel);
      VertexConsumer vertexConsumer = spriteIdentifier.getVertexConsumer(vertexConsumerProvider, signModel::getLayer);
      signModel.root.render(matrixStack, vertexConsumer, i, j);
      matrixStack.pop();
      float l = 0.010416667F;
      matrixStack.translate(0.0D, 0.3333333432674408D, 0.046666666865348816D);
      matrixStack.scale(0.010416667F, -0.010416667F, 0.010416667F);
      int m = getColor(signBlockEntity);
      int n = true;
      OrderedText[] orderedTexts = signBlockEntity.updateSign(MinecraftClient.getInstance().shouldFilterText(), (text) -> {
         List<OrderedText> list = this.textRenderer.wrapLines(text, 90);
         return list.isEmpty() ? OrderedText.EMPTY : (OrderedText)list.get(0);
      });
      int q;
      boolean bl2;
      int r;
      if (signBlockEntity.isGlowingText()) {
         q = signBlockEntity.getTextColor().getSignColor();
         bl2 = shouldRender(signBlockEntity, q);
         r = 15728880;
      } else {
         q = m;
         bl2 = false;
         r = i;
      }

      for(int s = 0; s < 4; ++s) {
         OrderedText orderedText = orderedTexts[s];
         float t = (float)(-this.textRenderer.getWidth(orderedText) / 2);
         if (bl2) {
            this.textRenderer.method_37296(orderedText, t, (float)(s * 10 - 20), q, m, matrixStack.peek().getModel(), vertexConsumerProvider, r);
         } else {
            this.textRenderer.draw((OrderedText)orderedText, t, (float)(s * 10 - 20), q, false, matrixStack.peek().getModel(), vertexConsumerProvider, false, 0, r);
         }
      }

      matrixStack.pop();
   }

   private static boolean shouldRender(SignBlockEntity sign, int signColor) {
      if (signColor == DyeColor.BLACK.getSignColor()) {
         return true;
      } else {
         MinecraftClient minecraftClient = MinecraftClient.getInstance();
         ClientPlayerEntity clientPlayerEntity = minecraftClient.player;
         if (clientPlayerEntity != null && minecraftClient.options.getPerspective().isFirstPerson() && clientPlayerEntity.isUsingSpyglass()) {
            return true;
         } else {
            Entity entity = minecraftClient.getCameraEntity();
            return entity != null && entity.squaredDistanceTo(Vec3d.ofCenter(sign.getPos())) < (double)RENDER_DISTANCE;
         }
      }
   }

   private static int getColor(SignBlockEntity sign) {
      int i = sign.getTextColor().getSignColor();
      double d = 0.4D;
      int j = (int)((double)NativeImage.getRed(i) * 0.4D);
      int k = (int)((double)NativeImage.getGreen(i) * 0.4D);
      int l = (int)((double)NativeImage.getBlue(i) * 0.4D);
      return i == DyeColor.BLACK.getSignColor() && sign.isGlowingText() ? -988212 : NativeImage.getAbgrColor(0, l, k, j);
   }

   public static SignType getSignType(Block block) {
      SignType signType2;
      if (block instanceof AbstractSignBlock) {
         signType2 = ((AbstractSignBlock)block).getSignType();
      } else {
         signType2 = SignType.OAK;
      }

      return signType2;
   }

   public static SignBlockEntityRenderer.SignModel createSignModel(EntityModelLoader entityModelLoader, SignType type) {
      return new SignBlockEntityRenderer.SignModel(entityModelLoader.getModelPart(EntityModelLayers.createSign(type)));
   }

   public static TexturedModelData getTexturedModelData() {
      ModelData modelData = new ModelData();
      ModelPartData modelPartData = modelData.getRoot();
      modelPartData.addChild("sign", ModelPartBuilder.create().uv(0, 0).cuboid(-12.0F, -14.0F, -1.0F, 24.0F, 12.0F, 2.0F), ModelTransform.NONE);
      modelPartData.addChild("stick", ModelPartBuilder.create().uv(0, 14).cuboid(-1.0F, -2.0F, -1.0F, 2.0F, 14.0F, 2.0F), ModelTransform.NONE);
      return TexturedModelData.of(modelData, 64, 32);
   }

   @Environment(EnvType.CLIENT)
   public static final class SignModel extends Model {
      public final ModelPart root;
      public final ModelPart stick;

      public SignModel(ModelPart root) {
         super(RenderLayer::getEntityCutoutNoCull);
         this.root = root;
         this.stick = root.getChild("stick");
      }

      public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
         this.root.render(matrices, vertices, light, overlay, red, green, blue, alpha);
      }
   }
}
