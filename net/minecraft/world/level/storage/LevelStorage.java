package net.minecraft.world.level.storage;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.UnmodifiableIterator;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import net.minecraft.SharedConstants;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.datafixer.Schemas;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resource.DataPackSettings;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.FileNameUtil;
import net.minecraft.util.ProgressListener;
import net.minecraft.util.Util;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.dynamic.RegistryLookupCodec;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.World;
import net.minecraft.world.WorldSaveHandler;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.LevelProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class LevelStorage {
   static final Logger LOGGER = LogManager.getLogger();
   static final DateTimeFormatter TIME_FORMATTER;
   private static final String DEFAULT_ICON = "icon.png";
   private static final ImmutableList<String> GENERATOR_OPTION_KEYS;
   final Path savesDirectory;
   private final Path backupsDirectory;
   final DataFixer dataFixer;

   public LevelStorage(Path savesDirectory, Path backupsDirectory, DataFixer dataFixer) {
      this.dataFixer = dataFixer;

      try {
         Files.createDirectories(Files.exists(savesDirectory, new LinkOption[0]) ? savesDirectory.toRealPath() : savesDirectory);
      } catch (IOException var5) {
         throw new RuntimeException(var5);
      }

      this.savesDirectory = savesDirectory;
      this.backupsDirectory = backupsDirectory;
   }

   public static LevelStorage create(Path path) {
      return new LevelStorage(path, path.resolve("../backups"), Schemas.getFixer());
   }

   private static <T> Pair<GeneratorOptions, Lifecycle> readGeneratorProperties(Dynamic<T> levelData, DataFixer dataFixer, int version) {
      Dynamic<T> dynamic = levelData.get("WorldGenSettings").orElseEmptyMap();
      UnmodifiableIterator var4 = GENERATOR_OPTION_KEYS.iterator();

      while(var4.hasNext()) {
         String string = (String)var4.next();
         Optional<? extends Dynamic<?>> optional = levelData.get(string).result();
         if (optional.isPresent()) {
            dynamic = dynamic.set(string, (Dynamic)optional.get());
         }
      }

      Dynamic<T> dynamic2 = dataFixer.update(TypeReferences.CHUNK_GENERATOR_SETTINGS, dynamic, version, SharedConstants.getGameVersion().getWorldVersion());
      DataResult<GeneratorOptions> dataResult = GeneratorOptions.CODEC.parse(dynamic2);
      Logger var10002 = LOGGER;
      Objects.requireNonNull(var10002);
      return Pair.of((GeneratorOptions)dataResult.resultOrPartial(Util.addPrefix("WorldGenSettings: ", var10002::error)).orElseGet(() -> {
         DataResult var10000 = RegistryLookupCodec.of(Registry.DIMENSION_TYPE_KEY).codec().parse(dynamic2);
         Logger var10002 = LOGGER;
         Objects.requireNonNull(var10002);
         Registry<DimensionType> registry = (Registry)var10000.resultOrPartial(Util.addPrefix("Dimension type registry: ", var10002::error)).orElseThrow(() -> {
            return new IllegalStateException("Failed to get dimension registry");
         });
         var10000 = RegistryLookupCodec.of(Registry.BIOME_KEY).codec().parse(dynamic2);
         var10002 = LOGGER;
         Objects.requireNonNull(var10002);
         Registry<Biome> registry2 = (Registry)var10000.resultOrPartial(Util.addPrefix("Biome registry: ", var10002::error)).orElseThrow(() -> {
            return new IllegalStateException("Failed to get biome registry");
         });
         var10000 = RegistryLookupCodec.of(Registry.CHUNK_GENERATOR_SETTINGS_KEY).codec().parse(dynamic2);
         var10002 = LOGGER;
         Objects.requireNonNull(var10002);
         Registry<ChunkGeneratorSettings> registry3 = (Registry)var10000.resultOrPartial(Util.addPrefix("Noise settings registry: ", var10002::error)).orElseThrow(() -> {
            return new IllegalStateException("Failed to get noise settings registry");
         });
         return GeneratorOptions.getDefaultOptions(registry, registry2, registry3);
      }), dataResult.lifecycle());
   }

   private static DataPackSettings parseDataPackSettings(Dynamic<?> dynamic) {
      DataResult var10000 = DataPackSettings.CODEC.parse(dynamic);
      Logger var10001 = LOGGER;
      Objects.requireNonNull(var10001);
      return (DataPackSettings)var10000.resultOrPartial(var10001::error).orElse(DataPackSettings.SAFE_MODE);
   }

   public String getFormatName() {
      return "Anvil";
   }

   public List<LevelSummary> getLevelList() throws LevelStorageException {
      if (!Files.isDirectory(this.savesDirectory, new LinkOption[0])) {
         throw new LevelStorageException((new TranslatableText("selectWorld.load_folder_access")).getString());
      } else {
         List<LevelSummary> list = Lists.newArrayList();
         File[] files = this.savesDirectory.toFile().listFiles();
         File[] var3 = files;
         int var4 = files.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            File file = var3[var5];
            if (file.isDirectory()) {
               boolean bl2;
               try {
                  bl2 = SessionLock.isLocked(file.toPath());
               } catch (Exception var9) {
                  LOGGER.warn((String)"Failed to read {} lock", (Object)file, (Object)var9);
                  continue;
               }

               LevelSummary levelSummary = (LevelSummary)this.readLevelProperties(file, this.createLevelDataParser(file, bl2));
               if (levelSummary != null) {
                  list.add(levelSummary);
               }
            }
         }

         return list;
      }
   }

   int getCurrentVersion() {
      return 19133;
   }

   @Nullable
   <T> T readLevelProperties(File file, BiFunction<File, DataFixer, T> levelDataParser) {
      if (!file.exists()) {
         return null;
      } else {
         File file2 = new File(file, "level.dat");
         if (file2.exists()) {
            T object = levelDataParser.apply(file2, this.dataFixer);
            if (object != null) {
               return object;
            }
         }

         file2 = new File(file, "level.dat_old");
         return file2.exists() ? levelDataParser.apply(file2, this.dataFixer) : null;
      }
   }

   @Nullable
   private static DataPackSettings readDataPackSettings(File file, DataFixer dataFixer) {
      try {
         NbtCompound nbtCompound = NbtIo.readCompressed(file);
         NbtCompound nbtCompound2 = nbtCompound.getCompound("Data");
         nbtCompound2.remove("Player");
         int i = nbtCompound2.contains("DataVersion", 99) ? nbtCompound2.getInt("DataVersion") : -1;
         Dynamic<NbtElement> dynamic = dataFixer.update(DataFixTypes.LEVEL.getTypeReference(), new Dynamic(NbtOps.INSTANCE, nbtCompound2), i, SharedConstants.getGameVersion().getWorldVersion());
         return (DataPackSettings)dynamic.get("DataPacks").result().map(LevelStorage::parseDataPackSettings).orElse(DataPackSettings.SAFE_MODE);
      } catch (Exception var6) {
         LOGGER.error((String)"Exception reading {}", (Object)file, (Object)var6);
         return null;
      }
   }

   static BiFunction<File, DataFixer, LevelProperties> createLevelDataParser(DynamicOps<NbtElement> dynamicOps, DataPackSettings dataPackSettings) {
      return (file, dataFixer) -> {
         try {
            NbtCompound nbtCompound = NbtIo.readCompressed(file);
            NbtCompound nbtCompound2 = nbtCompound.getCompound("Data");
            NbtCompound nbtCompound3 = nbtCompound2.contains("Player", 10) ? nbtCompound2.getCompound("Player") : null;
            nbtCompound2.remove("Player");
            int i = nbtCompound2.contains("DataVersion", 99) ? nbtCompound2.getInt("DataVersion") : -1;
            Dynamic<NbtElement> dynamic = dataFixer.update(DataFixTypes.LEVEL.getTypeReference(), new Dynamic(dynamicOps, nbtCompound2), i, SharedConstants.getGameVersion().getWorldVersion());
            Pair<GeneratorOptions, Lifecycle> pair = readGeneratorProperties(dynamic, dataFixer, i);
            SaveVersionInfo saveVersionInfo = SaveVersionInfo.fromDynamic(dynamic);
            LevelInfo levelInfo = LevelInfo.fromDynamic(dynamic, dataPackSettings);
            return LevelProperties.readProperties(dynamic, dataFixer, i, nbtCompound3, levelInfo, saveVersionInfo, (GeneratorOptions)pair.getFirst(), (Lifecycle)pair.getSecond());
         } catch (Exception var12) {
            LOGGER.error((String)"Exception reading {}", (Object)file, (Object)var12);
            return null;
         }
      };
   }

   BiFunction<File, DataFixer, LevelSummary> createLevelDataParser(File file, boolean locked) {
      return (file2, dataFixer) -> {
         try {
            NbtCompound nbtCompound = NbtIo.readCompressed(file2);
            NbtCompound nbtCompound2 = nbtCompound.getCompound("Data");
            nbtCompound2.remove("Player");
            int i = nbtCompound2.contains("DataVersion", 99) ? nbtCompound2.getInt("DataVersion") : -1;
            Dynamic<NbtElement> dynamic = dataFixer.update(DataFixTypes.LEVEL.getTypeReference(), new Dynamic(NbtOps.INSTANCE, nbtCompound2), i, SharedConstants.getGameVersion().getWorldVersion());
            SaveVersionInfo saveVersionInfo = SaveVersionInfo.fromDynamic(dynamic);
            int j = saveVersionInfo.getLevelFormatVersion();
            if (j != 19132 && j != 19133) {
               return null;
            } else {
               boolean bl2 = j != this.getCurrentVersion();
               File file3 = new File(file, "icon.png");
               DataPackSettings dataPackSettings = (DataPackSettings)dynamic.get("DataPacks").result().map(LevelStorage::parseDataPackSettings).orElse(DataPackSettings.SAFE_MODE);
               LevelInfo levelInfo = LevelInfo.fromDynamic(dynamic, dataPackSettings);
               return new LevelSummary(levelInfo, saveVersionInfo, file.getName(), bl2, locked, file3);
            }
         } catch (Exception var15) {
            LOGGER.error((String)"Exception reading {}", (Object)file2, (Object)var15);
            return null;
         }
      };
   }

   public boolean isLevelNameValid(String name) {
      try {
         Path path = this.savesDirectory.resolve(name);
         Files.createDirectory(path);
         Files.deleteIfExists(path);
         return true;
      } catch (IOException var3) {
         return false;
      }
   }

   public boolean levelExists(String name) {
      return Files.isDirectory(this.savesDirectory.resolve(name), new LinkOption[0]);
   }

   public Path getSavesDirectory() {
      return this.savesDirectory;
   }

   public Path getBackupsDirectory() {
      return this.backupsDirectory;
   }

   public LevelStorage.Session createSession(String directoryName) throws IOException {
      return new LevelStorage.Session(directoryName);
   }

   static {
      TIME_FORMATTER = (new DateTimeFormatterBuilder()).appendValue(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD).appendLiteral('-').appendValue(ChronoField.MONTH_OF_YEAR, 2).appendLiteral('-').appendValue(ChronoField.DAY_OF_MONTH, 2).appendLiteral('_').appendValue(ChronoField.HOUR_OF_DAY, 2).appendLiteral('-').appendValue(ChronoField.MINUTE_OF_HOUR, 2).appendLiteral('-').appendValue(ChronoField.SECOND_OF_MINUTE, 2).toFormatter();
      GENERATOR_OPTION_KEYS = ImmutableList.of("RandomSeed", "generatorName", "generatorOptions", "generatorVersion", "legacy_custom_options", "MapFeatures", "BonusChest");
   }

   public class Session implements AutoCloseable {
      final SessionLock lock;
      final Path directory;
      private final String directoryName;
      private final Map<WorldSavePath, Path> paths = Maps.newHashMap();

      public Session(String directoryName) throws IOException {
         this.directoryName = directoryName;
         this.directory = LevelStorage.this.savesDirectory.resolve(directoryName);
         this.lock = SessionLock.create(this.directory);
      }

      public String getDirectoryName() {
         return this.directoryName;
      }

      public Path getDirectory(WorldSavePath savePath) {
         return (Path)this.paths.computeIfAbsent(savePath, (path) -> {
            return this.directory.resolve(path.getRelativePath());
         });
      }

      public File getWorldDirectory(RegistryKey<World> key) {
         return DimensionType.getSaveDirectory(key, this.directory.toFile());
      }

      private void checkValid() {
         if (!this.lock.isValid()) {
            throw new IllegalStateException("Lock is no longer valid");
         }
      }

      public WorldSaveHandler createSaveHandler() {
         this.checkValid();
         return new WorldSaveHandler(this, LevelStorage.this.dataFixer);
      }

      public boolean needsConversion() {
         LevelSummary levelSummary = this.getLevelSummary();
         return levelSummary != null && levelSummary.getVersionInfo().getLevelFormatVersion() != LevelStorage.this.getCurrentVersion();
      }

      public boolean convert(ProgressListener progressListener) {
         this.checkValid();
         return AnvilLevelStorage.convertLevel(this, progressListener);
      }

      @Nullable
      public LevelSummary getLevelSummary() {
         this.checkValid();
         return (LevelSummary)LevelStorage.this.readLevelProperties(this.directory.toFile(), LevelStorage.this.createLevelDataParser(this.directory.toFile(), false));
      }

      @Nullable
      public SaveProperties readLevelProperties(DynamicOps<NbtElement> dynamicOps, DataPackSettings dataPackSettings) {
         this.checkValid();
         return (SaveProperties)LevelStorage.this.readLevelProperties(this.directory.toFile(), LevelStorage.createLevelDataParser(dynamicOps, dataPackSettings));
      }

      @Nullable
      public DataPackSettings getDataPackSettings() {
         this.checkValid();
         return (DataPackSettings)LevelStorage.this.readLevelProperties(this.directory.toFile(), LevelStorage::readDataPackSettings);
      }

      public void backupLevelDataFile(DynamicRegistryManager dynamicRegistryManager, SaveProperties saveProperties) {
         this.backupLevelDataFile(dynamicRegistryManager, saveProperties, (NbtCompound)null);
      }

      public void backupLevelDataFile(DynamicRegistryManager dynamicRegistryManager, SaveProperties saveProperties, @Nullable NbtCompound nbtCompound) {
         File file = this.directory.toFile();
         NbtCompound nbtCompound2 = saveProperties.cloneWorldNbt(dynamicRegistryManager, nbtCompound);
         NbtCompound nbtCompound3 = new NbtCompound();
         nbtCompound3.put("Data", nbtCompound2);

         try {
            File file2 = File.createTempFile("level", ".dat", file);
            NbtIo.writeCompressed(nbtCompound3, file2);
            File file3 = new File(file, "level.dat_old");
            File file4 = new File(file, "level.dat");
            Util.backupAndReplace(file4, file2, file3);
         } catch (Exception var10) {
            LevelStorage.LOGGER.error((String)"Failed to save level {}", (Object)file, (Object)var10);
         }

      }

      public File getIconFile() {
         this.checkValid();
         return this.directory.resolve("icon.png").toFile();
      }

      public void deleteSessionLock() throws IOException {
         this.checkValid();
         final Path path = this.directory.resolve("session.lock");
         int i = 1;

         while(i <= 5) {
            LevelStorage.LOGGER.info((String)"Attempt {}...", (Object)i);

            try {
               Files.walkFileTree(this.directory, new SimpleFileVisitor<Path>() {
                  public FileVisitResult visitFile(Path pathx, BasicFileAttributes basicFileAttributes) throws IOException {
                     if (!pathx.equals(path)) {
                        LevelStorage.LOGGER.debug((String)"Deleting {}", (Object)pathx);
                        Files.delete(pathx);
                     }

                     return FileVisitResult.CONTINUE;
                  }

                  public FileVisitResult postVisitDirectory(Path pathx, IOException iOException) throws IOException {
                     if (iOException != null) {
                        throw iOException;
                     } else {
                        if (pathx.equals(Session.this.directory)) {
                           Session.this.lock.close();
                           Files.deleteIfExists(path);
                        }

                        Files.delete(pathx);
                        return FileVisitResult.CONTINUE;
                     }
                  }
               });
               break;
            } catch (IOException var6) {
               if (i >= 5) {
                  throw var6;
               }

               LevelStorage.LOGGER.warn((String)"Failed to delete {}", (Object)this.directory, (Object)var6);

               try {
                  Thread.sleep(500L);
               } catch (InterruptedException var5) {
               }

               ++i;
            }
         }

      }

      public void save(String name) throws IOException {
         this.checkValid();
         File file = new File(LevelStorage.this.savesDirectory.toFile(), this.directoryName);
         if (file.exists()) {
            File file2 = new File(file, "level.dat");
            if (file2.exists()) {
               NbtCompound nbtCompound = NbtIo.readCompressed(file2);
               NbtCompound nbtCompound2 = nbtCompound.getCompound("Data");
               nbtCompound2.putString("LevelName", name);
               NbtIo.writeCompressed(nbtCompound, file2);
            }

         }
      }

      public long createBackup() throws IOException {
         this.checkValid();
         String var10000 = LocalDateTime.now().format(LevelStorage.TIME_FORMATTER);
         String string = var10000 + "_" + this.directoryName;
         Path path = LevelStorage.this.getBackupsDirectory();

         try {
            Files.createDirectories(Files.exists(path, new LinkOption[0]) ? path.toRealPath() : path);
         } catch (IOException var9) {
            throw new RuntimeException(var9);
         }

         Path path2 = path.resolve(FileNameUtil.getNextUniqueName(path, string, ".zip"));
         final ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(path2)));

         try {
            final Path path3 = Paths.get(this.directoryName);
            Files.walkFileTree(this.directory, new SimpleFileVisitor<Path>() {
               public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                  if (path.endsWith("session.lock")) {
                     return FileVisitResult.CONTINUE;
                  } else {
                     String string = path3.resolve(Session.this.directory.relativize(path)).toString().replace('\\', '/');
                     ZipEntry zipEntry = new ZipEntry(string);
                     zipOutputStream.putNextEntry(zipEntry);
                     com.google.common.io.Files.asByteSource(path.toFile()).copyTo((OutputStream)zipOutputStream);
                     zipOutputStream.closeEntry();
                     return FileVisitResult.CONTINUE;
                  }
               }
            });
         } catch (Throwable var8) {
            try {
               zipOutputStream.close();
            } catch (Throwable var7) {
               var8.addSuppressed(var7);
            }

            throw var8;
         }

         zipOutputStream.close();
         return Files.size(path2);
      }

      public void close() throws IOException {
         this.lock.close();
      }
   }
}
