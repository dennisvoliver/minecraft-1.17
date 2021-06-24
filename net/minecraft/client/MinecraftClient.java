package net.minecraft.client;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Queues;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.hash.Hashing;
import com.google.gson.JsonElement;
import com.mojang.authlib.AuthenticationService;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.minecraft.OfflineSocialInteractions;
import com.mojang.authlib.minecraft.SocialInteractionsService;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.blaze3d.platform.GlDebugInfo;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Function4;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.Proxy;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.class_6412;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.font.FontManager;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GlDebug;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.WindowFramebuffer;
import net.minecraft.client.gui.WorldGenerationProgressTracker;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.BackupPromptScreen;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.CreditsScreen;
import net.minecraft.client.gui.screen.DatapackFailureScreen;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.gui.screen.OutOfMemoryScreen;
import net.minecraft.client.gui.screen.Overlay;
import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.client.gui.screen.SaveLevelScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.screen.SleepingChatScreen;
import net.minecraft.client.gui.screen.SplashScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.advancement.AdvancementsScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.multiplayer.SocialInteractionsScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.client.gui.screen.world.EditWorldScreen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.network.SocialInteractionsManager;
import net.minecraft.client.option.AoMode;
import net.minecraft.client.option.ChatVisibility;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.HotbarStorage;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Option;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.BlockModels;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.debug.DebugRenderer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderers;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.resource.ClientBuiltinResourcePackProvider;
import net.minecraft.client.resource.FoliageColormapResourceSupplier;
import net.minecraft.client.resource.Format3ResourcePack;
import net.minecraft.client.resource.Format4ResourcePack;
import net.minecraft.client.resource.GrassColormapResourceSupplier;
import net.minecraft.client.resource.ResourceReloadLogger;
import net.minecraft.client.resource.SplashTextResourceSupplier;
import net.minecraft.client.resource.VideoWarningManager;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.resource.language.LanguageManager;
import net.minecraft.client.search.IdentifierSearchableContainer;
import net.minecraft.client.search.SearchManager;
import net.minecraft.client.search.SearchableContainer;
import net.minecraft.client.search.TextSearchableContainer;
import net.minecraft.client.sound.MusicTracker;
import net.minecraft.client.sound.MusicType;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.texture.PaintingManager;
import net.minecraft.client.texture.PlayerSkinProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.StatusEffectSpriteManager;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.client.toast.TutorialToast;
import net.minecraft.client.tutorial.TutorialManager;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.client.util.Session;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.WindowProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.profiler.DebugRecorder;
import net.minecraft.client.util.profiler.DummyRecorder;
import net.minecraft.client.util.profiler.ProfilerDumper;
import net.minecraft.client.util.profiler.Recorder;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.datafixer.Schemas;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.Durations;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SkullItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkState;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.resource.DataPackSettings;
import net.minecraft.resource.FileResourcePackProvider;
import net.minecraft.resource.ReloadableResourceManager;
import net.minecraft.resource.ReloadableResourceManagerImpl;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourcePackProvider;
import net.minecraft.resource.ResourcePackSource;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.ServerResourceManager;
import net.minecraft.resource.VanillaDataPackProvider;
import net.minecraft.resource.metadata.PackResourceMetadata;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.QueueingWorldGenerationProgressListener;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.sound.MusicSound;
import net.minecraft.tag.ItemTags;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.KeybindText;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.FileNameUtil;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.MetricsData;
import net.minecraft.util.SystemDetails;
import net.minecraft.util.TickDurationMonitor;
import net.minecraft.util.Unit;
import net.minecraft.util.UserCache;
import net.minecraft.util.Util;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.dynamic.RegistryOps;
import net.minecraft.util.dynamic.RegistryReadingOps;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.profiler.DummyProfiler;
import net.minecraft.util.profiler.ProfileResult;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.ProfilerTiming;
import net.minecraft.util.profiler.TickTimeTracker;
import net.minecraft.util.profiler.ZipCompressor;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.snooper.Snooper;
import net.minecraft.util.snooper.SnooperListener;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.LevelProperties;
import net.minecraft.world.level.storage.LevelStorage;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

/**
 * Represents a logical Minecraft client.
 * The logical Minecraft client is responsible for rendering, sound playback and control input.
 * The Minecraft client also manages connections to a logical server which may be the client's {@link net.minecraft.server.integrated.IntegratedServer} or a remote server.
 * The Minecraft client instance may be obtained using {@link MinecraftClient#getInstance()}.
 * 
 * <p>Rendering on a Minecraft client is split into several facilities.
 * The primary entrypoint for rendering is {@link net.minecraft.client.render.GameRenderer#render(float, long, boolean)}.
 * <div class="fabric"><table border=1>
 * <caption>Rendering facilities</caption>
 * <tr>
 *  <th><b>Thing to render</b></th> <th><b>Rendering facility</b></th>
 * </tr>
 * <tr>
 *  <td>World</td> <td>{@link net.minecraft.client.render.WorldRenderer}</td>
 * </tr>
 * <tr>
 *  <td>Blocks and Fluids</td> <td>{@link net.minecraft.client.render.block.BlockRenderManager}</td>
 * </tr>
 * <tr>
 *  <td>Entities</td> <td>{@link net.minecraft.client.render.entity.EntityRenderDispatcher}</td>
 * </tr>
 * <tr>
 *  <td>Block entities</td> <td>{@link net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher}</td>
 * </tr>
 * <tr>
 *  <td>Items</td> <td>{@link net.minecraft.client.render.item.ItemRenderer}</td>
 * </tr>
 * <tr>
 *  <td>Items held in hand</td> <td>{@link net.minecraft.client.render.item.HeldItemRenderer}</td>
 * </tr>
 * <tr>
 *  <td>Text</td> <td>{@link net.minecraft.client.font.TextRenderer}</td>
 * </tr>
 * <tr>
 *  <td>Game hud (health bar, hunger bar)</td> <td>{@link net.minecraft.client.gui.hud.InGameHud}</td>
 * </tr>
 * </table></div>
 * 
 * @see net.minecraft.server.integrated.IntegratedServer
 * @see net.minecraft.client.render.GameRenderer
 */
@Environment(EnvType.CLIENT)
public class MinecraftClient extends ReentrantThreadExecutor<Runnable> implements SnooperListener, WindowEventHandler {
   private static MinecraftClient instance;
   private static final Logger LOGGER = LogManager.getLogger();
   public static final boolean IS_SYSTEM_MAC;
   private static final int field_32145 = 10;
   public static final Identifier DEFAULT_FONT_ID;
   public static final Identifier UNICODE_FONT_ID;
   public static final Identifier ALT_TEXT_RENDERER_ID;
   private static final CompletableFuture<Unit> COMPLETED_UNIT_FUTURE;
   private static final Text SOCIAL_INTERACTIONS_NOT_AVAILABLE;
   /**
    * A message, in English, displayed in a dialog when a GLFW error is encountered.
    * 
    * @see net.minecraft.client.util.Window#throwGlError(int, long)
    */
   public static final String GL_ERROR_DIALOGUE = "Please make sure you have up-to-date drivers (see aka.ms/mcdriver for instructions).";
   private final File resourcePackDir;
   private final PropertyMap sessionPropertyMap;
   private final TextureManager textureManager;
   private final DataFixer dataFixer;
   private final WindowProvider windowProvider;
   private final Window window;
   private final RenderTickCounter renderTickCounter = new RenderTickCounter(20.0F, 0L);
   private final Snooper snooper = new Snooper("client", this, Util.getMeasuringTimeMs());
   private final BufferBuilderStorage bufferBuilders;
   public final WorldRenderer worldRenderer;
   private final EntityRenderDispatcher entityRenderDispatcher;
   private final ItemRenderer itemRenderer;
   private final HeldItemRenderer heldItemRenderer;
   public final ParticleManager particleManager;
   private final SearchManager searchManager = new SearchManager();
   private final Session session;
   public final TextRenderer textRenderer;
   public final GameRenderer gameRenderer;
   public final DebugRenderer debugRenderer;
   private final AtomicReference<WorldGenerationProgressTracker> worldGenProgressTracker = new AtomicReference();
   public final InGameHud inGameHud;
   public final GameOptions options;
   private final HotbarStorage creativeHotbarStorage;
   public final Mouse mouse;
   public final Keyboard keyboard;
   /**
    * The directory that stores options, worlds, resource packs, logs, etc.
    */
   public final File runDirectory;
   private final String gameVersion;
   private final String versionType;
   private final Proxy netProxy;
   private final LevelStorage levelStorage;
   public final MetricsData metricsData = new MetricsData();
   private final boolean is64Bit;
   private final boolean isDemo;
   private final boolean multiplayerEnabled;
   private final boolean onlineChatEnabled;
   private final ReloadableResourceManager resourceManager;
   private final ClientBuiltinResourcePackProvider builtinPackProvider;
   private final ResourcePackManager resourcePackManager;
   private final LanguageManager languageManager;
   private final BlockColors blockColors;
   private final ItemColors itemColors;
   private final Framebuffer framebuffer;
   private final SoundManager soundManager;
   private final MusicTracker musicTracker;
   private final FontManager fontManager;
   private final SplashTextResourceSupplier splashTextLoader;
   private final VideoWarningManager videoWarningManager;
   private final MinecraftSessionService sessionService;
   private final SocialInteractionsService socialInteractionsService;
   private final PlayerSkinProvider skinProvider;
   private final BakedModelManager bakedModelManager;
   private final BlockRenderManager blockRenderManager;
   private final PaintingManager paintingManager;
   private final StatusEffectSpriteManager statusEffectSpriteManager;
   private final ToastManager toastManager;
   private final MinecraftClientGame game = new MinecraftClientGame(this);
   private final TutorialManager tutorialManager;
   private final SocialInteractionsManager socialInteractionsManager;
   private final EntityModelLoader entityModelLoader;
   private final BlockEntityRenderDispatcher blockEntityRenderDispatcher;
   public static byte[] memoryReservedForCrash;
   @Nullable
   public ClientPlayerInteractionManager interactionManager;
   /**
    * Represents the world the client is currently viewing.
    * This field is not null when in game.
    */
   @Nullable
   public ClientWorld world;
   /**
    * Represents the client's own player.
    * This field is not null when in game.
    */
   @Nullable
   public ClientPlayerEntity player;
   @Nullable
   private IntegratedServer server;
   @Nullable
   private ServerInfo currentServerEntry;
   /**
    * The client connection to the integrated server.
    * This is only used when connecting to the integrated server.
    * 
    * @see net.minecraft.client.gui.screen.ConnectScreen
    */
   @Nullable
   private ClientConnection integratedServerConnection;
   private boolean integratedServerRunning;
   @Nullable
   public Entity cameraEntity;
   @Nullable
   public Entity targetedEntity;
   @Nullable
   public HitResult crosshairTarget;
   /**
    * The cooldown for using items when {@linkplain net.minecraft.client.option.GameOptions#keyUse the item use button} is held down.
    */
   private int itemUseCooldown;
   protected int attackCooldown;
   private boolean paused;
   private float pausedTickDelta;
   private long lastMetricsSampleTime = Util.getMeasuringTimeNano();
   private long nextDebugInfoUpdateTime;
   private int fpsCounter;
   public boolean skipGameRender;
   /**
    * The Minecraft client's currently open screen.
    * This field should only be used to get the current screen.
    * For changing the screen use {@link MinecraftClient#openScreen(Screen)}
    * 
    * @see MinecraftClient#openScreen(Screen)
    */
   @Nullable
   public Screen currentScreen;
   @Nullable
   private Overlay overlay;
   private boolean connectedToRealms;
   private Thread thread;
   private volatile boolean running = true;
   @Nullable
   private CrashReport crashReport;
   private static int currentFps;
   public String fpsDebugString = "";
   public boolean wireFrame;
   public boolean debugChunkInfo;
   public boolean debugChunkOcclusion;
   public boolean chunkCullingEnabled = true;
   private boolean windowFocused;
   private final Queue<Runnable> renderTaskQueue = Queues.newConcurrentLinkedQueue();
   @Nullable
   private CompletableFuture<Void> resourceReloadFuture;
   @Nullable
   private TutorialToast socialInteractionsToast;
   private Profiler profiler;
   private int trackingTick;
   private final TickTimeTracker tickTimeTracker;
   @Nullable
   private ProfileResult tickProfilerResult;
   private Recorder debugRecorder;
   private final ResourceReloadLogger resourceReloadLogger;
   private String openProfilerSection;

