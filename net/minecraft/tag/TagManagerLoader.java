package net.minecraft.tag;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class TagManagerLoader implements ResourceReloader {
   private static final Logger LOGGER = LogManager.getLogger();
   private final DynamicRegistryManager registryManager;
   private TagManager tagManager;

   public TagManagerLoader(DynamicRegistryManager registryManager) {
      this.tagManager = TagManager.EMPTY;
      this.registryManager = registryManager;
   }

   public TagManager getTagManager() {
      return this.tagManager;
   }

   public CompletableFuture<Void> reload(ResourceReloader.Synchronizer synchronizer, ResourceManager manager, Profiler prepareProfiler, Profiler applyProfiler, Executor prepareExecutor, Executor applyExecutor) {
      List<TagManagerLoader.RequiredGroup<?>> list = Lists.newArrayList();
      RequiredTagListRegistry.forEach((requiredTagList) -> {
         TagManagerLoader.RequiredGroup<?> requiredGroup = this.buildRequiredGroup(manager, prepareExecutor, requiredTagList);
         if (requiredGroup != null) {
            list.add(requiredGroup);
         }

      });
      CompletableFuture var10000 = CompletableFuture.allOf((CompletableFuture[])list.stream().map((requiredGroup) -> {
         return requiredGroup.groupLoadFuture;
      }).toArray((i) -> {
         return new CompletableFuture[i];
      }));
      Objects.requireNonNull(synchronizer);
      return var10000.thenCompose(synchronizer::whenPrepared).thenAcceptAsync((void_) -> {
         TagManager.Builder builder = new TagManager.Builder();
         list.forEach((requiredGroup) -> {
            requiredGroup.addTo(builder);
         });
         TagManager tagManager = builder.build();
         Multimap<RegistryKey<? extends Registry<?>>, Identifier> multimap = RequiredTagListRegistry.getMissingTags(tagManager);
         if (!multimap.isEmpty()) {
            Stream var10002 = multimap.entries().stream().map((entry) -> {
               Object var10000 = entry.getKey();
               return var10000 + ":" + entry.getValue();
            }).sorted();
            throw new IllegalStateException("Missing required tags: " + (String)var10002.collect(Collectors.joining(",")));
         } else {
            ServerTagManagerHolder.setTagManager(tagManager);
            this.tagManager = tagManager;
         }
      }, applyExecutor);
   }

   @Nullable
   private <T> TagManagerLoader.RequiredGroup<T> buildRequiredGroup(ResourceManager resourceManager, Executor prepareExecutor, RequiredTagList<T> requirement) {
      Optional<? extends Registry<T>> optional = this.registryManager.getOptional(requirement.getRegistryKey());
      if (optional.isPresent()) {
         Registry<T> registry = (Registry)optional.get();
         Objects.requireNonNull(registry);
         TagGroupLoader<T> tagGroupLoader = new TagGroupLoader(registry::getOrEmpty, requirement.getDataType());
         CompletableFuture<? extends TagGroup<T>> completableFuture = CompletableFuture.supplyAsync(() -> {
            return tagGroupLoader.load(resourceManager);
         }, prepareExecutor);
         return new TagManagerLoader.RequiredGroup(requirement, completableFuture);
      } else {
         LOGGER.warn((String)"Can't find registry for {}", (Object)requirement.getRegistryKey());
         return null;
      }
   }

   private static class RequiredGroup<T> {
      private final RequiredTagList<T> requirement;
      final CompletableFuture<? extends TagGroup<T>> groupLoadFuture;

      RequiredGroup(RequiredTagList<T> requiredTagList, CompletableFuture<? extends TagGroup<T>> completableFuture) {
         this.requirement = requiredTagList;
         this.groupLoadFuture = completableFuture;
      }

      public void addTo(TagManager.Builder builder) {
         builder.add(this.requirement.getRegistryKey(), (TagGroup)this.groupLoadFuture.join());
      }
   }
}
