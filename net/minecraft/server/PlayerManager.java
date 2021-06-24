package net.minecraft.server;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import io.netty.buffer.Unpooled;
import java.io.File;
import java.net.SocketAddress;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.MessageType;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ChunkLoadDistanceS2CPacket;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.network.packet.s2c.play.DifficultyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusEffectS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.ExperienceBarUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerAbilitiesS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerSpawnPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.SynchronizeRecipesS2CPacket;
import net.minecraft.network.packet.s2c.play.SynchronizeTagsS2CPacket;
import net.minecraft.network.packet.s2c.play.TeamS2CPacket;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldBorderCenterChangedS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldBorderInitializeS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldBorderInterpolateSizeS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldBorderSizeChangedS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldBorderWarningBlocksChangedS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldBorderWarningTimeChangedS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.ServerStatHandler;
import net.minecraft.stat.Stats;
import net.minecraft.tag.BlockTags;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.UserCache;
import net.minecraft.util.Util;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.WorldProperties;
import net.minecraft.world.WorldSaveHandler;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.border.WorldBorderListener;
import net.minecraft.world.dimension.DimensionType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public abstract class PlayerManager {
   public static final File BANNED_PLAYERS_FILE = new File("banned-players.json");
   public static final File BANNED_IPS_FILE = new File("banned-ips.json");
   public static final File OPERATORS_FILE = new File("ops.json");
   public static final File WHITELIST_FILE = new File("whitelist.json");
   private static final Logger LOGGER = LogManager.getLogger();
   private static final int LATENCY_UPDATE_INTERVAL = 600;
   private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
   private final MinecraftServer server;
   private final List<ServerPlayerEntity> players = Lists.newArrayList();
   private final Map<UUID, ServerPlayerEntity> playerMap = Maps.newHashMap();
   private final BannedPlayerList bannedProfiles;
   private final BannedIpList bannedIps;
   private final OperatorList ops;
   private final Whitelist whitelist;
   private final Map<UUID, ServerStatHandler> statisticsMap;
   private final Map<UUID, PlayerAdvancementTracker> advancementTrackers;
   private final WorldSaveHandler saveHandler;
   private boolean whitelistEnabled;
   private final DynamicRegistryManager.Impl registryManager;
   protected final int maxPlayers;
   private int viewDistance;
   private boolean cheatsAllowed;
   private static final boolean field_29791 = false;
   private int latencyUpdateTimer;

   public PlayerManager(MinecraftServer server, DynamicRegistryManager.Impl registryManager, WorldSaveHandler saveHandler, int maxPlayers) {
      this.bannedProfiles = new BannedPlayerList(BANNED_PLAYERS_FILE);
      this.bannedIps = new BannedIpList(BANNED_IPS_FILE);
      this.ops = new OperatorList(OPERATORS_FILE);
      this.whitelist = new Whitelist(WHITELIST_FILE);
      this.statisticsMap = Maps.newHashMap();
      this.advancementTrackers = Maps.newHashMap();
      this.server = server;
      this.registryManager = registryManager;
      this.maxPlayers = maxPlayers;
      this.saveHandler = saveHandler;
   }

   public void onPlayerConnect(ClientConnection connection, ServerPlayerEntity player) {
      GameProfile gameProfile = player.getGameProfile();
      UserCache userCache = this.server.getUserCache();
      GameProfile gameProfile2 = userCache.getByUuid(gameProfile.getId());
      String string = gameProfile2 == null ? gameProfile.getName() : gameProfile2.getName();
      userCache.add(gameProfile);
      NbtCompound nbtCompound = this.loadPlayerData(player);
      RegistryKey var23;
      if (nbtCompound != null) {
         DataResult var10000 = DimensionType.worldFromDimensionNbt(new Dynamic(NbtOps.INSTANCE, nbtCompound.get("Dimension")));
         Logger var10001 = LOGGER;
         Objects.requireNonNull(var10001);
         var23 = (RegistryKey)var10000.resultOrPartial(var10001::error).orElse(World.OVERWORLD);
      } else {
         var23 = World.OVERWORLD;
      }

      RegistryKey<World> registryKey = var23;
      ServerWorld serverWorld = this.server.getWorld(registryKey);
      ServerWorld serverWorld3;
      if (serverWorld == null) {
         LOGGER.warn((String)"Unknown respawn dimension {}, defaulting to overworld", (Object)registryKey);
         serverWorld3 = this.server.getOverworld();
      } else {
         serverWorld3 = serverWorld;
      }

      player.setWorld(serverWorld3);
      String string2 = "local";
      if (connection.getAddress() != null) {
         string2 = connection.getAddress().toString();
      }

      LOGGER.info((String)"{}[{}] logged in with entity id {} at ({}, {}, {})", (Object)player.getName().getString(), string2, player.getId(), player.getX(), player.getY(), player.getZ());
      WorldProperties worldProperties = serverWorld3.getLevelProperties();
      player.setGameMode(nbtCompound);
      ServerPlayNetworkHandler serverPlayNetworkHandler = new ServerPlayNetworkHandler(this.server, connection, player);
      GameRules gameRules = serverWorld3.getGameRules();
      boolean bl = gameRules.getBoolean(GameRules.DO_IMMEDIATE_RESPAWN);
      boolean bl2 = gameRules.getBoolean(GameRules.REDUCED_DEBUG_INFO);
      serverPlayNetworkHandler.sendPacket(new GameJoinS2CPacket(player.getId(), player.interactionManager.getGameMode(), player.interactionManager.getPreviousGameMode(), BiomeAccess.hashSeed(serverWorld3.getSeed()), worldProperties.isHardcore(), this.server.getWorldRegistryKeys(), this.registryManager, serverWorld3.getDimension(), serverWorld3.getRegistryKey(), this.getMaxPlayerCount(), this.viewDistance, bl2, !bl, serverWorld3.isDebugWorld(), serverWorld3.isFlat()));
      serverPlayNetworkHandler.sendPacket(new CustomPayloadS2CPacket(CustomPayloadS2CPacket.BRAND, (new PacketByteBuf(Unpooled.buffer())).writeString(this.getServer().getServerModName())));
      serverPlayNetworkHandler.sendPacket(new DifficultyS2CPacket(worldProperties.getDifficulty(), worldProperties.isDifficultyLocked()));
      serverPlayNetworkHandler.sendPacket(new PlayerAbilitiesS2CPacket(player.getAbilities()));
      serverPlayNetworkHandler.sendPacket(new UpdateSelectedSlotS2CPacket(player.getInventory().selectedSlot));
      serverPlayNetworkHandler.sendPacket(new SynchronizeRecipesS2CPacket(this.server.getRecipeManager().values()));
      serverPlayNetworkHandler.sendPacket(new SynchronizeTagsS2CPacket(this.server.getTagManager().toPacket(this.registryManager)));
      this.sendCommandTree(player);
      player.getStatHandler().updateStatSet();
      player.getRecipeBook().sendInitRecipesPacket(player);
      this.sendScoreboard(serverWorld3.getScoreboard(), player);
      this.server.forcePlayerSampleUpdate();
      TranslatableText mutableText2;
      if (player.getGameProfile().getName().equalsIgnoreCase(string)) {
         mutableText2 = new TranslatableText("multiplayer.player.joined", new Object[]{player.getDisplayName()});
      } else {
         mutableText2 = new TranslatableText("multiplayer.player.joined.renamed", new Object[]{player.getDisplayName(), string});
      }

      this.broadcastChatMessage(mutableText2.formatted(Formatting.YELLOW), MessageType.SYSTEM, Util.NIL_UUID);
      serverPlayNetworkHandler.requestTeleport(player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
      this.players.add(player);
      this.playerMap.put(player.getUuid(), player);
      this.sendToAll(new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, new ServerPlayerEntity[]{player}));

      for(int i = 0; i < this.players.size(); ++i) {
         player.networkHandler.sendPacket(new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, new ServerPlayerEntity[]{(ServerPlayerEntity)this.players.get(i)}));
      }

      serverWorld3.onPlayerConnected(player);
      this.server.getBossBarManager().onPlayerConnect(player);
      this.sendWorldInfo(player, serverWorld3);
      if (!this.server.getResourcePackUrl().isEmpty()) {
         player.sendResourcePackUrl(this.server.getResourcePackUrl(), this.server.getResourcePackHash(), this.server.requireResourcePack(), this.server.getResourcePackPrompt());
      }

      Iterator var24 = player.getStatusEffects().iterator();

      while(var24.hasNext()) {
         StatusEffectInstance statusEffectInstance = (StatusEffectInstance)var24.next();
         serverPlayNetworkHandler.sendPacket(new EntityStatusEffectS2CPacket(player.getId(), statusEffectInstance));
      }

      if (nbtCompound != null && nbtCompound.contains("RootVehicle", 10)) {
         NbtCompound nbtCompound2 = nbtCompound.getCompound("RootVehicle");
         Entity entity = EntityType.loadEntityWithPassengers(nbtCompound2.getCompound("Entity"), serverWorld3, (vehicle) -> {
            return !serverWorld3.tryLoadEntity(vehicle) ? null : vehicle;
         });
         if (entity != null) {
            UUID uUID2;
            if (nbtCompound2.containsUuid("Attach")) {
               uUID2 = nbtCompound2.getUuid("Attach");
            } else {
               uUID2 = null;
            }

            Iterator var21;
            Entity entity3;
            if (entity.getUuid().equals(uUID2)) {
               player.startRiding(entity, true);
            } else {
               var21 = entity.getPassengersDeep().iterator();

               while(var21.hasNext()) {
                  entity3 = (Entity)var21.next();
                  if (entity3.getUuid().equals(uUID2)) {
                     player.startRiding(entity3, true);
                     break;
                  }
               }
            }

            if (!player.hasVehicle()) {
               LOGGER.warn("Couldn't reattach entity to player");
               entity.discard();
               var21 = entity.getPassengersDeep().iterator();

               while(var21.hasNext()) {
                  entity3 = (Entity)var21.next();
                  entity3.discard();
               }
            }
         }
      }

      player.onSpawn();
   }

   protected void sendScoreboard(ServerScoreboard scoreboard, ServerPlayerEntity player) {
      Set<ScoreboardObjective> set = Sets.newHashSet();
      Iterator var4 = scoreboard.getTeams().iterator();

      while(var4.hasNext()) {
         Team team = (Team)var4.next();
         player.networkHandler.sendPacket(TeamS2CPacket.updateTeam(team, true));
      }

      for(int i = 0; i < 19; ++i) {
         ScoreboardObjective scoreboardObjective = scoreboard.getObjectiveForSlot(i);
         if (scoreboardObjective != null && !set.contains(scoreboardObjective)) {
            List<Packet<?>> list = scoreboard.createChangePackets(scoreboardObjective);
            Iterator var7 = list.iterator();

            while(var7.hasNext()) {
               Packet<?> packet = (Packet)var7.next();
               player.networkHandler.sendPacket(packet);
            }

            set.add(scoreboardObjective);
         }
      }

   }

   public void setMainWorld(ServerWorld world) {
      world.getWorldBorder().addListener(new WorldBorderListener() {
         public void onSizeChange(WorldBorder border, double size) {
            PlayerManager.this.sendToAll(new WorldBorderSizeChangedS2CPacket(border));
         }

         public void onInterpolateSize(WorldBorder border, double fromSize, double toSize, long time) {
            PlayerManager.this.sendToAll(new WorldBorderInterpolateSizeS2CPacket(border));
         }

         public void onCenterChanged(WorldBorder border, double centerX, double centerZ) {
            PlayerManager.this.sendToAll(new WorldBorderCenterChangedS2CPacket(border));
         }

         public void onWarningTimeChanged(WorldBorder border, int warningTime) {
            PlayerManager.this.sendToAll(new WorldBorderWarningTimeChangedS2CPacket(border));
         }

         public void onWarningBlocksChanged(WorldBorder border, int warningBlockDistance) {
            PlayerManager.this.sendToAll(new WorldBorderWarningBlocksChangedS2CPacket(border));
         }

         public void onDamagePerBlockChanged(WorldBorder border, double damagePerBlock) {
         }

         public void onSafeZoneChanged(WorldBorder border, double safeZoneRadius) {
         }
      });
   }

   @Nullable
   public NbtCompound loadPlayerData(ServerPlayerEntity player) {
      NbtCompound nbtCompound = this.server.getSaveProperties().getPlayerData();
      NbtCompound nbtCompound3;
      if (player.getName().getString().equals(this.server.getUserName()) && nbtCompound != null) {
         nbtCompound3 = nbtCompound;
         player.readNbt(nbtCompound);
         LOGGER.debug("loading single player");
      } else {
         nbtCompound3 = this.saveHandler.loadPlayerData(player);
      }

      return nbtCompound3;
   }

   protected void savePlayerData(ServerPlayerEntity player) {
      this.saveHandler.savePlayerData(player);
      ServerStatHandler serverStatHandler = (ServerStatHandler)this.statisticsMap.get(player.getUuid());
      if (serverStatHandler != null) {
         serverStatHandler.save();
      }

      PlayerAdvancementTracker playerAdvancementTracker = (PlayerAdvancementTracker)this.advancementTrackers.get(player.getUuid());
      if (playerAdvancementTracker != null) {
         playerAdvancementTracker.save();
      }

   }

   public void remove(ServerPlayerEntity player) {
      ServerWorld serverWorld = player.getServerWorld();
      player.incrementStat(Stats.LEAVE_GAME);
      this.savePlayerData(player);
      if (player.hasVehicle()) {
         Entity entity = player.getRootVehicle();
         if (entity.hasPlayerRider()) {
            LOGGER.debug("Removing player mount");
            player.stopRiding();
            entity.streamPassengersAndSelf().forEach((entityx) -> {
               entityx.setRemoved(Entity.RemovalReason.UNLOADED_WITH_PLAYER);
            });
         }
      }

      player.detach();
      serverWorld.removePlayer(player, Entity.RemovalReason.UNLOADED_WITH_PLAYER);
      player.getAdvancementTracker().clearCriteria();
      this.players.remove(player);
      this.server.getBossBarManager().onPlayerDisconnect(player);
      UUID uUID = player.getUuid();
      ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)this.playerMap.get(uUID);
      if (serverPlayerEntity == player) {
         this.playerMap.remove(uUID);
         this.statisticsMap.remove(uUID);
         this.advancementTrackers.remove(uUID);
      }

      this.sendToAll(new PlayerListS2CPacket(PlayerListS2CPacket.Action.REMOVE_PLAYER, new ServerPlayerEntity[]{player}));
   }

   @Nullable
   public Text checkCanJoin(SocketAddress address, GameProfile profile) {
      TranslatableText mutableText2;
      if (this.bannedProfiles.contains(profile)) {
         BannedPlayerEntry bannedPlayerEntry = (BannedPlayerEntry)this.bannedProfiles.get(profile);
         mutableText2 = new TranslatableText("multiplayer.disconnect.banned.reason", new Object[]{bannedPlayerEntry.getReason()});
         if (bannedPlayerEntry.getExpiryDate() != null) {
            mutableText2.append((Text)(new TranslatableText("multiplayer.disconnect.banned.expiration", new Object[]{DATE_FORMATTER.format(bannedPlayerEntry.getExpiryDate())})));
         }

         return mutableText2;
      } else if (!this.isWhitelisted(profile)) {
         return new TranslatableText("multiplayer.disconnect.not_whitelisted");
      } else if (this.bannedIps.isBanned(address)) {
         BannedIpEntry bannedIpEntry = this.bannedIps.get(address);
         mutableText2 = new TranslatableText("multiplayer.disconnect.banned_ip.reason", new Object[]{bannedIpEntry.getReason()});
         if (bannedIpEntry.getExpiryDate() != null) {
            mutableText2.append((Text)(new TranslatableText("multiplayer.disconnect.banned_ip.expiration", new Object[]{DATE_FORMATTER.format(bannedIpEntry.getExpiryDate())})));
         }

         return mutableText2;
      } else {
         return this.players.size() >= this.maxPlayers && !this.canBypassPlayerLimit(profile) ? new TranslatableText("multiplayer.disconnect.server_full") : null;
      }
   }

   public ServerPlayerEntity createPlayer(GameProfile profile) {
      UUID uUID = PlayerEntity.getUuidFromProfile(profile);
      List<ServerPlayerEntity> list = Lists.newArrayList();

      for(int i = 0; i < this.players.size(); ++i) {
         ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)this.players.get(i);
         if (serverPlayerEntity.getUuid().equals(uUID)) {
            list.add(serverPlayerEntity);
         }
      }

      ServerPlayerEntity serverPlayerEntity2 = (ServerPlayerEntity)this.playerMap.get(profile.getId());
      if (serverPlayerEntity2 != null && !list.contains(serverPlayerEntity2)) {
         list.add(serverPlayerEntity2);
      }

      Iterator var8 = list.iterator();

      while(var8.hasNext()) {
         ServerPlayerEntity serverPlayerEntity3 = (ServerPlayerEntity)var8.next();
         serverPlayerEntity3.networkHandler.disconnect(new TranslatableText("multiplayer.disconnect.duplicate_login"));
      }

      return new ServerPlayerEntity(this.server, this.server.getOverworld(), profile);
   }

   public ServerPlayerEntity respawnPlayer(ServerPlayerEntity player, boolean alive) {
      this.players.remove(player);
      player.getServerWorld().removePlayer(player, Entity.RemovalReason.DISCARDED);
      BlockPos blockPos = player.getSpawnPointPosition();
      float f = player.getSpawnAngle();
      boolean bl = player.isSpawnPointSet();
      ServerWorld serverWorld = this.server.getWorld(player.getSpawnPointDimension());
      Optional optional2;
      if (serverWorld != null && blockPos != null) {
         optional2 = PlayerEntity.findRespawnPosition(serverWorld, blockPos, f, bl, alive);
      } else {
         optional2 = Optional.empty();
      }

      ServerWorld serverWorld2 = serverWorld != null && optional2.isPresent() ? serverWorld : this.server.getOverworld();
      ServerPlayerEntity serverPlayerEntity = new ServerPlayerEntity(this.server, serverWorld2, player.getGameProfile());
      serverPlayerEntity.networkHandler = player.networkHandler;
      serverPlayerEntity.copyFrom(player, alive);
      serverPlayerEntity.setId(player.getId());
      serverPlayerEntity.setMainArm(player.getMainArm());
      Iterator var10 = player.getScoreboardTags().iterator();

      while(var10.hasNext()) {
         String string = (String)var10.next();
         serverPlayerEntity.addScoreboardTag(string);
      }

      boolean bl2 = false;
      if (optional2.isPresent()) {
         BlockState blockState = serverWorld2.getBlockState(blockPos);
         boolean bl3 = blockState.isOf(Blocks.RESPAWN_ANCHOR);
         Vec3d vec3d = (Vec3d)optional2.get();
         float h;
         if (!blockState.isIn(BlockTags.BEDS) && !bl3) {
            h = f;
         } else {
            Vec3d vec3d2 = Vec3d.ofBottomCenter(blockPos).subtract(vec3d).normalize();
            h = (float)MathHelper.wrapDegrees(MathHelper.atan2(vec3d2.z, vec3d2.x) * 57.2957763671875D - 90.0D);
         }

         serverPlayerEntity.refreshPositionAndAngles(vec3d.x, vec3d.y, vec3d.z, h, 0.0F);
         serverPlayerEntity.setSpawnPoint(serverWorld2.getRegistryKey(), blockPos, f, bl, false);
         bl2 = !alive && bl3;
      } else if (blockPos != null) {
         serverPlayerEntity.networkHandler.sendPacket(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.NO_RESPAWN_BLOCK, GameStateChangeS2CPacket.DEMO_OPEN_SCREEN));
      }

      while(!serverWorld2.isSpaceEmpty(serverPlayerEntity) && serverPlayerEntity.getY() < (double)serverWorld2.getTopY()) {
         serverPlayerEntity.setPosition(serverPlayerEntity.getX(), serverPlayerEntity.getY() + 1.0D, serverPlayerEntity.getZ());
      }

      WorldProperties worldProperties = serverPlayerEntity.world.getLevelProperties();
      serverPlayerEntity.networkHandler.sendPacket(new PlayerRespawnS2CPacket(serverPlayerEntity.world.getDimension(), serverPlayerEntity.world.getRegistryKey(), BiomeAccess.hashSeed(serverPlayerEntity.getServerWorld().getSeed()), serverPlayerEntity.interactionManager.getGameMode(), serverPlayerEntity.interactionManager.getPreviousGameMode(), serverPlayerEntity.getServerWorld().isDebugWorld(), serverPlayerEntity.getServerWorld().isFlat(), alive));
      serverPlayerEntity.networkHandler.requestTeleport(serverPlayerEntity.getX(), serverPlayerEntity.getY(), serverPlayerEntity.getZ(), serverPlayerEntity.getYaw(), serverPlayerEntity.getPitch());
      serverPlayerEntity.networkHandler.sendPacket(new PlayerSpawnPositionS2CPacket(serverWorld2.getSpawnPos(), serverWorld2.getSpawnAngle()));
      serverPlayerEntity.networkHandler.sendPacket(new DifficultyS2CPacket(worldProperties.getDifficulty(), worldProperties.isDifficultyLocked()));
      serverPlayerEntity.networkHandler.sendPacket(new ExperienceBarUpdateS2CPacket(serverPlayerEntity.experienceProgress, serverPlayerEntity.totalExperience, serverPlayerEntity.experienceLevel));
      this.sendWorldInfo(serverPlayerEntity, serverWorld2);
      this.sendCommandTree(serverPlayerEntity);
      serverWorld2.onPlayerRespawned(serverPlayerEntity);
      this.players.add(serverPlayerEntity);
      this.playerMap.put(serverPlayerEntity.getUuid(), serverPlayerEntity);
      serverPlayerEntity.onSpawn();
      serverPlayerEntity.setHealth(serverPlayerEntity.getHealth());
      if (bl2) {
         serverPlayerEntity.networkHandler.sendPacket(new PlaySoundS2CPacket(SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.BLOCKS, (double)blockPos.getX(), (double)blockPos.getY(), (double)blockPos.getZ(), 1.0F, 1.0F));
      }

      return serverPlayerEntity;
   }

   public void sendCommandTree(ServerPlayerEntity player) {
      GameProfile gameProfile = player.getGameProfile();
      int i = this.server.getPermissionLevel(gameProfile);
      this.sendCommandTree(player, i);
   }

   public void updatePlayerLatency() {
      if (++this.latencyUpdateTimer > 600) {
         this.sendToAll(new PlayerListS2CPacket(PlayerListS2CPacket.Action.UPDATE_LATENCY, this.players));
         this.latencyUpdateTimer = 0;
      }

   }

   public void sendToAll(Packet<?> packet) {
      Iterator var2 = this.players.iterator();

      while(var2.hasNext()) {
         ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)var2.next();
         serverPlayerEntity.networkHandler.sendPacket(packet);
      }

   }

   public void sendToDimension(Packet<?> packet, RegistryKey<World> dimension) {
      Iterator var3 = this.players.iterator();

      while(var3.hasNext()) {
         ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)var3.next();
         if (serverPlayerEntity.world.getRegistryKey() == dimension) {
            serverPlayerEntity.networkHandler.sendPacket(packet);
         }
      }

   }

   public void sendToTeam(PlayerEntity source, Text message) {
      AbstractTeam abstractTeam = source.getScoreboardTeam();
      if (abstractTeam != null) {
         Collection<String> collection = abstractTeam.getPlayerList();
         Iterator var5 = collection.iterator();

         while(var5.hasNext()) {
            String string = (String)var5.next();
            ServerPlayerEntity serverPlayerEntity = this.getPlayer(string);
            if (serverPlayerEntity != null && serverPlayerEntity != source) {
               serverPlayerEntity.sendSystemMessage(message, source.getUuid());
            }
         }

      }
   }

   public void sendToOtherTeams(PlayerEntity source, Text message) {
      AbstractTeam abstractTeam = source.getScoreboardTeam();
      if (abstractTeam == null) {
         this.broadcastChatMessage(message, MessageType.SYSTEM, source.getUuid());
      } else {
         for(int i = 0; i < this.players.size(); ++i) {
            ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)this.players.get(i);
            if (serverPlayerEntity.getScoreboardTeam() != abstractTeam) {
               serverPlayerEntity.sendSystemMessage(message, source.getUuid());
            }
         }

      }
   }

   public String[] getPlayerNames() {
      String[] strings = new String[this.players.size()];

      for(int i = 0; i < this.players.size(); ++i) {
         strings[i] = ((ServerPlayerEntity)this.players.get(i)).getGameProfile().getName();
      }

      return strings;
   }

   public BannedPlayerList getUserBanList() {
      return this.bannedProfiles;
   }

   public BannedIpList getIpBanList() {
      return this.bannedIps;
   }

   public void addToOperators(GameProfile profile) {
      this.ops.add(new OperatorEntry(profile, this.server.getOpPermissionLevel(), this.ops.isOp(profile)));
      ServerPlayerEntity serverPlayerEntity = this.getPlayer(profile.getId());
      if (serverPlayerEntity != null) {
         this.sendCommandTree(serverPlayerEntity);
      }

   }

   public void removeFromOperators(GameProfile profile) {
      this.ops.remove(profile);
      ServerPlayerEntity serverPlayerEntity = this.getPlayer(profile.getId());
      if (serverPlayerEntity != null) {
         this.sendCommandTree(serverPlayerEntity);
      }

   }

   private void sendCommandTree(ServerPlayerEntity player, int permissionLevel) {
      if (player.networkHandler != null) {
         byte d;
         if (permissionLevel <= 0) {
            d = 24;
         } else if (permissionLevel >= 4) {
            d = 28;
         } else {
            d = (byte)(24 + permissionLevel);
         }

         player.networkHandler.sendPacket(new EntityStatusS2CPacket(player, d));
      }

      this.server.getCommandManager().sendCommandTree(player);
   }

   public boolean isWhitelisted(GameProfile profile) {
      return !this.whitelistEnabled || this.ops.contains(profile) || this.whitelist.contains(profile);
   }

   public boolean isOperator(GameProfile profile) {
      return this.ops.contains(profile) || this.server.isHost(profile) && this.server.getSaveProperties().areCommandsAllowed() || this.cheatsAllowed;
   }

   @Nullable
   public ServerPlayerEntity getPlayer(String name) {
      Iterator var2 = this.players.iterator();

      ServerPlayerEntity serverPlayerEntity;
      do {
         if (!var2.hasNext()) {
            return null;
         }

         serverPlayerEntity = (ServerPlayerEntity)var2.next();
      } while(!serverPlayerEntity.getGameProfile().getName().equalsIgnoreCase(name));

      return serverPlayerEntity;
   }

   public void sendToAround(@Nullable PlayerEntity player, double x, double y, double z, double distance, RegistryKey<World> worldKey, Packet<?> packet) {
      for(int i = 0; i < this.players.size(); ++i) {
         ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)this.players.get(i);
         if (serverPlayerEntity != player && serverPlayerEntity.world.getRegistryKey() == worldKey) {
            double d = x - serverPlayerEntity.getX();
            double e = y - serverPlayerEntity.getY();
            double f = z - serverPlayerEntity.getZ();
            if (d * d + e * e + f * f < distance * distance) {
               serverPlayerEntity.networkHandler.sendPacket(packet);
            }
         }
      }

   }

   public void saveAllPlayerData() {
      for(int i = 0; i < this.players.size(); ++i) {
         this.savePlayerData((ServerPlayerEntity)this.players.get(i));
      }

   }

   public Whitelist getWhitelist() {
      return this.whitelist;
   }

   public String[] getWhitelistedNames() {
      return this.whitelist.getNames();
   }

   public OperatorList getOpList() {
      return this.ops;
   }

   public String[] getOpNames() {
      return this.ops.getNames();
   }

   public void reloadWhitelist() {
   }

   public void sendWorldInfo(ServerPlayerEntity player, ServerWorld world) {
      WorldBorder worldBorder = this.server.getOverworld().getWorldBorder();
      player.networkHandler.sendPacket(new WorldBorderInitializeS2CPacket(worldBorder));
      player.networkHandler.sendPacket(new WorldTimeUpdateS2CPacket(world.getTime(), world.getTimeOfDay(), world.getGameRules().getBoolean(GameRules.DO_DAYLIGHT_CYCLE)));
      player.networkHandler.sendPacket(new PlayerSpawnPositionS2CPacket(world.getSpawnPos(), world.getSpawnAngle()));
      if (world.isRaining()) {
         player.networkHandler.sendPacket(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.RAIN_STARTED, GameStateChangeS2CPacket.DEMO_OPEN_SCREEN));
         player.networkHandler.sendPacket(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.RAIN_GRADIENT_CHANGED, world.getRainGradient(1.0F)));
         player.networkHandler.sendPacket(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.THUNDER_GRADIENT_CHANGED, world.getThunderGradient(1.0F)));
      }

   }

   public void sendPlayerStatus(ServerPlayerEntity player) {
      player.playerScreenHandler.syncState();
      player.markHealthDirty();
      player.networkHandler.sendPacket(new UpdateSelectedSlotS2CPacket(player.getInventory().selectedSlot));
   }

   public int getCurrentPlayerCount() {
      return this.players.size();
   }

   public int getMaxPlayerCount() {
      return this.maxPlayers;
   }

   public boolean isWhitelistEnabled() {
      return this.whitelistEnabled;
   }

   public void setWhitelistEnabled(boolean whitelistEnabled) {
      this.whitelistEnabled = whitelistEnabled;
   }

   public List<ServerPlayerEntity> getPlayersByIp(String ip) {
      List<ServerPlayerEntity> list = Lists.newArrayList();
      Iterator var3 = this.players.iterator();

      while(var3.hasNext()) {
         ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)var3.next();
         if (serverPlayerEntity.getIp().equals(ip)) {
            list.add(serverPlayerEntity);
         }
      }

      return list;
   }

   public int getViewDistance() {
      return this.viewDistance;
   }

   public MinecraftServer getServer() {
      return this.server;
   }

   /**
    * Gets the user data of the player hosting the Minecraft server.
    * 
    * @return the user data of the host of the server if the server is an integrated server, otherwise {@code null}
    */
   public NbtCompound getUserData() {
      return null;
   }

   public void setCheatsAllowed(boolean cheatsAllowed) {
      this.cheatsAllowed = cheatsAllowed;
   }

   public void disconnectAllPlayers() {
      for(int i = 0; i < this.players.size(); ++i) {
         ((ServerPlayerEntity)this.players.get(i)).networkHandler.disconnect(new TranslatableText("multiplayer.disconnect.server_shutdown"));
      }

   }

   public void broadcastChatMessage(Text message, MessageType type, UUID sender) {
      this.server.sendSystemMessage(message, sender);
      Iterator var4 = this.players.iterator();

      while(var4.hasNext()) {
         ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)var4.next();
         serverPlayerEntity.sendMessage(message, type, sender);
      }

   }

   public void broadcast(Text serverMessage, Function<ServerPlayerEntity, Text> playerMessageFactory, MessageType playerMessageType, UUID sender) {
      this.server.sendSystemMessage(serverMessage, sender);
      Iterator var5 = this.players.iterator();

      while(var5.hasNext()) {
         ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)var5.next();
         Text text = (Text)playerMessageFactory.apply(serverPlayerEntity);
         if (text != null) {
            serverPlayerEntity.sendMessage(text, playerMessageType, sender);
         }
      }

   }

   public ServerStatHandler createStatHandler(PlayerEntity player) {
      UUID uUID = player.getUuid();
      ServerStatHandler serverStatHandler = uUID == null ? null : (ServerStatHandler)this.statisticsMap.get(uUID);
      if (serverStatHandler == null) {
         File file = this.server.getSavePath(WorldSavePath.STATS).toFile();
         File file2 = new File(file, uUID + ".json");
         if (!file2.exists()) {
            File file3 = new File(file, player.getName().getString() + ".json");
            if (file3.exists() && file3.isFile()) {
               file3.renameTo(file2);
            }
         }

         serverStatHandler = new ServerStatHandler(this.server, file2);
         this.statisticsMap.put(uUID, serverStatHandler);
      }

      return serverStatHandler;
   }

   public PlayerAdvancementTracker getAdvancementTracker(ServerPlayerEntity player) {
      UUID uUID = player.getUuid();
      PlayerAdvancementTracker playerAdvancementTracker = (PlayerAdvancementTracker)this.advancementTrackers.get(uUID);
      if (playerAdvancementTracker == null) {
         File file = this.server.getSavePath(WorldSavePath.ADVANCEMENTS).toFile();
         File file2 = new File(file, uUID + ".json");
         playerAdvancementTracker = new PlayerAdvancementTracker(this.server.getDataFixer(), this, this.server.getAdvancementLoader(), file2, player);
         this.advancementTrackers.put(uUID, playerAdvancementTracker);
      }

      playerAdvancementTracker.setOwner(player);
      return playerAdvancementTracker;
   }

   public void setViewDistance(int viewDistance) {
      this.viewDistance = viewDistance;
      this.sendToAll(new ChunkLoadDistanceS2CPacket(viewDistance));
      Iterator var2 = this.server.getWorlds().iterator();

      while(var2.hasNext()) {
         ServerWorld serverWorld = (ServerWorld)var2.next();
         if (serverWorld != null) {
            serverWorld.getChunkManager().applyViewDistance(viewDistance);
         }
      }

   }

   /**
    * Gets a list of all players on a Minecraft server.
    * This list should not be modified!
    */
   public List<ServerPlayerEntity> getPlayerList() {
      return this.players;
   }

   @Nullable
   public ServerPlayerEntity getPlayer(UUID uuid) {
      return (ServerPlayerEntity)this.playerMap.get(uuid);
   }

   public boolean canBypassPlayerLimit(GameProfile profile) {
      return false;
   }

   public void onDataPacksReloaded() {
      Iterator var1 = this.advancementTrackers.values().iterator();

      while(var1.hasNext()) {
         PlayerAdvancementTracker playerAdvancementTracker = (PlayerAdvancementTracker)var1.next();
         playerAdvancementTracker.reload(this.server.getAdvancementLoader());
      }

      this.sendToAll(new SynchronizeTagsS2CPacket(this.server.getTagManager().toPacket(this.registryManager)));
      SynchronizeRecipesS2CPacket synchronizeRecipesS2CPacket = new SynchronizeRecipesS2CPacket(this.server.getRecipeManager().values());
      Iterator var5 = this.players.iterator();

      while(var5.hasNext()) {
         ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)var5.next();
         serverPlayerEntity.networkHandler.sendPacket(synchronizeRecipesS2CPacket);
         serverPlayerEntity.getRecipeBook().sendInitRecipesPacket(serverPlayerEntity);
      }

   }

   public boolean areCheatsAllowed() {
      return this.cheatsAllowed;
   }
}