   public MinecraftClient(RunArgs args) {
      super("Client");
      this.profiler = DummyProfiler.INSTANCE;
      this.tickTimeTracker = new TickTimeTracker(Util.nanoTimeSupplier, () -> {
         return this.trackingTick;
      });
      this.debugRecorder = DummyRecorder.INSTANCE;
      this.resourceReloadLogger = new ResourceReloadLogger();
      this.openProfilerSection = "root";
      instance = this;
      this.runDirectory = args.directories.runDir;
      File file = args.directories.assetDir;
      this.resourcePackDir = args.directories.resourcePackDir;
      this.gameVersion = args.game.version;
      this.versionType = args.game.versionType;
      this.sessionPropertyMap = args.network.profileProperties;
      this.builtinPackProvider = new ClientBuiltinResourcePackProvider(new File(this.runDirectory, "server-resource-packs"), args.directories.getResourceIndex());
      this.resourcePackManager = new ResourcePackManager(MinecraftClient::createResourcePackProfile, new ResourcePackProvider[]{this.builtinPackProvider, new FileResourcePackProvider(this.resourcePackDir, ResourcePackSource.PACK_SOURCE_NONE)});
      this.netProxy = args.network.netProxy;
      YggdrasilAuthenticationService yggdrasilAuthenticationService = new YggdrasilAuthenticationService(this.netProxy);
      this.sessionService = yggdrasilAuthenticationService.createMinecraftSessionService();
      this.socialInteractionsService = this.createSocialInteractionsService(yggdrasilAuthenticationService, args);
      this.session = args.network.session;
      LOGGER.info((String)"Setting user: {}", (Object)this.session.getUsername());
      LOGGER.debug((String)"(Session ID is {})", (Object)this.session.getSessionId());
      this.isDemo = args.game.demo;
      this.multiplayerEnabled = !args.game.multiplayerDisabled;
      this.onlineChatEnabled = !args.game.onlineChatDisabled;
      this.is64Bit = checkIs64Bit();
      this.server = null;
      String string2;
      int j;
      if (this.isMultiplayerEnabled() && args.autoConnect.serverAddress != null) {
         string2 = args.autoConnect.serverAddress;
         j = args.autoConnect.serverPort;
      } else {
         string2 = null;
         j = 0;
      }

      KeybindText.setTranslator(KeyBinding::getLocalizedName);
      this.dataFixer = Schemas.getFixer();
      this.toastManager = new ToastManager(this);
      this.thread = Thread.currentThread();
      this.options = new GameOptions(this, this.runDirectory);
      this.tutorialManager = new TutorialManager(this, this.options);
      this.creativeHotbarStorage = new HotbarStorage(this.runDirectory, this.dataFixer);
      LOGGER.info((String)"Backend library: {}", (Object)RenderSystem.getBackendDescription());
      WindowSettings windowSettings2;
      if (this.options.overrideHeight > 0 && this.options.overrideWidth > 0) {
         windowSettings2 = new WindowSettings(this.options.overrideWidth, this.options.overrideHeight, args.windowSettings.fullscreenWidth, args.windowSettings.fullscreenHeight, args.windowSettings.fullscreen);
      } else {
         windowSettings2 = args.windowSettings;
      }

      Util.nanoTimeSupplier = RenderSystem.initBackendSystem();
      this.windowProvider = new WindowProvider(this);
      this.window = this.windowProvider.createWindow(windowSettings2, this.options.fullscreenResolution, this.getWindowTitle());
      this.onWindowFocusChanged(true);

      try {
         InputStream inputStream = this.getResourcePackProvider().getPack().open(ResourceType.CLIENT_RESOURCES, new Identifier("icons/icon_16x16.png"));
         InputStream inputStream2 = this.getResourcePackProvider().getPack().open(ResourceType.CLIENT_RESOURCES, new Identifier("icons/icon_32x32.png"));
         this.window.setIcon(inputStream, inputStream2);
      } catch (IOException var9) {
         LOGGER.error((String)"Couldn't set icon", (Throwable)var9);
      }

      this.window.setFramerateLimit(this.options.maxFps);
      this.mouse = new Mouse(this);
      this.mouse.setup(this.window.getHandle());
      this.keyboard = new Keyboard(this);
      this.keyboard.setup(this.window.getHandle());
      RenderSystem.initRenderer(this.options.glDebugVerbosity, false);
      this.framebuffer = new WindowFramebuffer(this.window.getFramebufferWidth(), this.window.getFramebufferHeight());
      this.framebuffer.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
      this.framebuffer.clear(IS_SYSTEM_MAC);
      this.resourceManager = new ReloadableResourceManagerImpl(ResourceType.CLIENT_RESOURCES);
      this.resourcePackManager.scanPacks();
      this.options.addResourcePackProfilesToManager(this.resourcePackManager);
      this.languageManager = new LanguageManager(this.options.language);
      this.resourceManager.registerReloader(this.languageManager);
      this.textureManager = new TextureManager(this.resourceManager);
      this.resourceManager.registerReloader(this.textureManager);
      this.skinProvider = new PlayerSkinProvider(this.textureManager, new File(file, "skins"), this.sessionService);
      this.levelStorage = new LevelStorage(this.runDirectory.toPath().resolve("saves"), this.runDirectory.toPath().resolve("backups"), this.dataFixer);
      this.soundManager = new SoundManager(this.resourceManager, this.options);
      this.resourceManager.registerReloader(this.soundManager);
      this.splashTextLoader = new SplashTextResourceSupplier(this.session);
      this.resourceManager.registerReloader(this.splashTextLoader);
      this.musicTracker = new MusicTracker(this);
      this.fontManager = new FontManager(this.textureManager);
      this.textRenderer = this.fontManager.createTextRenderer();
      this.resourceManager.registerReloader(this.fontManager.getResourceReloadListener());
      this.initFont(this.forcesUnicodeFont());
      this.resourceManager.registerReloader(new GrassColormapResourceSupplier());
      this.resourceManager.registerReloader(new FoliageColormapResourceSupplier());
      this.window.setPhase("Startup");
      RenderSystem.setupDefaultState(0, 0, this.window.getFramebufferWidth(), this.window.getFramebufferHeight());
      this.window.setPhase("Post startup");
      this.blockColors = BlockColors.create();
      this.itemColors = ItemColors.create(this.blockColors);
      this.bakedModelManager = new BakedModelManager(this.textureManager, this.blockColors, this.options.mipmapLevels);
      this.resourceManager.registerReloader(this.bakedModelManager);
      this.entityModelLoader = new EntityModelLoader();
      this.resourceManager.registerReloader(this.entityModelLoader);
      this.blockEntityRenderDispatcher = new BlockEntityRenderDispatcher(this.textRenderer, this.entityModelLoader, this::getBlockRenderManager);
      this.resourceManager.registerReloader(this.blockEntityRenderDispatcher);
      BuiltinModelItemRenderer builtinModelItemRenderer = new BuiltinModelItemRenderer(this.blockEntityRenderDispatcher, this.entityModelLoader);
      this.resourceManager.registerReloader(builtinModelItemRenderer);
      this.itemRenderer = new ItemRenderer(this.textureManager, this.bakedModelManager, this.itemColors, builtinModelItemRenderer);
      this.entityRenderDispatcher = new EntityRenderDispatcher(this.textureManager, this.itemRenderer, this.textRenderer, this.options, this.entityModelLoader);
      this.resourceManager.registerReloader(this.entityRenderDispatcher);
      this.heldItemRenderer = new HeldItemRenderer(this);
      this.resourceManager.registerReloader(this.itemRenderer);
      this.bufferBuilders = new BufferBuilderStorage();
      this.gameRenderer = new GameRenderer(this, this.resourceManager, this.bufferBuilders);
      this.resourceManager.registerReloader(this.gameRenderer);
      this.socialInteractionsManager = new SocialInteractionsManager(this, this.socialInteractionsService);
      this.blockRenderManager = new BlockRenderManager(this.bakedModelManager.getBlockModels(), builtinModelItemRenderer, this.blockColors);
      this.resourceManager.registerReloader(this.blockRenderManager);
      this.worldRenderer = new WorldRenderer(this, this.bufferBuilders);
      this.resourceManager.registerReloader(this.worldRenderer);
      this.initializeSearchableContainers();
      this.resourceManager.registerReloader(this.searchManager);
      this.particleManager = new ParticleManager(this.world, this.textureManager);
      this.resourceManager.registerReloader(this.particleManager);
      this.paintingManager = new PaintingManager(this.textureManager);
      this.resourceManager.registerReloader(this.paintingManager);
      this.statusEffectSpriteManager = new StatusEffectSpriteManager(this.textureManager);
      this.resourceManager.registerReloader(this.statusEffectSpriteManager);
      this.videoWarningManager = new VideoWarningManager();
      this.resourceManager.registerReloader(this.videoWarningManager);
      this.inGameHud = new InGameHud(this);
      this.debugRenderer = new DebugRenderer(this);
      RenderSystem.setErrorCallback(this::handleGlErrorByDisableVsync);
      if (this.framebuffer.textureWidth == this.window.getFramebufferWidth() && this.framebuffer.textureHeight == this.window.getFramebufferHeight()) {
         if (this.options.fullscreen && !this.window.isFullscreen()) {
            this.window.toggleFullscreen();
            this.options.fullscreen = this.window.isFullscreen();
         }
      } else {
         int var10002 = this.window.getFramebufferWidth();
         StringBuilder stringBuilder = new StringBuilder("Recovering from unsupported resolution (" + var10002 + "x" + this.window.getFramebufferHeight() + ").\nPlease make sure you have up-to-date drivers (see aka.ms/mcdriver for instructions).");
         if (GlDebug.isDebugMessageEnabled()) {
            stringBuilder.append("\n\nReported GL debug messages:\n").append(String.join("\n", GlDebug.collectDebugMessages()));
         }

         this.window.setWindowedSize(this.framebuffer.textureWidth, this.framebuffer.textureHeight);
         TinyFileDialogs.tinyfd_messageBox((CharSequence)"Minecraft", (CharSequence)stringBuilder.toString(), (CharSequence)"ok", (CharSequence)"error", false);
      }

      this.window.setVsync(this.options.enableVsync);
      this.window.setRawMouseMotion(this.options.rawMouseInput);
      this.window.logOnGlError();
      this.onResolutionChanged();
      if (string2 != null) {
         ConnectScreen.connect(new TitleScreen(), this, new ServerAddress(string2, j), (ServerInfo)null);
      } else {
         this.openScreen(new TitleScreen(true));
      }

      this.gameRenderer.preloadShaders(this.getResourcePackProvider().getPack());
      SplashScreen.init(this);
      List<ResourcePack> list = this.resourcePackManager.createResourcePacks();
      this.resourceReloadLogger.reload(ResourceReloadLogger.ReloadReason.INITIAL, list);
      this.setOverlay(new SplashScreen(this, this.resourceManager.reload(Util.getMainWorkerExecutor(), this, (CompletableFuture)COMPLETED_UNIT_FUTURE, (List)list), (throwable) -> {
         Util.ifPresentOrElse(throwable, this::handleResourceReloadException, () -> {
            if (SharedConstants.isDevelopment) {
               this.checkGameData();
            }

            this.resourceReloadLogger.finish();
         });
      }, false));
   }

   public void updateWindowTitle() {
      this.window.setTitle(this.getWindowTitle());
   }

   private String getWindowTitle() {
      StringBuilder stringBuilder = new StringBuilder("Minecraft");
      if (this.isModded()) {
         stringBuilder.append("*");
      }

      stringBuilder.append(" ");
      stringBuilder.append(SharedConstants.getGameVersion().getName());
      ClientPlayNetworkHandler clientPlayNetworkHandler = this.getNetworkHandler();
      if (clientPlayNetworkHandler != null && clientPlayNetworkHandler.getConnection().isOpen()) {
         stringBuilder.append(" - ");
         if (this.server != null && !this.server.isRemote()) {
            stringBuilder.append(I18n.translate("title.singleplayer"));
         } else if (this.isConnectedToRealms()) {
            stringBuilder.append(I18n.translate("title.multiplayer.realms"));
         } else if (this.server == null && (this.currentServerEntry == null || !this.currentServerEntry.isLocal())) {
            stringBuilder.append(I18n.translate("title.multiplayer.other"));
         } else {
            stringBuilder.append(I18n.translate("title.multiplayer.lan"));
         }
      }

      return stringBuilder.toString();
   }

   private SocialInteractionsService createSocialInteractionsService(YggdrasilAuthenticationService yggdrasilAuthenticationService, RunArgs runArgs) {
      try {
         return yggdrasilAuthenticationService.createSocialInteractionsService(runArgs.network.session.getAccessToken());
      } catch (AuthenticationException var4) {
         LOGGER.error((String)"Failed to verify authentication", (Throwable)var4);
         return new OfflineSocialInteractions();
      }
   }

   /**
    * Checks if this client is modded.
    * 
    * <p>This checks the client's brand and if the MinecraftClient's class is still signed.
    */
   public boolean isModded() {
      return !"vanilla".equals(ClientBrandRetriever.getClientModName()) || MinecraftClient.class.getSigners() == null;
   }

   private void handleResourceReloadException(Throwable exception) {
      if (this.resourcePackManager.getEnabledNames().size() > 1) {
         LiteralText text2;
         if (exception instanceof ReloadableResourceManagerImpl.PackAdditionFailedException) {
            text2 = new LiteralText(((ReloadableResourceManagerImpl.PackAdditionFailedException)exception).getPack().getName());
         } else {
            text2 = null;
         }

         this.onResourceReloadFailure(exception, text2);
      } else {
         Util.throwUnchecked(exception);
      }

   }

   public void onResourceReloadFailure(Throwable exception, @Nullable Text resourceName) {
      LOGGER.info("Caught error loading resourcepacks, removing all selected resourcepacks", exception);
      this.resourceReloadLogger.recover(exception);
      this.resourcePackManager.setEnabledProfiles(Collections.emptyList());
      this.options.resourcePacks.clear();
      this.options.incompatibleResourcePacks.clear();
      this.options.write();
      this.reloadResources(true).thenRun(() -> {
         ToastManager toastManager = this.getToastManager();
         SystemToast.show(toastManager, SystemToast.Type.PACK_LOAD_FAILURE, new TranslatableText("resourcePack.load_fail"), resourceName);
      });
   }

   public void run() {
      this.thread = Thread.currentThread();

      try {
         boolean bl = false;

         while(this.running) {
            if (this.crashReport != null) {
               printCrashReport(this.crashReport);
               return;
            }

            try {
               TickDurationMonitor tickDurationMonitor = TickDurationMonitor.create("Renderer");
               boolean bl2 = this.shouldMonitorTickDuration();
               this.profiler = this.startMonitor(bl2, tickDurationMonitor);
               this.profiler.startTick();
               this.debugRecorder.start();
               this.render(!bl);
               this.debugRecorder.read();
               this.profiler.endTick();
               this.endMonitor(bl2, tickDurationMonitor);
            } catch (OutOfMemoryError var4) {
               if (bl) {
                  throw var4;
               }

               this.cleanUpAfterCrash();
               this.openScreen(new OutOfMemoryScreen());
               System.gc();
               LOGGER.fatal((String)"Out of memory", (Throwable)var4);
               bl = true;
            }
         }
      } catch (CrashException var5) {
         this.addDetailsToCrashReport(var5.getReport());
         this.cleanUpAfterCrash();
         LOGGER.fatal((String)"Reported exception thrown!", (Throwable)var5);
         printCrashReport(var5.getReport());
      } catch (Throwable var6) {
         CrashReport crashReport = this.addDetailsToCrashReport(new CrashReport("Unexpected error", var6));
         LOGGER.fatal("Unreported exception thrown!", var6);
         this.cleanUpAfterCrash();
         printCrashReport(crashReport);
      }

   }

   void initFont(boolean forcesUnicode) {
      this.fontManager.setIdOverrides(forcesUnicode ? ImmutableMap.of(DEFAULT_FONT_ID, UNICODE_FONT_ID) : ImmutableMap.of());
   }

