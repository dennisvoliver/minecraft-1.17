package net.minecraft.nbt;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.UnmodifiableIterator;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.fluid.FluidState;
import net.minecraft.nbt.visitor.NbtOrderedStringFormatter;
import net.minecraft.nbt.visitor.NbtTextFormatter;
import net.minecraft.state.State;
import net.minecraft.state.StateManager;
import net.minecraft.text.Text;
import net.minecraft.util.ChatUtil;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.DynamicSerializableUuid;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public final class NbtHelper {
   private static final Comparator<NbtList> field_27816 = Comparator.comparingInt((nbtList) -> {
      return nbtList.getInt(1);
   }).thenComparingInt((nbtList) -> {
      return nbtList.getInt(0);
   }).thenComparingInt((nbtList) -> {
      return nbtList.getInt(2);
   });
   private static final Comparator<NbtList> field_27817 = Comparator.comparingDouble((nbtList) -> {
      return nbtList.getDouble(1);
   }).thenComparingDouble((nbtList) -> {
      return nbtList.getDouble(0);
   }).thenComparingDouble((nbtList) -> {
      return nbtList.getDouble(2);
   });
   public static final String field_33224 = "data";
   private static final char field_33225 = '{';
   private static final char field_33226 = '}';
   private static final String field_33227 = ",";
   private static final char field_33228 = ':';
   private static final Splitter COMMA_SPLITTER = Splitter.on(",");
   private static final Splitter field_27819 = Splitter.on(':').limit(2);
   private static final Logger LOGGER = LogManager.getLogger();
   private static final int field_33229 = 2;
   private static final int field_33230 = -1;

   private NbtHelper() {
   }

   @Nullable
   public static GameProfile toGameProfile(NbtCompound compound) {
      String string = null;
      UUID uUID = null;
      if (compound.contains("Name", 8)) {
         string = compound.getString("Name");
      }

      if (compound.containsUuid("Id")) {
         uUID = compound.getUuid("Id");
      }

      try {
         GameProfile gameProfile = new GameProfile(uUID, string);
         if (compound.contains("Properties", 10)) {
            NbtCompound nbtCompound = compound.getCompound("Properties");
            Iterator var5 = nbtCompound.getKeys().iterator();

            while(var5.hasNext()) {
               String string2 = (String)var5.next();
               NbtList nbtList = nbtCompound.getList(string2, 10);

               for(int i = 0; i < nbtList.size(); ++i) {
                  NbtCompound nbtCompound2 = nbtList.getCompound(i);
                  String string3 = nbtCompound2.getString("Value");
                  if (nbtCompound2.contains("Signature", 8)) {
                     gameProfile.getProperties().put(string2, new Property(string2, string3, nbtCompound2.getString("Signature")));
                  } else {
                     gameProfile.getProperties().put(string2, new Property(string2, string3));
                  }
               }
            }
         }

         return gameProfile;
      } catch (Throwable var11) {
         return null;
      }
   }

   public static NbtCompound writeGameProfile(NbtCompound compound, GameProfile profile) {
      if (!ChatUtil.isEmpty(profile.getName())) {
         compound.putString("Name", profile.getName());
      }

      if (profile.getId() != null) {
         compound.putUuid("Id", profile.getId());
      }

      if (!profile.getProperties().isEmpty()) {
         NbtCompound nbtCompound = new NbtCompound();
         Iterator var3 = profile.getProperties().keySet().iterator();

         while(var3.hasNext()) {
            String string = (String)var3.next();
            NbtList nbtList = new NbtList();

            NbtCompound nbtCompound2;
            for(Iterator var6 = profile.getProperties().get(string).iterator(); var6.hasNext(); nbtList.add(nbtCompound2)) {
               Property property = (Property)var6.next();
               nbtCompound2 = new NbtCompound();
               nbtCompound2.putString("Value", property.getValue());
               if (property.hasSignature()) {
                  nbtCompound2.putString("Signature", property.getSignature());
               }
            }

            nbtCompound.put(string, nbtList);
         }

         compound.put("Properties", nbtCompound);
      }

      return compound;
   }

   @VisibleForTesting
   public static boolean matches(@Nullable NbtElement standard, @Nullable NbtElement subject, boolean equalValue) {
      if (standard == subject) {
         return true;
      } else if (standard == null) {
         return true;
      } else if (subject == null) {
         return false;
      } else if (!standard.getClass().equals(subject.getClass())) {
         return false;
      } else if (standard instanceof NbtCompound) {
         NbtCompound nbtCompound = (NbtCompound)standard;
         NbtCompound nbtCompound2 = (NbtCompound)subject;
         Iterator var11 = nbtCompound.getKeys().iterator();

         String string;
         NbtElement nbtElement;
         do {
            if (!var11.hasNext()) {
               return true;
            }

            string = (String)var11.next();
            nbtElement = nbtCompound.get(string);
         } while(matches(nbtElement, nbtCompound2.get(string), equalValue));

         return false;
      } else if (standard instanceof NbtList && equalValue) {
         NbtList nbtList = (NbtList)standard;
         NbtList nbtList2 = (NbtList)subject;
         if (nbtList.isEmpty()) {
            return nbtList2.isEmpty();
         } else {
            for(int i = 0; i < nbtList.size(); ++i) {
               NbtElement nbtElement2 = nbtList.get(i);
               boolean bl = false;

               for(int j = 0; j < nbtList2.size(); ++j) {
                  if (matches(nbtElement2, nbtList2.get(j), equalValue)) {
                     bl = true;
                     break;
                  }
               }

               if (!bl) {
                  return false;
               }
            }

            return true;
         }
      } else {
         return standard.equals(subject);
      }
   }

   /**
    * Serializes a {@link UUID} into its equivalent NBT representation.
    * 
    * @since 20w10a
    */
   public static NbtIntArray fromUuid(UUID uuid) {
      return new NbtIntArray(DynamicSerializableUuid.toIntArray(uuid));
   }

   /**
    * Deserializes an NBT element into a {@link UUID}.
    * The NBT element's data must have the same structure as the output of {@link #fromUuid}.
    * 
    * @throws IllegalArgumentException if {@code element} is not a valid representation of a UUID
    * @since 20w10a
    */
   public static UUID toUuid(NbtElement element) {
      if (element.getNbtType() != NbtIntArray.TYPE) {
         String var10002 = NbtIntArray.TYPE.getCrashReportName();
         throw new IllegalArgumentException("Expected UUID-Tag to be of type " + var10002 + ", but found " + element.getNbtType().getCrashReportName() + ".");
      } else {
         int[] is = ((NbtIntArray)element).getIntArray();
         if (is.length != 4) {
            throw new IllegalArgumentException("Expected UUID-Array to be of length 4, but found " + is.length + ".");
         } else {
            return DynamicSerializableUuid.toUuid(is);
         }
      }
   }

   public static BlockPos toBlockPos(NbtCompound compound) {
      return new BlockPos(compound.getInt("X"), compound.getInt("Y"), compound.getInt("Z"));
   }

   public static NbtCompound fromBlockPos(BlockPos pos) {
      NbtCompound nbtCompound = new NbtCompound();
      nbtCompound.putInt("X", pos.getX());
      nbtCompound.putInt("Y", pos.getY());
      nbtCompound.putInt("Z", pos.getZ());
      return nbtCompound;
   }

   public static BlockState toBlockState(NbtCompound compound) {
      if (!compound.contains("Name", 8)) {
         return Blocks.AIR.getDefaultState();
      } else {
         Block block = (Block)Registry.BLOCK.get(new Identifier(compound.getString("Name")));
         BlockState blockState = block.getDefaultState();
         if (compound.contains("Properties", 10)) {
            NbtCompound nbtCompound = compound.getCompound("Properties");
            StateManager<Block, BlockState> stateManager = block.getStateManager();
            Iterator var5 = nbtCompound.getKeys().iterator();

            while(var5.hasNext()) {
               String string = (String)var5.next();
               net.minecraft.state.property.Property<?> property = stateManager.getProperty(string);
               if (property != null) {
                  blockState = (BlockState)withProperty(blockState, property, string, nbtCompound, compound);
               }
            }
         }

         return blockState;
      }
   }

   private static <S extends State<?, S>, T extends Comparable<T>> S withProperty(S state, net.minecraft.state.property.Property<T> property, String key, NbtCompound properties, NbtCompound root) {
      Optional<T> optional = property.parse(properties.getString(key));
      if (optional.isPresent()) {
         return (State)state.with(property, (Comparable)optional.get());
      } else {
         LOGGER.warn((String)"Unable to read property: {} with value: {} for blockstate: {}", (Object)key, properties.getString(key), root.toString());
         return state;
      }
   }

   public static NbtCompound fromBlockState(BlockState state) {
      NbtCompound nbtCompound = new NbtCompound();
      nbtCompound.putString("Name", Registry.BLOCK.getId(state.getBlock()).toString());
      ImmutableMap<net.minecraft.state.property.Property<?>, Comparable<?>> immutableMap = state.getEntries();
      if (!immutableMap.isEmpty()) {
         NbtCompound nbtCompound2 = new NbtCompound();
         UnmodifiableIterator var4 = immutableMap.entrySet().iterator();

         while(var4.hasNext()) {
            Entry<net.minecraft.state.property.Property<?>, Comparable<?>> entry = (Entry)var4.next();
            net.minecraft.state.property.Property<?> property = (net.minecraft.state.property.Property)entry.getKey();
            nbtCompound2.putString(property.getName(), nameValue(property, (Comparable)entry.getValue()));
         }

         nbtCompound.put("Properties", nbtCompound2);
      }

      return nbtCompound;
   }

   public static NbtCompound method_36115(FluidState fluidState) {
      NbtCompound nbtCompound = new NbtCompound();
      nbtCompound.putString("Name", Registry.FLUID.getId(fluidState.getFluid()).toString());
      ImmutableMap<net.minecraft.state.property.Property<?>, Comparable<?>> immutableMap = fluidState.getEntries();
      if (!immutableMap.isEmpty()) {
         NbtCompound nbtCompound2 = new NbtCompound();
         UnmodifiableIterator var4 = immutableMap.entrySet().iterator();

         while(var4.hasNext()) {
            Entry<net.minecraft.state.property.Property<?>, Comparable<?>> entry = (Entry)var4.next();
            net.minecraft.state.property.Property<?> property = (net.minecraft.state.property.Property)entry.getKey();
            nbtCompound2.putString(property.getName(), nameValue(property, (Comparable)entry.getValue()));
         }

         nbtCompound.put("Properties", nbtCompound2);
      }

      return nbtCompound;
   }

   private static <T extends Comparable<T>> String nameValue(net.minecraft.state.property.Property<T> property, Comparable<?> value) {
      return property.name(value);
   }

   public static String method_36118(NbtElement nbtElement) {
      return method_36117(nbtElement, false);
   }

   public static String method_36117(NbtElement nbtElement, boolean bl) {
      return method_36116(new StringBuilder(), nbtElement, 0, bl).toString();
   }

   public static StringBuilder method_36116(StringBuilder stringBuilder, NbtElement nbtElement, int i, boolean bl) {
      int var9;
      int x;
      int o;
      int k;
      String string2;
      int r;
      switch(nbtElement.getType()) {
      case 0:
         break;
      case 1:
      case 2:
      case 3:
      case 4:
      case 5:
      case 6:
      case 8:
         stringBuilder.append(nbtElement);
         break;
      case 7:
         NbtByteArray nbtByteArray = (NbtByteArray)nbtElement;
         byte[] bs = nbtByteArray.getByteArray();
         o = bs.length;
         method_36114(i, stringBuilder).append("byte[").append(o).append("] {\n");
         if (!bl) {
            method_36114(i + 1, stringBuilder).append(" // Skipped, supply withBinaryBlobs true");
         } else {
            method_36114(i + 1, stringBuilder);

            for(k = 0; k < bs.length; ++k) {
               if (k != 0) {
                  stringBuilder.append(',');
               }

               if (k % 16 == 0 && k / 16 > 0) {
                  stringBuilder.append('\n');
                  if (k < bs.length) {
                     method_36114(i + 1, stringBuilder);
                  }
               } else if (k != 0) {
                  stringBuilder.append(' ');
               }

               stringBuilder.append(String.format("0x%02X", bs[k] & 255));
            }
         }

         stringBuilder.append('\n');
         method_36114(i, stringBuilder).append('}');
         break;
      case 9:
         NbtList nbtList = (NbtList)nbtElement;
         int l = nbtList.size();
         int m = nbtList.getHeldType();
         string2 = m == 0 ? "undefined" : NbtTypes.byId(m).getCommandFeedbackName();
         method_36114(i, stringBuilder).append("list<").append(string2).append(">[").append(l).append("] [");
         if (l != 0) {
            stringBuilder.append('\n');
         }

         for(r = 0; r < l; ++r) {
            if (r != 0) {
               stringBuilder.append(",\n");
            }

            method_36114(i + 1, stringBuilder);
            method_36116(stringBuilder, nbtList.get(r), i + 1, bl);
         }

         if (l != 0) {
            stringBuilder.append('\n');
         }

         method_36114(i, stringBuilder).append(']');
         break;
      case 10:
         NbtCompound nbtCompound = (NbtCompound)nbtElement;
         List<String> list = Lists.newArrayList((Iterable)nbtCompound.getKeys());
         Collections.sort(list);
         method_36114(i, stringBuilder).append('{');
         if (stringBuilder.length() - stringBuilder.lastIndexOf("\n") > 2 * (i + 1)) {
            stringBuilder.append('\n');
            method_36114(i + 1, stringBuilder);
         }

         o = list.stream().mapToInt(String::length).max().orElse(0);
         string2 = Strings.repeat(" ", o);

         for(r = 0; r < list.size(); ++r) {
            if (r != 0) {
               stringBuilder.append(",\n");
            }

            String string3 = (String)list.get(r);
            method_36114(i + 1, stringBuilder).append('"').append(string3).append('"').append(string2, 0, string2.length() - string3.length()).append(": ");
            method_36116(stringBuilder, nbtCompound.get(string3), i + 1, bl);
         }

         if (!list.isEmpty()) {
            stringBuilder.append('\n');
         }

         method_36114(i, stringBuilder).append('}');
         break;
      case 11:
         NbtIntArray nbtIntArray = (NbtIntArray)nbtElement;
         int[] is = nbtIntArray.getIntArray();
         o = 0;
         int[] var7 = is;
         r = is.length;

         for(var9 = 0; var9 < r; ++var9) {
            x = var7[var9];
            o = Math.max(o, String.format("%X", x).length());
         }

         k = is.length;
         method_36114(i, stringBuilder).append("int[").append(k).append("] {\n");
         if (!bl) {
            method_36114(i + 1, stringBuilder).append(" // Skipped, supply withBinaryBlobs true");
         } else {
            method_36114(i + 1, stringBuilder);

            for(r = 0; r < is.length; ++r) {
               if (r != 0) {
                  stringBuilder.append(',');
               }

               if (r % 16 == 0 && r / 16 > 0) {
                  stringBuilder.append('\n');
                  if (r < is.length) {
                     method_36114(i + 1, stringBuilder);
                  }
               } else if (r != 0) {
                  stringBuilder.append(' ');
               }

               stringBuilder.append(String.format("0x%0" + o + "X", is[r]));
            }
         }

         stringBuilder.append('\n');
         method_36114(i, stringBuilder).append('}');
         break;
      case 12:
         NbtLongArray nbtLongArray = (NbtLongArray)nbtElement;
         long[] ls = nbtLongArray.getLongArray();
         long u = 0L;
         long[] var8 = ls;
         var9 = ls.length;

         for(x = 0; x < var9; ++x) {
            long v = var8[x];
            u = Math.max(u, (long)String.format("%X", v).length());
         }

         long w = (long)ls.length;
         method_36114(i, stringBuilder).append("long[").append(w).append("] {\n");
         if (!bl) {
            method_36114(i + 1, stringBuilder).append(" // Skipped, supply withBinaryBlobs true");
         } else {
            method_36114(i + 1, stringBuilder);

            for(x = 0; x < ls.length; ++x) {
               if (x != 0) {
                  stringBuilder.append(',');
               }

               if (x % 16 == 0 && x / 16 > 0) {
                  stringBuilder.append('\n');
                  if (x < ls.length) {
                     method_36114(i + 1, stringBuilder);
                  }
               } else if (x != 0) {
                  stringBuilder.append(' ');
               }

               stringBuilder.append(String.format("0x%0" + u + "X", ls[x]));
            }
         }

         stringBuilder.append('\n');
         method_36114(i, stringBuilder).append('}');
         break;
      default:
         stringBuilder.append("<UNKNOWN :(>");
      }

      return stringBuilder;
   }

   private static StringBuilder method_36114(int i, StringBuilder stringBuilder) {
      int j = stringBuilder.lastIndexOf("\n") + 1;
      int k = stringBuilder.length() - j;

      for(int l = 0; l < 2 * i - k; ++l) {
         stringBuilder.append(' ');
      }

      return stringBuilder;
   }

   /**
    * Uses the data fixer to update an NBT compound object to the latest data version.
    * 
    * @param fixer the data fixer
    * @param fixTypes the fix types
    * @param compound the NBT compound object to fix
    * @param oldVersion the data version of the NBT compound object
    */
   public static NbtCompound update(DataFixer fixer, DataFixTypes fixTypes, NbtCompound compound, int oldVersion) {
      return update(fixer, fixTypes, compound, oldVersion, SharedConstants.getGameVersion().getWorldVersion());
   }

   /**
    * Uses the data fixer to update an NBT compound object.
    * 
    * @param fixer the data fixer
    * @param fixTypes the fix types
    * @param compound the NBT compound object to fix
    * @param oldVersion the data version of the NBT compound object
    * @param targetVersion the data version to update the NBT compound object to
    */
   public static NbtCompound update(DataFixer fixer, DataFixTypes fixTypes, NbtCompound compound, int oldVersion, int targetVersion) {
      return (NbtCompound)fixer.update(fixTypes.getTypeReference(), new Dynamic(NbtOps.INSTANCE, compound), oldVersion, targetVersion).getValue();
   }

   public static Text toPrettyPrintedText(NbtElement element) {
      return (new NbtTextFormatter("", 0)).apply(element);
   }

   public static String toPrettyPrintedString(NbtCompound compound) {
      return (new NbtOrderedStringFormatter()).apply(method_32273(compound));
   }

   public static NbtCompound method_32260(String string) throws CommandSyntaxException {
      return method_32275(StringNbtReader.parse(string));
   }

   @VisibleForTesting
   static NbtCompound method_32273(NbtCompound compound) {
      boolean bl = compound.contains("palettes", 9);
      NbtList nbtList2;
      if (bl) {
         nbtList2 = compound.getList("palettes", 9).getList(0);
      } else {
         nbtList2 = compound.getList("palette", 10);
      }

      Stream var10000 = nbtList2.stream();
      Objects.requireNonNull(NbtCompound.class);
      NbtList nbtList3 = (NbtList)var10000.map(NbtCompound.class::cast).map(NbtHelper::method_32277).map(NbtString::of).collect(Collectors.toCollection(NbtList::new));
      compound.put("palette", nbtList3);
      NbtList nbtList8;
      NbtList nbtList7;
      if (bl) {
         nbtList8 = new NbtList();
         nbtList7 = compound.getList("palettes", 9);
         var10000 = nbtList7.stream();
         Objects.requireNonNull(NbtList.class);
         var10000.map(NbtList.class::cast).forEach((nbtList3x) -> {
            NbtCompound nbtCompound = new NbtCompound();

            for(int i = 0; i < nbtList3x.size(); ++i) {
               nbtCompound.putString(nbtList3.getString(i), method_32277(nbtList3x.getCompound(i)));
            }

            nbtList8.add(nbtCompound);
         });
         compound.put("palettes", nbtList8);
      }

      if (compound.contains("entities", 10)) {
         nbtList8 = compound.getList("entities", 10);
         var10000 = nbtList8.stream();
         Objects.requireNonNull(NbtCompound.class);
         nbtList7 = (NbtList)var10000.map(NbtCompound.class::cast).sorted(Comparator.comparing((nbtCompound) -> {
            return nbtCompound.getList("pos", 6);
         }, field_27817)).collect(Collectors.toCollection(NbtList::new));
         compound.put("entities", nbtList7);
      }

      var10000 = compound.getList("blocks", 10).stream();
      Objects.requireNonNull(NbtCompound.class);
      nbtList8 = (NbtList)var10000.map(NbtCompound.class::cast).sorted(Comparator.comparing((nbtCompound) -> {
         return nbtCompound.getList("pos", 3);
      }, field_27816)).peek((nbtCompound) -> {
         nbtCompound.putString("state", nbtList3.getString(nbtCompound.getInt("state")));
      }).collect(Collectors.toCollection(NbtList::new));
      compound.put("data", nbtList8);
      compound.remove("blocks");
      return compound;
   }

   @VisibleForTesting
   static NbtCompound method_32275(NbtCompound compound) {
      NbtList nbtList = compound.getList("palette", 8);
      Stream var10000 = nbtList.stream();
      Objects.requireNonNull(NbtString.class);
      Map<String, NbtElement> map = (Map)var10000.map(NbtString.class::cast).map(NbtString::asString).collect(ImmutableMap.toImmutableMap(Function.identity(), NbtHelper::method_32267));
      if (compound.contains("palettes", 9)) {
         Stream var10002 = compound.getList("palettes", 10).stream();
         Objects.requireNonNull(NbtCompound.class);
         compound.put("palettes", (NbtElement)var10002.map(NbtCompound.class::cast).map((nbtCompoundx) -> {
            Stream var10000 = map.keySet().stream();
            Objects.requireNonNull(nbtCompoundx);
            return (NbtList)var10000.map(nbtCompoundx::getString).map(NbtHelper::method_32267).collect(Collectors.toCollection(NbtList::new));
         }).collect(Collectors.toCollection(NbtList::new)));
         compound.remove("palette");
      } else {
         compound.put("palette", (NbtElement)map.values().stream().collect(Collectors.toCollection(NbtList::new)));
      }

      if (compound.contains("data", 9)) {
         Object2IntMap<String> object2IntMap = new Object2IntOpenHashMap();
         object2IntMap.defaultReturnValue(-1);

         for(int i = 0; i < nbtList.size(); ++i) {
            object2IntMap.put(nbtList.getString(i), i);
         }

         NbtList nbtList2 = compound.getList("data", 10);

         for(int j = 0; j < nbtList2.size(); ++j) {
            NbtCompound nbtCompound = nbtList2.getCompound(j);
            String string = nbtCompound.getString("state");
            int k = object2IntMap.getInt(string);
            if (k == -1) {
               throw new IllegalStateException("Entry " + string + " missing from palette");
            }

            nbtCompound.putInt("state", k);
         }

         compound.put("blocks", nbtList2);
         compound.remove("data");
      }

      return compound;
   }

   @VisibleForTesting
   static String method_32277(NbtCompound compound) {
      StringBuilder stringBuilder = new StringBuilder(compound.getString("Name"));
      if (compound.contains("Properties", 10)) {
         NbtCompound nbtCompound = compound.getCompound("Properties");
         String string = (String)nbtCompound.getKeys().stream().sorted().map((stringx) -> {
            return stringx + ":" + nbtCompound.get(stringx).asString();
         }).collect(Collectors.joining(","));
         stringBuilder.append('{').append(string).append('}');
      }

      return stringBuilder.toString();
   }

   @VisibleForTesting
   static NbtCompound method_32267(String string) {
      NbtCompound nbtCompound = new NbtCompound();
      int i = string.indexOf(123);
      String string4;
      if (i >= 0) {
         string4 = string.substring(0, i);
         NbtCompound nbtCompound2 = new NbtCompound();
         if (i + 2 <= string.length()) {
            String string3 = string.substring(i + 1, string.indexOf(125, i));
            COMMA_SPLITTER.split(string3).forEach((string2) -> {
               List<String> list = field_27819.splitToList(string2);
               if (list.size() == 2) {
                  nbtCompound2.putString((String)list.get(0), (String)list.get(1));
               } else {
                  LOGGER.error((String)"Something went wrong parsing: '{}' -- incorrect gamedata!", (Object)string);
               }

            });
            nbtCompound.put("Properties", nbtCompound2);
         }
      } else {
         string4 = string;
      }

      nbtCompound.putString("Name", string4);
      return nbtCompound;
   }
}
