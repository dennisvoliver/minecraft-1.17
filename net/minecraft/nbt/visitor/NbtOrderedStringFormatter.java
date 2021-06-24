package net.minecraft.nbt.visitor;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtByteArray;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtLong;
import net.minecraft.nbt.NbtLongArray;
import net.minecraft.nbt.NbtNull;
import net.minecraft.nbt.NbtShort;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.Util;

/**
 * Formats an NBT element as a multiline string where named elements inside of compound objects
 * are sorted according to a defined ordering.
 */
public class NbtOrderedStringFormatter implements NbtElementVisitor {
   /**
    * Contains the names of elements which should appear before any other element in a compound object, even
    * when they would otherwise appear later lexicographically. The list of elements which should be
    * prioritized differs depending on the path of the compound object.
    */
   private static final Map<String, List<String>> ENTRY_ORDER_OVERRIDES = (Map)Util.make(Maps.newHashMap(), (map) -> {
      map.put("{}", Lists.newArrayList((Object[])("DataVersion", "author", "size", "data", "entities", "palette", "palettes")));
      map.put("{}.data.[].{}", Lists.newArrayList((Object[])("pos", "state", "nbt")));
      map.put("{}.entities.[].{}", Lists.newArrayList((Object[])("blockPos", "pos")));
   });
   /**
    * Contains paths for which the indentation prefix should not be prepended to the result.
    */
   private static final Set<String> IGNORED_PATHS = Sets.newHashSet((Object[])("{}.size.[]", "{}.data.[].{}", "{}.palette.[].{}", "{}.entities.[].{}"));
   private static final Pattern SIMPLE_NAME = Pattern.compile("[A-Za-z0-9._+-]+");
   private static final String KEY_VALUE_SEPARATOR = String.valueOf(':');
   private static final String ENTRY_SEPARATOR = String.valueOf(',');
   private static final String field_33234 = "[";
   private static final String field_33235 = "]";
   private static final String field_33236 = ";";
   private static final String field_33237 = " ";
   private static final String field_33238 = "{";
   private static final String field_33239 = "}";
   private static final String field_33240 = "\n";
   private final String prefix;
   private final int indentationLevel;
   private final List<String> pathParts;
   private String result;

   public NbtOrderedStringFormatter() {
      this("    ", 0, Lists.newArrayList());
   }

   public NbtOrderedStringFormatter(String prefix, int indentationLevel, List<String> pathParts) {
      this.prefix = prefix;
      this.indentationLevel = indentationLevel;
      this.pathParts = pathParts;
   }

   public String apply(NbtElement element) {
      element.accept(this);
      return this.result;
   }

   public void visitString(NbtString element) {
      this.result = NbtString.escape(element.asString());
   }

   public void visitByte(NbtByte element) {
      this.result = element.numberValue() + "b";
   }

   public void visitShort(NbtShort element) {
      this.result = element.numberValue() + "s";
   }

   public void visitInt(NbtInt element) {
      this.result = String.valueOf(element.numberValue());
   }

   public void visitLong(NbtLong element) {
      this.result = element.numberValue() + "L";
   }

   public void visitFloat(NbtFloat element) {
      this.result = element.floatValue() + "f";
   }

   public void visitDouble(NbtDouble element) {
      this.result = element.doubleValue() + "d";
   }

   public void visitByteArray(NbtByteArray element) {
      StringBuilder stringBuilder = (new StringBuilder("[")).append("B").append(";");
      byte[] bs = element.getByteArray();

      for(int i = 0; i < bs.length; ++i) {
         stringBuilder.append(" ").append(bs[i]).append("B");
         if (i != bs.length - 1) {
            stringBuilder.append(ENTRY_SEPARATOR);
         }
      }

      stringBuilder.append("]");
      this.result = stringBuilder.toString();
   }

   public void visitIntArray(NbtIntArray element) {
      StringBuilder stringBuilder = (new StringBuilder("[")).append("I").append(";");
      int[] is = element.getIntArray();

      for(int i = 0; i < is.length; ++i) {
         stringBuilder.append(" ").append(is[i]);
         if (i != is.length - 1) {
            stringBuilder.append(ENTRY_SEPARATOR);
         }
      }

      stringBuilder.append("]");
      this.result = stringBuilder.toString();
   }

