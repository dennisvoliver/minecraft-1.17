package net.minecraft.util;

import com.google.common.collect.Maps;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.SharedConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.PhysicalMemory;
import oshi.hardware.VirtualMemory;
import oshi.hardware.CentralProcessor.ProcessorIdentifier;

public class SystemDetails {
   public static final long MEBI = 1048576L;
   private static final long GIGA = 1000000000L;
   private static final Logger LOGGER = LogManager.getLogger();
   private static final String OPERATING_SYSTEM;
   private static final String JAVA_VERSION;
   private static final String JVM_VERSION;
   private final Map<String, String> sections = Maps.newLinkedHashMap();

   public SystemDetails() {
      this.addSection("Minecraft Version", SharedConstants.getGameVersion().getName());
      this.addSection("Minecraft Version ID", SharedConstants.getGameVersion().getId());
      this.addSection("Operating System", OPERATING_SYSTEM);
      this.addSection("Java Version", JAVA_VERSION);
      this.addSection("Java VM Version", JVM_VERSION);
      this.addSection("Memory", () -> {
         Runtime runtime = Runtime.getRuntime();
         long l = runtime.maxMemory();
         long m = runtime.totalMemory();
         long n = runtime.freeMemory();
         long o = l / 1048576L;
         long p = m / 1048576L;
         long q = n / 1048576L;
         return n + " bytes (" + q + " MiB) / " + m + " bytes (" + p + " MiB) up to " + l + " bytes (" + o + " MiB)";
      });
      this.addSection("CPUs", () -> {
         return String.valueOf(Runtime.getRuntime().availableProcessors());
      });
      this.tryAddGroup("hardware", () -> {
         this.addHardwareGroup(new SystemInfo());
      });
      this.addSection("JVM Flags", () -> {
         List<String> list = (List)Util.getJVMFlags().collect(Collectors.toList());
         return String.format("%d total; %s", list.size(), String.join(" ", list));
      });
   }

   public void addSection(String name, String value) {
      this.sections.put(name, value);
   }

   public void addSection(String name, Supplier<String> valueSupplier) {
      try {
         this.addSection(name, (String)valueSupplier.get());
      } catch (Exception var4) {
         LOGGER.warn((String)"Failed to get system info for {}", (Object)name, (Object)var4);
         this.addSection(name, "ERR");
      }

   }

   private void addHardwareGroup(SystemInfo systemInfo) {
      HardwareAbstractionLayer hardwareAbstractionLayer = systemInfo.getHardware();
      this.tryAddGroup("processor", () -> {
         this.addProcessorGroup(hardwareAbstractionLayer.getProcessor());
      });
      this.tryAddGroup("graphics", () -> {
         this.addGraphicsCardGroup(hardwareAbstractionLayer.getGraphicsCards());
      });
      this.tryAddGroup("memory", () -> {
         this.addGlobalMemoryGroup(hardwareAbstractionLayer.getMemory());
      });
   }

   private void tryAddGroup(String name, Runnable adder) {
      try {
         adder.run();
      } catch (Throwable var4) {
         LOGGER.warn((String)"Failed retrieving info for group {}", (Object)name, (Object)var4);
      }

   }

   private void addPhysicalMemoryGroup(List<PhysicalMemory> memories) {
      int i = 0;
      Iterator var3 = memories.iterator();

      while(var3.hasNext()) {
         PhysicalMemory physicalMemory = (PhysicalMemory)var3.next();
         String string = String.format("Memory slot #%d ", i++);
         this.addSection(string + "capacity (MB)", () -> {
            return String.format("%.2f", (float)physicalMemory.getCapacity() / 1048576.0F);
         });
         this.addSection(string + "clockSpeed (GHz)", () -> {
            return String.format("%.2f", (float)physicalMemory.getClockSpeed() / 1.0E9F);
         });
         String var6 = string + "type";
         Objects.requireNonNull(physicalMemory);
         this.addSection(var6, physicalMemory::getMemoryType);
      }

   }

