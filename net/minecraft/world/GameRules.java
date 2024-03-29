package net.minecraft.world;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.serialization.DynamicLike;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class GameRules {
   public static final int DEFAULT_RANDOM_TICK_SPEED = 3;
   static final Logger LOGGER = LogManager.getLogger();
   private static final Map<GameRules.Key<?>, GameRules.Type<?>> RULE_TYPES = Maps.newTreeMap(Comparator.comparing((key) -> {
      return key.name;
   }));
   public static final GameRules.Key<GameRules.BooleanRule> DO_FIRE_TICK;
   /**
    * A {@linkplain Rule game rule} which regulates whether mobs can modify the world.
    * 
    * <p>Generally one is expected to test this rule before an entity modifies the world.
    * 
    * <p>In vanilla, this includes:
    * <ul>
    * <li>Whether creeper explosions destroy blocks
    * <li>Whether a zombie can break down a door
    * <li>Whether a wither killing an entity will place or drop a wither rose
    * </ul>
    */
   public static final GameRules.Key<GameRules.BooleanRule> DO_MOB_GRIEFING;
   /**
    * A {@linkplain Rule game rule} which regulates whether player inventories should be persist through respawning.
    */
   public static final GameRules.Key<GameRules.BooleanRule> KEEP_INVENTORY;
   /**
    * A {@linkplain Rule game rule} which regulates whether mobs can spawn naturally.
    */
   public static final GameRules.Key<GameRules.BooleanRule> DO_MOB_SPAWNING;
   /**
    * A {@linkplain Rule game rule} which regulates whether mobs should drop loot on death.
    */
   public static final GameRules.Key<GameRules.BooleanRule> DO_MOB_LOOT;
   /**
    * A {@linkplain Rule game rule} which regulates whether blocks should drop their items when broken.
    */
   public static final GameRules.Key<GameRules.BooleanRule> DO_TILE_DROPS;
   public static final GameRules.Key<GameRules.BooleanRule> DO_ENTITY_DROPS;
   public static final GameRules.Key<GameRules.BooleanRule> COMMAND_BLOCK_OUTPUT;
   public static final GameRules.Key<GameRules.BooleanRule> NATURAL_REGENERATION;
   public static final GameRules.Key<GameRules.BooleanRule> DO_DAYLIGHT_CYCLE;
   public static final GameRules.Key<GameRules.BooleanRule> LOG_ADMIN_COMMANDS;
   public static final GameRules.Key<GameRules.BooleanRule> SHOW_DEATH_MESSAGES;
   public static final GameRules.Key<GameRules.IntRule> RANDOM_TICK_SPEED;
   public static final GameRules.Key<GameRules.BooleanRule> SEND_COMMAND_FEEDBACK;
   /**
    * A {@linkplain Rule game rule} which regulates whether clients' {@linkplain net.minecraft.client.gui.hud.DebugHud debug HUD}s show reduced information.
    * 
    * <p>When the value of this rule is changed, all connected clients will be notified to update their display.
    * In vanilla, this includes the visibility of coordinates on the clients' debug HUDs.
    */
   public static final GameRules.Key<GameRules.BooleanRule> REDUCED_DEBUG_INFO;
   public static final GameRules.Key<GameRules.BooleanRule> SPECTATORS_GENERATE_CHUNKS;
   public static final GameRules.Key<GameRules.IntRule> SPAWN_RADIUS;
   public static final GameRules.Key<GameRules.BooleanRule> DISABLE_ELYTRA_MOVEMENT_CHECK;
   /**
    * A {@linkplain Rule game rule} which regulates the number of entities that can be crammed into a block space before they incur cramming damage.
    */
   public static final GameRules.Key<GameRules.IntRule> MAX_ENTITY_CRAMMING;
   public static final GameRules.Key<GameRules.BooleanRule> DO_WEATHER_CYCLE;
   public static final GameRules.Key<GameRules.BooleanRule> DO_LIMITED_CRAFTING;
   public static final GameRules.Key<GameRules.IntRule> MAX_COMMAND_CHAIN_LENGTH;
   /**
    * A {@linkplain Rule game rule} which regulates whether a player's advancements should be announced in chat.
    */
   public static final GameRules.Key<GameRules.BooleanRule> ANNOUNCE_ADVANCEMENTS;
   /**
    * A {@linkplain Rule game rule} which regulates whether raids should occur.
    * 
    * <p>If this rule is set to {@code true} while raids are occurring, the raids will be stopped.
    */
   public static final GameRules.Key<GameRules.BooleanRule> DISABLE_RAIDS;
   public static final GameRules.Key<GameRules.BooleanRule> DO_INSOMNIA;
   /**
    * A {@linkplain Rule game rule} which regulates whether a player should immediately respawn upon death.
    */
   public static final GameRules.Key<GameRules.BooleanRule> DO_IMMEDIATE_RESPAWN;
   public static final GameRules.Key<GameRules.BooleanRule> DROWNING_DAMAGE;
   public static final GameRules.Key<GameRules.BooleanRule> FALL_DAMAGE;
   public static final GameRules.Key<GameRules.BooleanRule> FIRE_DAMAGE;
   public static final GameRules.Key<GameRules.BooleanRule> FREEZE_DAMAGE;
   public static final GameRules.Key<GameRules.BooleanRule> DO_PATROL_SPAWNING;
   public static final GameRules.Key<GameRules.BooleanRule> DO_TRADER_SPAWNING;
   public static final GameRules.Key<GameRules.BooleanRule> FORGIVE_DEAD_PLAYERS;
   public static final GameRules.Key<GameRules.BooleanRule> UNIVERSAL_ANGER;
   public static final GameRules.Key<GameRules.IntRule> PLAYERS_SLEEPING_PERCENTAGE;
   private final Map<GameRules.Key<?>, GameRules.Rule<?>> rules;

   private static <T extends GameRules.Rule<T>> GameRules.Key<T> register(String name, GameRules.Category category, GameRules.Type<T> type) {
      GameRules.Key<T> key = new GameRules.Key(name, category);
      GameRules.Type<?> type2 = (GameRules.Type)RULE_TYPES.put(key, type);
      if (type2 != null) {
         throw new IllegalStateException("Duplicate game rule registration for " + name);
      } else {
         return key;
      }
   }

   public GameRules(DynamicLike<?> dynamicLike) {
      this();
      this.load(dynamicLike);
   }

   public GameRules() {
      this.rules = (Map)RULE_TYPES.entrySet().stream().collect(ImmutableMap.toImmutableMap(Entry::getKey, (e) -> {
         return ((GameRules.Type)e.getValue()).createRule();
      }));
   }

   private GameRules(Map<GameRules.Key<?>, GameRules.Rule<?>> rules) {
      this.rules = rules;
   }

   public <T extends GameRules.Rule<T>> T get(GameRules.Key<T> key) {
      return (GameRules.Rule)this.rules.get(key);
   }

   public NbtCompound toNbt() {
      NbtCompound nbtCompound = new NbtCompound();
      this.rules.forEach((key, rule) -> {
         nbtCompound.putString(key.name, rule.serialize());
      });
      return nbtCompound;
   }

   private void load(DynamicLike<?> dynamicLike) {
      this.rules.forEach((key, rule) -> {
         Optional var10000 = dynamicLike.get(key.name).asString().result();
         Objects.requireNonNull(rule);
         var10000.ifPresent(rule::deserialize);
      });
   }

   public GameRules copy() {
      return new GameRules((Map)this.rules.entrySet().stream().collect(ImmutableMap.toImmutableMap(Entry::getKey, (entry) -> {
         return ((GameRules.Rule)entry.getValue()).copy();
      })));
   }

   /**
    * Make the visitor visit all registered game rules.
    * 
    * <p>The visitation involves calling both {@link Visitor#visit(GameRules.Key, GameRules.Type)} and {@code visitX} for every game rule, where X is the current rule's concrete type such as a boolean.
    */
   public static void accept(GameRules.Visitor visitor) {
      RULE_TYPES.forEach((key, type) -> {
         accept(visitor, key, type);
      });
   }

   private static <T extends GameRules.Rule<T>> void accept(GameRules.Visitor consumer, GameRules.Key<?> key, GameRules.Type<?> type) {
      consumer.visit(key, type);
      type.accept(consumer, key);
   }

   public void setAllValues(GameRules rules, @Nullable MinecraftServer server) {
      rules.rules.keySet().forEach((key) -> {
         this.setValue(key, rules, server);
      });
   }

   private <T extends GameRules.Rule<T>> void setValue(GameRules.Key<T> key, GameRules rules, @Nullable MinecraftServer server) {
      T rule = rules.get(key);
      this.get(key).setValue(rule, server);
   }

   public boolean getBoolean(GameRules.Key<GameRules.BooleanRule> rule) {
      return ((GameRules.BooleanRule)this.get(rule)).get();
   }

   public int getInt(GameRules.Key<GameRules.IntRule> rule) {
      return ((GameRules.IntRule)this.get(rule)).get();
   }

   static {
      DO_FIRE_TICK = register("doFireTick", GameRules.Category.UPDATES, GameRules.BooleanRule.create(true));
      DO_MOB_GRIEFING = register("mobGriefing", GameRules.Category.MOBS, GameRules.BooleanRule.create(true));
      KEEP_INVENTORY = register("keepInventory", GameRules.Category.PLAYER, GameRules.BooleanRule.create(false));
      DO_MOB_SPAWNING = register("doMobSpawning", GameRules.Category.SPAWNING, GameRules.BooleanRule.create(true));
      DO_MOB_LOOT = register("doMobLoot", GameRules.Category.DROPS, GameRules.BooleanRule.create(true));
      DO_TILE_DROPS = register("doTileDrops", GameRules.Category.DROPS, GameRules.BooleanRule.create(true));
      DO_ENTITY_DROPS = register("doEntityDrops", GameRules.Category.DROPS, GameRules.BooleanRule.create(true));
      COMMAND_BLOCK_OUTPUT = register("commandBlockOutput", GameRules.Category.CHAT, GameRules.BooleanRule.create(true));
      NATURAL_REGENERATION = register("naturalRegeneration", GameRules.Category.PLAYER, GameRules.BooleanRule.create(true));
      DO_DAYLIGHT_CYCLE = register("doDaylightCycle", GameRules.Category.UPDATES, GameRules.BooleanRule.create(true));
      LOG_ADMIN_COMMANDS = register("logAdminCommands", GameRules.Category.CHAT, GameRules.BooleanRule.create(true));
      SHOW_DEATH_MESSAGES = register("showDeathMessages", GameRules.Category.CHAT, GameRules.BooleanRule.create(true));
      RANDOM_TICK_SPEED = register("randomTickSpeed", GameRules.Category.UPDATES, GameRules.IntRule.create(3));
      SEND_COMMAND_FEEDBACK = register("sendCommandFeedback", GameRules.Category.CHAT, GameRules.BooleanRule.create(true));
      REDUCED_DEBUG_INFO = register("reducedDebugInfo", GameRules.Category.MISC, GameRules.BooleanRule.create(false, (server, rule) -> {
         byte b = rule.get() ? 22 : 23;
         Iterator var3 = server.getPlayerManager().getPlayerList().iterator();

         while(var3.hasNext()) {
            ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)var3.next();
            serverPlayerEntity.networkHandler.sendPacket(new EntityStatusS2CPacket(serverPlayerEntity, (byte)b));
         }

      }));
      SPECTATORS_GENERATE_CHUNKS = register("spectatorsGenerateChunks", GameRules.Category.PLAYER, GameRules.BooleanRule.create(true));
      SPAWN_RADIUS = register("spawnRadius", GameRules.Category.PLAYER, GameRules.IntRule.create(10));
      DISABLE_ELYTRA_MOVEMENT_CHECK = register("disableElytraMovementCheck", GameRules.Category.PLAYER, GameRules.BooleanRule.create(false));
      MAX_ENTITY_CRAMMING = register("maxEntityCramming", GameRules.Category.MOBS, GameRules.IntRule.create(24));
      DO_WEATHER_CYCLE = register("doWeatherCycle", GameRules.Category.UPDATES, GameRules.BooleanRule.create(true));
      DO_LIMITED_CRAFTING = register("doLimitedCrafting", GameRules.Category.PLAYER, GameRules.BooleanRule.create(false));
      MAX_COMMAND_CHAIN_LENGTH = register("maxCommandChainLength", GameRules.Category.MISC, GameRules.IntRule.create(65536));
      ANNOUNCE_ADVANCEMENTS = register("announceAdvancements", GameRules.Category.CHAT, GameRules.BooleanRule.create(true));
      DISABLE_RAIDS = register("disableRaids", GameRules.Category.MOBS, GameRules.BooleanRule.create(false));
      DO_INSOMNIA = register("doInsomnia", GameRules.Category.SPAWNING, GameRules.BooleanRule.create(true));
      DO_IMMEDIATE_RESPAWN = register("doImmediateRespawn", GameRules.Category.PLAYER, GameRules.BooleanRule.create(false, (server, rule) -> {
         Iterator var2 = server.getPlayerManager().getPlayerList().iterator();

         while(var2.hasNext()) {
            ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)var2.next();
            serverPlayerEntity.networkHandler.sendPacket(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.IMMEDIATE_RESPAWN, rule.get() ? 1.0F : GameStateChangeS2CPacket.DEMO_OPEN_SCREEN));
         }

      }));
      DROWNING_DAMAGE = register("drowningDamage", GameRules.Category.PLAYER, GameRules.BooleanRule.create(true));
      FALL_DAMAGE = register("fallDamage", GameRules.Category.PLAYER, GameRules.BooleanRule.create(true));
      FIRE_DAMAGE = register("fireDamage", GameRules.Category.PLAYER, GameRules.BooleanRule.create(true));
      FREEZE_DAMAGE = register("freezeDamage", GameRules.Category.PLAYER, GameRules.BooleanRule.create(true));
      DO_PATROL_SPAWNING = register("doPatrolSpawning", GameRules.Category.SPAWNING, GameRules.BooleanRule.create(true));
      DO_TRADER_SPAWNING = register("doTraderSpawning", GameRules.Category.SPAWNING, GameRules.BooleanRule.create(true));
      FORGIVE_DEAD_PLAYERS = register("forgiveDeadPlayers", GameRules.Category.MOBS, GameRules.BooleanRule.create(true));
      UNIVERSAL_ANGER = register("universalAnger", GameRules.Category.MOBS, GameRules.BooleanRule.create(false));
      PLAYERS_SLEEPING_PERCENTAGE = register("playersSleepingPercentage", GameRules.Category.PLAYER, GameRules.IntRule.create(100));
   }

   public static final class Key<T extends GameRules.Rule<T>> {
      final String name;
      private final GameRules.Category category;

      public Key(String name, GameRules.Category category) {
         this.name = name;
         this.category = category;
      }

      public String toString() {
         return this.name;
      }

      public boolean equals(Object o) {
         if (this == o) {
            return true;
         } else {
            return o instanceof GameRules.Key && ((GameRules.Key)o).name.equals(this.name);
         }
      }

      public int hashCode() {
         return this.name.hashCode();
      }

      public String getName() {
         return this.name;
      }

      public String getTranslationKey() {
         return "gamerule." + this.name;
      }

      public GameRules.Category getCategory() {
         return this.category;
      }
   }

   public static enum Category {
      PLAYER("gamerule.category.player"),
      MOBS("gamerule.category.mobs"),
      SPAWNING("gamerule.category.spawning"),
      DROPS("gamerule.category.drops"),
      UPDATES("gamerule.category.updates"),
      CHAT("gamerule.category.chat"),
      MISC("gamerule.category.misc");

      private final String category;

      private Category(String category) {
         this.category = category;
      }

      public String getCategory() {
         return this.category;
      }
   }

   public static class Type<T extends GameRules.Rule<T>> {
      private final Supplier<ArgumentType<?>> argumentType;
      private final Function<GameRules.Type<T>, T> ruleFactory;
      final BiConsumer<MinecraftServer, T> changeCallback;
      private final GameRules.Acceptor<T> ruleAcceptor;

      Type(Supplier<ArgumentType<?>> argumentType, Function<GameRules.Type<T>, T> ruleFactory, BiConsumer<MinecraftServer, T> changeCallback, GameRules.Acceptor<T> ruleAcceptor) {
         this.argumentType = argumentType;
         this.ruleFactory = ruleFactory;
         this.changeCallback = changeCallback;
         this.ruleAcceptor = ruleAcceptor;
      }

      public RequiredArgumentBuilder<ServerCommandSource, ?> argument(String name) {
         return CommandManager.argument(name, (ArgumentType)this.argumentType.get());
      }

      public T createRule() {
         return (GameRules.Rule)this.ruleFactory.apply(this);
      }

      public void accept(GameRules.Visitor consumer, GameRules.Key<T> key) {
         this.ruleAcceptor.call(consumer, key, this);
      }
   }

   public abstract static class Rule<T extends GameRules.Rule<T>> {
      protected final GameRules.Type<T> type;

      public Rule(GameRules.Type<T> type) {
         this.type = type;
      }

      protected abstract void setFromArgument(CommandContext<ServerCommandSource> context, String name);

      public void set(CommandContext<ServerCommandSource> context, String name) {
         this.setFromArgument(context, name);
         this.changed(((ServerCommandSource)context.getSource()).getMinecraftServer());
      }

      protected void changed(@Nullable MinecraftServer server) {
         if (server != null) {
            this.type.changeCallback.accept(server, this.getThis());
         }

      }

      protected abstract void deserialize(String value);

      public abstract String serialize();

      public String toString() {
         return this.serialize();
      }

      public abstract int getCommandResult();

      protected abstract T getThis();

      protected abstract T copy();

      public abstract void setValue(T rule, @Nullable MinecraftServer server);
   }

   /**
    * A visitor used to visit all game rules.
    */
   public interface Visitor {
      /**
       * Visit a game rule.
       * 
       * <p>It is expected all game rules regardless of type will be visited using this method.
       */
      default <T extends GameRules.Rule<T>> void visit(GameRules.Key<T> key, GameRules.Type<T> type) {
      }

      /**
       * Visit a boolean rule.
       * 
       * <p>Note {@link #visit(GameRules.Key, GameRules.Type)} will be called before this method.
       */
      default void visitBoolean(GameRules.Key<GameRules.BooleanRule> key, GameRules.Type<GameRules.BooleanRule> type) {
      }

      /**
       * Visit an integer rule.
       * 
       * <p>Note {@link #visit(GameRules.Key, GameRules.Type)} will be called before this method.
       */
      default void visitInt(GameRules.Key<GameRules.IntRule> key, GameRules.Type<GameRules.IntRule> type) {
      }
   }

   public static class BooleanRule extends GameRules.Rule<GameRules.BooleanRule> {
      private boolean value;

      static GameRules.Type<GameRules.BooleanRule> create(boolean initialValue, BiConsumer<MinecraftServer, GameRules.BooleanRule> changeCallback) {
         return new GameRules.Type(BoolArgumentType::bool, (type) -> {
            return new GameRules.BooleanRule(type, initialValue);
         }, changeCallback, GameRules.Visitor::visitBoolean);
      }

      static GameRules.Type<GameRules.BooleanRule> create(boolean initialValue) {
         return create(initialValue, (server, rule) -> {
         });
      }

      public BooleanRule(GameRules.Type<GameRules.BooleanRule> type, boolean initialValue) {
         super(type);
         this.value = initialValue;
      }

      protected void setFromArgument(CommandContext<ServerCommandSource> context, String name) {
         this.value = BoolArgumentType.getBool(context, name);
      }

      public boolean get() {
         return this.value;
      }

      public void set(boolean value, @Nullable MinecraftServer server) {
         this.value = value;
         this.changed(server);
      }

      public String serialize() {
         return Boolean.toString(this.value);
      }

      protected void deserialize(String value) {
         this.value = Boolean.parseBoolean(value);
      }

      public int getCommandResult() {
         return this.value ? 1 : 0;
      }

      protected GameRules.BooleanRule getThis() {
         return this;
      }

      protected GameRules.BooleanRule copy() {
         return new GameRules.BooleanRule(this.type, this.value);
      }

      public void setValue(GameRules.BooleanRule booleanRule, @Nullable MinecraftServer minecraftServer) {
         this.value = booleanRule.value;
         this.changed(minecraftServer);
      }
   }

   public static class IntRule extends GameRules.Rule<GameRules.IntRule> {
      private int value;

      private static GameRules.Type<GameRules.IntRule> create(int initialValue, BiConsumer<MinecraftServer, GameRules.IntRule> changeCallback) {
         return new GameRules.Type(IntegerArgumentType::integer, (type) -> {
            return new GameRules.IntRule(type, initialValue);
         }, changeCallback, GameRules.Visitor::visitInt);
      }

      static GameRules.Type<GameRules.IntRule> create(int initialValue) {
         return create(initialValue, (server, rule) -> {
         });
      }

      public IntRule(GameRules.Type<GameRules.IntRule> rule, int initialValue) {
         super(rule);
         this.value = initialValue;
      }

      protected void setFromArgument(CommandContext<ServerCommandSource> context, String name) {
         this.value = IntegerArgumentType.getInteger(context, name);
      }

      public int get() {
         return this.value;
      }

      public void set(int value, @Nullable MinecraftServer server) {
         this.value = value;
         this.changed(server);
      }

      public String serialize() {
         return Integer.toString(this.value);
      }

      protected void deserialize(String value) {
         this.value = parseInt(value);
      }

      /**
       * Validates that an input is valid for this rule.
       */
      public boolean validate(String input) {
         try {
            this.value = Integer.parseInt(input);
            return true;
         } catch (NumberFormatException var3) {
            return false;
         }
      }

      private static int parseInt(String input) {
         if (!input.isEmpty()) {
            try {
               return Integer.parseInt(input);
            } catch (NumberFormatException var2) {
               GameRules.LOGGER.warn((String)"Failed to parse integer {}", (Object)input);
            }
         }

         return 0;
      }

      public int getCommandResult() {
         return this.value;
      }

      protected GameRules.IntRule getThis() {
         return this;
      }

      protected GameRules.IntRule copy() {
         return new GameRules.IntRule(this.type, this.value);
      }

      public void setValue(GameRules.IntRule intRule, @Nullable MinecraftServer minecraftServer) {
         this.value = intRule.value;
         this.changed(minecraftServer);
      }
   }

   private interface Acceptor<T extends GameRules.Rule<T>> {
      void call(GameRules.Visitor consumer, GameRules.Key<T> key, GameRules.Type<T> type);
   }
}
