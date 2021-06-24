package net.minecraft.client.option;

import com.google.common.base.Charsets;
import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.client.tutorial.TutorialStep;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.VideoMode;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.packet.c2s.play.ClientSettingsC2SPacket;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Arm;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.Util;
import net.minecraft.world.Difficulty;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class GameOptions {
   static final Logger LOGGER = LogManager.getLogger();
   private static final Gson GSON = new Gson();
   private static final TypeToken<List<String>> STRING_LIST_TYPE = new TypeToken<List<String>>() {
   };
   public static final int field_32149 = 2;
   public static final int field_32150 = 4;
   public static final int field_32152 = 8;
   public static final int field_32153 = 12;
   public static final int field_32154 = 16;
   public static final int field_32155 = 32;
   private static final Splitter COLON_SPLITTER = Splitter.on(':').limit(2);
   private static final float field_32151 = 1.0F;
   public boolean monochromeLogo;
   public double mouseSensitivity = 0.5D;
   public int viewDistance;
   public float entityDistanceScaling = 1.0F;
   public int maxFps = 120;
   public CloudRenderMode cloudRenderMode;
   public GraphicsMode graphicsMode;
   public AoMode ao;
   public List<String> resourcePacks;
   public List<String> incompatibleResourcePacks;
   public ChatVisibility chatVisibility;
   public double chatOpacity;
   public double chatLineSpacing;
   public double textBackgroundOpacity;
   @Nullable
   public String fullscreenResolution;
   public boolean hideServerAddress;
   public boolean advancedItemTooltips;
   public boolean pauseOnLostFocus;
   private final Set<PlayerModelPart> enabledPlayerModelParts;
   public Arm mainArm;
   public int overrideWidth;
   public int overrideHeight;
   public boolean heldItemTooltips;
   public double chatScale;
   public double chatWidth;
   public double chatHeightUnfocused;
   public double chatHeightFocused;
   public double chatDelay;
   public int mipmapLevels;
   private final Object2FloatMap<SoundCategory> soundVolumeLevels;
   public boolean useNativeTransport;
   public AttackIndicator attackIndicator;
   public TutorialStep tutorialStep;
   public boolean joinedFirstServer;
   public boolean hideBundleTutorial;
   public int biomeBlendRadius;
   public double mouseWheelSensitivity;
   public boolean rawMouseInput;
   public int glDebugVerbosity;
   public boolean autoJump;
   public boolean autoSuggestions;
   public boolean chatColors;
   public boolean chatLinks;
   public boolean chatLinksPrompt;
   public boolean enableVsync;
   public boolean entityShadows;
   public boolean forceUnicodeFont;
   public boolean invertYMouse;
   public boolean discreteMouseScroll;
   public boolean realmsNotifications;
   public boolean reducedDebugInfo;
   public boolean snooperEnabled;
   public boolean showSubtitles;
   public boolean backgroundForChatOnly;
   public boolean touchscreen;
   public boolean fullscreen;
   public boolean bobView;
   public boolean sneakToggled;
   public boolean sprintToggled;
   public boolean skipMultiplayerWarning;
   public boolean hideMatchedNames;
   public final KeyBinding keyForward;
   public final KeyBinding keyLeft;
   public final KeyBinding keyBack;
   public final KeyBinding keyRight;
   public final KeyBinding keyJump;
   public final KeyBinding keySneak;
   public final KeyBinding keySprint;
   public final KeyBinding keyInventory;
   public final KeyBinding keySwapHands;
   public final KeyBinding keyDrop;
   public final KeyBinding keyUse;
   public final KeyBinding keyAttack;
   public final KeyBinding keyPickItem;
   public final KeyBinding keyChat;
   public final KeyBinding keyPlayerList;
   public final KeyBinding keyCommand;
   public final KeyBinding keySocialInteractions;
   public final KeyBinding keyScreenshot;
   public final KeyBinding keyTogglePerspective;
   public final KeyBinding keySmoothCamera;
   public final KeyBinding keyFullscreen;
   public final KeyBinding keySpectatorOutlines;
   public final KeyBinding keyAdvancements;
   public final KeyBinding[] keysHotbar;
   public final KeyBinding keySaveToolbarActivator;
   public final KeyBinding keyLoadToolbarActivator;
   public final KeyBinding[] keysAll;
   protected MinecraftClient client;
   private final File optionsFile;
   public Difficulty difficulty;
   public boolean hudHidden;
   private Perspective perspective;
   public boolean debugEnabled;
   public boolean debugProfilerEnabled;
   public boolean debugTpsEnabled;
   public String lastServer;
   public boolean smoothCameraEnabled;
   public double fov;
   public float distortionEffectScale;
   public float fovEffectScale;
   public double gamma;
   public int guiScale;
   public ParticlesMode particles;
   public NarratorMode narrator;
   public String language;
   public boolean syncChunkWrites;

   public GameOptions(MinecraftClient client, File optionsFile) {
      this.cloudRenderMode = CloudRenderMode.FANCY;
      this.graphicsMode = GraphicsMode.FANCY;
      this.ao = AoMode.MAX;
      this.resourcePacks = Lists.newArrayList();
      this.incompatibleResourcePacks = Lists.newArrayList();
      this.chatVisibility = ChatVisibility.FULL;
      this.chatOpacity = 1.0D;
      this.textBackgroundOpacity = 0.5D;
      this.pauseOnLostFocus = true;
      this.enabledPlayerModelParts = EnumSet.allOf(PlayerModelPart.class);
      this.mainArm = Arm.RIGHT;
      this.heldItemTooltips = true;
      this.chatScale = 1.0D;
      this.chatWidth = 1.0D;
      this.chatHeightUnfocused = 0.44366195797920227D;
      this.chatHeightFocused = 1.0D;
      this.mipmapLevels = 4;
      this.soundVolumeLevels = (Object2FloatMap)Util.make(new Object2FloatOpenHashMap(), (object2FloatOpenHashMap) -> {
         object2FloatOpenHashMap.defaultReturnValue(1.0F);
      });
      this.useNativeTransport = true;
      this.attackIndicator = AttackIndicator.CROSSHAIR;
      this.tutorialStep = TutorialStep.MOVEMENT;
      this.joinedFirstServer = false;
      this.hideBundleTutorial = false;
      this.biomeBlendRadius = 2;
      this.mouseWheelSensitivity = 1.0D;
      this.rawMouseInput = true;
      this.glDebugVerbosity = 1;
      this.autoJump = true;
      this.autoSuggestions = true;
      this.chatColors = true;
      this.chatLinks = true;
      this.chatLinksPrompt = true;
      this.enableVsync = true;
      this.entityShadows = true;
      this.realmsNotifications = true;
      this.snooperEnabled = true;
      this.backgroundForChatOnly = true;
      this.bobView = true;
      this.hideMatchedNames = true;
      this.keyForward = new KeyBinding("key.forward", GLFW.GLFW_KEY_W, "key.categories.movement");
      this.keyLeft = new KeyBinding("key.left", GLFW.GLFW_KEY_A, "key.categories.movement");
      this.keyBack = new KeyBinding("key.back", GLFW.GLFW_KEY_S, "key.categories.movement");
      this.keyRight = new KeyBinding("key.right", GLFW.GLFW_KEY_D, "key.categories.movement");
      this.keyJump = new KeyBinding("key.jump", GLFW.GLFW_KEY_SPACE, "key.categories.movement");
      this.keySneak = new StickyKeyBinding("key.sneak", GLFW.GLFW_KEY_LEFT_SHIFT, "key.categories.movement", () -> {
         return this.sneakToggled;
      });
      this.keySprint = new StickyKeyBinding("key.sprint", GLFW.GLFW_KEY_LEFT_CONTROL, "key.categories.movement", () -> {
         return this.sprintToggled;
      });
      this.keyInventory = new KeyBinding("key.inventory", GLFW.GLFW_KEY_E, "key.categories.inventory");
      this.keySwapHands = new KeyBinding("key.swapOffhand", GLFW.GLFW_KEY_F, "key.categories.inventory");
      this.keyDrop = new KeyBinding("key.drop", GLFW.GLFW_KEY_Q, "key.categories.inventory");
      this.keyUse = new KeyBinding("key.use", InputUtil.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_RIGHT, "key.categories.gameplay");
      this.keyAttack = new KeyBinding("key.attack", InputUtil.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_LEFT, "key.categories.gameplay");
      this.keyPickItem = new KeyBinding("key.pickItem", InputUtil.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_MIDDLE, "key.categories.gameplay");
      this.keyChat = new KeyBinding("key.chat", GLFW.GLFW_KEY_T, "key.categories.multiplayer");
      this.keyPlayerList = new KeyBinding("key.playerlist", GLFW.GLFW_KEY_TAB, "key.categories.multiplayer");
      this.keyCommand = new KeyBinding("key.command", GLFW.GLFW_KEY_SLASH, "key.categories.multiplayer");
      this.keySocialInteractions = new KeyBinding("key.socialInteractions", GLFW.GLFW_KEY_P, "key.categories.multiplayer");
      this.keyScreenshot = new KeyBinding("key.screenshot", GLFW.GLFW_KEY_F2, "key.categories.misc");
      this.keyTogglePerspective = new KeyBinding("key.togglePerspective", GLFW.GLFW_KEY_F5, "key.categories.misc");
      this.keySmoothCamera = new KeyBinding("key.smoothCamera", InputUtil.UNKNOWN_KEY.getCode(), "key.categories.misc");
      this.keyFullscreen = new KeyBinding("key.fullscreen", GLFW.GLFW_KEY_F11, "key.categories.misc");
      this.keySpectatorOutlines = new KeyBinding("key.spectatorOutlines", InputUtil.UNKNOWN_KEY.getCode(), "key.categories.misc");
      this.keyAdvancements = new KeyBinding("key.advancements", GLFW.GLFW_KEY_L, "key.categories.misc");
      this.keysHotbar = new KeyBinding[]{new KeyBinding("key.hotbar.1", GLFW.GLFW_KEY_1, "key.categories.inventory"), new KeyBinding("key.hotbar.2", GLFW.GLFW_KEY_2, "key.categories.inventory"), new KeyBinding("key.hotbar.3", GLFW.GLFW_KEY_3, "key.categories.inventory"), new KeyBinding("key.hotbar.4", GLFW.GLFW_KEY_4, "key.categories.inventory"), new KeyBinding("key.hotbar.5", GLFW.GLFW_KEY_5, "key.categories.inventory"), new KeyBinding("key.hotbar.6", GLFW.GLFW_KEY_6, "key.categories.inventory"), new KeyBinding("key.hotbar.7", GLFW.GLFW_KEY_7, "key.categories.inventory"), new KeyBinding("key.hotbar.8", GLFW.GLFW_KEY_8, "key.categories.inventory"), new KeyBinding("key.hotbar.9", GLFW.GLFW_KEY_9, "key.categories.inventory")};
      this.keySaveToolbarActivator = new KeyBinding("key.saveToolbarActivator", GLFW.GLFW_KEY_C, "key.categories.creative");
      this.keyLoadToolbarActivator = new KeyBinding("key.loadToolbarActivator", GLFW.GLFW_KEY_X, "key.categories.creative");
      this.keysAll = (KeyBinding[])ArrayUtils.addAll((Object[])(new KeyBinding[]{this.keyAttack, this.keyUse, this.keyForward, this.keyLeft, this.keyBack, this.keyRight, this.keyJump, this.keySneak, this.keySprint, this.keyDrop, this.keyInventory, this.keyChat, this.keyPlayerList, this.keyPickItem, this.keyCommand, this.keySocialInteractions, this.keyScreenshot, this.keyTogglePerspective, this.keySmoothCamera, this.keyFullscreen, this.keySpectatorOutlines, this.keySwapHands, this.keySaveToolbarActivator, this.keyLoadToolbarActivator, this.keyAdvancements}), (Object[])this.keysHotbar);
      this.difficulty = Difficulty.NORMAL;
      this.perspective = Perspective.FIRST_PERSON;
      this.lastServer = "";
      this.fov = 70.0D;
      this.distortionEffectScale = 1.0F;
      this.fovEffectScale = 1.0F;
      this.particles = ParticlesMode.ALL;
      this.narrator = NarratorMode.OFF;
      this.language = "en_us";
      this.client = client;
      this.optionsFile = new File(optionsFile, "options.txt");
      if (client.is64Bit() && Runtime.getRuntime().maxMemory() >= 1000000000L) {
         Option.RENDER_DISTANCE.setMax(32.0F);
      } else {
         Option.RENDER_DISTANCE.setMax(16.0F);
      }

      this.viewDistance = client.is64Bit() ? 12 : 8;
      this.syncChunkWrites = Util.getOperatingSystem() == Util.OperatingSystem.WINDOWS;
      this.load();
   }

   public float getTextBackgroundOpacity(float fallback) {
      return this.backgroundForChatOnly ? fallback : (float)this.textBackgroundOpacity;
   }

   public int getTextBackgroundColor(float fallbackOpacity) {
      return (int)(this.getTextBackgroundOpacity(fallbackOpacity) * 255.0F) << 24 & -16777216;
   }

   public int getTextBackgroundColor(int fallbackColor) {
      return this.backgroundForChatOnly ? fallbackColor : (int)(this.textBackgroundOpacity * 255.0D) << 24 & -16777216;
   }

   public void setKeyCode(KeyBinding key, InputUtil.Key code) {
      key.setBoundKey(code);
      this.write();
   }

   private void accept(GameOptions.Visitor visitor) {
      this.autoJump = visitor.visitBoolean("autoJump", this.autoJump);
      this.autoSuggestions = visitor.visitBoolean("autoSuggestions", this.autoSuggestions);
      this.chatColors = visitor.visitBoolean("chatColors", this.chatColors);
      this.chatLinks = visitor.visitBoolean("chatLinks", this.chatLinks);
      this.chatLinksPrompt = visitor.visitBoolean("chatLinksPrompt", this.chatLinksPrompt);
      this.enableVsync = visitor.visitBoolean("enableVsync", this.enableVsync);
      this.entityShadows = visitor.visitBoolean("entityShadows", this.entityShadows);
      this.forceUnicodeFont = visitor.visitBoolean("forceUnicodeFont", this.forceUnicodeFont);
      this.discreteMouseScroll = visitor.visitBoolean("discrete_mouse_scroll", this.discreteMouseScroll);
      this.invertYMouse = visitor.visitBoolean("invertYMouse", this.invertYMouse);
      this.realmsNotifications = visitor.visitBoolean("realmsNotifications", this.realmsNotifications);
      this.reducedDebugInfo = visitor.visitBoolean("reducedDebugInfo", this.reducedDebugInfo);
      this.snooperEnabled = visitor.visitBoolean("snooperEnabled", this.snooperEnabled);
      this.showSubtitles = visitor.visitBoolean("showSubtitles", this.showSubtitles);
      this.touchscreen = visitor.visitBoolean("touchscreen", this.touchscreen);
      this.fullscreen = visitor.visitBoolean("fullscreen", this.fullscreen);
      this.bobView = visitor.visitBoolean("bobView", this.bobView);
      this.sneakToggled = visitor.visitBoolean("toggleCrouch", this.sneakToggled);
      this.sprintToggled = visitor.visitBoolean("toggleSprint", this.sprintToggled);
      this.monochromeLogo = visitor.visitBoolean("darkMojangStudiosBackground", this.monochromeLogo);
      this.mouseSensitivity = visitor.visitDouble("mouseSensitivity", this.mouseSensitivity);
      this.fov = visitor.visitDouble("fov", (this.fov - 70.0D) / 40.0D) * 40.0D + 70.0D;
      this.distortionEffectScale = visitor.visitFloat("screenEffectScale", this.distortionEffectScale);
      this.fovEffectScale = visitor.visitFloat("fovEffectScale", this.fovEffectScale);
      this.gamma = visitor.visitDouble("gamma", this.gamma);
      this.viewDistance = visitor.visitInt("renderDistance", this.viewDistance);
      this.entityDistanceScaling = visitor.visitFloat("entityDistanceScaling", this.entityDistanceScaling);
      this.guiScale = visitor.visitInt("guiScale", this.guiScale);
      this.particles = (ParticlesMode)visitor.visitObject("particles", this.particles, (IntFunction)(ParticlesMode::byId), (ToIntFunction)(ParticlesMode::getId));
      this.maxFps = visitor.visitInt("maxFps", this.maxFps);
      this.difficulty = (Difficulty)visitor.visitObject("difficulty", this.difficulty, (IntFunction)(Difficulty::byOrdinal), (ToIntFunction)(Difficulty::getId));
      this.graphicsMode = (GraphicsMode)visitor.visitObject("graphicsMode", this.graphicsMode, (IntFunction)(GraphicsMode::byId), (ToIntFunction)(GraphicsMode::getId));
      this.ao = (AoMode)visitor.visitObject("ao", this.ao, (Function)(GameOptions::loadAo), (Function)((ao) -> {
         return Integer.toString(ao.getId());
      }));
      this.biomeBlendRadius = visitor.visitInt("biomeBlendRadius", this.biomeBlendRadius);
      this.cloudRenderMode = (CloudRenderMode)visitor.visitObject("renderClouds", this.cloudRenderMode, (Function)(GameOptions::loadCloudRenderMode), (Function)(GameOptions::saveCloudRenderMode));
      List var10003 = this.resourcePacks;
      Function var10004 = GameOptions::parseList;
      Gson var10005 = GSON;
      Objects.requireNonNull(var10005);
      this.resourcePacks = (List)visitor.visitObject("resourcePacks", var10003, (Function)var10004, (Function)(var10005::toJson));
      var10003 = this.incompatibleResourcePacks;
      var10004 = GameOptions::parseList;
      var10005 = GSON;
      Objects.requireNonNull(var10005);
      this.incompatibleResourcePacks = (List)visitor.visitObject("incompatibleResourcePacks", var10003, (Function)var10004, (Function)(var10005::toJson));
      this.lastServer = visitor.visitString("lastServer", this.lastServer);
      this.language = visitor.visitString("lang", this.language);
      this.chatVisibility = (ChatVisibility)visitor.visitObject("chatVisibility", this.chatVisibility, (IntFunction)(ChatVisibility::byId), (ToIntFunction)(ChatVisibility::getId));
      this.chatOpacity = visitor.visitDouble("chatOpacity", this.chatOpacity);
      this.chatLineSpacing = visitor.visitDouble("chatLineSpacing", this.chatLineSpacing);
      this.textBackgroundOpacity = visitor.visitDouble("textBackgroundOpacity", this.textBackgroundOpacity);
      this.backgroundForChatOnly = visitor.visitBoolean("backgroundForChatOnly", this.backgroundForChatOnly);
      this.hideServerAddress = visitor.visitBoolean("hideServerAddress", this.hideServerAddress);
      this.advancedItemTooltips = visitor.visitBoolean("advancedItemTooltips", this.advancedItemTooltips);
      this.pauseOnLostFocus = visitor.visitBoolean("pauseOnLostFocus", this.pauseOnLostFocus);
      this.overrideWidth = visitor.visitInt("overrideWidth", this.overrideWidth);
      this.overrideHeight = visitor.visitInt("overrideHeight", this.overrideHeight);
      this.heldItemTooltips = visitor.visitBoolean("heldItemTooltips", this.heldItemTooltips);
      this.chatHeightFocused = visitor.visitDouble("chatHeightFocused", this.chatHeightFocused);
      this.chatDelay = visitor.visitDouble("chatDelay", this.chatDelay);
      this.chatHeightUnfocused = visitor.visitDouble("chatHeightUnfocused", this.chatHeightUnfocused);
      this.chatScale = visitor.visitDouble("chatScale", this.chatScale);
      this.chatWidth = visitor.visitDouble("chatWidth", this.chatWidth);
      this.mipmapLevels = visitor.visitInt("mipmapLevels", this.mipmapLevels);
      this.useNativeTransport = visitor.visitBoolean("useNativeTransport", this.useNativeTransport);
      this.mainArm = (Arm)visitor.visitObject("mainHand", this.mainArm, (Function)(GameOptions::loadArm), (Function)(GameOptions::saveArm));
      this.attackIndicator = (AttackIndicator)visitor.visitObject("attackIndicator", this.attackIndicator, (IntFunction)(AttackIndicator::byId), (ToIntFunction)(AttackIndicator::getId));
      this.narrator = (NarratorMode)visitor.visitObject("narrator", this.narrator, (IntFunction)(NarratorMode::byId), (ToIntFunction)(NarratorMode::getId));
      this.tutorialStep = (TutorialStep)visitor.visitObject("tutorialStep", this.tutorialStep, (Function)(TutorialStep::byName), (Function)(TutorialStep::getName));
      this.mouseWheelSensitivity = visitor.visitDouble("mouseWheelSensitivity", this.mouseWheelSensitivity);
      this.rawMouseInput = visitor.visitBoolean("rawMouseInput", this.rawMouseInput);
      this.glDebugVerbosity = visitor.visitInt("glDebugVerbosity", this.glDebugVerbosity);
      this.skipMultiplayerWarning = visitor.visitBoolean("skipMultiplayerWarning", this.skipMultiplayerWarning);
      this.hideMatchedNames = visitor.visitBoolean("hideMatchedNames", this.hideMatchedNames);
      this.joinedFirstServer = visitor.visitBoolean("joinedFirstServer", this.joinedFirstServer);
      this.hideBundleTutorial = visitor.visitBoolean("hideBundleTutorial", this.hideBundleTutorial);
      this.syncChunkWrites = visitor.visitBoolean("syncChunkWrites", this.syncChunkWrites);
      KeyBinding[] var2 = this.keysAll;
      int var3 = var2.length;

      int var4;
      for(var4 = 0; var4 < var3; ++var4) {
         KeyBinding keyBinding = var2[var4];
         String string = keyBinding.getBoundKeyTranslationKey();
         String string2 = visitor.visitString("key_" + keyBinding.getTranslationKey(), string);
         if (!string.equals(string2)) {
            keyBinding.setBoundKey(InputUtil.fromTranslationKey(string2));
         }
      }

      SoundCategory[] var8 = SoundCategory.values();
      var3 = var8.length;

      for(var4 = 0; var4 < var3; ++var4) {
         SoundCategory soundCategory = var8[var4];
         this.soundVolumeLevels.computeFloat(soundCategory, (category, currentLevel) -> {
            return visitor.visitFloat("soundCategory_" + category.getName(), currentLevel != null ? currentLevel : 1.0F);
         });
      }

      PlayerModelPart[] var9 = PlayerModelPart.values();
      var3 = var9.length;

      for(var4 = 0; var4 < var3; ++var4) {
         PlayerModelPart playerModelPart = var9[var4];
         boolean bl = this.enabledPlayerModelParts.contains(playerModelPart);
         boolean bl2 = visitor.visitBoolean("modelPart_" + playerModelPart.getName(), bl);
         if (bl2 != bl) {
            this.setPlayerModelPart(playerModelPart, bl2);
         }
      }

   }

   public void load() {
      try {
         if (!this.optionsFile.exists()) {
            return;
         }

         this.soundVolumeLevels.clear();
         NbtCompound nbtCompound = new NbtCompound();
         BufferedReader bufferedReader = Files.newReader(this.optionsFile, Charsets.UTF_8);

         try {
            bufferedReader.lines().forEach((line) -> {
               try {
                  Iterator<String> iterator = COLON_SPLITTER.split(line).iterator();
                  nbtCompound.putString((String)iterator.next(), (String)iterator.next());
               } catch (Exception var3) {
                  LOGGER.warn((String)"Skipping bad option: {}", (Object)line);
               }

            });
         } catch (Throwable var6) {
            if (bufferedReader != null) {
               try {
                  bufferedReader.close();
               } catch (Throwable var5) {
                  var6.addSuppressed(var5);
               }
            }

            throw var6;
         }

         if (bufferedReader != null) {
            bufferedReader.close();
         }

         final NbtCompound nbtCompound2 = this.update(nbtCompound);
         if (!nbtCompound2.contains("graphicsMode") && nbtCompound2.contains("fancyGraphics")) {
            if (isTrue(nbtCompound2.getString("fancyGraphics"))) {
               this.graphicsMode = GraphicsMode.FANCY;
            } else {
               this.graphicsMode = GraphicsMode.FAST;
            }
         }

         this.accept(new GameOptions.Visitor() {
            @Nullable
            private String find(String key) {
               return nbtCompound2.contains(key) ? nbtCompound2.getString(key) : null;
            }

            public int visitInt(String key, int current) {
               String string = this.find(key);
               if (string != null) {
                  try {
                     return Integer.parseInt(string);
                  } catch (NumberFormatException var5) {
                     GameOptions.LOGGER.warn((String)"Invalid integer value for option {} = {}", (Object)key, string, var5);
                  }
               }

               return current;
            }

            public boolean visitBoolean(String key, boolean current) {
               String string = this.find(key);
               return string != null ? GameOptions.isTrue(string) : current;
            }

            public String visitString(String key, String current) {
               return (String)MoreObjects.firstNonNull(this.find(key), current);
            }

            public double visitDouble(String key, double current) {
               String string = this.find(key);
               if (string != null) {
                  if (GameOptions.isTrue(string)) {
                     return 1.0D;
                  }

                  if (GameOptions.isFalse(string)) {
                     return 0.0D;
                  }

                  try {
                     return Double.parseDouble(string);
                  } catch (NumberFormatException var6) {
                     GameOptions.LOGGER.warn((String)"Invalid floating point value for option {} = {}", (Object)key, string, var6);
                  }
               }

               return current;
            }

            public float visitFloat(String key, float current) {
               String string = this.find(key);
               if (string != null) {
                  if (GameOptions.isTrue(string)) {
                     return 1.0F;
                  }

                  if (GameOptions.isFalse(string)) {
                     return 0.0F;
                  }

                  try {
                     return Float.parseFloat(string);
                  } catch (NumberFormatException var5) {
                     GameOptions.LOGGER.warn((String)"Invalid floating point value for option {} = {}", (Object)key, string, var5);
                  }
               }

               return current;
            }

            public <T> T visitObject(String key, T current, Function<String, T> decoder, Function<T, String> encoder) {
               String string = this.find(key);
               return string == null ? current : decoder.apply(string);
            }

            public <T> T visitObject(String key, T current, IntFunction<T> decoder, ToIntFunction<T> encoder) {
               String string = this.find(key);
               if (string != null) {
                  try {
                     return decoder.apply(Integer.parseInt(string));
                  } catch (Exception var7) {
                     GameOptions.LOGGER.warn((String)"Invalid integer value for option {} = {}", (Object)key, string, var7);
                  }
               }

               return current;
            }
         });
         if (nbtCompound2.contains("fullscreenResolution")) {
            this.fullscreenResolution = nbtCompound2.getString("fullscreenResolution");
         }

         if (this.client.getWindow() != null) {
            this.client.getWindow().setFramerateLimit(this.maxFps);
         }

         KeyBinding.updateKeysByCode();
      } catch (Exception var7) {
         LOGGER.error((String)"Failed to load options", (Throwable)var7);
      }

   }

   static boolean isTrue(String value) {
      return "true".equals(value);
   }

   static boolean isFalse(String value) {
      return "false".equals(value);
   }

   private NbtCompound update(NbtCompound nbt) {
      int i = 0;

      try {
         i = Integer.parseInt(nbt.getString("version"));
      } catch (RuntimeException var4) {
      }

      return NbtHelper.update(this.client.getDataFixer(), DataFixTypes.OPTIONS, nbt, i);
   }

   public void write() {
      try {
         final PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(this.optionsFile), StandardCharsets.UTF_8));

         try {
            printWriter.println("version:" + SharedConstants.getGameVersion().getWorldVersion());
            this.accept(new GameOptions.Visitor() {
               public void print(String key) {
                  printWriter.print(key);
                  printWriter.print(':');
               }

               public int visitInt(String key, int current) {
                  this.print(key);
                  printWriter.println(current);
                  return current;
               }

               public boolean visitBoolean(String key, boolean current) {
                  this.print(key);
                  printWriter.println(current);
                  return current;
               }

               public String visitString(String key, String current) {
                  this.print(key);
                  printWriter.println(current);
                  return current;
               }

               public double visitDouble(String key, double current) {
                  this.print(key);
                  printWriter.println(current);
                  return current;
               }

               public float visitFloat(String key, float current) {
                  this.print(key);
                  printWriter.println(current);
                  return current;
               }

               public <T> T visitObject(String key, T current, Function<String, T> decoder, Function<T, String> encoder) {
                  this.print(key);
                  printWriter.println((String)encoder.apply(current));
                  return current;
               }

               public <T> T visitObject(String key, T current, IntFunction<T> decoder, ToIntFunction<T> encoder) {
                  this.print(key);
                  printWriter.println(encoder.applyAsInt(current));
                  return current;
               }
            });
            if (this.client.getWindow().getVideoMode().isPresent()) {
               printWriter.println("fullscreenResolution:" + ((VideoMode)this.client.getWindow().getVideoMode().get()).asString());
            }
         } catch (Throwable var5) {
            try {
               printWriter.close();
            } catch (Throwable var4) {
               var5.addSuppressed(var4);
            }

            throw var5;
         }

         printWriter.close();
      } catch (Exception var6) {
         LOGGER.error((String)"Failed to save options", (Throwable)var6);
      }

      this.sendClientSettings();
   }

   public float getSoundVolume(SoundCategory category) {
      return this.soundVolumeLevels.getFloat(category);
   }

   public void setSoundVolume(SoundCategory category, float volume) {
      this.soundVolumeLevels.put(category, volume);
      this.client.getSoundManager().updateSoundVolume(category, volume);
   }

   /**
    * Sends the current client settings to the server if the client is
    * connected to a server.
    * 
    * <p>Called when a player joins the game or when client settings are
    * changed.
    */
   public void sendClientSettings() {
      if (this.client.player != null) {
         int i = 0;

         PlayerModelPart playerModelPart;
         for(Iterator var2 = this.enabledPlayerModelParts.iterator(); var2.hasNext(); i |= playerModelPart.getBitFlag()) {
            playerModelPart = (PlayerModelPart)var2.next();
         }

         this.client.player.networkHandler.sendPacket(new ClientSettingsC2SPacket(this.language, this.viewDistance, this.chatVisibility, this.chatColors, i, this.mainArm, this.client.shouldFilterText()));
      }

   }

   private void setPlayerModelPart(PlayerModelPart part, boolean enabled) {
      if (enabled) {
         this.enabledPlayerModelParts.add(part);
      } else {
         this.enabledPlayerModelParts.remove(part);
      }

   }

   public boolean isPlayerModelPartEnabled(PlayerModelPart part) {
      return this.enabledPlayerModelParts.contains(part);
   }

   public void togglePlayerModelPart(PlayerModelPart part, boolean enabled) {
      this.setPlayerModelPart(part, enabled);
      this.sendClientSettings();
   }

   public CloudRenderMode getCloudRenderMode() {
      return this.viewDistance >= 4 ? this.cloudRenderMode : CloudRenderMode.OFF;
   }

   public boolean shouldUseNativeTransport() {
      return this.useNativeTransport;
   }

   public void addResourcePackProfilesToManager(ResourcePackManager manager) {
      Set<String> set = Sets.newLinkedHashSet();
      Iterator iterator = this.resourcePacks.iterator();

      while(true) {
         while(iterator.hasNext()) {
            String string = (String)iterator.next();
            ResourcePackProfile resourcePackProfile = manager.getProfile(string);
            if (resourcePackProfile == null && !string.startsWith("file/")) {
               resourcePackProfile = manager.getProfile("file/" + string);
            }

            if (resourcePackProfile == null) {
               LOGGER.warn((String)"Removed resource pack {} from options because it doesn't seem to exist anymore", (Object)string);
               iterator.remove();
            } else if (!resourcePackProfile.getCompatibility().isCompatible() && !this.incompatibleResourcePacks.contains(string)) {
               LOGGER.warn((String)"Removed resource pack {} from options because it is no longer compatible", (Object)string);
               iterator.remove();
            } else if (resourcePackProfile.getCompatibility().isCompatible() && this.incompatibleResourcePacks.contains(string)) {
               LOGGER.info((String)"Removed resource pack {} from incompatibility list because it's now compatible", (Object)string);
               this.incompatibleResourcePacks.remove(string);
            } else {
               set.add(resourcePackProfile.getName());
            }
         }

         manager.setEnabledProfiles(set);
         return;
      }
   }

   public Perspective getPerspective() {
      return this.perspective;
   }

   public void setPerspective(Perspective perspective) {
      this.perspective = perspective;
   }

   private static List<String> parseList(String content) {
      List<String> list = (List)JsonHelper.deserialize(GSON, content, STRING_LIST_TYPE);
      return (List)(list != null ? list : Lists.newArrayList());
   }

   private static CloudRenderMode loadCloudRenderMode(String literal) {
      byte var2 = -1;
      switch(literal.hashCode()) {
      case 3135580:
         if (literal.equals("fast")) {
            var2 = 1;
         }
         break;
      case 3569038:
         if (literal.equals("true")) {
            var2 = 0;
         }
         break;
      case 97196323:
         if (literal.equals("false")) {
            var2 = 2;
         }
      }

      switch(var2) {
      case 0:
         return CloudRenderMode.FANCY;
      case 1:
         return CloudRenderMode.FAST;
      case 2:
      default:
         return CloudRenderMode.OFF;
      }
   }

   private static String saveCloudRenderMode(CloudRenderMode mode) {
      switch(mode) {
      case FANCY:
         return "true";
      case FAST:
         return "fast";
      case OFF:
      default:
         return "false";
      }
   }

   private static AoMode loadAo(String value) {
      if (isTrue(value)) {
         return AoMode.MAX;
      } else {
         return isFalse(value) ? AoMode.OFF : AoMode.byId(Integer.parseInt(value));
      }
   }

   private static Arm loadArm(String arm) {
      return "left".equals(arm) ? Arm.LEFT : Arm.RIGHT;
   }

   private static String saveArm(Arm arm) {
      return arm == Arm.LEFT ? "left" : "right";
   }

   public File getOptionsFile() {
      return this.optionsFile;
   }

   public String collectProfiledOptions() {
      ImmutableList<Pair<String, String>> immutableList = ImmutableList.builder().add((Object)Pair.of("ao", String.valueOf(this.ao))).add((Object)Pair.of("biomeBlendRadius", String.valueOf(this.biomeBlendRadius))).add((Object)Pair.of("enableVsync", String.valueOf(this.enableVsync))).add((Object)Pair.of("entityDistanceScaling", String.valueOf(this.entityDistanceScaling))).add((Object)Pair.of("entityShadows", String.valueOf(this.entityShadows))).add((Object)Pair.of("forceUnicodeFont", String.valueOf(this.forceUnicodeFont))).add((Object)Pair.of("fov", String.valueOf(this.fov))).add((Object)Pair.of("fovEffectScale", String.valueOf(this.fovEffectScale))).add((Object)Pair.of("fullscreen", String.valueOf(this.fullscreen))).add((Object)Pair.of("fullscreenResolution", String.valueOf(this.fullscreenResolution))).add((Object)Pair.of("gamma", String.valueOf(this.gamma))).add((Object)Pair.of("glDebugVerbosity", String.valueOf(this.glDebugVerbosity))).add((Object)Pair.of("graphicsMode", String.valueOf(this.graphicsMode))).add((Object)Pair.of("guiScale", String.valueOf(this.guiScale))).add((Object)Pair.of("maxFps", String.valueOf(this.maxFps))).add((Object)Pair.of("mipmapLevels", String.valueOf(this.mipmapLevels))).add((Object)Pair.of("narrator", String.valueOf(this.narrator))).add((Object)Pair.of("overrideHeight", String.valueOf(this.overrideHeight))).add((Object)Pair.of("overrideWidth", String.valueOf(this.overrideWidth))).add((Object)Pair.of("particles", String.valueOf(this.particles))).add((Object)Pair.of("reducedDebugInfo", String.valueOf(this.reducedDebugInfo))).add((Object)Pair.of("renderClouds", String.valueOf(this.cloudRenderMode))).add((Object)Pair.of("renderDistance", String.valueOf(this.viewDistance))).add((Object)Pair.of("resourcePacks", String.valueOf(this.resourcePacks))).add((Object)Pair.of("screenEffectScale", String.valueOf(this.distortionEffectScale))).add((Object)Pair.of("syncChunkWrites", String.valueOf(this.syncChunkWrites))).add((Object)Pair.of("useNativeTransport", String.valueOf(this.useNativeTransport))).build();
      return (String)immutableList.stream().map((option) -> {
         String var10000 = (String)option.getFirst();
         return var10000 + ": " + (String)option.getSecond();
      }).collect(Collectors.joining(System.lineSeparator()));
   }

   @Environment(EnvType.CLIENT)
   private interface Visitor {
      int visitInt(String key, int current);

      boolean visitBoolean(String key, boolean current);

      String visitString(String key, String current);

      double visitDouble(String key, double current);

      float visitFloat(String key, float current);

      <T> T visitObject(String key, T current, Function<String, T> decoder, Function<T, String> encoder);

      <T> T visitObject(String key, T current, IntFunction<T> decoder, ToIntFunction<T> encoder);
   }
}
