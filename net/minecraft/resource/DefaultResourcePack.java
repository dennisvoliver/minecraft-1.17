package net.minecraft.resource;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.resource.metadata.PackResourceMetadata;
import net.minecraft.resource.metadata.ResourceMetadataReader;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class DefaultResourcePack implements ResourcePack, ResourceFactory {
   public static Path resourcePath;
   private static final Logger LOGGER = LogManager.getLogger();
   public static Class<?> resourceClass;
   private static final Map<ResourceType, FileSystem> typeToFileSystem = (Map)Util.make(Maps.newHashMap(), (map) -> {
      Class var1 = DefaultResourcePack.class;
      synchronized(DefaultResourcePack.class) {
         ResourceType[] var2 = ResourceType.values();
         int var3 = var2.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            ResourceType resourceType = var2[var4];
            URL uRL = DefaultResourcePack.class.getResource("/" + resourceType.getDirectory() + "/.mcassetsroot");

            try {
               URI uRI = uRL.toURI();
               if ("jar".equals(uRI.getScheme())) {
                  FileSystem fileSystem2;
                  try {
                     fileSystem2 = FileSystems.getFileSystem(uRI);
                  } catch (Exception var11) {
                     fileSystem2 = FileSystems.newFileSystem(uRI, Collections.emptyMap());
                  }

                  map.put(resourceType, fileSystem2);
               }
            } catch (IOException | URISyntaxException var12) {
               LOGGER.error((String)"Couldn't get a list of all vanilla resources", (Throwable)var12);
            }
         }

      }
   });
   public final PackResourceMetadata metadata;
   public final Set<String> namespaces;

   public DefaultResourcePack(PackResourceMetadata metadata, String... namespaces) {
      this.metadata = metadata;
      this.namespaces = ImmutableSet.copyOf((Object[])namespaces);
   }

   public InputStream openRoot(String fileName) throws IOException {
      if (!fileName.contains("/") && !fileName.contains("\\")) {
         if (resourcePath != null) {
            Path path = resourcePath.resolve(fileName);
            if (Files.exists(path, new LinkOption[0])) {
               return Files.newInputStream(path);
            }
         }

         return this.getInputStream(fileName);
      } else {
         throw new IllegalArgumentException("Root resources can only be filenames, not paths (no / allowed!)");
      }
   }

   public InputStream open(ResourceType type, Identifier id) throws IOException {
      InputStream inputStream = this.findInputStream(type, id);
      if (inputStream != null) {
         return inputStream;
      } else {
         throw new FileNotFoundException(id.getPath());
      }
   }

   public Collection<Identifier> findResources(ResourceType type, String namespace, String prefix, int maxDepth, Predicate<String> pathFilter) {
      Set<Identifier> set = Sets.newHashSet();
      URI uRI;
      if (resourcePath != null) {
         try {
            getIdentifiers(set, maxDepth, namespace, resourcePath.resolve(type.getDirectory()), prefix, pathFilter);
         } catch (IOException var15) {
         }

         if (type == ResourceType.CLIENT_RESOURCES) {
            Enumeration enumeration = null;

            try {
               enumeration = resourceClass.getClassLoader().getResources(type.getDirectory() + "/");
            } catch (IOException var14) {
            }

            while(enumeration != null && enumeration.hasMoreElements()) {
               try {
                  uRI = ((URL)enumeration.nextElement()).toURI();
                  if ("file".equals(uRI.getScheme())) {
                     getIdentifiers(set, maxDepth, namespace, Paths.get(uRI), prefix, pathFilter);
                  }
               } catch (IOException | URISyntaxException var13) {
               }
            }
         }
      }

      try {
         URL uRL = DefaultResourcePack.class.getResource("/" + type.getDirectory() + "/.mcassetsroot");
         if (uRL == null) {
            LOGGER.error("Couldn't find .mcassetsroot, cannot load vanilla resources");
            return set;
         }

         uRI = uRL.toURI();
         if ("file".equals(uRI.getScheme())) {
            URL uRL2 = new URL(uRL.toString().substring(0, uRL.toString().length() - ".mcassetsroot".length()));
            Path path = Paths.get(uRL2.toURI());
            getIdentifiers(set, maxDepth, namespace, path, prefix, pathFilter);
         } else if ("jar".equals(uRI.getScheme())) {
            Path path2 = ((FileSystem)typeToFileSystem.get(type)).getPath("/" + type.getDirectory());
            getIdentifiers(set, maxDepth, "minecraft", path2, prefix, pathFilter);
         } else {
            LOGGER.error((String)"Unsupported scheme {} trying to list vanilla resources (NYI?)", (Object)uRI);
         }
      } catch (NoSuchFileException | FileNotFoundException var11) {
      } catch (IOException | URISyntaxException var12) {
         LOGGER.error((String)"Couldn't get a list of all vanilla resources", (Throwable)var12);
      }

      return set;
   }

   private static void getIdentifiers(Collection<Identifier> results, int maxDepth, String namespace, Path root, String prefix, Predicate<String> pathFilter) throws IOException {
      Path path = root.resolve(namespace);
      Stream stream = Files.walk(path.resolve(prefix), maxDepth, new FileVisitOption[0]);

      try {
         Stream var10000 = stream.filter((pathx) -> {
            return !pathx.endsWith(".mcmeta") && Files.isRegularFile(pathx, new LinkOption[0]) && pathFilter.test(pathx.getFileName().toString());
         }).map((pathx) -> {
            return new Identifier(namespace, path.relativize(pathx).toString().replaceAll("\\\\", "/"));
         });
         Objects.requireNonNull(results);
         var10000.forEach(results::add);
      } catch (Throwable var11) {
         if (stream != null) {
            try {
               stream.close();
            } catch (Throwable var10) {
               var11.addSuppressed(var10);
            }
         }

         throw var11;
      }

      if (stream != null) {
         stream.close();
      }

   }

   @Nullable
   protected InputStream findInputStream(ResourceType type, Identifier id) {
      String string = getPath(type, id);
      if (resourcePath != null) {
         Path var10000 = resourcePath;
         String var10001 = type.getDirectory();
         Path path = var10000.resolve(var10001 + "/" + id.getNamespace() + "/" + id.getPath());
         if (Files.exists(path, new LinkOption[0])) {
            try {
               return Files.newInputStream(path);
            } catch (IOException var7) {
            }
         }
      }

      try {
         URL uRL = DefaultResourcePack.class.getResource(string);
         return isValidUrl(string, uRL) ? uRL.openStream() : null;
      } catch (IOException var6) {
         return DefaultResourcePack.class.getResourceAsStream(string);
      }
   }

   private static String getPath(ResourceType type, Identifier id) {
      String var10000 = type.getDirectory();
      return "/" + var10000 + "/" + id.getNamespace() + "/" + id.getPath();
   }

   private static boolean isValidUrl(String fileName, @Nullable URL url) throws IOException {
      return url != null && (url.getProtocol().equals("jar") || DirectoryResourcePack.isValidPath(new File(url.getFile()), fileName));
   }

   @Nullable
   protected InputStream getInputStream(String path) {
      return DefaultResourcePack.class.getResourceAsStream("/" + path);
   }

   public boolean contains(ResourceType type, Identifier id) {
      String string = getPath(type, id);
      if (resourcePath != null) {
         Path var10000 = resourcePath;
         String var10001 = type.getDirectory();
         Path path = var10000.resolve(var10001 + "/" + id.getNamespace() + "/" + id.getPath());
         if (Files.exists(path, new LinkOption[0])) {
            return true;
         }
      }

      try {
         URL uRL = DefaultResourcePack.class.getResource(string);
         return isValidUrl(string, uRL);
      } catch (IOException var5) {
         return false;
      }
   }

   public Set<String> getNamespaces(ResourceType type) {
      return this.namespaces;
   }

   @Nullable
   public <T> T parseMetadata(ResourceMetadataReader<T> metaReader) throws IOException {
      try {
         InputStream inputStream = this.openRoot("pack.mcmeta");

         label51: {
            Object var4;
            try {
               if (inputStream == null) {
                  break label51;
               }

               T object = AbstractFileResourcePack.parseMetadata(metaReader, inputStream);
               if (object == null) {
                  break label51;
               }

               var4 = object;
            } catch (Throwable var6) {
               if (inputStream != null) {
                  try {
                     inputStream.close();
                  } catch (Throwable var5) {
                     var6.addSuppressed(var5);
                  }
               }

               throw var6;
            }

            if (inputStream != null) {
               inputStream.close();
            }

            return var4;
         }

         if (inputStream != null) {
            inputStream.close();
         }
      } catch (FileNotFoundException | RuntimeException var7) {
      }

      return metaReader == PackResourceMetadata.READER ? this.metadata : null;
   }

   public String getName() {
      return "Default";
   }

   public void close() {
   }

   public Resource getResource(final Identifier id) throws IOException {
      return new Resource() {
         @Nullable
         InputStream stream;

         public void close() throws IOException {
            if (this.stream != null) {
               this.stream.close();
            }

         }

         public Identifier getId() {
            return id;
         }

         public InputStream getInputStream() {
            try {
               this.stream = DefaultResourcePack.this.open(ResourceType.CLIENT_RESOURCES, id);
            } catch (IOException var2) {
               throw new UncheckedIOException("Could not get client resource from vanilla pack", var2);
            }

            return this.stream;
         }

         public boolean hasMetadata() {
            return false;
         }

         @Nullable
         public <T> T getMetadata(ResourceMetadataReader<T> metaReader) {
            return null;
         }

         public String getResourcePackName() {
            return id.toString();
         }
      };
   }
}
