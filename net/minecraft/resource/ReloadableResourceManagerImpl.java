package net.minecraft.resource;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ReloadableResourceManagerImpl implements ReloadableResourceManager {
   private static final Logger LOGGER = LogManager.getLogger();
   private final Map<String, NamespaceResourceManager> namespaceManagers = Maps.newHashMap();
   private final List<ResourceReloader> reloaders = Lists.newArrayList();
   private final Set<String> namespaces = Sets.newLinkedHashSet();
   private final List<ResourcePack> packs = Lists.newArrayList();
   private final ResourceType type;

   public ReloadableResourceManagerImpl(ResourceType type) {
      this.type = type;
   }

   public void addPack(ResourcePack pack) {
      this.packs.add(pack);

      NamespaceResourceManager namespaceResourceManager;
      for(Iterator var2 = pack.getNamespaces(this.type).iterator(); var2.hasNext(); namespaceResourceManager.addPack(pack)) {
         String string = (String)var2.next();
         this.namespaces.add(string);
         namespaceResourceManager = (NamespaceResourceManager)this.namespaceManagers.get(string);
         if (namespaceResourceManager == null) {
            namespaceResourceManager = new NamespaceResourceManager(this.type, string);
            this.namespaceManagers.put(string, namespaceResourceManager);
         }
      }

   }

   public Set<String> getAllNamespaces() {
      return this.namespaces;
   }

   public Resource getResource(Identifier id) throws IOException {
      ResourceManager resourceManager = (ResourceManager)this.namespaceManagers.get(id.getNamespace());
      if (resourceManager != null) {
         return resourceManager.getResource(id);
      } else {
         throw new FileNotFoundException(id.toString());
      }
   }

   public boolean containsResource(Identifier id) {
      ResourceManager resourceManager = (ResourceManager)this.namespaceManagers.get(id.getNamespace());
      return resourceManager != null ? resourceManager.containsResource(id) : false;
   }

   public List<Resource> getAllResources(Identifier id) throws IOException {
      ResourceManager resourceManager = (ResourceManager)this.namespaceManagers.get(id.getNamespace());
      if (resourceManager != null) {
         return resourceManager.getAllResources(id);
      } else {
         throw new FileNotFoundException(id.toString());
      }
   }

   public Collection<Identifier> findResources(String startingPath, Predicate<String> pathPredicate) {
      Set<Identifier> set = Sets.newHashSet();
      Iterator var4 = this.namespaceManagers.values().iterator();

      while(var4.hasNext()) {
         NamespaceResourceManager namespaceResourceManager = (NamespaceResourceManager)var4.next();
         set.addAll(namespaceResourceManager.findResources(startingPath, pathPredicate));
      }

      List<Identifier> list = Lists.newArrayList((Iterable)set);
      Collections.sort(list);
      return list;
   }

   private void clear() {
      this.namespaceManagers.clear();
      this.namespaces.clear();
      this.packs.forEach(ResourcePack::close);
      this.packs.clear();
   }

   public void close() {
      this.clear();
   }

   public void registerReloader(ResourceReloader reloader) {
      this.reloaders.add(reloader);
   }

   public ResourceReload reload(Executor prepareExecutor, Executor applyExecutor, CompletableFuture<Unit> initialStage, List<ResourcePack> packs) {
      LOGGER.info("Reloading ResourceManager: {}", () -> {
         return packs.stream().map(ResourcePack::getName).collect(Collectors.joining(", "));
      });
      this.clear();
      Iterator var5 = packs.iterator();

      while(var5.hasNext()) {
         ResourcePack resourcePack = (ResourcePack)var5.next();

         try {
            this.addPack(resourcePack);
         } catch (Exception var8) {
            LOGGER.error((String)"Failed to add resource pack {}", (Object)resourcePack.getName(), (Object)var8);
            return new ReloadableResourceManagerImpl.FailedReload(new ReloadableResourceManagerImpl.PackAdditionFailedException(resourcePack, var8));
         }
      }

      return (ResourceReload)(LOGGER.isDebugEnabled() ? new ProfiledResourceReload(this, Lists.newArrayList((Iterable)this.reloaders), prepareExecutor, applyExecutor, initialStage) : SimpleResourceReload.create(this, Lists.newArrayList((Iterable)this.reloaders), prepareExecutor, applyExecutor, initialStage));
   }

   public Stream<ResourcePack> streamResourcePacks() {
      return this.packs.stream();
   }

   static class FailedReload implements ResourceReload {
      private final ReloadableResourceManagerImpl.PackAdditionFailedException exception;
      private final CompletableFuture<Unit> future;

      public FailedReload(ReloadableResourceManagerImpl.PackAdditionFailedException exception) {
         this.exception = exception;
         this.future = new CompletableFuture();
         this.future.completeExceptionally(exception);
      }

      public CompletableFuture<Unit> whenComplete() {
         return this.future;
      }

      public float getProgress() {
         return 0.0F;
      }

      public boolean isComplete() {
         return true;
      }

      public void throwException() {
         throw this.exception;
      }
   }

   public static class PackAdditionFailedException extends RuntimeException {
      private final ResourcePack pack;

      public PackAdditionFailedException(ResourcePack pack, Throwable cause) {
         super(pack.getName(), cause);
         this.pack = pack;
      }

      public ResourcePack getPack() {
         return this.pack;
      }
   }
}
