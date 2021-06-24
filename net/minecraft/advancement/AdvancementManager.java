package net.minecraft.advancement;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class AdvancementManager {
   private static final Logger LOGGER = LogManager.getLogger();
   private final Map<Identifier, Advancement> advancements = Maps.newHashMap();
   private final Set<Advancement> roots = Sets.newLinkedHashSet();
   private final Set<Advancement> dependents = Sets.newLinkedHashSet();
   private AdvancementManager.Listener listener;

   private void remove(Advancement advancement) {
      Iterator var2 = advancement.getChildren().iterator();

      while(var2.hasNext()) {
         Advancement advancement2 = (Advancement)var2.next();
         this.remove(advancement2);
      }

      LOGGER.info((String)"Forgot about advancement {}", (Object)advancement.getId());
      this.advancements.remove(advancement.getId());
      if (advancement.getParent() == null) {
         this.roots.remove(advancement);
         if (this.listener != null) {
            this.listener.onRootRemoved(advancement);
         }
      } else {
         this.dependents.remove(advancement);
         if (this.listener != null) {
            this.listener.onDependentRemoved(advancement);
         }
      }

   }

   public void removeAll(Set<Identifier> advancements) {
      Iterator var2 = advancements.iterator();

      while(var2.hasNext()) {
         Identifier identifier = (Identifier)var2.next();
         Advancement advancement = (Advancement)this.advancements.get(identifier);
         if (advancement == null) {
            LOGGER.warn((String)"Told to remove advancement {} but I don't know what that is", (Object)identifier);
         } else {
            this.remove(advancement);
         }
      }

   }

   public void load(Map<Identifier, Advancement.Task> map) {
      HashMap map2 = Maps.newHashMap(map);

      label42:
      while(!map2.isEmpty()) {
         boolean bl = false;
         Iterator iterator = map2.entrySet().iterator();

         Entry entry2;
         while(iterator.hasNext()) {
            entry2 = (Entry)iterator.next();
            Identifier identifier = (Identifier)entry2.getKey();
            Advancement.Task task = (Advancement.Task)entry2.getValue();
            Map var10001 = this.advancements;
            Objects.requireNonNull(var10001);
            if (task.findParent(var10001::get)) {
               Advancement advancement = task.build(identifier);
               this.advancements.put(identifier, advancement);
               bl = true;
               iterator.remove();
               if (advancement.getParent() == null) {
                  this.roots.add(advancement);
                  if (this.listener != null) {
                     this.listener.onRootAdded(advancement);
                  }
               } else {
                  this.dependents.add(advancement);
                  if (this.listener != null) {
                     this.listener.onDependentAdded(advancement);
                  }
               }
            }
         }

         if (!bl) {
            iterator = map2.entrySet().iterator();

            while(true) {
               if (!iterator.hasNext()) {
                  break label42;
               }

               entry2 = (Entry)iterator.next();
               LOGGER.error("Couldn't load advancement {}: {}", entry2.getKey(), entry2.getValue());
            }
         }
      }

      LOGGER.info((String)"Loaded {} advancements", (Object)this.advancements.size());
   }

   public void clear() {
      this.advancements.clear();
      this.roots.clear();
      this.dependents.clear();
      if (this.listener != null) {
         this.listener.onClear();
      }

   }

   public Iterable<Advancement> getRoots() {
      return this.roots;
   }

   public Collection<Advancement> getAdvancements() {
      return this.advancements.values();
   }

   @Nullable
   public Advancement get(Identifier id) {
      return (Advancement)this.advancements.get(id);
   }

   public void setListener(@Nullable AdvancementManager.Listener listener) {
      this.listener = listener;
      if (listener != null) {
         Iterator var2 = this.roots.iterator();

         Advancement advancement2;
         while(var2.hasNext()) {
            advancement2 = (Advancement)var2.next();
            listener.onRootAdded(advancement2);
         }

         var2 = this.dependents.iterator();

         while(var2.hasNext()) {
            advancement2 = (Advancement)var2.next();
            listener.onDependentAdded(advancement2);
         }
      }

   }

   public interface Listener {
      void onRootAdded(Advancement root);

      void onRootRemoved(Advancement root);

      void onDependentAdded(Advancement dependent);

      void onDependentRemoved(Advancement dependent);

      void onClear();
   }
}
