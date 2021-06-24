package net.minecraft.client.render.item;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.AbstractBannerBlock;
import net.minecraft.block.AbstractSkullBlock;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.SkullBlock;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.block.entity.BannerPattern;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.ConduitBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.block.entity.TrappedChestBlockEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BannerBlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.SkullBlockEntityModel;
import net.minecraft.client.render.block.entity.SkullBlockEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.client.render.entity.model.ShieldEntityModel;
import net.minecraft.client.render.entity.model.TridentEntityModel;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ShieldItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.apache.commons.lang3.StringUtils;

@Environment(EnvType.CLIENT)
public class BuiltinModelItemRenderer implements SynchronousResourceReloader {
   private static final ShulkerBoxBlockEntity[] RENDER_SHULKER_BOX_DYED = (ShulkerBoxBlockEntity[])Arrays.stream(DyeColor.values()).sorted(Comparator.comparingInt(DyeColor::getId)).map((dyeColor) -> {
      return new ShulkerBoxBlockEntity(dyeColor, BlockPos.ORIGIN, Blocks.SHULKER_BOX.getDefaultState());
   }).toArray((i) -> {
      return new ShulkerBoxBlockEntity[i];
   });
   private static final ShulkerBoxBlockEntity RENDER_SHULKER_BOX;
   private final ChestBlockEntity renderChestNormal;
   private final ChestBlockEntity renderChestTrapped;
   private final EnderChestBlockEntity renderChestEnder;
   private final BannerBlockEntity renderBanner;
   private final BedBlockEntity renderBed;
   private final ConduitBlockEntity renderConduit;
   private ShieldEntityModel modelShield;
   private TridentEntityModel modelTrident;
   private Map<SkullBlock.SkullType, SkullBlockEntityModel> skullModels;
   private final BlockEntityRenderDispatcher blockEntityRenderDispatcher;
   private final EntityModelLoader entityModelLoader;

   public BuiltinModelItemRenderer(BlockEntityRenderDispatcher blockEntityRenderDispatcher, EntityModelLoader entityModelLoader) {
      this.renderChestNormal = new ChestBlockEntity(BlockPos.ORIGIN, Blocks.CHEST.getDefaultState());
      this.renderChestTrapped = new TrappedChestBlockEntity(BlockPos.ORIGIN, Blocks.TRAPPED_CHEST.getDefaultState());
      this.renderChestEnder = new EnderChestBlockEntity(BlockPos.ORIGIN, Blocks.ENDER_CHEST.getDefaultState());
      this.renderBanner = new BannerBlockEntity(BlockPos.ORIGIN, Blocks.WHITE_BANNER.getDefaultState());
      this.renderBed = new BedBlockEntity(BlockPos.ORIGIN, Blocks.RED_BED.getDefaultState());
      this.renderConduit = new ConduitBlockEntity(BlockPos.ORIGIN, Blocks.CONDUIT.getDefaultState());
      this.blockEntityRenderDispatcher = blockEntityRenderDispatcher;
      this.entityModelLoader = entityModelLoader;
   }

   public void reload(ResourceManager manager) {
      this.modelShield = new ShieldEntityModel(this.entityModelLoader.getModelPart(EntityModelLayers.SHIELD));
      this.modelTrident = new TridentEntityModel(this.entityModelLoader.getModelPart(EntityModelLayers.TRIDENT));
      this.skullModels = SkullBlockEntityRenderer.getModels(this.entityModelLoader);
   }