   private void addVertualMemoryGroup(VirtualMemory virtualMemory) {
      this.addSection("Virtual memory max (MB)", () -> {
         return String.format("%.2f", (float)virtualMemory.getVirtualMax() / 1048576.0F);
      });
      this.addSection("Virtual memory used (MB)", () -> {
         return String.format("%.2f", (float)virtualMemory.getVirtualInUse() / 1048576.0F);
      });
      this.addSection("Swap memory total (MB)", () -> {
         return String.format("%.2f", (float)virtualMemory.getSwapTotal() / 1048576.0F);
      });
      this.addSection("Swap memory used (MB)", () -> {
         return String.format("%.2f", (float)virtualMemory.getSwapUsed() / 1048576.0F);
      });
   }

   private void addGlobalMemoryGroup(GlobalMemory globalMemory) {
      this.tryAddGroup("physical memory", () -> {
         this.addPhysicalMemoryGroup(globalMemory.getPhysicalMemory());
      });
      this.tryAddGroup("virtual memory", () -> {
         this.addVertualMemoryGroup(globalMemory.getVirtualMemory());
      });
   }

   private void addGraphicsCardGroup(List<GraphicsCard> graphicsCards) {
      int i = 0;
      Iterator var3 = graphicsCards.iterator();

      while(var3.hasNext()) {
         GraphicsCard graphicsCard = (GraphicsCard)var3.next();
         String string = String.format("Graphics card #%d ", i++);
         String var6 = string + "name";
         Objects.requireNonNull(graphicsCard);
         this.addSection(var6, graphicsCard::getName);
         var6 = string + "vendor";
         Objects.requireNonNull(graphicsCard);
         this.addSection(var6, graphicsCard::getVendor);
         this.addSection(string + "VRAM (MB)", () -> {
            return String.format("%.2f", (float)graphicsCard.getVRam() / 1048576.0F);
         });
         var6 = string + "deviceId";
         Objects.requireNonNull(graphicsCard);
         this.addSection(var6, graphicsCard::getDeviceId);
         var6 = string + "versionInfo";
         Objects.requireNonNull(graphicsCard);
         this.addSection(var6, graphicsCard::getVersionInfo);
      }

   }

   private void addProcessorGroup(CentralProcessor centralProcessor) {
      ProcessorIdentifier processorIdentifier = centralProcessor.getProcessorIdentifier();
      Objects.requireNonNull(processorIdentifier);
      this.addSection("Processor Vendor", processorIdentifier::getVendor);
      Objects.requireNonNull(processorIdentifier);
      this.addSection("Processor Name", processorIdentifier::getName);
      Objects.requireNonNull(processorIdentifier);
      this.addSection("Identifier", processorIdentifier::getIdentifier);
      Objects.requireNonNull(processorIdentifier);
      this.addSection("Microarchitecture", processorIdentifier::getMicroarchitecture);
      this.addSection("Frequency (GHz)", () -> {
         return String.format("%.2f", (float)processorIdentifier.getVendorFreq() / 1.0E9F);
      });
      this.addSection("Number of physical packages", () -> {
         return String.valueOf(centralProcessor.getPhysicalPackageCount());
      });
      this.addSection("Number of physical CPUs", () -> {
         return String.valueOf(centralProcessor.getPhysicalProcessorCount());
      });
      this.addSection("Number of logical CPUs", () -> {
         return String.valueOf(centralProcessor.getLogicalProcessorCount());
      });
   }

   public void writeTo(StringBuilder stringBuilder) {
      stringBuilder.append("-- ").append("System Details").append(" --\n");
      stringBuilder.append("Details:");
      this.sections.forEach((name, value) -> {
         stringBuilder.append("\n\t");
         stringBuilder.append(name);
         stringBuilder.append(": ");
         stringBuilder.append(value);
      });
   }

   public String collect() {
      return (String)this.sections.entrySet().stream().map((entry) -> {
         String var10000 = (String)entry.getKey();
         return var10000 + ": " + (String)entry.getValue();
      }).collect(Collectors.joining(System.lineSeparator()));
   }

   static {
      String var10000 = System.getProperty("os.name");
      OPERATING_SYSTEM = var10000 + " (" + System.getProperty("os.arch") + ") version " + System.getProperty("os.version");
      var10000 = System.getProperty("java.version");
      JAVA_VERSION = var10000 + ", " + System.getProperty("java.vendor");
      var10000 = System.getProperty("java.vm.name");
      JVM_VERSION = var10000 + " (" + System.getProperty("java.vm.info") + "), " + System.getProperty("java.vm.vendor");
   }
}
