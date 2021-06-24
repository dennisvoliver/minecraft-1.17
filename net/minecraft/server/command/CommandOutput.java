package net.minecraft.server.command;

import java.util.UUID;
import net.minecraft.text.Text;

/**
 * Represents a subject which can receive command feedback.
 */
public interface CommandOutput {
   CommandOutput DUMMY = new CommandOutput() {
      public void sendSystemMessage(Text message, UUID sender) {
      }

      public boolean shouldReceiveFeedback() {
         return false;
      }

      public boolean shouldTrackOutput() {
         return false;
      }

      public boolean shouldBroadcastConsoleToOps() {
         return false;
      }
   };

   void sendSystemMessage(Text message, UUID sender);

   boolean shouldReceiveFeedback();

   boolean shouldTrackOutput();

   boolean shouldBroadcastConsoleToOps();

   default boolean cannotBeSilenced() {
      return false;
   }
}