   private void initializeSearchableContainers() {
      TextSearchableContainer<ItemStack> textSearchableContainer = new TextSearchableContainer((itemStack) -> {
         return itemStack.getTooltip((PlayerEntity)null, TooltipContext.Default.NORMAL).stream().map((text) -> {
            return Formatting.strip(text.getString()).trim();
         }).filter((string) -> {
            return !string.isEmpty();
         });
      }, (itemStack) -> {
         return Stream.of(Registry.ITEM.getId(itemStack.getItem()));
      });
      IdentifierSearchableContainer<ItemStack> identifierSearchableContainer = new IdentifierSearchableContainer((itemStack) -> {
         return ItemTags.getTagGroup().getTagsFor(itemStack.getItem()).stream();
      });
      DefaultedList<ItemStack> defaultedList = DefaultedList.of();
      Iterator var4 = Registry.ITEM.iterator();

      while(var4.hasNext()) {
         Item item = (Item)var4.next();
         item.appendStacks(ItemGroup.SEARCH, defaultedList);
      }

      defaultedList.forEach((itemStack) -> {
         textSearchableContainer.add(itemStack);
         identifierSearchableContainer.add(itemStack);
      });
      TextSearchableContainer<RecipeResultCollection> textSearchableContainer2 = new TextSearchableContainer((recipeResultCollection) -> {
         return recipeResultCollection.getAllRecipes().stream().flatMap((recipe) -> {
            return recipe.getOutput().getTooltip((PlayerEntity)null, TooltipContext.Default.NORMAL).stream();
         }).map((text) -> {
            return Formatting.strip(text.getString()).trim();
         }).filter((string) -> {
            return !string.isEmpty();
         });
      }, (recipeResultCollection) -> {
         return recipeResultCollection.getAllRecipes().stream().map((recipe) -> {
            return Registry.ITEM.getId(recipe.getOutput().getItem());
         });
      });
      this.searchManager.put(SearchManager.ITEM_TOOLTIP, textSearchableContainer);
      this.searchManager.put(SearchManager.ITEM_TAG, identifierSearchableContainer);
      this.searchManager.put(SearchManager.RECIPE_OUTPUT, textSearchableContainer2);
   }

   private void handleGlErrorByDisableVsync(int error, long description) {
      this.options.enableVsync = false;
      this.options.write();
   }

   private static boolean checkIs64Bit() {
      String[] strings = new String[]{"sun.arch.data.model", "com.ibm.vm.bitmode", "os.arch"};
      String[] var1 = strings;
      int var2 = strings.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         String string = var1[var3];
         String string2 = System.getProperty(string);
         if (string2 != null && string2.contains("64")) {
            return true;
         }
      }

