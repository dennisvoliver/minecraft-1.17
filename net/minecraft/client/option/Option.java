package net.minecraft.client.option;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.resource.VideoWarningManager;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.client.util.Window;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Arm;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public abstract class Option {
   protected static final int field_32147 = 200;
   public static final DoubleOption BIOME_BLEND_RADIUS = new DoubleOption("options.biomeBlendRadius", 0.0D, 7.0D, 1.0F, (gameOptions) -> {
      return (double)gameOptions.biomeBlendRadius;
   }, (gameOptions, biomeBlendRadius) -> {
      gameOptions.biomeBlendRadius = MathHelper.clamp((int)((int)biomeBlendRadius), (int)0, (int)7);
      MinecraftClient.getInstance().worldRenderer.reload();
   }, (gameOptions, option) -> {
      double d = option.get(gameOptions);
      int i = (int)d * 2 + 1;
      return option.getGenericLabel(new TranslatableText("options.biomeBlendRadius." + i));
   });
   public static final DoubleOption CHAT_HEIGHT_FOCUSED = new DoubleOption("options.chat.height.focused", 0.0D, 1.0D, 0.0F, (gameOptions) -> {
      return gameOptions.chatHeightFocused;
   }, (gameOptions, chatHeightFocused) -> {
      gameOptions.chatHeightFocused = chatHeightFocused;
      MinecraftClient.getInstance().inGameHud.getChatHud().reset();
   }, (gameOptions, option) -> {
      double d = option.getRatio(option.get(gameOptions));
      return option.getPixelLabel(ChatHud.getHeight(d));
   });
   public static final DoubleOption SATURATION = new DoubleOption("options.chat.height.unfocused", 0.0D, 1.0D, 0.0F, (gameOptions) -> {
      return gameOptions.chatHeightUnfocused;
   }, (gameOptions, chatHeightUnfocused) -> {
      gameOptions.chatHeightUnfocused = chatHeightUnfocused;
      MinecraftClient.getInstance().inGameHud.getChatHud().reset();
   }, (gameOptions, option) -> {
      double d = option.getRatio(option.get(gameOptions));
      return option.getPixelLabel(ChatHud.getHeight(d));
   });
   public static final DoubleOption CHAT_OPACITY = new DoubleOption("options.chat.opacity", 0.0D, 1.0D, 0.0F, (gameOptions) -> {
      return gameOptions.chatOpacity;
   }, (gameOptions, chatOpacity) -> {
      gameOptions.chatOpacity = chatOpacity;
      MinecraftClient.getInstance().inGameHud.getChatHud().reset();
   }, (gameOptions, option) -> {
      double d = option.getRatio(option.get(gameOptions));
      return option.getPercentLabel(d * 0.9D + 0.1D);
   });
   public static final DoubleOption CHAT_SCALE = new DoubleOption("options.chat.scale", 0.0D, 1.0D, 0.0F, (gameOptions) -> {
      return gameOptions.chatScale;
   }, (gameOptions, chatScale) -> {
      gameOptions.chatScale = chatScale;
      MinecraftClient.getInstance().inGameHud.getChatHud().reset();
   }, (gameOptions, option) -> {
      double d = option.getRatio(option.get(gameOptions));
      return (Text)(d == 0.0D ? ScreenTexts.composeToggleText(option.getDisplayPrefix(), false) : option.getPercentLabel(d));
   });
   public static final DoubleOption CHAT_WIDTH = new DoubleOption("options.chat.width", 0.0D, 1.0D, 0.0F, (gameOptions) -> {
      return gameOptions.chatWidth;
   }, (gameOptions, chatWidth) -> {
      gameOptions.chatWidth = chatWidth;
      MinecraftClient.getInstance().inGameHud.getChatHud().reset();
   }, (gameOptions, option) -> {
      double d = option.getRatio(option.get(gameOptions));
      return option.getPixelLabel(ChatHud.getWidth(d));
   });
   public static final DoubleOption CHAT_LINE_SPACING = new DoubleOption("options.chat.line_spacing", 0.0D, 1.0D, 0.0F, (gameOptions) -> {
      return gameOptions.chatLineSpacing;
   }, (gameOptions, chatLineSpacing) -> {
      gameOptions.chatLineSpacing = chatLineSpacing;
   }, (gameOptions, option) -> {
      return option.getPercentLabel(option.getRatio(option.get(gameOptions)));
   });
   public static final DoubleOption CHAT_DELAY_INSTANT = new DoubleOption("options.chat.delay_instant", 0.0D, 6.0D, 0.1F, (gameOptions) -> {
      return gameOptions.chatDelay;
   }, (gameOptions, chatDelay) -> {
      gameOptions.chatDelay = chatDelay;
   }, (gameOptions, option) -> {
      double d = option.get(gameOptions);
      return d <= 0.0D ? new TranslatableText("options.chat.delay_none") : new TranslatableText("options.chat.delay", new Object[]{String.format("%.1f", d)});
   });
   public static final DoubleOption FOV = new DoubleOption("options.fov", 30.0D, 110.0D, 1.0F, (gameOptions) -> {
      return gameOptions.fov;
   }, (gameOptions, fov) -> {
      gameOptions.fov = fov;
   }, (gameOptions, option) -> {
      double d = option.get(gameOptions);
      if (d == 70.0D) {
         return option.getGenericLabel(new TranslatableText("options.fov.min"));
      } else {
         return d == option.getMax() ? option.getGenericLabel(new TranslatableText("options.fov.max")) : option.getGenericLabel((int)d);
      }
   });
   private static final Text FOV_EFFECT_SCALE_TOOLTIP = new TranslatableText("options.fovEffectScale.tooltip");
   public static final DoubleOption FOV_EFFECT_SCALE = new DoubleOption("options.fovEffectScale", 0.0D, 1.0D, 0.0F, (gameOptions) -> {
      return Math.pow((double)gameOptions.fovEffectScale, 2.0D);
   }, (gameOptions, fovEffectScale) -> {
      gameOptions.fovEffectScale = (float)Math.sqrt(fovEffectScale);
   }, (gameOptions, option) -> {
      double d = option.getRatio(option.get(gameOptions));
      return d == 0.0D ? option.getGenericLabel(ScreenTexts.OFF) : option.getPercentLabel(d);
   }, (client) -> {
      return client.textRenderer.wrapLines(FOV_EFFECT_SCALE_TOOLTIP, 200);
   });
   private static final Text DISTORTION_EFFECT_SCALE_TOOLTIP = new TranslatableText("options.screenEffectScale.tooltip");
   public static final DoubleOption DISTORTION_EFFECT_SCALE = new DoubleOption("options.screenEffectScale", 0.0D, 1.0D, 0.0F, (gameOptions) -> {
      return (double)gameOptions.distortionEffectScale;
   }, (gameOptions, distortionEffectScale) -> {
      gameOptions.distortionEffectScale = distortionEffectScale.floatValue();
   }, (gameOptions, option) -> {
      double d = option.getRatio(option.get(gameOptions));
      return d == 0.0D ? option.getGenericLabel(ScreenTexts.OFF) : option.getPercentLabel(d);
   }, (client) -> {
      return client.textRenderer.wrapLines(DISTORTION_EFFECT_SCALE_TOOLTIP, 200);
   });
   public static final DoubleOption FRAMERATE_LIMIT = new DoubleOption("options.framerateLimit", 10.0D, 260.0D, 10.0F, (gameOptions) -> {
      return (double)gameOptions.maxFps;
   }, (gameOptions, maxFps) -> {
      gameOptions.maxFps = (int)maxFps;
      MinecraftClient.getInstance().getWindow().setFramerateLimit(gameOptions.maxFps);
   }, (gameOptions, option) -> {
      double d = option.get(gameOptions);
      return d == option.getMax() ? option.getGenericLabel(new TranslatableText("options.framerateLimit.max")) : option.getGenericLabel(new TranslatableText("options.framerate", new Object[]{(int)d}));
   });
   public static final DoubleOption GAMMA = new DoubleOption("options.gamma", 0.0D, 1.0D, 0.0F, (gameOptions) -> {
      return gameOptions.gamma;
   }, (gameOptions, gamma) -> {
      gameOptions.gamma = gamma;
   }, (gameOptions, option) -> {
      double d = option.getRatio(option.get(gameOptions));
      if (d == 0.0D) {
         return option.getGenericLabel(new TranslatableText("options.gamma.min"));
      } else {
         return d == 1.0D ? option.getGenericLabel(new TranslatableText("options.gamma.max")) : option.getPercentAdditionLabel((int)(d * 100.0D));
      }
   });
   public static final DoubleOption MIPMAP_LEVELS = new DoubleOption("options.mipmapLevels", 0.0D, 4.0D, 1.0F, (gameOptions) -> {
      return (double)gameOptions.mipmapLevels;
   }, (gameOptions, mipmapLevels) -> {
      gameOptions.mipmapLevels = (int)mipmapLevels;
   }, (gameOptions, option) -> {
      double d = option.get(gameOptions);
      return (Text)(d == 0.0D ? ScreenTexts.composeToggleText(option.getDisplayPrefix(), false) : option.getGenericLabel((int)d));
   });
   public static final DoubleOption MOUSE_WHEEL_SENSITIVITY = new LogarithmicOption("options.mouseWheelSensitivity", 0.01D, 10.0D, 0.01F, (gameOptions) -> {
      return gameOptions.mouseWheelSensitivity;
   }, (gameOptions, mouseWheelSensitivity) -> {
      gameOptions.mouseWheelSensitivity = mouseWheelSensitivity;
   }, (gameOptions, option) -> {
      double d = option.getRatio(option.get(gameOptions));
      return option.getGenericLabel(new LiteralText(String.format("%.2f", option.getValue(d))));
   });
   public static final CyclingOption<Boolean> RAW_MOUSE_INPUT = CyclingOption.create("options.rawMouseInput", (gameOptions) -> {
      return gameOptions.rawMouseInput;
   }, (gameOptions, option, rawMouseInput) -> {
      gameOptions.rawMouseInput = rawMouseInput;
      Window window = MinecraftClient.getInstance().getWindow();
      if (window != null) {
         window.setRawMouseMotion(rawMouseInput);
      }

   });
   public static final DoubleOption RENDER_DISTANCE = new DoubleOption("options.renderDistance", 2.0D, 16.0D, 1.0F, (gameOptions) -> {
      return (double)gameOptions.viewDistance;
   }, (gameOptions, viewDistance) -> {
      gameOptions.viewDistance = (int)viewDistance;
      MinecraftClient.getInstance().worldRenderer.scheduleTerrainUpdate();
   }, (gameOptions, option) -> {
      double d = option.get(gameOptions);
      return option.getGenericLabel(new TranslatableText("options.chunks", new Object[]{(int)d}));
   });
   public static final DoubleOption ENTITY_DISTANCE_SCALING = new DoubleOption("options.entityDistanceScaling", 0.5D, 5.0D, 0.25F, (gameOptions) -> {
      return (double)gameOptions.entityDistanceScaling;
   }, (gameOptions, entityDistanceScaling) -> {
      gameOptions.entityDistanceScaling = (float)entityDistanceScaling;
   }, (gameOptions, option) -> {
      double d = option.get(gameOptions);
      return option.getPercentLabel(d);
   });
   public static final DoubleOption SENSITIVITY = new DoubleOption("options.sensitivity", 0.0D, 1.0D, 0.0F, (gameOptions) -> {
      return gameOptions.mouseSensitivity;
   }, (gameOptions, mouseSensitivity) -> {
      gameOptions.mouseSensitivity = mouseSensitivity;
   }, (gameOptions, option) -> {
      double d = option.getRatio(option.get(gameOptions));
      if (d == 0.0D) {
         return option.getGenericLabel(new TranslatableText("options.sensitivity.min"));
      } else {
         return d == 1.0D ? option.getGenericLabel(new TranslatableText("options.sensitivity.max")) : option.getPercentLabel(2.0D * d);
      }
   });
   public static final DoubleOption TEXT_BACKGROUND_OPACITY = new DoubleOption("options.accessibility.text_background_opacity", 0.0D, 1.0D, 0.0F, (gameOptions) -> {
      return gameOptions.textBackgroundOpacity;
   }, (gameOptions, textBackgroundOpacity) -> {
      gameOptions.textBackgroundOpacity = textBackgroundOpacity;
      MinecraftClient.getInstance().inGameHud.getChatHud().reset();
   }, (gameOptions, option) -> {
      return option.getPercentLabel(option.getRatio(option.get(gameOptions)));
   });
   public static final CyclingOption<AoMode> AO = CyclingOption.create("options.ao", (Object[])AoMode.values(), (Function)((aoMode) -> {
      return new TranslatableText(aoMode.getTranslationKey());
   }), (gameOptions) -> {
      return gameOptions.ao;
   }, (gameOptions, option, aoMode) -> {
      gameOptions.ao = aoMode;
      MinecraftClient.getInstance().worldRenderer.reload();
   });
   public static final CyclingOption<AttackIndicator> ATTACK_INDICATOR = CyclingOption.create("options.attackIndicator", (Object[])AttackIndicator.values(), (Function)((attackIndicator) -> {
      return new TranslatableText(attackIndicator.getTranslationKey());
   }), (gameOptions) -> {
      return gameOptions.attackIndicator;
   }, (gameOptions, option, attackIndicator) -> {
      gameOptions.attackIndicator = attackIndicator;
   });
   public static final CyclingOption<ChatVisibility> VISIBILITY = CyclingOption.create("options.chat.visibility", (Object[])ChatVisibility.values(), (Function)((chatVisibility) -> {
      return new TranslatableText(chatVisibility.getTranslationKey());
   }), (gameOptions) -> {
      return gameOptions.chatVisibility;
   }, (gameOptions, option, chatVisibility) -> {
      gameOptions.chatVisibility = chatVisibility;
   });
   private static final Text FAST_GRAPHICS_TOOLTIP = new TranslatableText("options.graphics.fast.tooltip");
   private static final Text FABULOUS_GRAPHICS_TOOLTIP;
   private static final Text FANCY_GRAPHICS_TOOLTIP;
   public static final CyclingOption<GraphicsMode> GRAPHICS;
   public static final CyclingOption GUI_SCALE;
   public static final CyclingOption<Arm> MAIN_HAND;
   public static final CyclingOption<NarratorMode> NARRATOR;
   public static final CyclingOption<ParticlesMode> PARTICLES;
   public static final CyclingOption<CloudRenderMode> CLOUDS;
   public static final CyclingOption<Boolean> TEXT_BACKGROUND;
   private static final Text HIDE_MATCHED_NAMES_TOOLTIP;
   public static final CyclingOption<Boolean> AUTO_JUMP;
   public static final CyclingOption<Boolean> AUTO_SUGGESTIONS;
   public static final CyclingOption<Boolean> CHAT_COLOR;
   public static final CyclingOption<Boolean> HIDE_MATCHED_NAMES;
   public static final CyclingOption<Boolean> CHAT_LINKS;
   public static final CyclingOption<Boolean> CHAT_LINKS_PROMPT;
   public static final CyclingOption<Boolean> DISCRETE_MOUSE_SCROLL;
   public static final CyclingOption<Boolean> VSYNC;
   public static final CyclingOption<Boolean> ENTITY_SHADOWS;
   public static final CyclingOption<Boolean> FORCE_UNICODE_FONT;
   public static final CyclingOption<Boolean> INVERT_MOUSE;
   public static final CyclingOption<Boolean> REALMS_NOTIFICATIONS;
   public static final CyclingOption<Boolean> REDUCED_DEBUG_INFO;
   public static final CyclingOption<Boolean> SUBTITLES;
   public static final CyclingOption<Boolean> SNOOPER;
   private static final Text TOGGLE_TEXT;
   private static final Text HOLD_TEXT;
   public static final CyclingOption<Boolean> SNEAK_TOGGLED;
   public static final CyclingOption<Boolean> SPRINT_TOGGLED;
   public static final CyclingOption<Boolean> TOUCHSCREEN;
   public static final CyclingOption<Boolean> FULLSCREEN;
   public static final CyclingOption<Boolean> VIEW_BOBBING;
   private static final Text MONOCHROME_LOGO_TOOLTIP;
   public static final CyclingOption<Boolean> MONOCHROME_LOGO;
   private final Text key;

   public Option(String key) {
      this.key = new TranslatableText(key);
   }

   public abstract ClickableWidget createButton(GameOptions options, int x, int y, int width);

   protected Text getDisplayPrefix() {
      return this.key;
   }

   protected Text getPixelLabel(int pixel) {
      return new TranslatableText("options.pixel_value", new Object[]{this.getDisplayPrefix(), pixel});
   }

   protected Text getPercentLabel(double proportion) {
      return new TranslatableText("options.percent_value", new Object[]{this.getDisplayPrefix(), (int)(proportion * 100.0D)});
   }

   protected Text getPercentAdditionLabel(int percentage) {
      return new TranslatableText("options.percent_add_value", new Object[]{this.getDisplayPrefix(), percentage});
   }

   protected Text getGenericLabel(Text value) {
      return new TranslatableText("options.generic_value", new Object[]{this.getDisplayPrefix(), value});
   }

   protected Text getGenericLabel(int value) {
      return this.getGenericLabel(new LiteralText(Integer.toString(value)));
   }

   static {
      FABULOUS_GRAPHICS_TOOLTIP = new TranslatableText("options.graphics.fabulous.tooltip", new Object[]{(new TranslatableText("options.graphics.fabulous")).formatted(Formatting.ITALIC)});
      FANCY_GRAPHICS_TOOLTIP = new TranslatableText("options.graphics.fancy.tooltip");
      GRAPHICS = CyclingOption.create("options.graphics", Arrays.asList(GraphicsMode.values()), (List)Stream.of(GraphicsMode.values()).filter((graphicsMode) -> {
         return graphicsMode != GraphicsMode.FABULOUS;
      }).collect(Collectors.toList()), () -> {
         return MinecraftClient.getInstance().getVideoWarningManager().hasCancelledAfterWarning();
      }, (graphicsMode) -> {
         MutableText mutableText = new TranslatableText(graphicsMode.getTranslationKey());
         return (Text)(graphicsMode == GraphicsMode.FABULOUS ? mutableText.formatted(Formatting.ITALIC) : mutableText);
      }, (gameOptions) -> {
         return gameOptions.graphicsMode;
      }, (gameOptions, option, graphicsMode) -> {
         MinecraftClient minecraftClient = MinecraftClient.getInstance();
         VideoWarningManager videoWarningManager = minecraftClient.getVideoWarningManager();
         if (graphicsMode == GraphicsMode.FABULOUS && videoWarningManager.canWarn()) {
            videoWarningManager.scheduleWarning();
         } else {
            gameOptions.graphicsMode = graphicsMode;
            minecraftClient.worldRenderer.reload();
         }
      }).tooltip((client) -> {
         List<OrderedText> list = client.textRenderer.wrapLines(FAST_GRAPHICS_TOOLTIP, 200);
         List<OrderedText> list2 = client.textRenderer.wrapLines(FANCY_GRAPHICS_TOOLTIP, 200);
         List<OrderedText> list3 = client.textRenderer.wrapLines(FABULOUS_GRAPHICS_TOOLTIP, 200);
         return (graphicsMode) -> {
            switch(graphicsMode) {
            case FANCY:
               return list2;
            case FAST:
               return list;
            case FABULOUS:
               return list3;
            default:
               return ImmutableList.of();
            }
         };
      });
      GUI_SCALE = CyclingOption.create("options.guiScale", () -> {
         return (List)IntStream.rangeClosed(0, MinecraftClient.getInstance().getWindow().calculateScaleFactor(0, MinecraftClient.getInstance().forcesUnicodeFont())).boxed().collect(Collectors.toList());
      }, (guiScale) -> {
         return (Text)(guiScale == 0 ? new TranslatableText("options.guiScale.auto") : new LiteralText(Integer.toString(guiScale)));
      }, (gameOptions) -> {
         return gameOptions.guiScale;
      }, (gameOptions, option, guiScale) -> {
         gameOptions.guiScale = guiScale;
      });
      MAIN_HAND = CyclingOption.create("options.mainHand", (Object[])Arm.values(), (Function)(Arm::getOptionName), (gameOptions) -> {
         return gameOptions.mainArm;
      }, (gameOptions, option, mainArm) -> {
         gameOptions.mainArm = mainArm;
         gameOptions.sendClientSettings();
      });
      NARRATOR = CyclingOption.create("options.narrator", (Object[])NarratorMode.values(), (Function)((narrator) -> {
         return (Text)(NarratorManager.INSTANCE.isActive() ? narrator.getName() : new TranslatableText("options.narrator.notavailable"));
      }), (gameOptions) -> {
         return gameOptions.narrator;
      }, (gameOptions, option, narrator) -> {
         gameOptions.narrator = narrator;
         NarratorManager.INSTANCE.addToast(narrator);
      });
      PARTICLES = CyclingOption.create("options.particles", (Object[])ParticlesMode.values(), (Function)((particlesMode) -> {
         return new TranslatableText(particlesMode.getTranslationKey());
      }), (gameOptions) -> {
         return gameOptions.particles;
      }, (gameOptions, option, particlesMode) -> {
         gameOptions.particles = particlesMode;
      });
      CLOUDS = CyclingOption.create("options.renderClouds", (Object[])CloudRenderMode.values(), (Function)((cloudRenderMode) -> {
         return new TranslatableText(cloudRenderMode.getTranslationKey());
      }), (gameOptions) -> {
         return gameOptions.cloudRenderMode;
      }, (gameOptions, option, cloudRenderMode) -> {
         gameOptions.cloudRenderMode = cloudRenderMode;
         if (MinecraftClient.isFabulousGraphicsOrBetter()) {
            Framebuffer framebuffer = MinecraftClient.getInstance().worldRenderer.getCloudsFramebuffer();
            if (framebuffer != null) {
               framebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);
            }
         }

      });
      TEXT_BACKGROUND = CyclingOption.create("options.accessibility.text_background", (Text)(new TranslatableText("options.accessibility.text_background.chat")), (Text)(new TranslatableText("options.accessibility.text_background.everywhere")), (gameOptions) -> {
         return gameOptions.backgroundForChatOnly;
      }, (gameOptions, option, backgroundForChatOnly) -> {
         gameOptions.backgroundForChatOnly = backgroundForChatOnly;
      });
      HIDE_MATCHED_NAMES_TOOLTIP = new TranslatableText("options.hideMatchedNames.tooltip");
      AUTO_JUMP = CyclingOption.create("options.autoJump", (gameOptions) -> {
         return gameOptions.autoJump;
      }, (gameOptions, option, autoJump) -> {
         gameOptions.autoJump = autoJump;
      });
      AUTO_SUGGESTIONS = CyclingOption.create("options.autoSuggestCommands", (gameOptions) -> {
         return gameOptions.autoSuggestions;
      }, (gameOptions, option, autoSuggestions) -> {
         gameOptions.autoSuggestions = autoSuggestions;
      });
      CHAT_COLOR = CyclingOption.create("options.chat.color", (gameOptions) -> {
         return gameOptions.chatColors;
      }, (gameOptions, option, chatColors) -> {
         gameOptions.chatColors = chatColors;
      });
      HIDE_MATCHED_NAMES = CyclingOption.create("options.hideMatchedNames", HIDE_MATCHED_NAMES_TOOLTIP, (gameOptions) -> {
         return gameOptions.hideMatchedNames;
      }, (gameOptions, option, hideMatchedNames) -> {
         gameOptions.hideMatchedNames = hideMatchedNames;
      });
      CHAT_LINKS = CyclingOption.create("options.chat.links", (gameOptions) -> {
         return gameOptions.chatLinks;
      }, (gameOptions, option, chatLinks) -> {
         gameOptions.chatLinks = chatLinks;
      });
      CHAT_LINKS_PROMPT = CyclingOption.create("options.chat.links.prompt", (gameOptions) -> {
         return gameOptions.chatLinksPrompt;
      }, (gameOptions, option, chatLinksPrompt) -> {
         gameOptions.chatLinksPrompt = chatLinksPrompt;
      });
      DISCRETE_MOUSE_SCROLL = CyclingOption.create("options.discrete_mouse_scroll", (gameOptions) -> {
         return gameOptions.discreteMouseScroll;
      }, (gameOptions, option, discreteMouseScroll) -> {
         gameOptions.discreteMouseScroll = discreteMouseScroll;
      });
      VSYNC = CyclingOption.create("options.vsync", (gameOptions) -> {
         return gameOptions.enableVsync;
      }, (gameOptions, option, enableVsync) -> {
         gameOptions.enableVsync = enableVsync;
         if (MinecraftClient.getInstance().getWindow() != null) {
            MinecraftClient.getInstance().getWindow().setVsync(gameOptions.enableVsync);
         }

      });
      ENTITY_SHADOWS = CyclingOption.create("options.entityShadows", (gameOptions) -> {
         return gameOptions.entityShadows;
      }, (gameOptions, option, entityShadows) -> {
         gameOptions.entityShadows = entityShadows;
      });
      FORCE_UNICODE_FONT = CyclingOption.create("options.forceUnicodeFont", (gameOptions) -> {
         return gameOptions.forceUnicodeFont;
      }, (gameOptions, option, forceUnicodeFont) -> {
         gameOptions.forceUnicodeFont = forceUnicodeFont;
         MinecraftClient minecraftClient = MinecraftClient.getInstance();
         if (minecraftClient.getWindow() != null) {
            minecraftClient.initFont(forceUnicodeFont);
         }

      });
      INVERT_MOUSE = CyclingOption.create("options.invertMouse", (gameOptions) -> {
         return gameOptions.invertYMouse;
      }, (gameOptions, option, invertYMouse) -> {
         gameOptions.invertYMouse = invertYMouse;
      });
      REALMS_NOTIFICATIONS = CyclingOption.create("options.realmsNotifications", (gameOptions) -> {
         return gameOptions.realmsNotifications;
      }, (gameOptions, option, realmsNotifications) -> {
         gameOptions.realmsNotifications = realmsNotifications;
      });
      REDUCED_DEBUG_INFO = CyclingOption.create("options.reducedDebugInfo", (gameOptions) -> {
         return gameOptions.reducedDebugInfo;
      }, (gameOptions, option, reducedDebugInfo) -> {
         gameOptions.reducedDebugInfo = reducedDebugInfo;
      });
      SUBTITLES = CyclingOption.create("options.showSubtitles", (gameOptions) -> {
         return gameOptions.showSubtitles;
      }, (gameOptions, option, showSubtitles) -> {
         gameOptions.showSubtitles = showSubtitles;
      });
      SNOOPER = CyclingOption.create("options.snooper", (gameOptions) -> {
         if (gameOptions.snooperEnabled) {
         }

         return false;
      }, (gameOptions, option, snooperEnabled) -> {
         gameOptions.snooperEnabled = snooperEnabled;
      });
      TOGGLE_TEXT = new TranslatableText("options.key.toggle");
      HOLD_TEXT = new TranslatableText("options.key.hold");
      SNEAK_TOGGLED = CyclingOption.create("key.sneak", TOGGLE_TEXT, HOLD_TEXT, (gameOptions) -> {
         return gameOptions.sneakToggled;
      }, (gameOptions, option, sneakToggled) -> {
         gameOptions.sneakToggled = sneakToggled;
      });
      SPRINT_TOGGLED = CyclingOption.create("key.sprint", TOGGLE_TEXT, HOLD_TEXT, (gameOptions) -> {
         return gameOptions.sprintToggled;
      }, (gameOptions, option, sprintToggled) -> {
         gameOptions.sprintToggled = sprintToggled;
      });
      TOUCHSCREEN = CyclingOption.create("options.touchscreen", (gameOptions) -> {
         return gameOptions.touchscreen;
      }, (gameOptions, option, touchscreen) -> {
         gameOptions.touchscreen = touchscreen;
      });
      FULLSCREEN = CyclingOption.create("options.fullscreen", (gameOptions) -> {
         return gameOptions.fullscreen;
      }, (gameOptions, option, fullscreen) -> {
         gameOptions.fullscreen = fullscreen;
         MinecraftClient minecraftClient = MinecraftClient.getInstance();
         if (minecraftClient.getWindow() != null && minecraftClient.getWindow().isFullscreen() != gameOptions.fullscreen) {
            minecraftClient.getWindow().toggleFullscreen();
            gameOptions.fullscreen = minecraftClient.getWindow().isFullscreen();
         }

      });
      VIEW_BOBBING = CyclingOption.create("options.viewBobbing", (gameOptions) -> {
         return gameOptions.bobView;
      }, (gameOptions, option, bobView) -> {
         gameOptions.bobView = bobView;
      });
      MONOCHROME_LOGO_TOOLTIP = new TranslatableText("options.darkMojangStudiosBackgroundColor.tooltip");
      MONOCHROME_LOGO = CyclingOption.create("options.darkMojangStudiosBackgroundColor", MONOCHROME_LOGO_TOOLTIP, (gameOptions) -> {
         return gameOptions.monochromeLogo;
      }, (gameOptions, option, monochromeLogo) -> {
         gameOptions.monochromeLogo = monochromeLogo;
      });
   }
}