   public void render(ItemStack stack, ModelTransformation.Mode mode, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
      Item item = stack.getItem();
      if (item instanceof BlockItem) {
         Block block = ((BlockItem)item).getBlock();
         if (block instanceof AbstractSkullBlock) {
            GameProfile gameProfile = null;
            if (stack.hasTag()) {
               NbtCompound nbtCompound = stack.getTag();
               if (nbtCompound.contains("SkullOwner", 10)) {
                  gameProfile = NbtHelper.toGameProfile(nbtCompound.getCompound("SkullOwner"));
               } else if (nbtCompound.contains("SkullOwner", 8) && !StringUtils.isBlank(nbtCompound.getString("SkullOwner"))) {
                  gameProfile = new GameProfile((UUID)null, nbtCompound.getString("SkullOwner"));
                  nbtCompound.remove("SkullOwner");
                  SkullBlockEntity.loadProperties(gameProfile, (gameProfilex) -> {
                     nbtCompound.put("SkullOwner", NbtHelper.writeGameProfile(new NbtCompound(), gameProfilex));
                  });
               }
            }

            SkullBlock.SkullType skullType = ((AbstractSkullBlock)block).getSkullType();
            SkullBlockEntityModel skullBlockEntityModel = (SkullBlockEntityModel)this.skullModels.get(skullType);
            RenderLayer renderLayer = SkullBlockEntityRenderer.getRenderLayer(skullType, gameProfile);
            SkullBlockEntityRenderer.renderSkull((Direction)null, 180.0F, 0.0F, matrices, vertexConsumers, light, skullBlockEntityModel, renderLayer);
         } else {
            BlockState blockState = block.getDefaultState();
            Object blockEntity9;
            if (block instanceof AbstractBannerBlock) {
               this.renderBanner.readFrom(stack, ((AbstractBannerBlock)block).getColor());
               blockEntity9 = this.renderBanner;
            } else if (block instanceof BedBlock) {
               this.renderBed.setColor(((BedBlock)block).getColor());
               blockEntity9 = this.renderBed;
            } else if (blockState.isOf(Blocks.CONDUIT)) {
               blockEntity9 = this.renderConduit;
            } else if (blockState.isOf(Blocks.CHEST)) {
               blockEntity9 = this.renderChestNormal;
            } else if (blockState.isOf(Blocks.ENDER_CHEST)) {
               blockEntity9 = this.renderChestEnder;
            } else if (blockState.isOf(Blocks.TRAPPED_CHEST)) {
               blockEntity9 = this.renderChestTrapped;
            } else {
               if (!(block instanceof ShulkerBoxBlock)) {
                  return;
               }

               DyeColor dyeColor = ShulkerBoxBlock.getColor(item);
               if (dyeColor == null) {
                  blockEntity9 = RENDER_SHULKER_BOX;
               } else {
                  blockEntity9 = RENDER_SHULKER_BOX_DYED[dyeColor.getId()];
               }
            }

            this.blockEntityRenderDispatcher.renderEntity((BlockEntity)blockEntity9, matrices, vertexConsumers, light, overlay);
         }
      } else {
         if (stack.isOf(Items.SHIELD)) {
            boolean bl = stack.getSubTag("BlockEntityTag") != null;
            matrices.push();
            matrices.scale(1.0F, -1.0F, -1.0F);
            SpriteIdentifier spriteIdentifier = bl ? ModelLoader.SHIELD_BASE : ModelLoader.SHIELD_BASE_NO_PATTERN;
            VertexConsumer vertexConsumer = spriteIdentifier.getSprite().getTextureSpecificVertexConsumer(ItemRenderer.getDirectItemGlintConsumer(vertexConsumers, this.modelShield.getLayer(spriteIdentifier.getAtlasId()), true, stack.hasGlint()));
            this.modelShield.getHandle().render(matrices, vertexConsumer, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
            if (bl) {
               List<Pair<BannerPattern, DyeColor>> list = BannerBlockEntity.getPatternsFromNbt(ShieldItem.getColor(stack), BannerBlockEntity.getPatternListTag(stack));
               BannerBlockEntityRenderer.renderCanvas(matrices, vertexConsumers, light, overlay, this.modelShield.getPlate(), spriteIdentifier, false, list, stack.hasGlint());
            } else {
               this.modelShield.getPlate().render(matrices, vertexConsumer, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
            }

            matrices.pop();
         } else if (stack.isOf(Items.TRIDENT)) {
            matrices.push();
            matrices.scale(1.0F, -1.0F, -1.0F);
            VertexConsumer vertexConsumer2 = ItemRenderer.getDirectItemGlintConsumer(vertexConsumers, this.modelTrident.getLayer(TridentEntityModel.TEXTURE), false, stack.hasGlint());
            this.modelTrident.render(matrices, vertexConsumer2, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
            matrices.pop();
         }

      }
   }

   static {
      RENDER_SHULKER_BOX = new ShulkerBoxBlockEntity(BlockPos.ORIGIN, Blocks.SHULKER_BOX.getDefaultState());
   }
}
