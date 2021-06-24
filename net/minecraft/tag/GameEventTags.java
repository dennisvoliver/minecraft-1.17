package net.minecraft.tag;

import net.minecraft.util.registry.Registry;
import net.minecraft.world.event.GameEvent;

public class GameEventTags {
   protected static final RequiredTagList<GameEvent> REQUIRED_TAGS;
   public static final Tag.Identified<GameEvent> VIBRATIONS;
   public static final Tag.Identified<GameEvent> IGNORE_VIBRATIONS_SNEAKING;

   private static Tag.Identified<GameEvent> register(String id) {
      return REQUIRED_TAGS.add(id);
   }

   public static TagGroup<GameEvent> getTagGroup() {
      return REQUIRED_TAGS.getGroup();
   }

   static {
      REQUIRED_TAGS = RequiredTagListRegistry.register(Registry.GAME_EVENT_KEY, "tags/game_events");
      VIBRATIONS = register("vibrations");
      IGNORE_VIBRATIONS_SNEAKING = register("ignore_vibrations_sneaking");
   }
}
