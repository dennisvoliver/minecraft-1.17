package net.minecraft.test;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.serialization.Lifecycle;
import java.net.Proxy;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import net.minecraft.datafixer.Schemas;
import net.minecraft.resource.DataPackSettings;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ServerResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.WorldGenerationProgressLogger;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.SystemDetails;
import net.minecraft.util.UserCache;
import net.minecraft.util.Util;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.chunk.FlatChunkGenerator;
import net.minecraft.world.gen.chunk.FlatChunkGeneratorConfig;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.LevelProperties;
import net.minecraft.world.level.storage.LevelStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class TestServer extends MinecraftServer {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final int field_33157 = 20;
   private final List<GameTestBatch> batches;
   private final BlockPos pos;
   private static final GameRules gameRules = (GameRules)Util.make(new GameRules(), (gameRules) -> {
      ((GameRules.BooleanRule)gameRules.get(GameRules.DO_MOB_SPAWNING)).set(false, (MinecraftServer)null);
      ((GameRules.BooleanRule)gameRules.get(GameRules.DO_WEATHER_CYCLE)).set(false, (MinecraftServer)null);
   });
   private static final LevelInfo testLevel;
   @Nullable
   private TestSet testSet;

   public TestServer(Thread serverThread, LevelStorage.Session session, ResourcePackManager dataPackManager, ServerResourceManager serverResourceManager, Collection<GameTestBatch> batches, BlockPos pos, DynamicRegistryManager.Impl registryManager) {
      this(serverThread, session, dataPackManager, serverResourceManager, batches, pos, registryManager, registryManager.get(Registry.BIOME_KEY), registryManager.get(Registry.DIMENSION_TYPE_KEY));
   }

   private TestServer(Thread serverThread, LevelStorage.Session session, ResourcePackManager dataPackManager, ServerResourceManager serverResourceManager, Collection<GameTestBatch> batches, BlockPos pos, DynamicRegistryManager.Impl registryManager, Registry<Biome> biomeRegistry, Registry<DimensionType> dimensionTypeRegistry) {
      super(serverThread, registryManager, session, new LevelProperties(testLevel, new GeneratorOptions(0L, false, false, GeneratorOptions.getRegistryWithReplacedOverworldGenerator(dimensionTypeRegistry, DimensionType.createDefaultDimensionOptions(dimensionTypeRegistry, biomeRegistry, registryManager.get(Registry.CHUNK_GENERATOR_SETTINGS_KEY), 0L), new FlatChunkGenerator(FlatChunkGeneratorConfig.getDefaultConfig(biomeRegistry)))), Lifecycle.stable()), dataPackManager, Proxy.NO_PROXY, Schemas.getFixer(), serverResourceManager, (MinecraftSessionService)null, (GameProfileRepository)null, (UserCache)null, WorldGenerationProgressLogger::new);
      this.batches = Lists.newArrayList((Iterable)batches);
      this.pos = pos;
      if (batches.isEmpty()) {
         throw new IllegalArgumentException("No test batches were given!");
      }
   }

   public boolean setupServer() {
      this.setPlayerManager(new PlayerManager(this, this.registryManager, this.saveHandler, 1) {
      });
      this.loadWorld();
      ServerWorld serverWorld = this.getOverworld();
      serverWorld.setSpawnPos(this.pos, 0.0F);
      serverWorld.getLevelProperties().setRaining(false);
      serverWorld.getLevelProperties().setRaining(false);
      return true;
   }

   public void tick(BooleanSupplier shouldKeepTicking) {
      super.tick(shouldKeepTicking);
      ServerWorld serverWorld = this.getOverworld();
      if (!this.isTesting()) {
         this.runTestBatches(serverWorld);
      }

      if (serverWorld.getTime() % 20L == 0L) {
         LOGGER.info(this.testSet.getResultString());
      }

      if (this.testSet.isDone()) {
         this.stop(false);
         LOGGER.info(this.testSet.getResultString());
         TestFailureLogger.stop();
         LOGGER.info((String)"========= {} GAME TESTS COMPLETE ======================", (Object)this.testSet.getTestCount());
         if (this.testSet.failed()) {
            LOGGER.info((String)"{} required tests failed :(", (Object)this.testSet.getFailedRequiredTestCount());
            this.testSet.getRequiredTests().forEach((test) -> {
               LOGGER.info((String)"   - {}", (Object)test.getStructurePath());
            });
         } else {
            LOGGER.info((String)"All {} required tests passed :)", (Object)this.testSet.getTestCount());
         }

         if (this.testSet.hasFailedOptionalTests()) {
            LOGGER.info((String)"{} optional tests failed", (Object)this.testSet.getFailedOptionalTestCount());
            this.testSet.getOptionalTests().forEach((test) -> {
               LOGGER.info((String)"   - {}", (Object)test.getStructurePath());
            });
         }

         LOGGER.info("====================================================");
      }

   }

   public SystemDetails populateCrashReport(SystemDetails systemDetails) {
      systemDetails.addSection("Type", "Game test server");
      return systemDetails;
   }

   public void exit() {
      super.exit();
      System.exit(this.testSet.getFailedRequiredTestCount());
   }

   public void setCrashReport(CrashReport report) {
      System.exit(1);
   }

   private void runTestBatches(ServerWorld world) {
      Collection<GameTestState> collection = TestUtil.runTestBatches(this.batches, new BlockPos(0, 4, 0), BlockRotation.NONE, world, TestManager.INSTANCE, 8);
      this.testSet = new TestSet(collection);
      LOGGER.info((String)"{} tests are now running!", (Object)this.testSet.getTestCount());
   }

   private boolean isTesting() {
      return this.testSet != null;
   }

   public boolean isHardcore() {
      return false;
   }

   public int getOpPermissionLevel() {
      return 0;
   }

   public int getFunctionPermissionLevel() {
      return 4;
   }

   public boolean shouldBroadcastRconToOps() {
      return false;
   }

   public boolean isDedicated() {
      return false;
   }

   public int getRateLimit() {
      return 0;
   }

   public boolean isUsingNativeTransport() {
      return false;
   }

   public boolean areCommandBlocksEnabled() {
      return true;
   }

   public boolean isRemote() {
      return false;
   }

   public boolean shouldBroadcastConsoleToOps() {
      return false;
   }

   public boolean isHost(GameProfile profile) {
      return false;
   }

   public Optional<String> getModdedStatusMessage() {
      return Optional.empty();
   }

   static {
      testLevel = new LevelInfo("Test Level", GameMode.CREATIVE, false, Difficulty.NORMAL, true, gameRules, DataPackSettings.SAFE_MODE);
   }
}
