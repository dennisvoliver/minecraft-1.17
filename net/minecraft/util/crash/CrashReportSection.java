package net.minecraft.util.crash;

import com.google.common.collect.Lists;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.HeightLimitView;
import org.jetbrains.annotations.Nullable;

public class CrashReportSection {
   private final String title;
   private final List<CrashReportSection.Element> elements = Lists.newArrayList();
   private StackTraceElement[] stackTrace = new StackTraceElement[0];

   public CrashReportSection(String title) {
      this.title = title;
   }

   public static String createPositionString(HeightLimitView world, double x, double y, double z) {
      return String.format(Locale.ROOT, "%.2f,%.2f,%.2f - %s", x, y, z, createPositionString(world, new BlockPos(x, y, z)));
   }

   public static String createPositionString(HeightLimitView world, BlockPos pos) {
      return createPositionString(world, pos.getX(), pos.getY(), pos.getZ());
   }

   public static String createPositionString(HeightLimitView world, int x, int y, int z) {
      StringBuilder stringBuilder = new StringBuilder();

      try {
         stringBuilder.append(String.format("World: (%d,%d,%d)", x, y, z));
      } catch (Throwable var19) {
         stringBuilder.append("(Error finding world loc)");
      }

      stringBuilder.append(", ");

      int u;
      int v;
      int w;
      int aa;
      int ab;
      int ac;
      int ad;
      int ae;
      int af;
      int ag;
      int ah;
      int ai;
      try {
         u = ChunkSectionPos.getSectionCoord(x);
         v = ChunkSectionPos.getSectionCoord(y);
         w = ChunkSectionPos.getSectionCoord(z);
         aa = x & 15;
         ab = y & 15;
         ac = z & 15;
         ad = ChunkSectionPos.getBlockCoord(u);
         ae = world.getBottomY();
         af = ChunkSectionPos.getBlockCoord(w);
         ag = ChunkSectionPos.getBlockCoord(u + 1) - 1;
         ah = world.getTopY() - 1;
         ai = ChunkSectionPos.getBlockCoord(w + 1) - 1;
         stringBuilder.append(String.format("Section: (at %d,%d,%d in %d,%d,%d; chunk contains blocks %d,%d,%d to %d,%d,%d)", aa, ab, ac, u, v, w, ad, ae, af, ag, ah, ai));
      } catch (Throwable var18) {
         stringBuilder.append("(Error finding chunk loc)");
      }

      stringBuilder.append(", ");

      try {
         u = x >> 9;
         v = z >> 9;
         w = u << 5;
         aa = v << 5;
         ab = (u + 1 << 5) - 1;
         ac = (v + 1 << 5) - 1;
         ad = u << 9;
         ae = world.getBottomY();
         af = v << 9;
         ag = (u + 1 << 9) - 1;
         ah = world.getTopY() - 1;
         ai = (v + 1 << 9) - 1;
         stringBuilder.append(String.format("Region: (%d,%d; contains chunks %d,%d to %d,%d, blocks %d,%d,%d to %d,%d,%d)", u, v, w, aa, ab, ac, ad, ae, af, ag, ah, ai));
      } catch (Throwable var17) {
         stringBuilder.append("(Error finding world loc)");
      }

      return stringBuilder.toString();
   }

   public CrashReportSection add(String name, CrashCallable<String> crashCallable) {
      try {
         this.add(name, crashCallable.call());
      } catch (Throwable var4) {
         this.add(name, var4);
      }

      return this;
   }

   public CrashReportSection add(String name, Object detail) {
      this.elements.add(new CrashReportSection.Element(name, detail));
      return this;
   }

   public void add(String name, Throwable throwable) {
      this.add(name, (Object)throwable);
   }

   public int initStackTrace(int ignoredCallCount) {
      StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
      if (stackTraceElements.length <= 0) {
         return 0;
      } else {
         this.stackTrace = new StackTraceElement[stackTraceElements.length - 3 - ignoredCallCount];
         System.arraycopy(stackTraceElements, 3 + ignoredCallCount, this.stackTrace, 0, this.stackTrace.length);
         return this.stackTrace.length;
      }
   }

   public boolean shouldGenerateStackTrace(StackTraceElement prev, StackTraceElement next) {
      if (this.stackTrace.length != 0 && prev != null) {
         StackTraceElement stackTraceElement = this.stackTrace[0];
         if (stackTraceElement.isNativeMethod() == prev.isNativeMethod() && stackTraceElement.getClassName().equals(prev.getClassName()) && stackTraceElement.getFileName().equals(prev.getFileName()) && stackTraceElement.getMethodName().equals(prev.getMethodName())) {
            if (next != null != this.stackTrace.length > 1) {
               return false;
            } else if (next != null && !this.stackTrace[1].equals(next)) {
               return false;
            } else {
               this.stackTrace[0] = prev;
               return true;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public void trimStackTraceEnd(int callCount) {
      StackTraceElement[] stackTraceElements = new StackTraceElement[this.stackTrace.length - callCount];
      System.arraycopy(this.stackTrace, 0, stackTraceElements, 0, stackTraceElements.length);
      this.stackTrace = stackTraceElements;
   }

   public void addStackTrace(StringBuilder crashReportBuilder) {
      crashReportBuilder.append("-- ").append(this.title).append(" --\n");
      crashReportBuilder.append("Details:");
      Iterator var2 = this.elements.iterator();

      while(var2.hasNext()) {
         CrashReportSection.Element element = (CrashReportSection.Element)var2.next();
         crashReportBuilder.append("\n\t");
         crashReportBuilder.append(element.getName());
         crashReportBuilder.append(": ");
         crashReportBuilder.append(element.getDetail());
      }

      if (this.stackTrace != null && this.stackTrace.length > 0) {
         crashReportBuilder.append("\nStacktrace:");
         StackTraceElement[] var6 = this.stackTrace;
         int var7 = var6.length;

         for(int var4 = 0; var4 < var7; ++var4) {
            StackTraceElement stackTraceElement = var6[var4];
            crashReportBuilder.append("\n\tat ");
            crashReportBuilder.append(stackTraceElement);
         }
      }

   }

   public StackTraceElement[] getStackTrace() {
      return this.stackTrace;
   }

   public static void addBlockInfo(CrashReportSection element, HeightLimitView world, BlockPos pos, @Nullable BlockState state) {
      if (state != null) {
         Objects.requireNonNull(state);
         element.add("Block", state::toString);
      }

      element.add("Block location", () -> {
         return createPositionString(world, pos);
      });
   }

   private static class Element {
      private final String name;
      private final String detail;

      public Element(String name, @Nullable Object detail) {
         this.name = name;
         if (detail == null) {
            this.detail = "~~NULL~~";
         } else if (detail instanceof Throwable) {
            Throwable throwable = (Throwable)detail;
            String var10001 = throwable.getClass().getSimpleName();
            this.detail = "~~ERROR~~ " + var10001 + ": " + throwable.getMessage();
         } else {
            this.detail = detail.toString();
         }

      }

      public String getName() {
         return this.name;
      }

      public String getDetail() {
         return this.detail;
      }
   }
}
