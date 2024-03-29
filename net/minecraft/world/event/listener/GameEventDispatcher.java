package net.minecraft.world.event.listener;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

/**
 * A game event dispatcher dispatches game events to its listeners.
 */
public interface GameEventDispatcher {
   /**
    * An unmodifiable, empty (non-operative) dispatcher.
    */
   GameEventDispatcher EMPTY = new GameEventDispatcher() {
      public boolean isEmpty() {
         return true;
      }

      public void addListener(GameEventListener listener) {
      }

      public void removeListener(GameEventListener listener) {
      }

      public void dispatch(GameEvent event, @Nullable Entity entity, BlockPos pos) {
      }
   };

   /**
    * Returns whether this dispatcher has no listeners.
    */
   boolean isEmpty();

   /**
    * Adds a listener to this dispatcher.
    * 
    * @param listener the listener to add
    */
   void addListener(GameEventListener listener);

   /**
    * Removes a listener from this dispatcher if it is present.
    * 
    * @param listener the listener to remove
    */
   void removeListener(GameEventListener listener);

   /**
    * Dispatches an event to all the listeners in this dispatcher.
    * 
    * @param event the event
    * @param entity an entity related to the event
    * @param pos the block position of the event
    */
   void dispatch(GameEvent event, @Nullable Entity entity, BlockPos pos);
}
