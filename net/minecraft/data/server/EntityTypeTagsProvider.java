package net.minecraft.data.server;

import java.nio.file.Path;
import net.minecraft.data.DataGenerator;
import net.minecraft.entity.EntityType;
import net.minecraft.tag.EntityTypeTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class EntityTypeTagsProvider extends AbstractTagProvider<EntityType<?>> {
   public EntityTypeTagsProvider(DataGenerator root) {
      super(root, Registry.ENTITY_TYPE);
   }

   protected void configure() {
      this.getOrCreateTagBuilder(EntityTypeTags.SKELETONS).add((Object[])(EntityType.SKELETON, EntityType.STRAY, EntityType.WITHER_SKELETON));
      this.getOrCreateTagBuilder(EntityTypeTags.RAIDERS).add((Object[])(EntityType.EVOKER, EntityType.PILLAGER, EntityType.RAVAGER, EntityType.VINDICATOR, EntityType.ILLUSIONER, EntityType.WITCH));
      this.getOrCreateTagBuilder(EntityTypeTags.BEEHIVE_INHABITORS).add((Object)EntityType.BEE);
      this.getOrCreateTagBuilder(EntityTypeTags.ARROWS).add((Object[])(EntityType.ARROW, EntityType.SPECTRAL_ARROW));
      this.getOrCreateTagBuilder(EntityTypeTags.IMPACT_PROJECTILES).addTag(EntityTypeTags.ARROWS).add((Object[])(EntityType.SNOWBALL, EntityType.FIREBALL, EntityType.SMALL_FIREBALL, EntityType.EGG, EntityType.TRIDENT, EntityType.DRAGON_FIREBALL, EntityType.WITHER_SKULL));
      this.getOrCreateTagBuilder(EntityTypeTags.POWDER_SNOW_WALKABLE_MOBS).add((Object[])(EntityType.RABBIT, EntityType.ENDERMITE, EntityType.SILVERFISH, EntityType.FOX));
      this.getOrCreateTagBuilder(EntityTypeTags.AXOLOTL_HUNT_TARGETS).add((Object[])(EntityType.TROPICAL_FISH, EntityType.PUFFERFISH, EntityType.SALMON, EntityType.COD, EntityType.SQUID, EntityType.GLOW_SQUID));
      this.getOrCreateTagBuilder(EntityTypeTags.AXOLOTL_ALWAYS_HOSTILES).add((Object[])(EntityType.DROWNED, EntityType.GUARDIAN, EntityType.ELDER_GUARDIAN));
      this.getOrCreateTagBuilder(EntityTypeTags.FREEZE_IMMUNE_ENTITY_TYPES).add((Object[])(EntityType.STRAY, EntityType.POLAR_BEAR, EntityType.SNOW_GOLEM, EntityType.WITHER));
      this.getOrCreateTagBuilder(EntityTypeTags.FREEZE_HURTS_EXTRA_TYPES).add((Object[])(EntityType.STRIDER, EntityType.BLAZE, EntityType.MAGMA_CUBE));
   }

   protected Path getOutput(Identifier id) {
      Path var10000 = this.root.getOutput();
      String var10001 = id.getNamespace();
      return var10000.resolve("data/" + var10001 + "/tags/entity_types/" + id.getPath() + ".json");
   }

   public String getName() {
      return "Entity Type Tags";
   }
}
