package net.minecraft.server.world;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mojang.datafixers.DataFixer;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityInteraction;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.InteractionObserver;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Npc;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SkeletonHorseEntity;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.map.MapState;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.network.packet.s2c.play.BlockEventS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundFromEntityS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerSpawnPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.VibrationS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldEventS2CPacket;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.network.DebugInfoSender;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.structure.StructureManager;
import net.minecraft.structure.StructureStart;
import net.minecraft.tag.TagManager;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ProgressListener;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.Unit;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.CsvWriter;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.DefaultedRegistry;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.village.raid.Raid;
import net.minecraft.village.raid.RaidManager;
import net.minecraft.world.EntityList;
import net.minecraft.world.ForcedChunkState;
import net.minecraft.world.GameRules;
import net.minecraft.world.Heightmap;
import net.minecraft.world.IdCountsState;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.PortalForcer;
import net.minecraft.world.ScheduledTick;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.Vibration;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.BlockEntityTickInvoker;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.entity.EntityHandler;
import net.minecraft.world.entity.EntityLookup;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.listener.EntityGameEventHandler;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionBehavior;
import net.minecraft.world.gen.Spawner;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraft.world.level.ServerWorldProperties;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestType;
import net.minecraft.world.storage.ChunkDataAccess;
import net.minecraft.world.storage.EntityChunkDataAccess;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ServerWorld extends World implements StructureWorldAccess {
   public static final BlockPos END_SPAWN_POS = new BlockPos(100, 50, 0);
   private static final Logger LOGGER = LogManager.getLogger();
   /**
    * The number of ticks ({@value}) the world will continue to tick entities after
    * all players have left and the world does not contain any forced chunks.
    */
   private static final int SERVER_IDLE_COOLDOWN = 300;
   final List<ServerPlayerEntity> players;
   private final ServerChunkManager serverChunkManager;
   private final MinecraftServer server;
   private final ServerWorldProperties worldProperties;
   final EntityList entityList;
   private final ServerEntityManager<Entity> entityManager;
   public boolean savingDisabled;
   private final SleepManager sleepManager;
   private int idleTimeout;
   private final PortalForcer portalForcer;
   private final ServerTickScheduler<Block> blockTickScheduler;
   private final ServerTickScheduler<Fluid> fluidTickScheduler;
   final Set<MobEntity> loadedMobs;
   protected final RaidManager raidManager;
   private final ObjectLinkedOpenHashSet<BlockEvent> syncedBlockEventQueue;
   private boolean inBlockTick;
   private final List<Spawner> spawners;
   @Nullable
   private final EnderDragonFight enderDragonFight;
   final Int2ObjectMap<EnderDragonPart> dragonParts;
   private final StructureAccessor structureAccessor;
   private final boolean shouldTickTime;

   public ServerWorld(MinecraftServer server, Executor workerExecutor, LevelStorage.Session session, ServerWorldProperties properties, RegistryKey<World> worldKey, DimensionType dimensionType, WorldGenerationProgressListener worldGenerationProgressListener, ChunkGenerator chunkGenerator, boolean debugWorld, long seed, List<Spawner> spawners, boolean shouldTickTime) {
      Objects.requireNonNull(server);
      super(properties, worldKey, dimensionType, server::getProfiler, false, debugWorld, seed);
      this.players = Lists.newArrayList();
      this.entityList = new EntityList();
      Predicate var10004 = (block) -> {
         return block == null || block.getDefaultState().isAir();
      };
      DefaultedRegistry var10005 = Registry.BLOCK;
      Objects.requireNonNull(var10005);
      this.blockTickScheduler = new ServerTickScheduler(this, var10004, var10005::getId, this::tickBlock);
      var10004 = (fluid) -> {
         return fluid == null || fluid == Fluids.EMPTY;
      };
      var10005 = Registry.FLUID;
      Objects.requireNonNull(var10005);
      this.fluidTickScheduler = new ServerTickScheduler(this, var10004, var10005::getId, this::tickFluid);
      this.loadedMobs = new ObjectOpenHashSet();
      this.syncedBlockEventQueue = new ObjectLinkedOpenHashSet();
      this.dragonParts = new Int2ObjectOpenHashMap();
      this.shouldTickTime = shouldTickTime;
      this.server = server;
      this.spawners = spawners;
      this.worldProperties = properties;
      boolean bl = server.syncChunkWrites();
      DataFixer dataFixer = server.getDataFixer();
      ChunkDataAccess<Entity> chunkDataAccess = new EntityChunkDataAccess(this, new File(session.getWorldDirectory(worldKey), "entities"), dataFixer, bl, server);
      this.entityManager = new ServerEntityManager(Entity.class, new ServerWorld.ServerEntityHandler(), chunkDataAccess);
      StructureManager var10006 = server.getStructureManager();
      int var10009 = server.getPlayerManager().getViewDistance();
      ServerEntityManager var10012 = this.entityManager;
      Objects.requireNonNull(var10012);
      this.serverChunkManager = new ServerChunkManager(this, session, dataFixer, var10006, workerExecutor, chunkGenerator, var10009, bl, worldGenerationProgressListener, var10012::updateTrackingStatus, () -> {
         return server.getOverworld().getPersistentStateManager();
      });
      this.portalForcer = new PortalForcer(this);
      this.calculateAmbientDarkness();
      this.initWeatherGradients();
      this.getWorldBorder().setMaxRadius(server.getMaxWorldBorderRadius());
      this.raidManager = (RaidManager)this.getPersistentStateManager().getOrCreate((nbtCompound) -> {
         return RaidManager.fromNbt(this, nbtCompound);
      }, () -> {
         return new RaidManager(this);
      }, RaidManager.nameFor(this.getDimension()));
      if (!server.isSinglePlayer()) {
         properties.setGameMode(server.getDefaultGameMode());
      }

      this.structureAccessor = new StructureAccessor(this, server.getSaveProperties().getGeneratorOptions());
      if (this.getDimension().hasEnderDragonFight()) {
         this.enderDragonFight = new EnderDragonFight(this, server.getSaveProperties().getGeneratorOptions().getSeed(), server.getSaveProperties().getDragonFight());
      } else {
         this.enderDragonFight = null;
      }

      this.sleepManager = new SleepManager();
   }

   public void setWeather(int clearDuration, int rainDuration, boolean raining, boolean thundering) {
      this.worldProperties.setClearWeatherTime(clearDuration);
      this.worldProperties.setRainTime(rainDuration);
      this.worldProperties.setThunderTime(rainDuration);
      this.worldProperties.setRaining(raining);
      this.worldProperties.setThundering(thundering);
   }

   public Biome getGeneratorStoredBiome(int biomeX, int biomeY, int biomeZ) {
      return this.getChunkManager().getChunkGenerator().getBiomeSource().getBiomeForNoiseGen(biomeX, biomeY, biomeZ);
   }

   public StructureAccessor getStructureAccessor() {
      return this.structureAccessor;
   }

   public void tick(BooleanSupplier shouldKeepTicking) {
      Profiler profiler = this.getProfiler();
      this.inBlockTick = true;
      profiler.push("world border");
      this.getWorldBorder().tick();
      profiler.swap("weather");
      boolean bl = this.isRaining();
      int l;
      if (this.getDimension().hasSkyLight()) {
         if (this.getGameRules().getBoolean(GameRules.DO_WEATHER_CYCLE)) {
            l = this.worldProperties.getClearWeatherTime();
            int j = this.worldProperties.getThunderTime();
            int k = this.worldProperties.getRainTime();
            boolean bl2 = this.properties.isThundering();
            boolean bl3 = this.properties.isRaining();
            if (l > 0) {
               --l;
               j = bl2 ? 0 : 1;
               k = bl3 ? 0 : 1;
               bl2 = false;
               bl3 = false;
            } else {
               if (j > 0) {
                  --j;
                  if (j == 0) {
                     bl2 = !bl2;
                  }
               } else if (bl2) {
                  j = this.random.nextInt(12000) + 3600;
               } else {
                  j = this.random.nextInt(168000) + 12000;
               }

               if (k > 0) {
                  --k;
                  if (k == 0) {
                     bl3 = !bl3;
                  }
               } else if (bl3) {
                  k = this.random.nextInt(12000) + 12000;
               } else {
                  k = this.random.nextInt(168000) + 12000;
               }
            }

            this.worldProperties.setThunderTime(j);
            this.worldProperties.setRainTime(k);
            this.worldProperties.setClearWeatherTime(l);
            this.worldProperties.setThundering(bl2);
            this.worldProperties.setRaining(bl3);
         }

         this.thunderGradientPrev = this.thunderGradient;
         if (this.properties.isThundering()) {
            this.thunderGradient = (float)((double)this.thunderGradient + 0.01D);
         } else {
            this.thunderGradient = (float)((double)this.thunderGradient - 0.01D);
         }

         this.thunderGradient = MathHelper.clamp(this.thunderGradient, 0.0F, 1.0F);
         this.rainGradientPrev = this.rainGradient;
         if (this.properties.isRaining()) {
            this.rainGradient = (float)((double)this.rainGradient + 0.01D);
         } else {
            this.rainGradient = (float)((double)this.rainGradient - 0.01D);
         }

         this.rainGradient = MathHelper.clamp(this.rainGradient, 0.0F, 1.0F);
      }

      if (this.rainGradientPrev != this.rainGradient) {
         this.server.getPlayerManager().sendToDimension(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.RAIN_GRADIENT_CHANGED, this.rainGradient), this.getRegistryKey());
      }

      if (this.thunderGradientPrev != this.thunderGradient) {
         this.server.getPlayerManager().sendToDimension(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.THUNDER_GRADIENT_CHANGED, this.thunderGradient), this.getRegistryKey());
      }

      if (bl != this.isRaining()) {
         if (bl) {
            this.server.getPlayerManager().sendToAll(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.RAIN_STOPPED, GameStateChangeS2CPacket.DEMO_OPEN_SCREEN));
         } else {
            this.server.getPlayerManager().sendToAll(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.RAIN_STARTED, GameStateChangeS2CPacket.DEMO_OPEN_SCREEN));
         }

         this.server.getPlayerManager().sendToAll(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.RAIN_GRADIENT_CHANGED, this.rainGradient));
         this.server.getPlayerManager().sendToAll(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.THUNDER_GRADIENT_CHANGED, this.thunderGradient));
      }

      l = this.getGameRules().getInt(GameRules.PLAYERS_SLEEPING_PERCENTAGE);
      if (this.sleepManager.canSkipNight(l) && this.sleepManager.canResetTime(l, this.players)) {
         if (this.getGameRules().getBoolean(GameRules.DO_DAYLIGHT_CYCLE)) {
            long m = this.properties.getTimeOfDay() + 24000L;
            this.setTimeOfDay(m - m % 24000L);
         }

         this.wakeSleepingPlayers();
         if (this.getGameRules().getBoolean(GameRules.DO_WEATHER_CYCLE)) {
            this.resetWeather();
         }
      }

      this.calculateAmbientDarkness();
      this.tickTime();
      profiler.swap("chunkSource");
      this.getChunkManager().tick(shouldKeepTicking);
      profiler.swap("tickPending");
      if (!this.isDebugWorld()) {
         this.blockTickScheduler.tick();
         this.fluidTickScheduler.tick();
      }

      profiler.swap("raid");
      this.raidManager.tick();
      profiler.swap("blockEvents");
      this.processSyncedBlockEvents();
      this.inBlockTick = false;
      profiler.pop();
      boolean bl4 = !this.players.isEmpty() || !this.getForcedChunks().isEmpty();
      if (bl4) {
         this.resetIdleTimeout();
      }

      if (bl4 || this.idleTimeout++ < 300) {
         profiler.push("entities");
         if (this.enderDragonFight != null) {
            profiler.push("dragonFight");
            this.enderDragonFight.tick();
            profiler.pop();
         }

         this.entityList.forEach((entity) -> {
            if (!entity.isRemoved()) {
               if (this.shouldCancelSpawn(entity)) {
                  entity.discard();
               } else {
                  profiler.push("checkDespawn");
                  entity.checkDespawn();
                  profiler.pop();
                  Entity entity2 = entity.getVehicle();
                  if (entity2 != null) {
                     if (!entity2.isRemoved() && entity2.hasPassenger(entity)) {
                        return;
                     }

                     entity.stopRiding();
                  }

                  profiler.push("tick");
                  this.tickEntity(this::tickEntity, entity);
                  profiler.pop();
               }
            }
         });
         profiler.pop();
         this.tickBlockEntities();
      }

      profiler.push("entityManagement");
      this.entityManager.tick();
      profiler.pop();
   }

   protected void tickTime() {
      if (this.shouldTickTime) {
         long l = this.properties.getTime() + 1L;
         this.worldProperties.setTime(l);
         this.worldProperties.getScheduledEvents().processEvents(this.server, l);
         if (this.properties.getGameRules().getBoolean(GameRules.DO_DAYLIGHT_CYCLE)) {
            this.setTimeOfDay(this.properties.getTimeOfDay() + 1L);
         }

      }
   }

   public void setTimeOfDay(long timeOfDay) {
      this.worldProperties.setTimeOfDay(timeOfDay);
   }

   public void tickSpawners(boolean spawnMonsters, boolean spawnAnimals) {
      Iterator var3 = this.spawners.iterator();

      while(var3.hasNext()) {
         Spawner spawner = (Spawner)var3.next();
         spawner.spawn(this, spawnMonsters, spawnAnimals);
      }

   }

   private boolean shouldCancelSpawn(Entity entity) {
      if (this.server.shouldSpawnAnimals() || !(entity instanceof AnimalEntity) && !(entity instanceof WaterCreatureEntity)) {
         return !this.server.shouldSpawnNpcs() && entity instanceof Npc;
      } else {
         return true;
      }
   }

   private void wakeSleepingPlayers() {
      this.sleepManager.clearSleeping();
      ((List)this.players.stream().filter(LivingEntity::isSleeping).collect(Collectors.toList())).forEach((player) -> {
         player.wakeUp(false, false);
      });
   }

   public void tickChunk(WorldChunk chunk, int randomTickSpeed) {
      ChunkPos chunkPos = chunk.getPos();
      boolean bl = this.isRaining();
      int i = chunkPos.getStartX();
      int j = chunkPos.getStartZ();
      Profiler profiler = this.getProfiler();
      profiler.push("thunder");
      BlockPos blockPos2;
      if (bl && this.isThundering() && this.random.nextInt(100000) == 0) {
         blockPos2 = this.getSurface(this.getRandomPosInChunk(i, 0, j, 15));
         if (this.hasRain(blockPos2)) {
            LocalDifficulty localDifficulty = this.getLocalDifficulty(blockPos2);
            boolean bl2 = this.getGameRules().getBoolean(GameRules.DO_MOB_SPAWNING) && this.random.nextDouble() < (double)localDifficulty.getLocalDifficulty() * 0.01D && !this.getBlockState(blockPos2.down()).isOf(Blocks.LIGHTNING_ROD);
            if (bl2) {
               SkeletonHorseEntity skeletonHorseEntity = (SkeletonHorseEntity)EntityType.SKELETON_HORSE.create(this);
               skeletonHorseEntity.setTrapped(true);
               skeletonHorseEntity.setBreedingAge(0);
               skeletonHorseEntity.setPosition((double)blockPos2.getX(), (double)blockPos2.getY(), (double)blockPos2.getZ());
               this.spawnEntity(skeletonHorseEntity);
            }

            LightningEntity lightningEntity = (LightningEntity)EntityType.LIGHTNING_BOLT.create(this);
            lightningEntity.refreshPositionAfterTeleport(Vec3d.ofBottomCenter(blockPos2));
            lightningEntity.setCosmetic(bl2);
            this.spawnEntity(lightningEntity);
         }
      }

      profiler.swap("iceandsnow");
      if (this.random.nextInt(16) == 0) {
         blockPos2 = this.getTopPosition(Heightmap.Type.MOTION_BLOCKING, this.getRandomPosInChunk(i, 0, j, 15));
         BlockPos blockPos3 = blockPos2.down();
         Biome biome = this.getBiome(blockPos2);
         if (biome.canSetIce(this, blockPos3)) {
            this.setBlockState(blockPos3, Blocks.ICE.getDefaultState());
         }

         if (bl) {
            if (biome.canSetSnow(this, blockPos2)) {
               this.setBlockState(blockPos2, Blocks.SNOW.getDefaultState());
            }

            BlockState blockState = this.getBlockState(blockPos3);
            Biome.Precipitation precipitation = this.getBiome(blockPos2).getPrecipitation();
            if (precipitation == Biome.Precipitation.RAIN && biome.isCold(blockPos3)) {
               precipitation = Biome.Precipitation.SNOW;
            }

            blockState.getBlock().precipitationTick(blockState, this, blockPos3, precipitation);
         }
      }

      profiler.swap("tickBlocks");
      if (randomTickSpeed > 0) {
         ChunkSection[] var17 = chunk.getSectionArray();
         int var19 = var17.length;

         for(int var21 = 0; var21 < var19; ++var21) {
            ChunkSection chunkSection = var17[var21];
            if (chunkSection != WorldChunk.EMPTY_SECTION && chunkSection.hasRandomTicks()) {
               int k = chunkSection.getYOffset();

               for(int l = 0; l < randomTickSpeed; ++l) {
                  BlockPos blockPos4 = this.getRandomPosInChunk(i, k, j, 15);
                  profiler.push("randomTick");
                  BlockState blockState2 = chunkSection.getBlockState(blockPos4.getX() - i, blockPos4.getY() - k, blockPos4.getZ() - j);
                  if (blockState2.hasRandomTicks()) {
                     blockState2.randomTick(this, blockPos4, this.random);
                  }

                  FluidState fluidState = blockState2.getFluidState();
                  if (fluidState.hasRandomTicks()) {
                     fluidState.onRandomTick(this, blockPos4, this.random);
                  }

                  profiler.pop();
               }
            }
         }
      }

      profiler.pop();
   }

   private Optional<BlockPos> getLightningRodPos(BlockPos pos) {
      Optional<BlockPos> optional = this.getPointOfInterestStorage().method_34712((poiType) -> {
         return poiType == PointOfInterestType.LIGHTNING_ROD;
      }, (posx) -> {
         return posx.getY() == this.toServerWorld().getTopY(Heightmap.Type.WORLD_SURFACE, posx.getX(), posx.getZ()) - 1;
      }, pos, 128, PointOfInterestStorage.OccupationStatus.ANY);
      return optional.map((posx) -> {
         return posx.up(1);
      });
   }

   protected BlockPos getSurface(BlockPos pos) {
      BlockPos blockPos = this.getTopPosition(Heightmap.Type.MOTION_BLOCKING, pos);
      Optional<BlockPos> optional = this.getLightningRodPos(blockPos);
      if (optional.isPresent()) {
         return (BlockPos)optional.get();
      } else {
         Box box = (new Box(blockPos, new BlockPos(blockPos.getX(), this.getTopY(), blockPos.getZ()))).expand(3.0D);
         List<LivingEntity> list = this.getEntitiesByClass(LivingEntity.class, box, (entity) -> {
            return entity != null && entity.isAlive() && this.isSkyVisible(entity.getBlockPos());
         });
         if (!list.isEmpty()) {
            return ((LivingEntity)list.get(this.random.nextInt(list.size()))).getBlockPos();
         } else {
            if (blockPos.getY() == this.getBottomY() - 1) {
               blockPos = blockPos.up(2);
            }

            return blockPos;
         }
      }
   }

   public boolean isInBlockTick() {
      return this.inBlockTick;
   }

   public boolean isSleepingEnabled() {
      return this.getGameRules().getInt(GameRules.PLAYERS_SLEEPING_PERCENTAGE) <= 100;
   }

   private void handleSleeping() {
      if (this.isSleepingEnabled()) {
         if (!this.getServer().isSinglePlayer() || this.getServer().isRemote()) {
            int i = this.getGameRules().getInt(GameRules.PLAYERS_SLEEPING_PERCENTAGE);
            TranslatableText text2;
            if (this.sleepManager.canSkipNight(i)) {
               text2 = new TranslatableText("sleep.skipping_night");
            } else {
               text2 = new TranslatableText("sleep.players_sleeping", new Object[]{this.sleepManager.getSleeping(), this.sleepManager.getNightSkippingRequirement(i)});
            }

            Iterator var3 = this.players.iterator();

            while(var3.hasNext()) {
               ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)var3.next();
               serverPlayerEntity.sendMessage(text2, true);
            }

         }
      }
   }

   public void updateSleepingPlayers() {
      if (!this.players.isEmpty() && this.sleepManager.update(this.players)) {
         this.handleSleeping();
      }

   }

   public ServerScoreboard getScoreboard() {
      return this.server.getScoreboard();
   }

   private void resetWeather() {
      this.worldProperties.setRainTime(0);
      this.worldProperties.setRaining(false);
      this.worldProperties.setThunderTime(0);
      this.worldProperties.setThundering(false);
   }

   public void resetIdleTimeout() {
      this.idleTimeout = 0;
   }

   private void tickFluid(ScheduledTick<Fluid> tick) {
      FluidState fluidState = this.getFluidState(tick.pos);
      if (fluidState.getFluid() == tick.getObject()) {
         fluidState.onScheduledTick(this, tick.pos);
      }

   }

   private void tickBlock(ScheduledTick<Block> tick) {
      BlockState blockState = this.getBlockState(tick.pos);
      if (blockState.isOf((Block)tick.getObject())) {
         blockState.scheduledTick(this, tick.pos, this.random);
      }

   }

   public void tickEntity(Entity entity) {
      entity.resetPosition();
      Profiler profiler = this.getProfiler();
      ++entity.age;
      this.getProfiler().push(() -> {
         return Registry.ENTITY_TYPE.getId(entity.getType()).toString();
      });
      profiler.visit("tickNonPassenger");
      entity.tick();
      this.getProfiler().pop();
      Iterator var3 = entity.getPassengerList().iterator();

      while(var3.hasNext()) {
         Entity entity2 = (Entity)var3.next();
         this.tickPassenger(entity, entity2);
      }

   }

   private void tickPassenger(Entity vehicle, Entity passenger) {
      if (!passenger.isRemoved() && passenger.getVehicle() == vehicle) {
         if (passenger instanceof PlayerEntity || this.entityList.has(passenger)) {
            passenger.resetPosition();
            ++passenger.age;
            Profiler profiler = this.getProfiler();
            profiler.push(() -> {
               return Registry.ENTITY_TYPE.getId(passenger.getType()).toString();
            });
            profiler.visit("tickPassenger");
            passenger.tickRiding();
            profiler.pop();
            Iterator var4 = passenger.getPassengerList().iterator();

            while(var4.hasNext()) {
               Entity entity = (Entity)var4.next();
               this.tickPassenger(passenger, entity);
            }

         }
      } else {
         passenger.stopRiding();
      }
   }

   public boolean canPlayerModifyAt(PlayerEntity player, BlockPos pos) {
      return !this.server.isSpawnProtected(this, pos, player) && this.getWorldBorder().contains(pos);
   }

   public void save(@Nullable ProgressListener progressListener, boolean flush, boolean bl) {
      ServerChunkManager serverChunkManager = this.getChunkManager();
      if (!bl) {
         if (progressListener != null) {
            progressListener.setTitle(new TranslatableText("menu.savingLevel"));
         }

         this.saveLevel();
         if (progressListener != null) {
            progressListener.setTask(new TranslatableText("menu.savingChunks"));
         }

         serverChunkManager.save(flush);
         if (flush) {
            this.entityManager.flush();
         } else {
            this.entityManager.save();
         }

      }
   }

   private void saveLevel() {
      if (this.enderDragonFight != null) {
         this.server.getSaveProperties().setDragonFight(this.enderDragonFight.toNbt());
      }

      this.getChunkManager().getPersistentStateManager().save();
   }

   /**
    * Computes a list of entities of the given type.
    * 
    * <strong>Warning:</strong> If {@code null} is passed as the entity type filter, care should be
    * taken that the type argument {@code T} is set to {@link Entity}, otherwise heap pollution
    * in the returned list or {@link ClassCastException} can occur.
    * 
    * @return a list of entities of the given type
    * 
    * @param predicate a predicate which returned entities must satisfy
    */
   public <T extends Entity> List<? extends T> getEntitiesByType(TypeFilter<Entity, T> typeFilter, Predicate<? super T> predicate) {
      List<T> list = Lists.newArrayList();
      this.getEntityLookup().forEach(typeFilter, (entity) -> {
         if (predicate.test(entity)) {
            list.add(entity);
         }

      });
      return list;
   }

   public List<? extends EnderDragonEntity> getAliveEnderDragons() {
      return this.getEntitiesByType(EntityType.ENDER_DRAGON, LivingEntity::isAlive);
   }

   public List<ServerPlayerEntity> getPlayers(Predicate<? super ServerPlayerEntity> predicate) {
      List<ServerPlayerEntity> list = Lists.newArrayList();
      Iterator var3 = this.players.iterator();

      while(var3.hasNext()) {
         ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)var3.next();
         if (predicate.test(serverPlayerEntity)) {
            list.add(serverPlayerEntity);
         }
      }

      return list;
   }

   @Nullable
   public ServerPlayerEntity getRandomAlivePlayer() {
      List<ServerPlayerEntity> list = this.getPlayers(LivingEntity::isAlive);
      return list.isEmpty() ? null : (ServerPlayerEntity)list.get(this.random.nextInt(list.size()));
   }

   public boolean spawnEntity(Entity entity) {
      return this.addEntity(entity);
   }

   public boolean tryLoadEntity(Entity entity) {
      return this.addEntity(entity);
   }

   public void onDimensionChanged(Entity entity) {
      this.addEntity(entity);
   }

   public void onPlayerTeleport(ServerPlayerEntity player) {
      this.addPlayer(player);
   }

   public void onPlayerChangeDimension(ServerPlayerEntity player) {
      this.addPlayer(player);
   }

   public void onPlayerConnected(ServerPlayerEntity player) {
      this.addPlayer(player);
   }

   public void onPlayerRespawned(ServerPlayerEntity player) {
      this.addPlayer(player);
   }

   private void addPlayer(ServerPlayerEntity player) {
      Entity entity = (Entity)this.getEntityLookup().get(player.getUuid());
      if (entity != null) {
         LOGGER.warn((String)"Force-added player with duplicate UUID {}", (Object)player.getUuid().toString());
         entity.detach();
         this.removePlayer((ServerPlayerEntity)entity, Entity.RemovalReason.DISCARDED);
      }

      this.entityManager.addEntity(player);
   }

   private boolean addEntity(Entity entity) {
      if (entity.isRemoved()) {
         LOGGER.warn((String)"Tried to add entity {} but it was marked as removed already", (Object)EntityType.getId(entity.getType()));
         return false;
      } else {
         return this.entityManager.addEntity(entity);
      }
   }

   public boolean shouldCreateNewEntityWithPassenger(Entity entity) {
      Stream var10000 = entity.streamSelfAndPassengers().map(Entity::getUuid);
      ServerEntityManager var10001 = this.entityManager;
      Objects.requireNonNull(var10001);
      if (var10000.anyMatch(var10001::has)) {
         return false;
      } else {
         this.spawnEntityAndPassengers(entity);
         return true;
      }
   }

   public void unloadEntities(WorldChunk chunk) {
      chunk.removeAllBlockEntities();
   }

   public void removePlayer(ServerPlayerEntity player, Entity.RemovalReason reason) {
      player.remove(reason);
   }

   public void setBlockBreakingInfo(int entityId, BlockPos pos, int progress) {
      Iterator var4 = this.server.getPlayerManager().getPlayerList().iterator();

      while(var4.hasNext()) {
         ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)var4.next();
         if (serverPlayerEntity != null && serverPlayerEntity.world == this && serverPlayerEntity.getId() != entityId) {
            double d = (double)pos.getX() - serverPlayerEntity.getX();
            double e = (double)pos.getY() - serverPlayerEntity.getY();
            double f = (double)pos.getZ() - serverPlayerEntity.getZ();
            if (d * d + e * e + f * f < 1024.0D) {
               serverPlayerEntity.networkHandler.sendPacket(new BlockBreakingProgressS2CPacket(entityId, pos, progress));
            }
         }
      }

   }

   public void playSound(@Nullable PlayerEntity player, double x, double y, double z, SoundEvent sound, SoundCategory category, float volume, float pitch) {
      this.server.getPlayerManager().sendToAround(player, x, y, z, volume > 1.0F ? (double)(16.0F * volume) : 16.0D, this.getRegistryKey(), new PlaySoundS2CPacket(sound, category, x, y, z, volume, pitch));
   }

   public void playSoundFromEntity(@Nullable PlayerEntity player, Entity entity, SoundEvent sound, SoundCategory category, float volume, float pitch) {
      this.server.getPlayerManager().sendToAround(player, entity.getX(), entity.getY(), entity.getZ(), volume > 1.0F ? (double)(16.0F * volume) : 16.0D, this.getRegistryKey(), new PlaySoundFromEntityS2CPacket(sound, category, entity, volume, pitch));
   }

   public void syncGlobalEvent(int eventId, BlockPos pos, int data) {
      this.server.getPlayerManager().sendToAll(new WorldEventS2CPacket(eventId, pos, data, true));
   }

   public void syncWorldEvent(@Nullable PlayerEntity player, int eventId, BlockPos pos, int data) {
      this.server.getPlayerManager().sendToAround(player, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), 64.0D, this.getRegistryKey(), new WorldEventS2CPacket(eventId, pos, data, false));
   }

   public int getLogicalHeight() {
      return this.getDimension().getLogicalHeight();
   }

   public void emitGameEvent(@Nullable Entity entity, GameEvent event, BlockPos pos) {
      this.emitGameEvent(entity, event, pos, event.getRange());
   }

   public void updateListeners(BlockPos pos, BlockState oldState, BlockState newState, int flags) {
      this.getChunkManager().markForUpdate(pos);
      VoxelShape voxelShape = oldState.getCollisionShape(this, pos);
      VoxelShape voxelShape2 = newState.getCollisionShape(this, pos);
      if (VoxelShapes.matchesAnywhere(voxelShape, voxelShape2, BooleanBiFunction.NOT_SAME)) {
         Iterator var7 = this.loadedMobs.iterator();

         while(var7.hasNext()) {
            MobEntity mobEntity = (MobEntity)var7.next();
            EntityNavigation entityNavigation = mobEntity.getNavigation();
            if (!entityNavigation.shouldRecalculatePath()) {
               entityNavigation.onBlockChanged(pos);
            }
         }

      }
   }

   public void sendEntityStatus(Entity entity, byte status) {
      this.getChunkManager().sendToNearbyPlayers(entity, new EntityStatusS2CPacket(entity, status));
   }

   public ServerChunkManager getChunkManager() {
      return this.serverChunkManager;
   }

   public Explosion createExplosion(@Nullable Entity entity, @Nullable DamageSource damageSource, @Nullable ExplosionBehavior behavior, double x, double y, double z, float power, boolean createFire, Explosion.DestructionType destructionType) {
      Explosion explosion = new Explosion(this, entity, damageSource, behavior, x, y, z, power, createFire, destructionType);
      explosion.collectBlocksAndDamageEntities();
      explosion.affectWorld(false);
      if (destructionType == Explosion.DestructionType.NONE) {
         explosion.clearAffectedBlocks();
      }

      Iterator var14 = this.players.iterator();

      while(var14.hasNext()) {
         ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)var14.next();
         if (serverPlayerEntity.squaredDistanceTo(x, y, z) < 4096.0D) {
            serverPlayerEntity.networkHandler.sendPacket(new ExplosionS2CPacket(x, y, z, power, explosion.getAffectedBlocks(), (Vec3d)explosion.getAffectedPlayers().get(serverPlayerEntity)));
         }
      }

      return explosion;
   }

   public void addSyncedBlockEvent(BlockPos pos, Block block, int type, int data) {
      this.syncedBlockEventQueue.add(new BlockEvent(pos, block, type, data));
   }

   private void processSyncedBlockEvents() {
      while(!this.syncedBlockEventQueue.isEmpty()) {
         BlockEvent blockEvent = (BlockEvent)this.syncedBlockEventQueue.removeFirst();
         if (this.processBlockEvent(blockEvent)) {
            this.server.getPlayerManager().sendToAround((PlayerEntity)null, (double)blockEvent.getPos().getX(), (double)blockEvent.getPos().getY(), (double)blockEvent.getPos().getZ(), 64.0D, this.getRegistryKey(), new BlockEventS2CPacket(blockEvent.getPos(), blockEvent.getBlock(), blockEvent.getType(), blockEvent.getData()));
         }
      }

   }

   private boolean processBlockEvent(BlockEvent event) {
      BlockState blockState = this.getBlockState(event.getPos());
      return blockState.isOf(event.getBlock()) ? blockState.onSyncedBlockEvent(this, event.getPos(), event.getType(), event.getData()) : false;
   }

   public ServerTickScheduler<Block> getBlockTickScheduler() {
      return this.blockTickScheduler;
   }

   public ServerTickScheduler<Fluid> getFluidTickScheduler() {
      return this.fluidTickScheduler;
   }

   @NotNull
   public MinecraftServer getServer() {
      return this.server;
   }

   public PortalForcer getPortalForcer() {
      return this.portalForcer;
   }

   public StructureManager getStructureManager() {
      return this.server.getStructureManager();
   }

   public void sendVibrationPacket(Vibration vibration) {
      BlockPos blockPos = vibration.getOrigin();
      VibrationS2CPacket vibrationS2CPacket = new VibrationS2CPacket(vibration);
      this.players.forEach((player) -> {
         this.sendToPlayerIfNearby(player, false, (double)blockPos.getX(), (double)blockPos.getY(), (double)blockPos.getZ(), vibrationS2CPacket);
      });
   }

   public <T extends ParticleEffect> int spawnParticles(T particle, double x, double y, double z, int count, double deltaX, double deltaY, double deltaZ, double speed) {
      ParticleS2CPacket particleS2CPacket = new ParticleS2CPacket(particle, false, x, y, z, (float)deltaX, (float)deltaY, (float)deltaZ, (float)speed, count);
      int i = 0;

      for(int j = 0; j < this.players.size(); ++j) {
         ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)this.players.get(j);
         if (this.sendToPlayerIfNearby(serverPlayerEntity, false, x, y, z, particleS2CPacket)) {
            ++i;
         }
      }

      return i;
   }

   public <T extends ParticleEffect> boolean spawnParticles(ServerPlayerEntity viewer, T particle, boolean force, double x, double y, double z, int count, double deltaX, double deltaY, double deltaZ, double speed) {
      Packet<?> packet = new ParticleS2CPacket(particle, force, x, y, z, (float)deltaX, (float)deltaY, (float)deltaZ, (float)speed, count);
      return this.sendToPlayerIfNearby(viewer, force, x, y, z, packet);
   }

   private boolean sendToPlayerIfNearby(ServerPlayerEntity player, boolean force, double x, double y, double z, Packet<?> packet) {
      if (player.getServerWorld() != this) {
         return false;
      } else {
         BlockPos blockPos = player.getBlockPos();
         if (blockPos.isWithinDistance(new Vec3d(x, y, z), force ? 512.0D : 32.0D)) {
            player.networkHandler.sendPacket(packet);
            return true;
         } else {
            return false;
         }
      }
   }

   @Nullable
   public Entity getEntityById(int id) {
      return (Entity)this.getEntityLookup().get(id);
   }

   @Deprecated
   @Nullable
   public Entity getDragonPart(int id) {
      Entity entity = (Entity)this.getEntityLookup().get(id);
      return entity != null ? entity : (Entity)this.dragonParts.get(id);
   }

   @Nullable
   public Entity getEntity(UUID uuid) {
      return (Entity)this.getEntityLookup().get(uuid);
   }

   @Nullable
   public BlockPos locateStructure(StructureFeature<?> feature, BlockPos pos, int radius, boolean skipExistingChunks) {
      return !this.server.getSaveProperties().getGeneratorOptions().shouldGenerateStructures() ? null : this.getChunkManager().getChunkGenerator().locateStructure(this, feature, pos, radius, skipExistingChunks);
   }

   @Nullable
   public BlockPos locateBiome(Biome biome, BlockPos pos, int radius, int i) {
      return this.getChunkManager().getChunkGenerator().getBiomeSource().locateBiome(pos.getX(), pos.getY(), pos.getZ(), radius, i, (biome2) -> {
         return biome2 == biome;
      }, this.random, true);
   }

   public RecipeManager getRecipeManager() {
      return this.server.getRecipeManager();
   }

   public TagManager getTagManager() {
      return this.server.getTagManager();
   }

   public boolean isSavingDisabled() {
      return this.savingDisabled;
   }

   public DynamicRegistryManager getRegistryManager() {
      return this.server.getRegistryManager();
   }

   public PersistentStateManager getPersistentStateManager() {
      return this.getChunkManager().getPersistentStateManager();
   }

   @Nullable
   public MapState getMapState(String id) {
      return (MapState)this.getServer().getOverworld().getPersistentStateManager().get(MapState::fromNbt, id);
   }

   public void putMapState(String id, MapState state) {
      this.getServer().getOverworld().getPersistentStateManager().set(id, state);
   }

   public int getNextMapId() {
      return ((IdCountsState)this.getServer().getOverworld().getPersistentStateManager().getOrCreate(IdCountsState::fromNbt, IdCountsState::new, "idcounts")).getNextMapId();
   }

   public void setSpawnPos(BlockPos pos, float angle) {
      ChunkPos chunkPos = new ChunkPos(new BlockPos(this.properties.getSpawnX(), 0, this.properties.getSpawnZ()));
      this.properties.setSpawnPos(pos, angle);
      this.getChunkManager().removeTicket(ChunkTicketType.START, chunkPos, 11, Unit.INSTANCE);
      this.getChunkManager().addTicket(ChunkTicketType.START, new ChunkPos(pos), 11, Unit.INSTANCE);
      this.getServer().getPlayerManager().sendToAll(new PlayerSpawnPositionS2CPacket(pos, angle));
   }

   public BlockPos getSpawnPos() {
      BlockPos blockPos = new BlockPos(this.properties.getSpawnX(), this.properties.getSpawnY(), this.properties.getSpawnZ());
      if (!this.getWorldBorder().contains(blockPos)) {
         blockPos = this.getTopPosition(Heightmap.Type.MOTION_BLOCKING, new BlockPos(this.getWorldBorder().getCenterX(), 0.0D, this.getWorldBorder().getCenterZ()));
      }

      return blockPos;
   }

   public float getSpawnAngle() {
      return this.properties.getSpawnAngle();
   }

   public LongSet getForcedChunks() {
      ForcedChunkState forcedChunkState = (ForcedChunkState)this.getPersistentStateManager().get(ForcedChunkState::fromNbt, "chunks");
      return (LongSet)(forcedChunkState != null ? LongSets.unmodifiable(forcedChunkState.getChunks()) : LongSets.EMPTY_SET);
   }

   public boolean setChunkForced(int x, int z, boolean forced) {
      ForcedChunkState forcedChunkState = (ForcedChunkState)this.getPersistentStateManager().getOrCreate(ForcedChunkState::fromNbt, ForcedChunkState::new, "chunks");
      ChunkPos chunkPos = new ChunkPos(x, z);
      long l = chunkPos.toLong();
      boolean bl2;
      if (forced) {
         bl2 = forcedChunkState.getChunks().add(l);
         if (bl2) {
            this.getChunk(x, z);
         }
      } else {
         bl2 = forcedChunkState.getChunks().remove(l);
      }

      forcedChunkState.setDirty(bl2);
      if (bl2) {
         this.getChunkManager().setChunkForced(chunkPos, forced);
      }

      return bl2;
   }

   public List<ServerPlayerEntity> getPlayers() {
      return this.players;
   }

   public void onBlockChanged(BlockPos pos, BlockState oldBlock, BlockState newBlock) {
      Optional<PointOfInterestType> optional = PointOfInterestType.from(oldBlock);
      Optional<PointOfInterestType> optional2 = PointOfInterestType.from(newBlock);
      if (!Objects.equals(optional, optional2)) {
         BlockPos blockPos = pos.toImmutable();
         optional.ifPresent((pointOfInterestType) -> {
            this.getServer().execute(() -> {
               this.getPointOfInterestStorage().remove(blockPos);
               DebugInfoSender.sendPoiRemoval(this, blockPos);
            });
         });
         optional2.ifPresent((pointOfInterestType) -> {
            this.getServer().execute(() -> {
               this.getPointOfInterestStorage().add(blockPos, pointOfInterestType);
               DebugInfoSender.sendPoiAddition(this, blockPos);
            });
         });
      }
   }

   public PointOfInterestStorage getPointOfInterestStorage() {
      return this.getChunkManager().getPointOfInterestStorage();
   }

   public boolean isNearOccupiedPointOfInterest(BlockPos pos) {
      return this.isNearOccupiedPointOfInterest(pos, 1);
   }

   public boolean isNearOccupiedPointOfInterest(ChunkSectionPos sectionPos) {
      return this.isNearOccupiedPointOfInterest(sectionPos.getCenterPos());
   }

   public boolean isNearOccupiedPointOfInterest(BlockPos pos, int maxDistance) {
      if (maxDistance > 6) {
         return false;
      } else {
         return this.getOccupiedPointOfInterestDistance(ChunkSectionPos.from(pos)) <= maxDistance;
      }
   }

   public int getOccupiedPointOfInterestDistance(ChunkSectionPos pos) {
      return this.getPointOfInterestStorage().getDistanceFromNearestOccupied(pos);
   }

   public RaidManager getRaidManager() {
      return this.raidManager;
   }

   @Nullable
   public Raid getRaidAt(BlockPos pos) {
      return this.raidManager.getRaidAt(pos, 9216);
   }

   public boolean hasRaidAt(BlockPos pos) {
      return this.getRaidAt(pos) != null;
   }

   public void handleInteraction(EntityInteraction interaction, Entity entity, InteractionObserver observer) {
      observer.onInteractionWith(interaction, entity);
   }

   public void dump(Path path) throws IOException {
      ThreadedAnvilChunkStorage threadedAnvilChunkStorage = this.getChunkManager().threadedAnvilChunkStorage;
      BufferedWriter writer = Files.newBufferedWriter(path.resolve("stats.txt"));

      try {
         writer.write(String.format("spawning_chunks: %d\n", threadedAnvilChunkStorage.getTicketManager().getSpawningChunkCount()));
         SpawnHelper.Info info = this.getChunkManager().getSpawnInfo();
         if (info != null) {
            ObjectIterator var5 = info.getGroupToCount().object2IntEntrySet().iterator();

            while(var5.hasNext()) {
               Entry<SpawnGroup> entry = (Entry)var5.next();
               writer.write(String.format("spawn_count.%s: %d\n", ((SpawnGroup)entry.getKey()).getName(), entry.getIntValue()));
            }
         }

         writer.write(String.format("entities: %s\n", this.entityManager.getDebugString()));
         writer.write(String.format("block_entity_tickers: %d\n", this.blockEntityTickers.size()));
         writer.write(String.format("block_ticks: %d\n", this.getBlockTickScheduler().getTicks()));
         writer.write(String.format("fluid_ticks: %d\n", this.getFluidTickScheduler().getTicks()));
         writer.write("distance_manager: " + threadedAnvilChunkStorage.getTicketManager().toDumpString() + "\n");
         writer.write(String.format("pending_tasks: %d\n", this.getChunkManager().getPendingTasks()));
      } catch (Throwable var22) {
         if (writer != null) {
            try {
               writer.close();
            } catch (Throwable var12) {
               var22.addSuppressed(var12);
            }
         }

         throw var22;
      }

      if (writer != null) {
         writer.close();
      }

      CrashReport crashReport = new CrashReport("Level dump", new Exception("dummy"));
      this.addDetailsToCrashReport(crashReport);
      BufferedWriter writer2 = Files.newBufferedWriter(path.resolve("example_crash.txt"));

      try {
         writer2.write(crashReport.asString());
      } catch (Throwable var19) {
         if (writer2 != null) {
            try {
               writer2.close();
            } catch (Throwable var13) {
               var19.addSuppressed(var13);
            }
         }

         throw var19;
      }

      if (writer2 != null) {
         writer2.close();
      }

      Path path2 = path.resolve("chunks.csv");
      BufferedWriter writer3 = Files.newBufferedWriter(path2);

      try {
         threadedAnvilChunkStorage.dump(writer3);
      } catch (Throwable var17) {
         if (writer3 != null) {
            try {
               writer3.close();
            } catch (Throwable var11) {
               var17.addSuppressed(var11);
            }
         }

         throw var17;
      }

      if (writer3 != null) {
         writer3.close();
      }

      Path path3 = path.resolve("entity_chunks.csv");
      BufferedWriter writer4 = Files.newBufferedWriter(path3);

      try {
         this.entityManager.dump(writer4);
      } catch (Throwable var20) {
         if (writer4 != null) {
            try {
               writer4.close();
            } catch (Throwable var15) {
               var20.addSuppressed(var15);
            }
         }

         throw var20;
      }

      if (writer4 != null) {
         writer4.close();
      }

      Path path4 = path.resolve("entities.csv");
      BufferedWriter writer5 = Files.newBufferedWriter(path4);

      try {
         dumpEntities(writer5, this.getEntityLookup().iterate());
      } catch (Throwable var21) {
         if (writer5 != null) {
            try {
               writer5.close();
            } catch (Throwable var16) {
               var21.addSuppressed(var16);
            }
         }

         throw var21;
      }

      if (writer5 != null) {
         writer5.close();
      }

      Path path5 = path.resolve("block_entities.csv");
      BufferedWriter writer6 = Files.newBufferedWriter(path5);

      try {
         this.dumpBlockEntities(writer6);
      } catch (Throwable var18) {
         if (writer6 != null) {
            try {
               writer6.close();
            } catch (Throwable var14) {
               var18.addSuppressed(var14);
            }
         }

         throw var18;
      }

      if (writer6 != null) {
         writer6.close();
      }

   }

   private static void dumpEntities(Writer writer, Iterable<Entity> entities) throws IOException {
      CsvWriter csvWriter = CsvWriter.makeHeader().addColumn("x").addColumn("y").addColumn("z").addColumn("uuid").addColumn("type").addColumn("alive").addColumn("display_name").addColumn("custom_name").startBody(writer);
      Iterator var3 = entities.iterator();

      while(var3.hasNext()) {
         Entity entity = (Entity)var3.next();
         Text text = entity.getCustomName();
         Text text2 = entity.getDisplayName();
         csvWriter.printRow(entity.getX(), entity.getY(), entity.getZ(), entity.getUuid(), Registry.ENTITY_TYPE.getId(entity.getType()), entity.isAlive(), text2.getString(), text != null ? text.getString() : null);
      }

   }

   private void dumpBlockEntities(Writer writer) throws IOException {
      CsvWriter csvWriter = CsvWriter.makeHeader().addColumn("x").addColumn("y").addColumn("z").addColumn("type").startBody(writer);
      Iterator var3 = this.blockEntityTickers.iterator();

      while(var3.hasNext()) {
         BlockEntityTickInvoker blockEntityTickInvoker = (BlockEntityTickInvoker)var3.next();
         BlockPos blockPos = blockEntityTickInvoker.getPos();
         csvWriter.printRow(blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockEntityTickInvoker.getName());
      }

   }

   @VisibleForTesting
   public void clearUpdatesInArea(BlockBox box) {
      this.syncedBlockEventQueue.removeIf((blockEvent) -> {
         return box.contains(blockEvent.getPos());
      });
   }

   public void updateNeighbors(BlockPos pos, Block block) {
      if (!this.isDebugWorld()) {
         this.updateNeighborsAlways(pos, block);
      }

   }

   public float getBrightness(Direction direction, boolean shaded) {
      return 1.0F;
   }

   public Iterable<Entity> iterateEntities() {
      return this.getEntityLookup().iterate();
   }

   public String toString() {
      return "ServerLevel[" + this.worldProperties.getLevelName() + "]";
   }

   public boolean isFlat() {
      return this.server.getSaveProperties().getGeneratorOptions().isFlatWorld();
   }

   public long getSeed() {
      return this.server.getSaveProperties().getGeneratorOptions().getSeed();
   }

   @Nullable
   public EnderDragonFight getEnderDragonFight() {
      return this.enderDragonFight;
   }

   public Stream<? extends StructureStart<?>> getStructures(ChunkSectionPos pos, StructureFeature<?> feature) {
      return this.getStructureAccessor().getStructuresWithChildren(pos, feature);
   }

   public ServerWorld toServerWorld() {
      return this;
   }

   @VisibleForTesting
   public String getDebugString() {
      return String.format("players: %s, entities: %s [%s], block_entities: %d [%s], block_ticks: %d, fluid_ticks: %d, chunk_source: %s", this.players.size(), this.entityManager.getDebugString(), getTopFive(this.entityManager.getLookup().iterate(), (entity) -> {
         return Registry.ENTITY_TYPE.getId(entity.getType()).toString();
      }), this.blockEntityTickers.size(), getTopFive(this.blockEntityTickers, BlockEntityTickInvoker::getName), this.getBlockTickScheduler().getTicks(), this.getFluidTickScheduler().getTicks(), this.asString());
   }

   /**
    * Categories {@code items} with the {@code classifier} and reports a message
    * indicating the top five biggest categories.
    * 
    * @param items the items to classify
    * @param classifier the classifier that determines the category of any item
    */
   private static <T> String getTopFive(Iterable<T> items, Function<T, String> classifier) {
      try {
         Object2IntOpenHashMap<String> object2IntOpenHashMap = new Object2IntOpenHashMap();
         Iterator var3 = items.iterator();

         while(var3.hasNext()) {
            T object = var3.next();
            String string = (String)classifier.apply(object);
            object2IntOpenHashMap.addTo(string, 1);
         }

         return (String)object2IntOpenHashMap.object2IntEntrySet().stream().sorted(Comparator.comparing(Entry::getIntValue).reversed()).limit(5L).map((entry) -> {
            String var10000 = (String)entry.getKey();
            return var10000 + ":" + entry.getIntValue();
         }).collect(Collectors.joining(","));
      } catch (Exception var6) {
         return "";
      }
   }

   public static void createEndSpawnPlatform(ServerWorld world) {
      BlockPos blockPos = END_SPAWN_POS;
      int i = blockPos.getX();
      int j = blockPos.getY() - 2;
      int k = blockPos.getZ();
      BlockPos.iterate(i - 2, j + 1, k - 2, i + 2, j + 3, k + 2).forEach((blockPosx) -> {
         world.setBlockState(blockPosx, Blocks.AIR.getDefaultState());
      });
      BlockPos.iterate(i - 2, j, k - 2, i + 2, j, k + 2).forEach((blockPosx) -> {
         world.setBlockState(blockPosx, Blocks.OBSIDIAN.getDefaultState());
      });
   }

   protected EntityLookup<Entity> getEntityLookup() {
      return this.entityManager.getLookup();
   }

   public void loadEntities(Stream<Entity> entities) {
      this.entityManager.loadEntities(entities);
   }

   public void addEntities(Stream<Entity> entities) {
      this.entityManager.addEntities(entities);
   }

   public void close() throws IOException {
      super.close();
      this.entityManager.close();
   }

   public String asString() {
      String var10000 = this.serverChunkManager.getDebugString();
      return "Chunks[S] W: " + var10000 + " E: " + this.entityManager.getDebugString();
   }

   public boolean method_37116(long l) {
      return this.entityManager.method_37252(l);
   }

   public boolean method_37117(BlockPos blockPos) {
      long l = ChunkPos.method_37232(blockPos);
      return this.serverChunkManager.method_37114(l) && this.method_37116(l);
   }

   public boolean method_37118(BlockPos blockPos) {
      return this.entityManager.method_37254(blockPos);
   }

   public boolean method_37115(ChunkPos chunkPos) {
      return this.entityManager.method_37253(chunkPos);
   }

   final class ServerEntityHandler implements EntityHandler<Entity> {
      public void create(Entity entity) {
      }

      public void destroy(Entity entity) {
         ServerWorld.this.getScoreboard().resetEntityScore(entity);
      }

      public void startTicking(Entity entity) {
         ServerWorld.this.entityList.add(entity);
      }

      public void stopTicking(Entity entity) {
         ServerWorld.this.entityList.remove(entity);
      }

      public void startTracking(Entity entity) {
         ServerWorld.this.getChunkManager().loadEntity(entity);
         if (entity instanceof ServerPlayerEntity) {
            ServerWorld.this.players.add((ServerPlayerEntity)entity);
            ServerWorld.this.updateSleepingPlayers();
         }

         if (entity instanceof MobEntity) {
            ServerWorld.this.loadedMobs.add((MobEntity)entity);
         }

         if (entity instanceof EnderDragonEntity) {
            EnderDragonPart[] var2 = ((EnderDragonEntity)entity).getBodyParts();
            int var3 = var2.length;

            for(int var4 = 0; var4 < var3; ++var4) {
               EnderDragonPart enderDragonPart = var2[var4];
               ServerWorld.this.dragonParts.put(enderDragonPart.getId(), enderDragonPart);
            }
         }

      }

      public void stopTracking(Entity entity) {
         ServerWorld.this.getChunkManager().unloadEntity(entity);
         if (entity instanceof ServerPlayerEntity) {
            ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)entity;
            ServerWorld.this.players.remove(serverPlayerEntity);
            ServerWorld.this.updateSleepingPlayers();
         }

         if (entity instanceof MobEntity) {
            ServerWorld.this.loadedMobs.remove(entity);
         }

         if (entity instanceof EnderDragonEntity) {
            EnderDragonPart[] var6 = ((EnderDragonEntity)entity).getBodyParts();
            int var3 = var6.length;

            for(int var4 = 0; var4 < var3; ++var4) {
               EnderDragonPart enderDragonPart = var6[var4];
               ServerWorld.this.dragonParts.remove(enderDragonPart.getId());
            }
         }

         EntityGameEventHandler entityGameEventHandler = entity.getGameEventHandler();
         if (entityGameEventHandler != null) {
            entityGameEventHandler.onEntityRemoval(entity.world);
         }

      }
   }
}
