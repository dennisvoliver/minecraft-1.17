package net.minecraft.tag;

/**
 * A class containing the single static instance of {@link TagManager} on the server.
 */
public class ServerTagManagerHolder {
   private static volatile TagManager tagManager = RequiredTagListRegistry.createBuiltinTagManager();

   public static TagManager getTagManager() {
      return tagManager;
   }

   public static void setTagManager(TagManager tagManager) {
      ServerTagManagerHolder.tagManager = tagManager;
   }
}
