package net.minecraft.client.render.block;

import com.google.common.collect.Maps;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

@Environment(EnvType.CLIENT)
public class BlockModels {
   private final Map<BlockState, BakedModel> models = Maps.newIdentityHashMap();
   private final BakedModelManager modelManager;

   public BlockModels(BakedModelManager modelManager) {
      this.modelManager = modelManager;
   }

   public Sprite getSprite(BlockState state) {
      return this.getModel(state).getSprite();
   }

   public BakedModel getModel(BlockState state) {
      BakedModel bakedModel = (BakedModel)this.models.get(state);
      if (bakedModel == null) {
         bakedModel = this.modelManager.getMissingModel();
      }

      return bakedModel;
   }

   public BakedModelManager getModelManager() {
      return this.modelManager;
   }

   public void reload() {
      this.models.clear();
      Iterator var1 = Registry.BLOCK.iterator();

      while(var1.hasNext()) {
         Block block = (Block)var1.next();
         block.getStateManager().getStates().forEach((state) -> {
            this.models.put(state, this.modelManager.getModel(getModelId(state)));
         });
      }

   }

   public static ModelIdentifier getModelId(BlockState state) {
      return getModelId(Registry.BLOCK.getId(state.getBlock()), state);
   }

   public static ModelIdentifier getModelId(Identifier id, BlockState state) {
      return new ModelIdentifier(id, propertyMapToString(state.getEntries()));
   }

   public static String propertyMapToString(Map<Property<?>, Comparable<?>> map) {
      StringBuilder stringBuilder = new StringBuilder();
      Iterator var2 = map.entrySet().iterator();

      while(var2.hasNext()) {
         Entry<Property<?>, Comparable<?>> entry = (Entry)var2.next();
         if (stringBuilder.length() != 0) {
            stringBuilder.append(',');
         }

         Property<?> property = (Property)entry.getKey();
         stringBuilder.append(property.getName());
         stringBuilder.append('=');
         stringBuilder.append(propertyValueToString(property, (Comparable)entry.getValue()));
      }

      return stringBuilder.toString();
   }

   private static <T extends Comparable<T>> String propertyValueToString(Property<T> property, Comparable<?> comparable) {
      return property.name(comparable);
   }
}