      return false;
   }

   public Framebuffer getFramebuffer() {
      return this.framebuffer;
   }

   public String getGameVersion() {
      return this.gameVersion;
   }

   public String getVersionType() {
      return this.versionType;
   }

   public void setCrashReport(CrashReport report) {
      this.crashReport = report;
   }

   public static void printCrashReport(CrashReport report) {
      File file = new File(getInstance().runDirectory, "crash-reports");
      SimpleDateFormat var10003 = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
      Date var10004 = new Date();
      File file2 = new File(file, "crash-" + var10003.format(var10004) + "-client.txt");
      Bootstrap.println(report.asString());
      if (report.getFile() != null) {
         Bootstrap.println("#@!@# Game crashed! Crash report saved to: #@!@# " + report.getFile());
         System.exit(-1);
      } else if (report.writeToFile(file2)) {
         Bootstrap.println("#@!@# Game crashed! Crash report saved to: #@!@# " + file2.getAbsolutePath());
         System.exit(-1);
      } else {
         Bootstrap.println("#@?@# Game crashed! Crash report could not be saved. #@?@#");
         System.exit(-2);
      }

   }

   public boolean forcesUnicodeFont() {
      return this.options.forceUnicodeFont;
   }

   public CompletableFuture<Void> reloadResources() {
      return this.reloadResources(false);
   }

   private CompletableFuture<Void> reloadResources(boolean force) {
      if (this.resourceReloadFuture != null) {
         return this.resourceReloadFuture;
      } else {
         CompletableFuture<Void> completableFuture = new CompletableFuture();
         if (!force && this.overlay instanceof SplashScreen) {
            this.resourceReloadFuture = completableFuture;
            return completableFuture;
         } else {
            this.resourcePackManager.scanPacks();
            List<ResourcePack> list = this.resourcePackManager.createResourcePacks();
            if (!force) {
               this.resourceReloadLogger.reload(ResourceReloadLogger.ReloadReason.MANUAL, list);
            }

            this.setOverlay(new SplashScreen(this, this.resourceManager.reload(Util.getMainWorkerExecutor(), this, (CompletableFuture)COMPLETED_UNIT_FUTURE, (List)list), (throwable) -> {
               Util.ifPresentOrElse(throwable, this::handleResourceReloadException, () -> {
                  this.worldRenderer.reload();
                  this.resourceReloadLogger.finish();
                  completableFuture.complete((Object)null);
               });
            }, true));
            return completableFuture;
         }
      }
   }

   private void checkGameData() {
      boolean bl = false;
      BlockModels blockModels = this.getBlockRenderManager().getModels();
      BakedModel bakedModel = blockModels.getModelManager().getMissingModel();
      Iterator var4 = Registry.BLOCK.iterator();

      while(var4.hasNext()) {
         Block block = (Block)var4.next();
         UnmodifiableIterator var6 = block.getStateManager().getStates().iterator();

         while(var6.hasNext()) {
            BlockState blockState = (BlockState)var6.next();
            if (blockState.getRenderType() == BlockRenderType.MODEL) {
               BakedModel bakedModel2 = blockModels.getModel(blockState);
               if (bakedModel2 == bakedModel) {
                  LOGGER.debug((String)"Missing model for: {}", (Object)blockState);
                  bl = true;
               }
            }
         }
      }

      Sprite sprite = bakedModel.getSprite();
      Iterator var13 = Registry.BLOCK.iterator();

      while(var13.hasNext()) {
         Block block2 = (Block)var13.next();
         UnmodifiableIterator var17 = block2.getStateManager().getStates().iterator();

         while(var17.hasNext()) {
            BlockState blockState2 = (BlockState)var17.next();
            Sprite sprite2 = blockModels.getSprite(blockState2);
            if (!blockState2.isAir() && sprite2 == sprite) {
               LOGGER.debug((String)"Missing particle icon for: {}", (Object)blockState2);
               bl = true;
            }
         }
      }

      DefaultedList<ItemStack> defaultedList = DefaultedList.of();
      Iterator var16 = Registry.ITEM.iterator();

      while(var16.hasNext()) {
         Item item = (Item)var16.next();
         defaultedList.clear();
         item.appendStacks(ItemGroup.SEARCH, defaultedList);
         Iterator var20 = defaultedList.iterator();

         while(var20.hasNext()) {
            ItemStack itemStack = (ItemStack)var20.next();
            String string = itemStack.getTranslationKey();
            String string2 = (new TranslatableText(string)).getString();
            if (string2.toLowerCase(Locale.ROOT).equals(item.getTranslationKey())) {
               LOGGER.debug((String)"Missing translation for: {} {} {}", (Object)itemStack, string, itemStack.getItem());
            }
         }
      }

      bl |= HandledScreens.isMissingScreens();
      bl |= EntityRenderers.isMissingRendererFactories();
      if (bl) {
         throw new IllegalStateException("Your game data is foobar, fix the errors above!");
      }
   }

   public LevelStorage getLevelStorage() {
      return this.levelStorage;
   }

   private void openChatScreen(String text) {
      MinecraftClient.ChatRestriction chatRestriction = this.getChatRestriction();
      if (!chatRestriction.allowsChat(this.isInSingleplayer())) {
         this.inGameHud.setOverlayMessage(chatRestriction.getDescription(), false);
      } else {
         this.openScreen(new ChatScreen(text));
      }

   }

   /**
    * Opens a new screen, changing the current screen if needed.
    * 
    * <p>If the screen being opened is {@code null} and the client is not in game, the title screen will be opened.
    * If the currently opened screen is {@code null} and player is dead then the death screen will be opened.
    * Otherwise the currently open screen will be closed.
    */
   public void openScreen(@Nullable Screen screen) {
      if (SharedConstants.isDevelopment && Thread.currentThread() != this.thread) {
         LOGGER.error("setScreen called from non-game thread");
      }

      if (this.currentScreen != null) {
         this.currentScreen.removed();
      }

      if (screen == null && this.world == null) {
         screen = new TitleScreen();
      } else if (screen == null && this.player.isDead()) {
         if (this.player.showsDeathScreen()) {
            screen = new DeathScreen((Text)null, this.world.getLevelProperties().isHardcore());
         } else {
            this.player.requestRespawn();
         }
      }

      this.currentScreen = (Screen)screen;
      BufferRenderer.unbindAll();
      if (screen != null) {
         this.mouse.unlockCursor();
         KeyBinding.unpressAll();
         ((Screen)screen).init(this, this.window.getScaledWidth(), this.window.getScaledHeight());
         this.skipGameRender = false;
      } else {
         this.soundManager.resumeAll();
         this.mouse.lockCursor();
      }

      this.updateWindowTitle();
   }

   public void setOverlay(@Nullable Overlay overlay) {
      this.overlay = overlay;
   }

   public void stop() {
      try {
         LOGGER.info("Stopping!");

         try {
            NarratorManager.INSTANCE.destroy();
         } catch (Throwable var7) {
         }

         try {
            if (this.world != null) {
               this.world.disconnect();
            }

            this.disconnect();
         } catch (Throwable var6) {
         }

         if (this.currentScreen != null) {
            this.currentScreen.removed();
         }

         this.close();
      } finally {
         Util.nanoTimeSupplier = System::nanoTime;
         if (this.crashReport == null) {
            System.exit(0);
         }

      }

   }

   public void close() {
      try {
         this.bakedModelManager.close();
         this.fontManager.close();
         this.gameRenderer.close();
         this.worldRenderer.close();
         this.soundManager.close();
         this.resourcePackManager.close();
         this.particleManager.clearAtlas();
         this.statusEffectSpriteManager.close();
         this.paintingManager.close();
         this.textureManager.close();
         this.resourceManager.close();
         Util.shutdownExecutors();
      } catch (Throwable var5) {
         LOGGER.error("Shutdown failure!", var5);
         throw var5;
      } finally {
         this.windowProvider.close();
         this.window.close();
      }

   }

   private void render(boolean tick) {
      this.window.setPhase("Pre render");
      long l = Util.getMeasuringTimeNano();
      if (this.window.shouldClose()) {
         this.scheduleStop();
      }

      if (this.resourceReloadFuture != null && !(this.overlay instanceof SplashScreen)) {
         CompletableFuture<Void> completableFuture = this.resourceReloadFuture;
         this.resourceReloadFuture = null;
         this.reloadResources().thenRun(() -> {
            completableFuture.complete((Object)null);
         });
      }

      Runnable runnable;
      while((runnable = (Runnable)this.renderTaskQueue.poll()) != null) {
         runnable.run();
      }

      int k;
      if (tick) {
         int i = this.renderTickCounter.beginRenderTick(Util.getMeasuringTimeMs());
         this.profiler.push("scheduledExecutables");
         this.runTasks();
         this.profiler.pop();
         this.profiler.push("tick");

         for(k = 0; k < Math.min(10, i); ++k) {
            this.profiler.visit("clientTick");
            this.tick();
         }

         this.profiler.pop();
      }

      this.mouse.updateMouse();
      this.window.setPhase("Render");
      this.profiler.push("sound");
      this.soundManager.updateListenerPosition(this.gameRenderer.getCamera());
      this.profiler.pop();
      this.profiler.push("render");
      MatrixStack matrixStack = RenderSystem.getModelViewStack();
      matrixStack.push();
      RenderSystem.applyModelViewMatrix();
      RenderSystem.clear(16640, IS_SYSTEM_MAC);
      this.framebuffer.beginWrite(true);
      BackgroundRenderer.method_23792();
      this.profiler.push("display");
      RenderSystem.enableTexture();
      RenderSystem.enableCull();
      this.profiler.pop();
      if (!this.skipGameRender) {
         this.profiler.swap("gameRenderer");
         this.gameRenderer.render(this.paused ? this.pausedTickDelta : this.renderTickCounter.tickDelta, l, tick);
         this.profiler.swap("toasts");
         this.toastManager.draw(new MatrixStack());
         this.profiler.pop();
      }

      if (this.tickProfilerResult != null) {
         this.profiler.push("fpsPie");
         this.drawProfilerResults(new MatrixStack(), this.tickProfilerResult);
         this.profiler.pop();
      }

      this.profiler.push("blit");
      this.framebuffer.endWrite();
      matrixStack.pop();
      matrixStack.push();
      RenderSystem.applyModelViewMatrix();
      this.framebuffer.draw(this.window.getFramebufferWidth(), this.window.getFramebufferHeight());
      matrixStack.pop();
      RenderSystem.applyModelViewMatrix();
      this.profiler.swap("updateDisplay");
      this.window.swapBuffers();
      k = this.getFramerateLimit();
      if ((double)k < Option.FRAMERATE_LIMIT.getMax()) {
         RenderSystem.limitDisplayFPS(k);
      }

      this.profiler.swap("yield");
      Thread.yield();
      this.profiler.pop();
      this.window.setPhase("Post render");
      ++this.fpsCounter;
      boolean bl = this.isIntegratedServerRunning() && (this.currentScreen != null && this.currentScreen.isPauseScreen() || this.overlay != null && this.overlay.pausesGame()) && !this.server.isRemote();
      if (this.paused != bl) {
         if (this.paused) {
            this.pausedTickDelta = this.renderTickCounter.tickDelta;
         } else {
            this.renderTickCounter.tickDelta = this.pausedTickDelta;
         }

         this.paused = bl;
      }

      long m = Util.getMeasuringTimeNano();
      this.metricsData.pushSample(m - this.lastMetricsSampleTime);
      this.lastMetricsSampleTime = m;
      this.profiler.push("fpsUpdate");

      while(Util.getMeasuringTimeMs() >= this.nextDebugInfoUpdateTime + 1000L) {
         currentFps = this.fpsCounter;
         this.fpsDebugString = String.format("%d fps T: %s%s%s%s B: %d", currentFps, (double)this.options.maxFps == Option.FRAMERATE_LIMIT.getMax() ? "inf" : this.options.maxFps, this.options.enableVsync ? " vsync" : "", this.options.graphicsMode.toString(), this.options.cloudRenderMode == CloudRenderMode.OFF ? "" : (this.options.cloudRenderMode == CloudRenderMode.FAST ? " fast-clouds" : " fancy-clouds"), this.options.biomeBlendRadius);
         this.nextDebugInfoUpdateTime += 1000L;
         this.fpsCounter = 0;
         this.snooper.update();
         if (!this.snooper.isActive()) {
            this.snooper.method_5482();
         }
      }

      this.profiler.pop();
   }

   private boolean shouldMonitorTickDuration() {
      return this.options.debugEnabled && this.options.debugProfilerEnabled && !this.options.hudHidden;
   }

   private Profiler startMonitor(boolean active, @Nullable TickDurationMonitor tickDurationMonitor) {
      if (!active && !this.debugRecorder.isActive()) {
         return (Profiler)(tickDurationMonitor == null ? DummyProfiler.INSTANCE : tickDurationMonitor.nextProfiler());
      } else if (active) {
         if (!this.tickTimeTracker.isActive()) {
            this.trackingTick = 0;
            this.tickTimeTracker.enable();
         }

         ++this.trackingTick;
         Profiler profiler = this.debugRecorder.isActive() ? Profiler.union(this.tickTimeTracker.getProfiler(), this.debugRecorder.getProfiler()) : this.tickTimeTracker.getProfiler();
         return TickDurationMonitor.tickProfiler(profiler, tickDurationMonitor);
      } else {
         if (this.tickTimeTracker.isActive()) {
            this.tickTimeTracker.disable();
         }

         return TickDurationMonitor.tickProfiler(this.debugRecorder.getProfiler(), tickDurationMonitor);
      }
   }

   private void endMonitor(boolean active, @Nullable TickDurationMonitor monitor) {
      if (monitor != null) {
         monitor.endTick();
      }

      if (active) {
         this.tickProfilerResult = this.tickTimeTracker.getResult();
      } else {
         this.tickProfilerResult = null;
      }

      this.profiler = this.tickTimeTracker.getProfiler();
   }

   public void onResolutionChanged() {
      int i = this.window.calculateScaleFactor(this.options.guiScale, this.forcesUnicodeFont());
      this.window.setScaleFactor((double)i);
      if (this.currentScreen != null) {
         this.currentScreen.resize(this, this.window.getScaledWidth(), this.window.getScaledHeight());
      }

      Framebuffer framebuffer = this.getFramebuffer();
      framebuffer.resize(this.window.getFramebufferWidth(), this.window.getFramebufferHeight(), IS_SYSTEM_MAC);
      this.gameRenderer.onResized(this.window.getFramebufferWidth(), this.window.getFramebufferHeight());
      this.mouse.onResolutionChanged();
   }

   public void onCursorEnterChanged() {
      this.mouse.setResolutionChanged();
   }

   private int getFramerateLimit() {
      return this.world != null || this.currentScreen == null && this.overlay == null ? this.window.getFramerateLimit() : 60;
   }

   public void cleanUpAfterCrash() {
      try {
         memoryReservedForCrash = new byte[0];
         this.worldRenderer.method_3267();
      } catch (Throwable var3) {
      }

      try {
         System.gc();
         if (this.integratedServerRunning && this.server != null) {
            this.server.stop(true);
         }

         this.disconnect(new SaveLevelScreen(new TranslatableText("menu.savingLevel")));
      } catch (Throwable var2) {
      }

      System.gc();
   }

   public boolean toggleDebugProfiler(Consumer<TranslatableText> consumer) {
      if (this.debugRecorder.isActive()) {
         this.method_37286();
         return false;
      } else {
         Consumer<ProfileResult> consumer2 = (profileResult) -> {
            int i = profileResult.getTickSpan();
            double d = (double)profileResult.getTimeSpan() / (double)Durations.field_33868;
            this.execute(() -> {
               consumer.accept(new TranslatableText("commands.debug.stopped", new Object[]{String.format(Locale.ROOT, "%.2f", d), i, String.format(Locale.ROOT, "%.2f", (double)i / d)}));
            });
         };
         Consumer<Path> consumer3 = (path) -> {
            Text text = (new LiteralText(path.toString())).formatted(Formatting.UNDERLINE).styled((style) -> {
               return style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, path.toFile().getParent()));
            });
            this.execute(() -> {
               consumer.accept(new TranslatableText("debug.profiling.stop", new Object[]{text}));
            });
         };
         SystemDetails systemDetails = addSystemDetailsToCrashReport(new SystemDetails(), this, this.languageManager, this.gameVersion, this.options);
         Consumer<List<Path>> consumer4 = (list) -> {
            Path path = this.method_37275(systemDetails, list);
            consumer3.accept(path);
         };
         Consumer consumer6;
         if (this.server == null) {
            consumer6 = (path) -> {
               consumer4.accept(ImmutableList.of(path));
            };
         } else {
            this.server.method_37324(systemDetails);
            CompletableFuture<Path> completableFuture = new CompletableFuture();
            CompletableFuture<Path> completableFuture2 = new CompletableFuture();
            CompletableFuture.allOf(completableFuture, completableFuture2).thenRunAsync(() -> {
               consumer4.accept(ImmutableList.of((Path)completableFuture.join(), (Path)completableFuture2.join()));
            }, Util.getIoWorkerExecutor());
            IntegratedServer var10000 = this.server;
            Consumer var10001 = (profileResult) -> {
            };
            Objects.requireNonNull(completableFuture2);
            var10000.method_37320(var10001, completableFuture2::complete);
            Objects.requireNonNull(completableFuture);
            consumer6 = completableFuture::complete;
         }

         this.debugRecorder = DebugRecorder.method_37191(new class_6412(Util.nanoTimeSupplier, this.worldRenderer), Util.nanoTimeSupplier, Util.getIoWorkerExecutor(), new ProfilerDumper("client"), (profileResult) -> {
            this.debugRecorder = DummyRecorder.INSTANCE;
            consumer2.accept(profileResult);
         }, consumer6);
         return true;
      }
   }

   private void method_37286() {
      this.debugRecorder.sample();
      if (this.server != null) {
         this.server.method_37323();
      }

   }

   private Path method_37275(SystemDetails systemDetails, List<Path> list) {
      String string2;
      if (this.isInSingleplayer()) {
         string2 = this.getServer().getSaveProperties().getLevelName();
      } else {
         string2 = this.getCurrentServerEntry().name;
      }

      Path path2;
      try {
         String string3 = String.format("%s-%s-%s", (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")).format(new Date()), string2, SharedConstants.getGameVersion().getId());
         String string4 = FileNameUtil.getNextUniqueName(ProfilerDumper.DEBUG_PROFILING_DIRECTORY, string3, ".zip");
         path2 = ProfilerDumper.DEBUG_PROFILING_DIRECTORY.resolve(string4);
      } catch (IOException var23) {
         throw new UncheckedIOException(var23);
      }

      boolean var18 = false;

      try {
         var18 = true;
         ZipCompressor zipCompressor = new ZipCompressor(path2);

         try {
            zipCompressor.write(Paths.get("system.txt"), systemDetails.collect());
            zipCompressor.write(Paths.get("client").resolve(this.options.getOptionsFile().getName()), this.options.collectProfiledOptions());
            Objects.requireNonNull(zipCompressor);
            list.forEach(zipCompressor::copyAll);
         } catch (Throwable var22) {
            try {
               zipCompressor.close();
            } catch (Throwable var19) {
               var22.addSuppressed(var19);
            }

            throw var22;
         }

         zipCompressor.close();
         var18 = false;
      } finally {
         if (var18) {
            Iterator var9 = list.iterator();

            while(var9.hasNext()) {
               Path path4 = (Path)var9.next();

               try {
                  FileUtils.forceDelete(path4.toFile());
               } catch (IOException var20) {
                  LOGGER.warn((String)"Failed to delete temporary profiling result {}", (Object)path4, (Object)var20);
               }
            }

         }
      }

      Iterator var26 = list.iterator();

      while(var26.hasNext()) {
         Path path3 = (Path)var26.next();

         try {
            FileUtils.forceDelete(path3.toFile());
         } catch (IOException var21) {
            LOGGER.warn((String)"Failed to delete temporary profiling result {}", (Object)path3, (Object)var21);
         }
      }

      return path2;
   }

   public void handleProfilerKeyPress(int digit) {
      if (this.tickProfilerResult != null) {
         List<ProfilerTiming> list = this.tickProfilerResult.getTimings(this.openProfilerSection);
         if (!list.isEmpty()) {
            ProfilerTiming profilerTiming = (ProfilerTiming)list.remove(0);
            if (digit == 0) {
               if (!profilerTiming.name.isEmpty()) {
                  int i = this.openProfilerSection.lastIndexOf(30);
                  if (i >= 0) {
                     this.openProfilerSection = this.openProfilerSection.substring(0, i);
                  }
               }
            } else {
               --digit;
               if (digit < list.size() && !"unspecified".equals(((ProfilerTiming)list.get(digit)).name)) {
                  if (!this.openProfilerSection.isEmpty()) {
                     this.openProfilerSection = this.openProfilerSection + "\u001e";
                  }

                  String var10001 = this.openProfilerSection;
                  this.openProfilerSection = var10001 + ((ProfilerTiming)list.get(digit)).name;
               }
            }

         }
      }
   }

   private void drawProfilerResults(MatrixStack matrices, ProfileResult profileResult) {
      List<ProfilerTiming> list = profileResult.getTimings(this.openProfilerSection);
      ProfilerTiming profilerTiming = (ProfilerTiming)list.remove(0);
      RenderSystem.clear(256, IS_SYSTEM_MAC);
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      Matrix4f matrix4f = Matrix4f.projectionMatrix(0.0F, (float)this.window.getFramebufferWidth(), 0.0F, (float)this.window.getFramebufferHeight(), 1000.0F, 3000.0F);
      RenderSystem.setProjectionMatrix(matrix4f);
      MatrixStack matrixStack = RenderSystem.getModelViewStack();
      matrixStack.loadIdentity();
      matrixStack.translate(0.0D, 0.0D, -2000.0D);
      RenderSystem.applyModelViewMatrix();
      RenderSystem.lineWidth(1.0F);
      RenderSystem.disableTexture();
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder bufferBuilder = tessellator.getBuffer();
      int i = true;
      int j = this.window.getFramebufferWidth() - 160 - 10;
      int k = this.window.getFramebufferHeight() - 320;
      RenderSystem.enableBlend();
      bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex((double)((float)j - 176.0F), (double)((float)k - 96.0F - 16.0F), 0.0D).color(200, 0, 0, 0).next();
      bufferBuilder.vertex((double)((float)j - 176.0F), (double)(k + 320), 0.0D).color(200, 0, 0, 0).next();
      bufferBuilder.vertex((double)((float)j + 176.0F), (double)(k + 320), 0.0D).color(200, 0, 0, 0).next();
      bufferBuilder.vertex((double)((float)j + 176.0F), (double)((float)k - 96.0F - 16.0F), 0.0D).color(200, 0, 0, 0).next();
      tessellator.draw();
      RenderSystem.disableBlend();
      double d = 0.0D;

      ProfilerTiming profilerTiming2;
      int v;
      for(Iterator var14 = list.iterator(); var14.hasNext(); d += profilerTiming2.parentSectionUsagePercentage) {
         profilerTiming2 = (ProfilerTiming)var14.next();
         int l = MathHelper.floor(profilerTiming2.parentSectionUsagePercentage / 4.0D) + 1;
         bufferBuilder.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
         v = profilerTiming2.getColor();
         int n = v >> 16 & 255;
         int o = v >> 8 & 255;
         int p = v & 255;
         bufferBuilder.vertex((double)j, (double)k, 0.0D).color(n, o, p, 255).next();

         int r;
         float s;
         float t;
         float u;
         for(r = l; r >= 0; --r) {
            s = (float)((d + profilerTiming2.parentSectionUsagePercentage * (double)r / (double)l) * 6.2831854820251465D / 100.0D);
            t = MathHelper.sin(s) * 160.0F;
            u = MathHelper.cos(s) * 160.0F * 0.5F;
            bufferBuilder.vertex((double)((float)j + t), (double)((float)k - u), 0.0D).color(n, o, p, 255).next();
         }

         tessellator.draw();
         bufferBuilder.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

         for(r = l; r >= 0; --r) {
            s = (float)((d + profilerTiming2.parentSectionUsagePercentage * (double)r / (double)l) * 6.2831854820251465D / 100.0D);
            t = MathHelper.sin(s) * 160.0F;
            u = MathHelper.cos(s) * 160.0F * 0.5F;
            if (!(u > 0.0F)) {
               bufferBuilder.vertex((double)((float)j + t), (double)((float)k - u), 0.0D).color(n >> 1, o >> 1, p >> 1, 255).next();
               bufferBuilder.vertex((double)((float)j + t), (double)((float)k - u + 10.0F), 0.0D).color(n >> 1, o >> 1, p >> 1, 255).next();
            }
         }

         tessellator.draw();
      }

      DecimalFormat decimalFormat = new DecimalFormat("##0.00");
      decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ROOT));
      RenderSystem.enableTexture();
      String string = ProfileResult.getHumanReadableName(profilerTiming.name);
      String string2 = "";
      if (!"unspecified".equals(string)) {
         string2 = string2 + "[0] ";
      }

      if (string.isEmpty()) {
         string2 = string2 + "ROOT ";
      } else {
         string2 = string2 + string + " ";
      }

      v = 16777215;
      this.textRenderer.drawWithShadow(matrices, string2, (float)(j - 160), (float)(k - 80 - 16), 16777215);
      String var10000 = decimalFormat.format(profilerTiming.totalUsagePercentage);
      string2 = var10000 + "%";
      this.textRenderer.drawWithShadow(matrices, string2, (float)(j + 160 - this.textRenderer.getWidth(string2)), (float)(k - 80 - 16), 16777215);

      for(int w = 0; w < list.size(); ++w) {
         ProfilerTiming profilerTiming3 = (ProfilerTiming)list.get(w);
         StringBuilder stringBuilder = new StringBuilder();
         if ("unspecified".equals(profilerTiming3.name)) {
            stringBuilder.append("[?] ");
         } else {
            stringBuilder.append("[").append(w + 1).append("] ");
         }

         String string3 = stringBuilder.append(profilerTiming3.name).toString();
         this.textRenderer.drawWithShadow(matrices, string3, (float)(j - 160), (float)(k + 80 + w * 8 + 20), profilerTiming3.getColor());
         var10000 = decimalFormat.format(profilerTiming3.parentSectionUsagePercentage);
         string3 = var10000 + "%";
         this.textRenderer.drawWithShadow(matrices, string3, (float)(j + 160 - 50 - this.textRenderer.getWidth(string3)), (float)(k + 80 + w * 8 + 20), profilerTiming3.getColor());
         var10000 = decimalFormat.format(profilerTiming3.totalUsagePercentage);
         string3 = var10000 + "%";
         this.textRenderer.drawWithShadow(matrices, string3, (float)(j + 160 - this.textRenderer.getWidth(string3)), (float)(k + 80 + w * 8 + 20), profilerTiming3.getColor());
      }

   }

   public void scheduleStop() {
      this.running = false;
   }

   public boolean isRunning() {
      return this.running;
   }

   public void openPauseMenu(boolean pause) {
      if (this.currentScreen == null) {
         boolean bl = this.isIntegratedServerRunning() && !this.server.isRemote();
         if (bl) {
            this.openScreen(new GameMenuScreen(!pause));
            this.soundManager.pauseAll();
         } else {
            this.openScreen(new GameMenuScreen(true));
         }

      }
   }

   private void handleBlockBreaking(boolean bl) {
      if (!bl) {
         this.attackCooldown = 0;
      }

      if (this.attackCooldown <= 0 && !this.player.isUsingItem()) {
         if (bl && this.crosshairTarget != null && this.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHitResult = (BlockHitResult)this.crosshairTarget;
            BlockPos blockPos = blockHitResult.getBlockPos();
            if (!this.world.getBlockState(blockPos).isAir()) {
               Direction direction = blockHitResult.getSide();
               if (this.interactionManager.updateBlockBreakingProgress(blockPos, direction)) {
                  this.particleManager.addBlockBreakingParticles(blockPos, direction);
                  this.player.swingHand(Hand.MAIN_HAND);
               }
            }

         } else {
            this.interactionManager.cancelBlockBreaking();
         }
      }
   }

   private void doAttack() {
      if (this.attackCooldown <= 0) {
         if (this.crosshairTarget == null) {
            LOGGER.error("Null returned as 'hitResult', this shouldn't happen!");
            if (this.interactionManager.hasLimitedAttackSpeed()) {
               this.attackCooldown = 10;
            }

         } else if (!this.player.isRiding()) {
            switch(this.crosshairTarget.getType()) {
            case ENTITY:
               this.interactionManager.attackEntity(this.player, ((EntityHitResult)this.crosshairTarget).getEntity());
               break;
            case BLOCK:
               BlockHitResult blockHitResult = (BlockHitResult)this.crosshairTarget;
               BlockPos blockPos = blockHitResult.getBlockPos();
               if (!this.world.getBlockState(blockPos).isAir()) {
                  this.interactionManager.attackBlock(blockPos, blockHitResult.getSide());
                  break;
               }
            case MISS:
               if (this.interactionManager.hasLimitedAttackSpeed()) {
                  this.attackCooldown = 10;
               }

               this.player.resetLastAttackedTicks();
            }

            this.player.swingHand(Hand.MAIN_HAND);
         }
      }
   }

   private void doItemUse() {
      if (!this.interactionManager.isBreakingBlock()) {
         this.itemUseCooldown = 4;
         if (!this.player.isRiding()) {
            if (this.crosshairTarget == null) {
               LOGGER.warn("Null returned as 'hitResult', this shouldn't happen!");
            }

            Hand[] var1 = Hand.values();
            int var2 = var1.length;

            for(int var3 = 0; var3 < var2; ++var3) {
               Hand hand = var1[var3];
               ItemStack itemStack = this.player.getStackInHand(hand);
               if (this.crosshairTarget != null) {
                  switch(this.crosshairTarget.getType()) {
                  case ENTITY:
                     EntityHitResult entityHitResult = (EntityHitResult)this.crosshairTarget;
                     Entity entity = entityHitResult.getEntity();
                     ActionResult actionResult = this.interactionManager.interactEntityAtLocation(this.player, entity, entityHitResult, hand);
                     if (!actionResult.isAccepted()) {
                        actionResult = this.interactionManager.interactEntity(this.player, entity, hand);
                     }

                     if (actionResult.isAccepted()) {
                        if (actionResult.shouldSwingHand()) {
                           this.player.swingHand(hand);
                        }

                        return;
                     }
                     break;
                  case BLOCK:
                     BlockHitResult blockHitResult = (BlockHitResult)this.crosshairTarget;
                     int i = itemStack.getCount();
                     ActionResult actionResult2 = this.interactionManager.interactBlock(this.player, this.world, hand, blockHitResult);
                     if (actionResult2.isAccepted()) {
                        if (actionResult2.shouldSwingHand()) {
                           this.player.swingHand(hand);
                           if (!itemStack.isEmpty() && (itemStack.getCount() != i || this.interactionManager.hasCreativeInventory())) {
                              this.gameRenderer.firstPersonRenderer.resetEquipProgress(hand);
                           }
                        }

                        return;
                     }

                     if (actionResult2 == ActionResult.FAIL) {
                        return;
                     }
                  }
               }

               if (!itemStack.isEmpty()) {
                  ActionResult actionResult3 = this.interactionManager.interactItem(this.player, this.world, hand);
                  if (actionResult3.isAccepted()) {
                     if (actionResult3.shouldSwingHand()) {
                        this.player.swingHand(hand);
                     }

                     this.gameRenderer.firstPersonRenderer.resetEquipProgress(hand);
                     return;
                  }
               }
            }

         }
      }
   }

   public MusicTracker getMusicTracker() {
      return this.musicTracker;
   }

   public void tick() {
      if (this.itemUseCooldown > 0) {
         --this.itemUseCooldown;
      }

      this.profiler.push("gui");
      if (!this.paused) {
         this.inGameHud.tick();
      }

      this.profiler.pop();
      this.gameRenderer.updateTargetedEntity(1.0F);
      this.tutorialManager.tick(this.world, this.crosshairTarget);
      this.profiler.push("gameMode");
      if (!this.paused && this.world != null) {
         this.interactionManager.tick();
      }

      this.profiler.swap("textures");
      if (this.world != null) {
         this.textureManager.tick();
      }

      if (this.currentScreen == null && this.player != null) {
         if (this.player.isDead() && !(this.currentScreen instanceof DeathScreen)) {
            this.openScreen((Screen)null);
         } else if (this.player.isSleeping() && this.world != null) {
            this.openScreen(new SleepingChatScreen());
         }
      } else if (this.currentScreen != null && this.currentScreen instanceof SleepingChatScreen && !this.player.isSleeping()) {
         this.openScreen((Screen)null);
      }

      if (this.currentScreen != null) {
         this.attackCooldown = 10000;
      }

      if (this.currentScreen != null) {
         Screen.wrapScreenError(() -> {
            this.currentScreen.tick();
         }, "Ticking screen", this.currentScreen.getClass().getCanonicalName());
      }

      if (!this.options.debugEnabled) {
         this.inGameHud.resetDebugHudChunk();
      }

      if (this.overlay == null && (this.currentScreen == null || this.currentScreen.passEvents)) {
         this.profiler.swap("Keybindings");
         this.handleInputEvents();
         if (this.attackCooldown > 0) {
            --this.attackCooldown;
         }
      }

      if (this.world != null) {
         this.profiler.swap("gameRenderer");
         if (!this.paused) {
            this.gameRenderer.tick();
         }

         this.profiler.swap("levelRenderer");
         if (!this.paused) {
            this.worldRenderer.tick();
         }

         this.profiler.swap("level");
         if (!this.paused) {
            if (this.world.getLightningTicksLeft() > 0) {
               this.world.setLightningTicksLeft(this.world.getLightningTicksLeft() - 1);
            }

            this.world.tickEntities();
         }
      } else if (this.gameRenderer.getShader() != null) {
         this.gameRenderer.disableShader();
      }

      if (!this.paused) {
         this.musicTracker.tick();
      }

      this.soundManager.tick(this.paused);
      if (this.world != null) {
         if (!this.paused) {
            if (!this.options.joinedFirstServer && this.isConnectedToServer()) {
               Text text = new TranslatableText("tutorial.socialInteractions.title");
               Text text2 = new TranslatableText("tutorial.socialInteractions.description", new Object[]{TutorialManager.keyToText("socialInteractions")});
               this.socialInteractionsToast = new TutorialToast(TutorialToast.Type.SOCIAL_INTERACTIONS, text, text2, true);
               this.tutorialManager.add(this.socialInteractionsToast, 160);
               this.options.joinedFirstServer = true;
               this.options.write();
            }

            this.tutorialManager.tick();

            try {
               this.world.tick(() -> {
                  return true;
               });
            } catch (Throwable var4) {
               CrashReport crashReport = CrashReport.create(var4, "Exception in world tick");
               if (this.world == null) {
                  CrashReportSection crashReportSection = crashReport.addElement("Affected level");
                  crashReportSection.add("Problem", (Object)"Level is null!");
               } else {
                  this.world.addDetailsToCrashReport(crashReport);
               }

               throw new CrashException(crashReport);
            }
         }

         this.profiler.swap("animateTick");
         if (!this.paused && this.world != null) {
            this.world.doRandomBlockDisplayTicks(this.player.getBlockX(), this.player.getBlockY(), this.player.getBlockZ());
         }

         this.profiler.swap("particles");
         if (!this.paused) {
            this.particleManager.tick();
         }
      } else if (this.integratedServerConnection != null) {
         this.profiler.swap("pendingConnection");
         this.integratedServerConnection.tick();
      }

      this.profiler.swap("keyboard");
      this.keyboard.pollDebugCrash();
      this.profiler.pop();
   }

   private boolean isConnectedToServer() {
      return !this.integratedServerRunning || this.server != null && this.server.isRemote();
   }

   private void handleInputEvents() {
      for(; this.options.keyTogglePerspective.wasPressed(); this.worldRenderer.scheduleTerrainUpdate()) {
         Perspective perspective = this.options.getPerspective();
         this.options.setPerspective(this.options.getPerspective().next());
         if (perspective.isFirstPerson() != this.options.getPerspective().isFirstPerson()) {
            this.gameRenderer.onCameraEntitySet(this.options.getPerspective().isFirstPerson() ? this.getCameraEntity() : null);
         }
      }

      while(this.options.keySmoothCamera.wasPressed()) {
         this.options.smoothCameraEnabled = !this.options.smoothCameraEnabled;
      }

      for(int i = 0; i < 9; ++i) {
         boolean bl = this.options.keySaveToolbarActivator.isPressed();
         boolean bl2 = this.options.keyLoadToolbarActivator.isPressed();
         if (this.options.keysHotbar[i].wasPressed()) {
            if (this.player.isSpectator()) {
               this.inGameHud.getSpectatorHud().selectSlot(i);
            } else if (!this.player.isCreative() || this.currentScreen != null || !bl2 && !bl) {
               this.player.getInventory().selectedSlot = i;
            } else {
               CreativeInventoryScreen.onHotbarKeyPress(this, i, bl2, bl);
            }
         }
      }

      while(this.options.keySocialInteractions.wasPressed()) {
         if (!this.isConnectedToServer()) {
            this.player.sendMessage(SOCIAL_INTERACTIONS_NOT_AVAILABLE, true);
            NarratorManager.INSTANCE.narrate(SOCIAL_INTERACTIONS_NOT_AVAILABLE);
         } else {
            if (this.socialInteractionsToast != null) {
               this.tutorialManager.remove(this.socialInteractionsToast);
               this.socialInteractionsToast = null;
            }

            this.openScreen(new SocialInteractionsScreen());
         }
      }

      while(this.options.keyInventory.wasPressed()) {
         if (this.interactionManager.hasRidingInventory()) {
            this.player.openRidingInventory();
         } else {
            this.tutorialManager.onInventoryOpened();
            this.openScreen(new InventoryScreen(this.player));
         }
      }

      while(this.options.keyAdvancements.wasPressed()) {
         this.openScreen(new AdvancementsScreen(this.player.networkHandler.getAdvancementHandler()));
      }

      while(this.options.keySwapHands.wasPressed()) {
         if (!this.player.isSpectator()) {
            this.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
         }
      }

      while(this.options.keyDrop.wasPressed()) {
         if (!this.player.isSpectator() && this.player.dropSelectedItem(Screen.hasControlDown())) {
            this.player.swingHand(Hand.MAIN_HAND);
         }
      }

      while(this.options.keyChat.wasPressed()) {
         this.openChatScreen("");
      }

      if (this.currentScreen == null && this.overlay == null && this.options.keyCommand.wasPressed()) {
         this.openChatScreen("/");
      }

      if (this.player.isUsingItem()) {
         if (!this.options.keyUse.isPressed()) {
            this.interactionManager.stopUsingItem(this.player);
         }

         label115:
         while(true) {
            if (!this.options.keyAttack.wasPressed()) {
               while(this.options.keyUse.wasPressed()) {
               }

               while(true) {
                  if (this.options.keyPickItem.wasPressed()) {
                     continue;
                  }
                  break label115;
               }
            }
         }
      } else {
         while(this.options.keyAttack.wasPressed()) {
            this.doAttack();
         }

         while(this.options.keyUse.wasPressed()) {
            this.doItemUse();
         }

         while(this.options.keyPickItem.wasPressed()) {
            this.doItemPick();
         }
      }

      if (this.options.keyUse.isPressed() && this.itemUseCooldown == 0 && !this.player.isUsingItem()) {
         this.doItemUse();
      }

      this.handleBlockBreaking(this.currentScreen == null && this.options.keyAttack.isPressed() && this.mouse.isCursorLocked());
   }

   public static DataPackSettings loadDataPackSettings(LevelStorage.Session storageSession) {
      MinecraftServer.convertLevel(storageSession);
      DataPackSettings dataPackSettings = storageSession.getDataPackSettings();
      if (dataPackSettings == null) {
         throw new IllegalStateException("Failed to load data pack config");
      } else {
         return dataPackSettings;
      }
   }

   public static SaveProperties createSaveProperties(LevelStorage.Session session, DynamicRegistryManager.Impl registryTracker, ResourceManager resourceManager, DataPackSettings dataPackSettings) {
      RegistryOps<NbtElement> registryOps = RegistryOps.method_36574(NbtOps.INSTANCE, resourceManager, registryTracker);
      SaveProperties saveProperties = session.readLevelProperties(registryOps, dataPackSettings);
      if (saveProperties == null) {
         throw new IllegalStateException("Failed to load world");
      } else {
         return saveProperties;
      }
   }

   public void startIntegratedServer(String worldName) {
      this.startIntegratedServer(worldName, DynamicRegistryManager.create(), MinecraftClient::loadDataPackSettings, MinecraftClient::createSaveProperties, false, MinecraftClient.WorldLoadAction.BACKUP);
   }

   public void createWorld(String worldName, LevelInfo levelInfo, DynamicRegistryManager.Impl registryTracker, GeneratorOptions generatorOptions) {
      this.startIntegratedServer(worldName, registryTracker, (session) -> {
         return levelInfo.getDataPackSettings();
      }, (session, registryManager, resourceManager, dataPackSettings) -> {
         RegistryReadingOps<JsonElement> registryReadingOps = RegistryReadingOps.of(JsonOps.INSTANCE, registryTracker);
         RegistryOps<JsonElement> registryOps = RegistryOps.method_36574(JsonOps.INSTANCE, resourceManager, registryTracker);
         DataResult<GeneratorOptions> dataResult = GeneratorOptions.CODEC.encodeStart(registryReadingOps, generatorOptions).setLifecycle(Lifecycle.stable()).flatMap((json) -> {
            return GeneratorOptions.CODEC.parse(registryOps, json);
         });
         Logger var10002 = LOGGER;
         Objects.requireNonNull(var10002);
         GeneratorOptions generatorOptions2 = (GeneratorOptions)dataResult.resultOrPartial(Util.addPrefix("Error reading worldgen settings after loading data packs: ", var10002::error)).orElse(generatorOptions);
         return new LevelProperties(levelInfo, generatorOptions2, dataResult.lifecycle());
      }, false, MinecraftClient.WorldLoadAction.CREATE);
   }

   private void startIntegratedServer(String worldName, DynamicRegistryManager.Impl registryTracker, Function<LevelStorage.Session, DataPackSettings> dataPackSettingsGetter, Function4<LevelStorage.Session, DynamicRegistryManager.Impl, ResourceManager, DataPackSettings, SaveProperties> savePropertiesGetter, boolean safeMode, MinecraftClient.WorldLoadAction worldLoadAction) {
      LevelStorage.Session session2;
      try {
         session2 = this.levelStorage.createSession(worldName);
      } catch (IOException var21) {
         LOGGER.warn((String)"Failed to read level {} data", (Object)worldName, (Object)var21);
         SystemToast.addWorldAccessFailureToast(this, worldName);
         this.openScreen((Screen)null);
         return;
      }

      MinecraftClient.IntegratedResourceManager integratedResourceManager2;
      try {
         integratedResourceManager2 = this.createIntegratedResourceManager(registryTracker, dataPackSettingsGetter, savePropertiesGetter, safeMode, session2);
      } catch (Exception var20) {
         LOGGER.warn((String)"Failed to load datapacks, can't proceed with server load", (Throwable)var20);
         this.openScreen(new DatapackFailureScreen(() -> {
            this.startIntegratedServer(worldName, registryTracker, dataPackSettingsGetter, savePropertiesGetter, true, worldLoadAction);
         }));

         try {
            session2.close();
         } catch (IOException var16) {
            LOGGER.warn((String)"Failed to unlock access to level {}", (Object)worldName, (Object)var16);
         }

         return;
      }

      SaveProperties saveProperties = integratedResourceManager2.getSaveProperties();
      boolean bl = saveProperties.getGeneratorOptions().isLegacyCustomizedType();
      boolean bl2 = saveProperties.getLifecycle() != Lifecycle.stable();
      if (worldLoadAction == MinecraftClient.WorldLoadAction.NONE || !bl && !bl2) {
         this.disconnect();
         this.worldGenProgressTracker.set((Object)null);

         try {
            session2.backupLevelDataFile(registryTracker, saveProperties);
            integratedResourceManager2.getServerResourceManager().loadRegistryTags();
            YggdrasilAuthenticationService yggdrasilAuthenticationService = new YggdrasilAuthenticationService(this.netProxy);
            MinecraftSessionService minecraftSessionService = yggdrasilAuthenticationService.createMinecraftSessionService();
            GameProfileRepository gameProfileRepository = yggdrasilAuthenticationService.createProfileRepository();
            UserCache userCache = new UserCache(gameProfileRepository, new File(this.runDirectory, MinecraftServer.USER_CACHE_FILE.getName()));
            userCache.method_37157(this);
            SkullBlockEntity.setUserCache(userCache);
            SkullBlockEntity.setSessionService(minecraftSessionService);
            UserCache.setUseRemote(false);
            this.server = (IntegratedServer)MinecraftServer.startServer((serverThread) -> {
               return new IntegratedServer(serverThread, this, registryTracker, session2, integratedResourceManager2.getResourcePackManager(), integratedResourceManager2.getServerResourceManager(), saveProperties, minecraftSessionService, gameProfileRepository, userCache, (i) -> {
                  WorldGenerationProgressTracker worldGenerationProgressTracker = new WorldGenerationProgressTracker(i + 0);
                  this.worldGenProgressTracker.set(worldGenerationProgressTracker);
                  Queue var10001 = this.renderTaskQueue;
                  Objects.requireNonNull(var10001);
                  return QueueingWorldGenerationProgressListener.create(worldGenerationProgressTracker, var10001::add);
               });
            });
            this.integratedServerRunning = true;
         } catch (Throwable var19) {
            CrashReport crashReport = CrashReport.create(var19, "Starting integrated server");
            CrashReportSection crashReportSection = crashReport.addElement("Starting integrated server");
            crashReportSection.add("Level ID", (Object)worldName);
            crashReportSection.add("Level Name", (Object)saveProperties.getLevelName());
            throw new CrashException(crashReport);
         }

         while(this.worldGenProgressTracker.get() == null) {
            Thread.yield();
         }

         LevelLoadingScreen levelLoadingScreen = new LevelLoadingScreen((WorldGenerationProgressTracker)this.worldGenProgressTracker.get());
         this.openScreen(levelLoadingScreen);
         this.profiler.push("waitForServer");

         while(!this.server.isLoading()) {
            levelLoadingScreen.tick();
            this.render(false);

            try {
               Thread.sleep(16L);
            } catch (InterruptedException var18) {
            }

            if (this.crashReport != null) {
               printCrashReport(this.crashReport);
               return;
            }
         }

         this.profiler.pop();
         SocketAddress socketAddress = this.server.getNetworkIo().bindLocal();
         ClientConnection clientConnection = ClientConnection.connectLocal(socketAddress);
         clientConnection.setPacketListener(new ClientLoginNetworkHandler(clientConnection, this, (Screen)null, (text) -> {
         }));
         clientConnection.send(new HandshakeC2SPacket(socketAddress.toString(), 0, NetworkState.LOGIN));
         clientConnection.send(new LoginHelloC2SPacket(this.getSession().getProfile()));
         this.integratedServerConnection = clientConnection;
      } else {
         this.showExperimentalWarning(worldLoadAction, worldName, bl, () -> {
            this.startIntegratedServer(worldName, registryTracker, dataPackSettingsGetter, savePropertiesGetter, safeMode, MinecraftClient.WorldLoadAction.NONE);
         });
         integratedResourceManager2.close();

         try {
            session2.close();
         } catch (IOException var17) {
            LOGGER.warn((String)"Failed to unlock access to level {}", (Object)worldName, (Object)var17);
         }

      }
   }

   private void showExperimentalWarning(MinecraftClient.WorldLoadAction worldLoadAction, String levelName, boolean legacyCustomized, Runnable onConfirm) {
      if (worldLoadAction == MinecraftClient.WorldLoadAction.BACKUP) {
         TranslatableText text3;
         TranslatableText text4;
         if (legacyCustomized) {
            text3 = new TranslatableText("selectWorld.backupQuestion.customized");
            text4 = new TranslatableText("selectWorld.backupWarning.customized");
         } else {
            text3 = new TranslatableText("selectWorld.backupQuestion.experimental");
            text4 = new TranslatableText("selectWorld.backupWarning.experimental");
         }

         this.openScreen(new BackupPromptScreen((Screen)null, (shouldBackup, eraseCache) -> {
            if (shouldBackup) {
               EditWorldScreen.onBackupConfirm(this.levelStorage, levelName);
            }

            onConfirm.run();
         }, text3, text4, false));
      } else {
         this.openScreen(new ConfirmScreen((bl) -> {
            if (bl) {
               onConfirm.run();
            } else {
               this.openScreen((Screen)null);

               try {
                  LevelStorage.Session session = this.levelStorage.createSession(levelName);

                  try {
                     session.deleteSessionLock();
                  } catch (Throwable var8) {
                     if (session != null) {
                        try {
                           session.close();
                        } catch (Throwable var7) {
                           var8.addSuppressed(var7);
                        }
                     }

                     throw var8;
                  }

                  if (session != null) {
                     session.close();
                  }
               } catch (IOException var9) {
                  SystemToast.addWorldDeleteFailureToast(this, levelName);
                  LOGGER.error((String)"Failed to delete world {}", (Object)levelName, (Object)var9);
               }
            }

         }, new TranslatableText("selectWorld.backupQuestion.experimental"), new TranslatableText("selectWorld.backupWarning.experimental"), ScreenTexts.PROCEED, ScreenTexts.CANCEL));
      }

   }

   public MinecraftClient.IntegratedResourceManager createIntegratedResourceManager(DynamicRegistryManager.Impl registryManager, Function<LevelStorage.Session, DataPackSettings> dataPackSettingsGetter, Function4<LevelStorage.Session, DynamicRegistryManager.Impl, ResourceManager, DataPackSettings, SaveProperties> savePropertiesGetter, boolean safeMode, LevelStorage.Session storageSession) throws InterruptedException, ExecutionException {
      DataPackSettings dataPackSettings = (DataPackSettings)dataPackSettingsGetter.apply(storageSession);
      ResourcePackManager resourcePackManager = new ResourcePackManager(ResourceType.SERVER_DATA, new ResourcePackProvider[]{new VanillaDataPackProvider(), new FileResourcePackProvider(storageSession.getDirectory(WorldSavePath.DATAPACKS).toFile(), ResourcePackSource.PACK_SOURCE_WORLD)});

      try {
         DataPackSettings dataPackSettings2 = MinecraftServer.loadDataPacks(resourcePackManager, dataPackSettings, safeMode);
         CompletableFuture<ServerResourceManager> completableFuture = ServerResourceManager.reload(resourcePackManager.createResourcePacks(), registryManager, CommandManager.RegistrationEnvironment.INTEGRATED, 2, Util.getMainWorkerExecutor(), this);
         Objects.requireNonNull(completableFuture);
         this.runTasks(completableFuture::isDone);
         ServerResourceManager serverResourceManager = (ServerResourceManager)completableFuture.get();
         SaveProperties saveProperties = (SaveProperties)savePropertiesGetter.apply(storageSession, registryManager, serverResourceManager.getResourceManager(), dataPackSettings2);
         return new MinecraftClient.IntegratedResourceManager(resourcePackManager, serverResourceManager, saveProperties);
      } catch (ExecutionException | InterruptedException var12) {
         resourcePackManager.close();
         throw var12;
      }
   }

   public void joinWorld(ClientWorld world) {
      ProgressScreen progressScreen = new ProgressScreen(true);
      progressScreen.setTitle(new TranslatableText("connect.joining"));
      this.reset(progressScreen);
      this.world = world;
      this.setWorld(world);
      if (!this.integratedServerRunning) {
         AuthenticationService authenticationService = new YggdrasilAuthenticationService(this.netProxy);
         MinecraftSessionService minecraftSessionService = authenticationService.createMinecraftSessionService();
         GameProfileRepository gameProfileRepository = authenticationService.createProfileRepository();
         UserCache userCache = new UserCache(gameProfileRepository, new File(this.runDirectory, MinecraftServer.USER_CACHE_FILE.getName()));
         userCache.method_37157(this);
         SkullBlockEntity.setUserCache(userCache);
         SkullBlockEntity.setSessionService(minecraftSessionService);
         UserCache.setUseRemote(false);
      }

   }

   public void disconnect() {
      this.disconnect(new ProgressScreen(true));
   }

   public void disconnect(Screen screen) {
      ClientPlayNetworkHandler clientPlayNetworkHandler = this.getNetworkHandler();
      if (clientPlayNetworkHandler != null) {
         this.cancelTasks();
         clientPlayNetworkHandler.clearWorld();
      }

      IntegratedServer integratedServer = this.server;
      this.server = null;
      this.gameRenderer.reset();
      this.interactionManager = null;
      NarratorManager.INSTANCE.clear();
      this.reset(screen);
      if (this.world != null) {
         if (integratedServer != null) {
            this.profiler.push("waitForServer");

            while(!integratedServer.isStopping()) {
               this.render(false);
            }

            this.profiler.pop();
         }

         this.builtinPackProvider.clear();
         this.inGameHud.clear();
         this.currentServerEntry = null;
         this.integratedServerRunning = false;
         this.game.onLeaveGameSession();
      }

      this.world = null;
      this.setWorld((ClientWorld)null);
      this.player = null;
   }

   private void reset(Screen screen) {
      this.profiler.push("forcedTick");
      this.soundManager.stopAll();
      this.cameraEntity = null;
      this.integratedServerConnection = null;
      this.openScreen(screen);
      this.render(false);
      this.profiler.pop();
   }

   public void method_29970(Screen screen) {
      this.profiler.push("forcedTick");
      this.openScreen(screen);
      this.render(false);
      this.profiler.pop();
   }

   private void setWorld(@Nullable ClientWorld world) {
      this.worldRenderer.setWorld(world);
      this.particleManager.setWorld(world);
      this.blockEntityRenderDispatcher.setWorld(world);
      this.updateWindowTitle();
   }

   public boolean isMultiplayerEnabled() {
      return this.multiplayerEnabled && this.socialInteractionsService.serversAllowed();
   }

   public boolean isRealmsEnabled() {
      return this.socialInteractionsService.realmsAllowed();
   }

   /**
    * Checks if the client should block messages from the {@code sender}.
    * 
    * <p>If true, messages will not be displayed in chat and narrator will not process
    * them.
    */
   public boolean shouldBlockMessages(UUID sender) {
      if (this.getChatRestriction().allowsChat(false)) {
         return this.socialInteractionsManager.isPlayerMuted(sender);
      } else {
         return (this.player == null || !sender.equals(this.player.getUuid())) && !sender.equals(Util.NIL_UUID);
      }
   }

   public MinecraftClient.ChatRestriction getChatRestriction() {
      if (this.options.chatVisibility == ChatVisibility.HIDDEN) {
         return MinecraftClient.ChatRestriction.DISABLED_BY_OPTIONS;
      } else if (!this.onlineChatEnabled) {
         return MinecraftClient.ChatRestriction.DISABLED_BY_LAUNCHER;
      } else {
         return !this.socialInteractionsService.chatAllowed() ? MinecraftClient.ChatRestriction.DISABLED_BY_PROFILE : MinecraftClient.ChatRestriction.ENABLED;
      }
   }

   public final boolean isDemo() {
      return this.isDemo;
   }

   @Nullable
   public ClientPlayNetworkHandler getNetworkHandler() {
      return this.player == null ? null : this.player.networkHandler;
   }

   public static boolean isHudEnabled() {
      return !instance.options.hudHidden;
   }

   public static boolean isFancyGraphicsOrBetter() {
      return instance.options.graphicsMode.getId() >= GraphicsMode.FANCY.getId();
   }

   public static boolean isFabulousGraphicsOrBetter() {
      return !instance.gameRenderer.isRenderingPanorama() && instance.options.graphicsMode.getId() >= GraphicsMode.FABULOUS.getId();
   }

   public static boolean isAmbientOcclusionEnabled() {
      return instance.options.ao != AoMode.OFF;
   }

   private void doItemPick() {
      if (this.crosshairTarget != null && this.crosshairTarget.getType() != HitResult.Type.MISS) {
         boolean bl = this.player.getAbilities().creativeMode;
         BlockEntity blockEntity = null;
         HitResult.Type type = this.crosshairTarget.getType();
         ItemStack itemStack3;
         if (type == HitResult.Type.BLOCK) {
            BlockPos blockPos = ((BlockHitResult)this.crosshairTarget).getBlockPos();
            BlockState blockState = this.world.getBlockState(blockPos);
            if (blockState.isAir()) {
               return;
            }

            Block block = blockState.getBlock();
            itemStack3 = block.getPickStack(this.world, blockPos, blockState);
            if (itemStack3.isEmpty()) {
               return;
            }

            if (bl && Screen.hasControlDown() && blockState.hasBlockEntity()) {
               blockEntity = this.world.getBlockEntity(blockPos);
            }
         } else {
            if (type != HitResult.Type.ENTITY || !bl) {
               return;
            }

            Entity entity = ((EntityHitResult)this.crosshairTarget).getEntity();
            itemStack3 = entity.getPickBlockStack();
            if (itemStack3 == null) {
               return;
            }
         }

         if (itemStack3.isEmpty()) {
            String string = "";
            if (type == HitResult.Type.BLOCK) {
               string = Registry.BLOCK.getId(this.world.getBlockState(((BlockHitResult)this.crosshairTarget).getBlockPos()).getBlock()).toString();
            } else if (type == HitResult.Type.ENTITY) {
               string = Registry.ENTITY_TYPE.getId(((EntityHitResult)this.crosshairTarget).getEntity().getType()).toString();
            }

            LOGGER.warn((String)"Picking on: [{}] {} gave null item", (Object)type, (Object)string);
         } else {
            PlayerInventory playerInventory = this.player.getInventory();
            if (blockEntity != null) {
               this.addBlockEntityNbt(itemStack3, blockEntity);
            }

            int i = playerInventory.getSlotWithStack(itemStack3);
            if (bl) {
               playerInventory.addPickBlock(itemStack3);
               this.interactionManager.clickCreativeStack(this.player.getStackInHand(Hand.MAIN_HAND), 36 + playerInventory.selectedSlot);
            } else if (i != -1) {
               if (PlayerInventory.isValidHotbarIndex(i)) {
                  playerInventory.selectedSlot = i;
               } else {
                  this.interactionManager.pickFromInventory(i);
               }
            }

         }
      }
   }

   private ItemStack addBlockEntityNbt(ItemStack stack, BlockEntity blockEntity) {
      NbtCompound nbtCompound = blockEntity.writeNbt(new NbtCompound());
      NbtCompound nbtCompound3;
      if (stack.getItem() instanceof SkullItem && nbtCompound.contains("SkullOwner")) {
         nbtCompound3 = nbtCompound.getCompound("SkullOwner");
         stack.getOrCreateTag().put("SkullOwner", nbtCompound3);
         return stack;
      } else {
         stack.putSubTag("BlockEntityTag", nbtCompound);
         nbtCompound3 = new NbtCompound();
         NbtList nbtList = new NbtList();
         nbtList.add(NbtString.of("\"(+NBT)\""));
         nbtCompound3.put("Lore", nbtList);
         stack.putSubTag("display", nbtCompound3);
         return stack;
      }
   }

   public CrashReport addDetailsToCrashReport(CrashReport report) {
      SystemDetails systemDetails = report.getSystemDetailsSection();
      addSystemDetailsToCrashReport(systemDetails, this, this.languageManager, this.gameVersion, this.options);
      if (this.world != null) {
         this.world.addDetailsToCrashReport(report);
      }

      if (this.server != null) {
         this.server.method_37324(systemDetails);
      }

      this.resourceReloadLogger.addReloadSection(report);
      return report;
   }

   public static void addSystemDetailsToCrashReport(@Nullable MinecraftClient client, @Nullable LanguageManager languageManager, String version, @Nullable GameOptions options, CrashReport report) {
      SystemDetails systemDetails = report.getSystemDetailsSection();
      addSystemDetailsToCrashReport(systemDetails, client, languageManager, version, options);
   }

   private static SystemDetails addSystemDetailsToCrashReport(SystemDetails systemDetails, @Nullable MinecraftClient client, @Nullable LanguageManager languageManager, String version, GameOptions options) {
      systemDetails.addSection("Launched Version", () -> {
         return version;
      });
      systemDetails.addSection("Backend library", RenderSystem::getBackendDescription);
      systemDetails.addSection("Backend API", RenderSystem::getApiDescription);
      systemDetails.addSection("Window size", () -> {
         return client != null ? client.window.getFramebufferWidth() + "x" + client.window.getFramebufferHeight() : "<not initialized>";
      });
      systemDetails.addSection("GL Caps", RenderSystem::getCapsString);
      systemDetails.addSection("GL debug messages", () -> {
         return GlDebug.isDebugMessageEnabled() ? String.join("\n", GlDebug.collectDebugMessages()) : "<disabled>";
      });
      systemDetails.addSection("Using VBOs", () -> {
         return "Yes";
      });
      systemDetails.addSection("Is Modded", () -> {
         String string = ClientBrandRetriever.getClientModName();
         if (!"vanilla".equals(string)) {
            return "Definitely; Client brand changed to '" + string + "'";
         } else {
            return MinecraftClient.class.getSigners() == null ? "Very likely; Jar signature invalidated" : "Probably not. Jar signature remains and client brand is untouched.";
         }
      });
      systemDetails.addSection("Type", "Client (map_client.txt)");
      if (options != null) {
         if (instance != null) {
            String string = instance.getVideoWarningManager().getWarningsAsString();
            if (string != null) {
               systemDetails.addSection("GPU Warnings", string);
            }
         }

         systemDetails.addSection("Graphics mode", options.graphicsMode.toString());
         systemDetails.addSection("Resource Packs", () -> {
            StringBuilder stringBuilder = new StringBuilder();
            Iterator var2 = options.resourcePacks.iterator();

            while(var2.hasNext()) {
               String string = (String)var2.next();
               if (stringBuilder.length() > 0) {
                  stringBuilder.append(", ");
               }

               stringBuilder.append(string);
               if (options.incompatibleResourcePacks.contains(string)) {
                  stringBuilder.append(" (incompatible)");
               }
            }

            return stringBuilder.toString();
         });
      }

      if (languageManager != null) {
         systemDetails.addSection("Current Language", () -> {
            return languageManager.getLanguage().toString();
         });
      }

      systemDetails.addSection("CPU", GlDebugInfo::getCpuInfo);
      return systemDetails;
   }

   public static MinecraftClient getInstance() {
      return instance;
   }

   public CompletableFuture<Void> reloadResourcesConcurrently() {
      return this.submit(this::reloadResources).thenCompose((completableFuture) -> {
         return completableFuture;
      });
   }

   public void addSnooperInfo(Snooper snooper) {
      snooper.addInfo("fps", currentFps);
      snooper.addInfo("vsync_enabled", this.options.enableVsync);
      snooper.addInfo("display_frequency", this.window.getRefreshRate());
      snooper.addInfo("display_type", this.window.isFullscreen() ? "fullscreen" : "windowed");
      snooper.addInfo("run_time", (Util.getMeasuringTimeMs() - snooper.getStartTime()) / 60L * 1000L);
      snooper.addInfo("current_action", this.getCurrentAction());
      snooper.addInfo("language", this.options.language == null ? "en_us" : this.options.language);
      String string = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? "little" : "big";
      snooper.addInfo("endianness", string);
      snooper.addInfo("subtitles", this.options.showSubtitles);
      snooper.addInfo("touch", this.options.touchscreen ? "touch" : "mouse");
      int i = 0;
      Iterator var4 = this.resourcePackManager.getEnabledProfiles().iterator();

      while(var4.hasNext()) {
         ResourcePackProfile resourcePackProfile = (ResourcePackProfile)var4.next();
         if (!resourcePackProfile.isAlwaysEnabled() && !resourcePackProfile.isPinned()) {
            int var10001 = i++;
            snooper.addInfo("resource_pack[" + var10001 + "]", resourcePackProfile.getName());
         }
      }

      snooper.addInfo("resource_packs", i);
      if (this.server != null) {
         snooper.addInfo("snooper_partner", this.server.getSnooper().getToken());
      }

   }

   private String getCurrentAction() {
      if (this.server != null) {
         return this.server.isRemote() ? "hosting_lan" : "singleplayer";
      } else if (this.currentServerEntry != null) {
         return this.currentServerEntry.isLocal() ? "playing_lan" : "multiplayer";
      } else {
         return "out_of_game";
      }
   }

   public void addInitialSnooperInfo(Snooper snooper) {
      snooper.addInitialInfo("client_brand", ClientBrandRetriever.getClientModName());
      snooper.addInitialInfo("launched_version", this.gameVersion);
      addGlInfo(snooper);
      snooper.addInitialInfo("gl_max_texture_size", RenderSystem.maxSupportedTextureSize());
      GameProfile gameProfile = this.session.getProfile();
      if (gameProfile.getId() != null) {
         snooper.addInitialInfo("uuid", Hashing.sha1().hashBytes(gameProfile.getId().toString().getBytes(Charsets.ISO_8859_1)).toString());
      }

   }

   private static void addGlInfo(Snooper snooper) {
      Objects.requireNonNull(snooper);
      GlDebugInfo.feedTo(snooper::addInitialInfo);
   }

   public boolean isSnooperEnabled() {
      return this.options.snooperEnabled;
   }

   public void setCurrentServerEntry(@Nullable ServerInfo serverEntry) {
      this.currentServerEntry = serverEntry;
   }

   @Nullable
   public ServerInfo getCurrentServerEntry() {
      return this.currentServerEntry;
   }

   public boolean isInSingleplayer() {
      return this.integratedServerRunning;
   }

   public boolean isIntegratedServerRunning() {
      return this.integratedServerRunning && this.server != null;
   }

   /**
    * Gets this client's own integrated server.
    * 
    * <p>The integrated server is only present when a local single player world is open.
    */
   @Nullable
   public IntegratedServer getServer() {
      return this.server;
   }

   public Snooper getSnooper() {
      return this.snooper;
   }

   public Session getSession() {
      return this.session;
   }

   public PropertyMap getSessionProperties() {
      if (this.sessionPropertyMap.isEmpty()) {
         GameProfile gameProfile = this.getSessionService().fillProfileProperties(this.session.getProfile(), false);
         this.sessionPropertyMap.putAll(gameProfile.getProperties());
      }

      return this.sessionPropertyMap;
   }

   public Proxy getNetworkProxy() {
      return this.netProxy;
   }

   public TextureManager getTextureManager() {
      return this.textureManager;
   }

   public ResourceManager getResourceManager() {
      return this.resourceManager;
   }

   public ResourcePackManager getResourcePackManager() {
      return this.resourcePackManager;
   }

   public ClientBuiltinResourcePackProvider getResourcePackProvider() {
      return this.builtinPackProvider;
   }

   public File getResourcePackDir() {
      return this.resourcePackDir;
   }

   public LanguageManager getLanguageManager() {
      return this.languageManager;
   }

   public Function<Identifier, Sprite> getSpriteAtlas(Identifier id) {
      SpriteAtlasTexture var10000 = this.bakedModelManager.getAtlas(id);
      Objects.requireNonNull(var10000);
      return var10000::getSprite;
   }

   public boolean is64Bit() {
      return this.is64Bit;
   }

   public boolean isPaused() {
      return this.paused;
   }

   public VideoWarningManager getVideoWarningManager() {
      return this.videoWarningManager;
   }

   public SoundManager getSoundManager() {
      return this.soundManager;
   }

   public MusicSound getMusicType() {
      if (this.currentScreen instanceof CreditsScreen) {
         return MusicType.CREDITS;
      } else if (this.player != null) {
         if (this.player.world.getRegistryKey() == World.END) {
            return this.inGameHud.getBossBarHud().shouldPlayDragonMusic() ? MusicType.DRAGON : MusicType.END;
         } else {
            Biome.Category category = this.player.world.getBiome(this.player.getBlockPos()).getCategory();
            if (this.musicTracker.isPlayingType(MusicType.UNDERWATER) || this.player.isSubmergedInWater() && (category == Biome.Category.OCEAN || category == Biome.Category.RIVER)) {
               return MusicType.UNDERWATER;
            } else {
               return this.player.world.getRegistryKey() != World.NETHER && this.player.getAbilities().creativeMode && this.player.getAbilities().allowFlying ? MusicType.CREATIVE : (MusicSound)this.world.getBiomeAccess().getBiomeForNoiseGen(this.player.getBlockPos()).getMusic().orElse(MusicType.GAME);
            }
         }
      } else {
         return MusicType.MENU;
      }
   }

   public MinecraftSessionService getSessionService() {
      return this.sessionService;
   }

   public PlayerSkinProvider getSkinProvider() {
      return this.skinProvider;
   }

   @Nullable
   public Entity getCameraEntity() {
      return this.cameraEntity;
   }

   public void setCameraEntity(Entity entity) {
      this.cameraEntity = entity;
      this.gameRenderer.onCameraEntitySet(entity);
   }

   /**
    * Checks if the provided {@code entity} should display an outline around its model.
    */
   public boolean hasOutline(Entity entity) {
      return entity.isGlowing() || this.player != null && this.player.isSpectator() && this.options.keySpectatorOutlines.isPressed() && entity.getType() == EntityType.PLAYER;
   }

   protected Thread getThread() {
      return this.thread;
   }

   protected Runnable createTask(Runnable runnable) {
      return runnable;
   }

   protected boolean canExecute(Runnable task) {
      return true;
   }

   public BlockRenderManager getBlockRenderManager() {
      return this.blockRenderManager;
   }

   public EntityRenderDispatcher getEntityRenderDispatcher() {
      return this.entityRenderDispatcher;
   }

   public BlockEntityRenderDispatcher getBlockEntityRenderDispatcher() {
      return this.blockEntityRenderDispatcher;
   }

   public ItemRenderer getItemRenderer() {
      return this.itemRenderer;
   }

   public HeldItemRenderer getHeldItemRenderer() {
      return this.heldItemRenderer;
   }

   public <T> SearchableContainer<T> getSearchableContainer(SearchManager.Key<T> key) {
      return this.searchManager.get(key);
   }

   public MetricsData getMetricsData() {
      return this.metricsData;
   }

   public boolean isConnectedToRealms() {
      return this.connectedToRealms;
   }

   public void setConnectedToRealms(boolean connectedToRealms) {
      this.connectedToRealms = connectedToRealms;
   }

   public DataFixer getDataFixer() {
      return this.dataFixer;
   }

   public float getTickDelta() {
      return this.renderTickCounter.tickDelta;
   }

   public float getLastFrameDuration() {
      return this.renderTickCounter.lastFrameDuration;
   }

   public BlockColors getBlockColors() {
      return this.blockColors;
   }

   public boolean hasReducedDebugInfo() {
      return this.player != null && this.player.hasReducedDebugInfo() || this.options.reducedDebugInfo;
   }

   public ToastManager getToastManager() {
      return this.toastManager;
   }

   public TutorialManager getTutorialManager() {
      return this.tutorialManager;
   }

   public boolean isWindowFocused() {
      return this.windowFocused;
   }

   public HotbarStorage getCreativeHotbarStorage() {
      return this.creativeHotbarStorage;
   }

   public BakedModelManager getBakedModelManager() {
      return this.bakedModelManager;
   }

   public PaintingManager getPaintingManager() {
      return this.paintingManager;
   }

   public StatusEffectSpriteManager getStatusEffectSpriteManager() {
      return this.statusEffectSpriteManager;
   }

   public void onWindowFocusChanged(boolean focused) {
      this.windowFocused = focused;
   }

   /**
    * Takes a panorama. The panorama is stored in the given {@code directory}, in
    * where 6 screenshots of size {@code width} and {@code height} will be taken.
    * 
    * @return a user-oriented piece of text for screenshot result
    */
   public Text takePanorama(File directory, int width, int height) {
      int i = this.window.getFramebufferWidth();
      int j = this.window.getFramebufferHeight();
      Framebuffer framebuffer = new SimpleFramebuffer(width, height, true, IS_SYSTEM_MAC);
      float f = this.player.getPitch();
      float g = this.player.getYaw();
      float h = this.player.prevPitch;
      float k = this.player.prevYaw;
      this.gameRenderer.setBlockOutlineEnabled(false);

      TranslatableText var12;
      try {
         this.gameRenderer.setRenderingPanorama(true);
         this.worldRenderer.reloadTransparencyShader();
         this.window.setFramebufferWidth(width);
         this.window.setFramebufferHeight(height);

         for(int l = 0; l < 6; ++l) {
            switch(l) {
            case 0:
               this.player.setYaw(g);
               this.player.setPitch(0.0F);
               break;
            case 1:
               this.player.setYaw((g + 90.0F) % 360.0F);
               this.player.setPitch(0.0F);
               break;
            case 2:
               this.player.setYaw((g + 180.0F) % 360.0F);
               this.player.setPitch(0.0F);
               break;
            case 3:
               this.player.setYaw((g - 90.0F) % 360.0F);
               this.player.setPitch(0.0F);
               break;
            case 4:
               this.player.setYaw(g);
               this.player.setPitch(-90.0F);
               break;
            case 5:
            default:
               this.player.setYaw(g);
               this.player.setPitch(90.0F);
            }

            this.player.prevYaw = this.player.getYaw();
            this.player.prevPitch = this.player.getPitch();
            framebuffer.beginWrite(true);
            this.gameRenderer.renderWorld(1.0F, 0L, new MatrixStack());

            try {
               Thread.sleep(10L);
            } catch (InterruptedException var17) {
            }

            ScreenshotRecorder.saveScreenshot(directory, "panorama_" + l + ".png", width, height, framebuffer, (textx) -> {
            });
         }

         Text text = (new LiteralText(directory.getName())).formatted(Formatting.UNDERLINE).styled((style) -> {
            return style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, directory.getAbsolutePath()));
         });
         var12 = new TranslatableText("screenshot.success", new Object[]{text});
         return var12;
      } catch (Exception var18) {
         LOGGER.error((String)"Couldn't save image", (Throwable)var18);
         var12 = new TranslatableText("screenshot.failure", new Object[]{var18.getMessage()});
      } finally {
         this.player.setPitch(f);
         this.player.setYaw(g);
         this.player.prevPitch = h;
         this.player.prevYaw = k;
         this.gameRenderer.setBlockOutlineEnabled(true);
         this.window.setFramebufferWidth(i);
         this.window.setFramebufferHeight(j);
         framebuffer.delete();
         this.gameRenderer.setRenderingPanorama(false);
         this.worldRenderer.reloadTransparencyShader();
         this.getFramebuffer().beginWrite(true);
      }

      return var12;
   }

   /**
    * Takes a huge screenshot in the tga file format.
    * 
    * <p>The {@code unitWidth} and {@code unitHeight} controls the size of the
    * partial image rendered; it does not affect the screenshot outcome, but may
    * affect the screenshot performance.
    * 
    * @return a user-oriented piece of text for screenshot result
    */
   private Text takeHugeScreenshot(File gameDirectory, int unitWidth, int unitHeight, int width, int height) {
      try {
         ByteBuffer byteBuffer = GlDebugInfo.allocateMemory(unitWidth * unitHeight * 3);
         ScreenshotRecorder screenshotRecorder = new ScreenshotRecorder(gameDirectory, width, height, unitHeight);
         float f = (float)width / (float)unitWidth;
         float g = (float)height / (float)unitHeight;
         float h = f > g ? f : g;

         for(int i = (height - 1) / unitHeight * unitHeight; i >= 0; i -= unitHeight) {
            for(int j = 0; j < width; j += unitWidth) {
               RenderSystem.setShaderTexture(0, SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
               float k = (float)(width - unitWidth) / 2.0F * 2.0F - (float)(j * 2);
               float l = (float)(height - unitHeight) / 2.0F * 2.0F - (float)(i * 2);
               k /= (float)unitWidth;
               l /= (float)unitHeight;
               this.gameRenderer.renderWithZoom(h, k, l);
               byteBuffer.clear();
               RenderSystem.pixelStore(3333, 1);
               RenderSystem.pixelStore(3317, 1);
               RenderSystem.readPixels(0, 0, unitWidth, unitHeight, 32992, 5121, byteBuffer);
               screenshotRecorder.getIntoBuffer(byteBuffer, j, i, unitWidth, unitHeight);
            }

            screenshotRecorder.writeToStream();
         }

         File file = screenshotRecorder.finish();
         GlDebugInfo.freeMemory(byteBuffer);
         Text text = (new LiteralText(file.getName())).formatted(Formatting.UNDERLINE).styled((style) -> {
            return style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, file.getAbsolutePath()));
         });
         return new TranslatableText("screenshot.success", new Object[]{text});
      } catch (Exception var15) {
         LOGGER.warn((String)"Couldn't save screenshot", (Throwable)var15);
         return new TranslatableText("screenshot.failure", new Object[]{var15.getMessage()});
      }
   }

   public Profiler getProfiler() {
      return this.profiler;
   }

   public MinecraftClientGame getGame() {
      return this.game;
   }

   @Nullable
   public WorldGenerationProgressTracker getWorldGenerationProgressTracker() {
      return (WorldGenerationProgressTracker)this.worldGenProgressTracker.get();
   }

   public SplashTextResourceSupplier getSplashTextLoader() {
      return this.splashTextLoader;
   }

   @Nullable
   public Overlay getOverlay() {
      return this.overlay;
   }

   public SocialInteractionsManager getSocialInteractionsManager() {
      return this.socialInteractionsManager;
   }

   public boolean shouldRenderAsync() {
      return false;
   }

   public Window getWindow() {
      return this.window;
   }

   public BufferBuilderStorage getBufferBuilders() {
      return this.bufferBuilders;
   }

   private static ResourcePackProfile createResourcePackProfile(String name, Text displayName, boolean alwaysEnabled, Supplier<ResourcePack> packFactory, PackResourceMetadata metadata, ResourcePackProfile.InsertionPosition insertionPosition, ResourcePackSource source) {
      int i = metadata.getPackFormat();
      Supplier<ResourcePack> supplier = packFactory;
      if (i <= 3) {
         supplier = createV3ResourcePackFactory(packFactory);
      }

      if (i <= 4) {
         supplier = createV4ResourcePackFactory(supplier);
      }

      return new ResourcePackProfile(name, displayName, alwaysEnabled, supplier, metadata, ResourceType.CLIENT_RESOURCES, insertionPosition, source);
   }

   private static Supplier<ResourcePack> createV3ResourcePackFactory(Supplier<ResourcePack> packFactory) {
      return () -> {
         return new Format3ResourcePack((ResourcePack)packFactory.get(), Format3ResourcePack.NEW_TO_OLD_MAP);
      };
   }

   private static Supplier<ResourcePack> createV4ResourcePackFactory(Supplier<ResourcePack> packFactory) {
      return () -> {
         return new Format4ResourcePack((ResourcePack)packFactory.get());
      };
   }

   public void setMipmapLevels(int mipmapLevels) {
      this.bakedModelManager.setMipmapLevels(mipmapLevels);
   }

   public EntityModelLoader getEntityModelLoader() {
      return this.entityModelLoader;
   }

   public boolean shouldFilterText() {
      return false;
   }

   static {
      IS_SYSTEM_MAC = Util.getOperatingSystem() == Util.OperatingSystem.OSX;
      DEFAULT_FONT_ID = new Identifier("default");
      UNICODE_FONT_ID = new Identifier("uniform");
      ALT_TEXT_RENDERER_ID = new Identifier("alt");
      COMPLETED_UNIT_FUTURE = CompletableFuture.completedFuture(Unit.INSTANCE);
      SOCIAL_INTERACTIONS_NOT_AVAILABLE = new TranslatableText("multiplayer.socialInteractions.not_available");
      memoryReservedForCrash = new byte[10485760];
   }

   /**
    * Represents the restrictions on chat on a Minecraft client.
    * 
    * @see MinecraftClient#getChatRestriction()
    */
   @Environment(EnvType.CLIENT)
   public static enum ChatRestriction {
      ENABLED(LiteralText.EMPTY) {
         public boolean allowsChat(boolean singlePlayer) {
            return true;
         }
      },
      DISABLED_BY_OPTIONS((new TranslatableText("chat.disabled.options")).formatted(Formatting.RED)) {
         public boolean allowsChat(boolean singlePlayer) {
            return false;
         }
      },
      DISABLED_BY_LAUNCHER((new TranslatableText("chat.disabled.launcher")).formatted(Formatting.RED)) {
         public boolean allowsChat(boolean singlePlayer) {
            return singlePlayer;
         }
      },
      DISABLED_BY_PROFILE((new TranslatableText("chat.disabled.profile")).formatted(Formatting.RED)) {
         public boolean allowsChat(boolean singlePlayer) {
            return singlePlayer;
         }
      };

      private final Text description;

      ChatRestriction(Text description) {
         this.description = description;
      }

      public Text getDescription() {
         return this.description;
      }

      public abstract boolean allowsChat(boolean singlePlayer);
   }

   @Environment(EnvType.CLIENT)
   private static enum WorldLoadAction {
      NONE,
      CREATE,
      BACKUP;
   }

   @Environment(EnvType.CLIENT)
   public static final class IntegratedResourceManager implements AutoCloseable {
      private final ResourcePackManager resourcePackManager;
      private final ServerResourceManager serverResourceManager;
      private final SaveProperties saveProperties;

      IntegratedResourceManager(ResourcePackManager resourcePackManager, ServerResourceManager serverResourceManager, SaveProperties saveProperties) {
         this.resourcePackManager = resourcePackManager;
         this.serverResourceManager = serverResourceManager;
         this.saveProperties = saveProperties;
      }

      public ResourcePackManager getResourcePackManager() {
         return this.resourcePackManager;
      }

      public ServerResourceManager getServerResourceManager() {
         return this.serverResourceManager;
      }

      public SaveProperties getSaveProperties() {
         return this.saveProperties;
      }

      public void close() {
         this.resourcePackManager.close();
         this.serverResourceManager.close();
      }
   }
}
