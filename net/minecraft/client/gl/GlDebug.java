package net.minecraft.client.gl;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.Untracker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.ARBDebugOutput;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GLDebugMessageARBCallback;
import org.lwjgl.opengl.GLDebugMessageARBCallbackI;
import org.lwjgl.opengl.GLDebugMessageCallback;
import org.lwjgl.opengl.GLDebugMessageCallbackI;
import org.lwjgl.opengl.KHRDebug;

@Environment(EnvType.CLIENT)
public class GlDebug {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final int DEBUG_MESSAGE_QUEUE_SIZE = 10;
   private static final Queue<GlDebug.DebugMessage> debugMessages = EvictingQueue.create(10);
   @Nullable
   private static volatile GlDebug.DebugMessage lastDebugMessage;
   private static final List<Integer> KHR_VERBOSITY_LEVELS = ImmutableList.of(37190, 37191, 37192, 33387);
   private static final List<Integer> ARB_VERBOSITY_LEVELS = ImmutableList.of(37190, 37191, 37192);
   private static boolean debugMessageEnabled;

   private static String unknown(int opcode) {
      return "Unknown (0x" + Integer.toHexString(opcode).toUpperCase() + ")";
   }

   public static String getSource(int opcode) {
      switch(opcode) {
      case 33350:
         return "API";
      case 33351:
         return "WINDOW SYSTEM";
      case 33352:
         return "SHADER COMPILER";
      case 33353:
         return "THIRD PARTY";
      case 33354:
         return "APPLICATION";
      case 33355:
         return "OTHER";
      default:
         return unknown(opcode);
      }
   }

   public static String getType(int opcode) {
      switch(opcode) {
      case 33356:
         return "ERROR";
      case 33357:
         return "DEPRECATED BEHAVIOR";
      case 33358:
         return "UNDEFINED BEHAVIOR";
      case 33359:
         return "PORTABILITY";
      case 33360:
         return "PERFORMANCE";
      case 33361:
         return "OTHER";
      case 33384:
         return "MARKER";
      default:
         return unknown(opcode);
      }
   }

   public static String getSeverity(int opcode) {
      switch(opcode) {
      case 33387:
         return "NOTIFICATION";
      case 37190:
         return "HIGH";
      case 37191:
         return "MEDIUM";
      case 37192:
         return "LOW";
      default:
         return unknown(opcode);
      }
   }

   private static void info(int source, int type, int id, int severity, int messageLength, long message, long l) {
      String string = GLDebugMessageCallback.getMessage(messageLength, message);
      GlDebug.DebugMessage debugMessage2;
      synchronized(debugMessages) {
         debugMessage2 = lastDebugMessage;
         if (debugMessage2 != null && debugMessage2.equals(source, type, id, severity, string)) {
            ++debugMessage2.count;
         } else {
            debugMessage2 = new GlDebug.DebugMessage(source, type, id, severity, string);
            debugMessages.add(debugMessage2);
            lastDebugMessage = debugMessage2;
         }
      }

      LOGGER.info((String)"OpenGL debug message: {}", (Object)debugMessage2);
   }

   public static List<String> collectDebugMessages() {
      synchronized(debugMessages) {
         List<String> list = Lists.newArrayListWithCapacity(debugMessages.size());
         Iterator var2 = debugMessages.iterator();

         while(var2.hasNext()) {
            GlDebug.DebugMessage debugMessage = (GlDebug.DebugMessage)var2.next();
            list.add(debugMessage + " x " + debugMessage.count);
         }

         return list;
      }
   }

   public static boolean isDebugMessageEnabled() {
      return debugMessageEnabled;
   }

   public static void enableDebug(int verbosity, boolean sync) {
      RenderSystem.assertThread(RenderSystem::isInInitPhase);
      if (verbosity > 0) {
         GLCapabilities gLCapabilities = GL.getCapabilities();
         int j;
         boolean bl2;
         if (gLCapabilities.GL_KHR_debug) {
            debugMessageEnabled = true;
            GL11.glEnable(37600);
            if (sync) {
               GL11.glEnable(33346);
            }

            for(j = 0; j < KHR_VERBOSITY_LEVELS.size(); ++j) {
               bl2 = j < verbosity;
               KHRDebug.glDebugMessageControl(4352, 4352, (Integer)KHR_VERBOSITY_LEVELS.get(j), (int[])((int[])null), bl2);
            }

            KHRDebug.glDebugMessageCallback((GLDebugMessageCallbackI)GLX.make(GLDebugMessageCallback.create(GlDebug::info), Untracker::untrack), 0L);
         } else if (gLCapabilities.GL_ARB_debug_output) {
            debugMessageEnabled = true;
            if (sync) {
               GL11.glEnable(33346);
            }

            for(j = 0; j < ARB_VERBOSITY_LEVELS.size(); ++j) {
               bl2 = j < verbosity;
               ARBDebugOutput.glDebugMessageControlARB(4352, 4352, (Integer)ARB_VERBOSITY_LEVELS.get(j), (int[])((int[])null), bl2);
            }

            ARBDebugOutput.glDebugMessageCallbackARB((GLDebugMessageARBCallbackI)GLX.make(GLDebugMessageARBCallback.create(GlDebug::info), Untracker::untrack), 0L);
         }

      }
   }

   @Environment(EnvType.CLIENT)
   static class DebugMessage {
      private final int id;
      private final int source;
      private final int type;
      private final int severity;
      private final String message;
      int count = 1;

      DebugMessage(int source, int type, int id, int severity, String message) {
         this.id = id;
         this.source = source;
         this.type = type;
         this.severity = severity;
         this.message = message;
      }

      boolean equals(int source, int type, int id, int severity, String message) {
         return type == this.type && source == this.source && id == this.id && severity == this.severity && message.equals(this.message);
      }

      public String toString() {
         int var10000 = this.id;
         return "id=" + var10000 + ", source=" + GlDebug.getSource(this.source) + ", type=" + GlDebug.getType(this.type) + ", severity=" + GlDebug.getSeverity(this.severity) + ", message='" + this.message + "'";
      }
   }
}
