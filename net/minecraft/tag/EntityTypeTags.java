package net.minecraft.tag;

import net.minecraft.entity.EntityType;
import net.minecraft.util.registry.Registry;

public final class EntityTypeTags {
   protected static final RequiredTagList<EntityType<?>> REQUIRED_TAGS;
   public static final Tag.Identified<EntityType<?>> SKELETONS;
   public static final Tag.Identified<EntityType<?>> RAIDERS;
   public static final Tag.Identified<EntityType<?>> BEEHIVE_INHABITORS;
   public static final Tag.Identified<EntityType<?>> ARROWS;
   public static final Tag.Identified<EntityType<?>> IMPACT_PROJECTILES;
   public static final Tag.Identified<EntityType<?>> POWDER_SNOW_WALKABLE_MOBS;
   public static final Tag.Identified<EntityType<?>> AXOLOTL_ALWAYS_HOSTILES;
   public static final Tag.Identified<EntityType<?>> AXOLOTL_HUNT_TARGETS;
   public static final Tag.Identified<EntityType<?>> FREEZE_IMMUNE_ENTITY_TYPES;
   public static final Tag.Identified<EntityType<?>> FREEZE_HURTS_EXTRA_TYPES;

   private EntityTypeTags() {
   }

   private static Tag.Identified<EntityType<?>> register(String id) {
      return REQUIRED_TAGS.add(id);
   }

   public static TagGroup<EntityType<?>> getTagGroup() {
      return REQUIRED_TAGS.getGroup();
   }

   static {
      REQUIRED_TAGS = RequiredTagListRegistry.register(Registry.ENTITY_TYPE_KEY, "tags/entity_types");
      SKELETONS = register("skeletons");
      RAIDERS = register("raiders");
      BEEHIVE_INHABITORS = register("beehive_inhabitors");
      ARROWS = register("arrows");
      IMPACT_PROJECTILES = register("impact_projectiles");
      POWDER_SNOW_WALKABLE_MOBS = register("powder_snow_walkable_mobs");
      AXOLOTL_ALWAYS_HOSTILES = register("axolotl_always_hostiles");
      AXOLOTL_HUNT_TARGETS = register("axolotl_hunt_targets");
      FREEZE_IMMUNE_ENTITY_TYPES = register("freeze_immune_entity_types");
      FREEZE_HURTS_EXTRA_TYPES = register("freeze_hurts_extra_types");
   }
}