   public void visitLongArray(NbtLongArray element) {
      String string = "L";
      StringBuilder stringBuilder = (new StringBuilder("[")).append("L").append(";");
      long[] ls = element.getLongArray();

      for(int i = 0; i < ls.length; ++i) {
         stringBuilder.append(" ").append(ls[i]).append("L");
         if (i != ls.length - 1) {
            stringBuilder.append(ENTRY_SEPARATOR);
         }
      }

      stringBuilder.append("]");
      this.result = stringBuilder.toString();
   }

   public void visitList(NbtList element) {
      if (element.isEmpty()) {
         this.result = "[]";
      } else {
         StringBuilder stringBuilder = new StringBuilder("[");
         this.pushPathPart("[]");
         String string = IGNORED_PATHS.contains(this.joinPath()) ? "" : this.prefix;
         if (!string.isEmpty()) {
            stringBuilder.append("\n");
         }

         for(int i = 0; i < element.size(); ++i) {
            stringBuilder.append(Strings.repeat(string, this.indentationLevel + 1));
            stringBuilder.append((new NbtOrderedStringFormatter(string, this.indentationLevel + 1, this.pathParts)).apply(element.get(i)));
            if (i != element.size() - 1) {
               stringBuilder.append(ENTRY_SEPARATOR).append(string.isEmpty() ? " " : "\n");
            }
         }

         if (!string.isEmpty()) {
            stringBuilder.append("\n").append(Strings.repeat(string, this.indentationLevel));
         }

         stringBuilder.append("]");
         this.result = stringBuilder.toString();
         this.popPathPart();
      }
   }

   public void visitCompound(NbtCompound compound) {
      if (compound.isEmpty()) {
         this.result = "{}";
      } else {
         StringBuilder stringBuilder = new StringBuilder("{");
         this.pushPathPart("{}");
         String string = IGNORED_PATHS.contains(this.joinPath()) ? "" : this.prefix;
         if (!string.isEmpty()) {
            stringBuilder.append("\n");
         }

         Collection<String> collection = this.getSortedNames(compound);
         Iterator iterator = collection.iterator();

         while(iterator.hasNext()) {
            String string2 = (String)iterator.next();
            NbtElement nbtElement = compound.get(string2);
            this.pushPathPart(string2);
            stringBuilder.append(Strings.repeat(string, this.indentationLevel + 1)).append(escapeName(string2)).append(KEY_VALUE_SEPARATOR).append(" ").append((new NbtOrderedStringFormatter(string, this.indentationLevel + 1, this.pathParts)).apply(nbtElement));
            this.popPathPart();
            if (iterator.hasNext()) {
               stringBuilder.append(ENTRY_SEPARATOR).append(string.isEmpty() ? " " : "\n");
            }
         }

         if (!string.isEmpty()) {
            stringBuilder.append("\n").append(Strings.repeat(string, this.indentationLevel));
         }

         stringBuilder.append("}");
         this.result = stringBuilder.toString();
         this.popPathPart();
      }
   }

   private void popPathPart() {
      this.pathParts.remove(this.pathParts.size() - 1);
   }

   private void pushPathPart(String part) {
      this.pathParts.add(part);
   }

   protected List<String> getSortedNames(NbtCompound compound) {
      Set<String> set = Sets.newHashSet((Iterable)compound.getKeys());
      List<String> list = Lists.newArrayList();
      List<String> list2 = (List)ENTRY_ORDER_OVERRIDES.get(this.joinPath());
      if (list2 != null) {
         Iterator var5 = list2.iterator();

         while(var5.hasNext()) {
            String string = (String)var5.next();
            if (set.remove(string)) {
               list.add(string);
            }
         }

         if (!set.isEmpty()) {
            Stream var10000 = set.stream().sorted();
            Objects.requireNonNull(list);
            var10000.forEach(list::add);
         }
      } else {
         list.addAll(set);
         Collections.sort(list);
      }

      return list;
   }

   public String joinPath() {
      return String.join(".", this.pathParts);
   }

   protected static String escapeName(String name) {
      return SIMPLE_NAME.matcher(name).matches() ? name : NbtString.escape(name);
   }

   public void visitNull(NbtNull element) {
   }
}
