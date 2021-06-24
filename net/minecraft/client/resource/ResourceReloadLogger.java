package net.minecraft.client.resource;

import com.google.common.collect.ImmutableList;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resource.ResourcePack;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class ResourceReloadLogger {
   private static final Logger LOGGER = LogManager.getLogger();
   @Nullable
   private ResourceReloadLogger.ReloadState reloadState;
   private int reloadCount;

   public void reload(ResourceReloadLogger.ReloadReason reason, List<ResourcePack> packs) {
      ++this.reloadCount;
      if (this.reloadState != null && !this.reloadState.finished) {
         LOGGER.warn("Reload already ongoing, replacing");
      }

      this.reloadState = new ResourceReloadLogger.ReloadState(reason, (List)packs.stream().map(ResourcePack::getName).collect(ImmutableList.toImmutableList()));
   }

   public void recover(Throwable throwable) {
      if (this.reloadState == null) {
         LOGGER.warn("Trying to signal reload recovery, but nothing was started");
         this.reloadState = new ResourceReloadLogger.ReloadState(ResourceReloadLogger.ReloadReason.UNKNOWN, ImmutableList.of());
      }

      this.reloadState.recovery = new ResourceReloadLogger.RecoveryEntry(throwable);
   }

   public void finish() {
      if (this.reloadState == null) {
         LOGGER.warn("Trying to finish reload, but nothing was started");
      } else {
         this.reloadState.finished = true;
      }

   }

   public void addReloadSection(CrashReport report) {
      CrashReportSection crashReportSection = report.addElement("Last reload");
      crashReportSection.add("Reload number", (Object)this.reloadCount);
      if (this.reloadState != null) {
         this.reloadState.addReloadSection(crashReportSection);
      }

   }

   @Environment(EnvType.CLIENT)
   static class ReloadState {
      private final ResourceReloadLogger.ReloadReason reason;
      private final List<String> packs;
      @Nullable
      ResourceReloadLogger.RecoveryEntry recovery;
      boolean finished;

      ReloadState(ResourceReloadLogger.ReloadReason reason, List<String> packs) {
         this.reason = reason;
         this.packs = packs;
      }

      public void addReloadSection(CrashReportSection section) {
         section.add("Reload reason", (Object)this.reason.name);
         section.add("Finished", (Object)(this.finished ? "Yes" : "No"));
         section.add("Packs", () -> {
            return String.join(", ", this.packs);
         });
         if (this.recovery != null) {
            this.recovery.addRecoverySection(section);
         }

      }
   }

   @Environment(EnvType.CLIENT)
   public static enum ReloadReason {
      INITIAL("initial"),
      MANUAL("manual"),
      UNKNOWN("unknown");

      final String name;

      private ReloadReason(String name) {
         this.name = name;
      }
   }

   @Environment(EnvType.CLIENT)
   private static class RecoveryEntry {
      private final Throwable throwable;

      RecoveryEntry(Throwable throwable) {
         this.throwable = throwable;
      }

      public void addRecoverySection(CrashReportSection section) {
         section.add("Recovery", (Object)"Yes");
         section.add("Recovery reason", () -> {
            StringWriter stringWriter = new StringWriter();
            this.throwable.printStackTrace(new PrintWriter(stringWriter));
            return stringWriter.toString();
         });
      }
   }
}
