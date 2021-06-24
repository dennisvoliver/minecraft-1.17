package net.minecraft.command;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

public interface CommandSource {
   Collection<String> getPlayerNames();

   default Collection<String> getEntitySuggestions() {
      return Collections.emptyList();
   }

   Collection<String> getTeamNames();

   Collection<Identifier> getSoundIds();

   Stream<Identifier> getRecipeIds();

   CompletableFuture<Suggestions> getCompletions(CommandContext<CommandSource> context, SuggestionsBuilder builder);

   default Collection<CommandSource.RelativePosition> getBlockPositionSuggestions() {
      return Collections.singleton(CommandSource.RelativePosition.ZERO_WORLD);
   }

   default Collection<CommandSource.RelativePosition> getPositionSuggestions() {
      return Collections.singleton(CommandSource.RelativePosition.ZERO_WORLD);
   }

   Set<RegistryKey<World>> getWorldKeys();

   DynamicRegistryManager getRegistryManager();

   boolean hasPermissionLevel(int level);

   static <T> void forEachMatching(Iterable<T> candidates, String string, Function<T, Identifier> identifier, Consumer<T> action) {
      boolean bl = string.indexOf(58) > -1;
      Iterator var5 = candidates.iterator();

      while(true) {
         while(var5.hasNext()) {
            T object = var5.next();
            Identifier identifier2 = (Identifier)identifier.apply(object);
            if (bl) {
               String string2 = identifier2.toString();
               if (method_27136(string, string2)) {
                  action.accept(object);
               }
            } else if (method_27136(string, identifier2.getNamespace()) || identifier2.getNamespace().equals("minecraft") && method_27136(string, identifier2.getPath())) {
               action.accept(object);
            }
         }

         return;
      }
   }

   static <T> void forEachMatching(Iterable<T> candidates, String string, String string2, Function<T, Identifier> identifier, Consumer<T> action) {
      if (string.isEmpty()) {
         candidates.forEach(action);
      } else {
         String string3 = Strings.commonPrefix(string, string2);
         if (!string3.isEmpty()) {
            String string4 = string.substring(string3.length());
            forEachMatching(candidates, string4, identifier, action);
         }
      }

   }

   static CompletableFuture<Suggestions> suggestIdentifiers(Iterable<Identifier> candidates, SuggestionsBuilder builder, String string) {
      String string2 = builder.getRemaining().toLowerCase(Locale.ROOT);
      forEachMatching(candidates, string2, string, (identifier) -> {
         return identifier;
      }, (identifier) -> {
         builder.suggest(string + identifier);
      });
      return builder.buildFuture();
   }

   static CompletableFuture<Suggestions> suggestIdentifiers(Iterable<Identifier> candidates, SuggestionsBuilder builder) {
      String string = builder.getRemaining().toLowerCase(Locale.ROOT);
      forEachMatching(candidates, string, (identifier) -> {
         return identifier;
      }, (identifier) -> {
         builder.suggest(identifier.toString());
      });
      return builder.buildFuture();
   }

   static <T> CompletableFuture<Suggestions> suggestFromIdentifier(Iterable<T> candidates, SuggestionsBuilder builder, Function<T, Identifier> identifier, Function<T, Message> tooltip) {
      String string = builder.getRemaining().toLowerCase(Locale.ROOT);
      forEachMatching(candidates, string, identifier, (object) -> {
         builder.suggest(((Identifier)identifier.apply(object)).toString(), (Message)tooltip.apply(object));
      });
      return builder.buildFuture();
   }

   static CompletableFuture<Suggestions> suggestIdentifiers(Stream<Identifier> stream, SuggestionsBuilder builder) {
      Objects.requireNonNull(stream);
      return suggestIdentifiers(stream::iterator, builder);
   }

   static <T> CompletableFuture<Suggestions> suggestFromIdentifier(Stream<T> candidates, SuggestionsBuilder builder, Function<T, Identifier> identifier, Function<T, Message> tooltip) {
      Objects.requireNonNull(candidates);
      return suggestFromIdentifier(candidates::iterator, builder, identifier, tooltip);
   }

   static CompletableFuture<Suggestions> suggestPositions(String string, Collection<CommandSource.RelativePosition> candidates, SuggestionsBuilder builder, Predicate<String> predicate) {
      List<String> list = Lists.newArrayList();
      if (Strings.isNullOrEmpty(string)) {
         Iterator var5 = candidates.iterator();

         while(var5.hasNext()) {
            CommandSource.RelativePosition relativePosition = (CommandSource.RelativePosition)var5.next();
            String string2 = relativePosition.x + " " + relativePosition.y + " " + relativePosition.z;
            if (predicate.test(string2)) {
               list.add(relativePosition.x);
               list.add(relativePosition.x + " " + relativePosition.y);
               list.add(string2);
            }
         }
      } else {
         String[] strings = string.split(" ");
         String string4;
         Iterator var10;
         CommandSource.RelativePosition relativePosition2;
         if (strings.length == 1) {
            var10 = candidates.iterator();

            while(var10.hasNext()) {
               relativePosition2 = (CommandSource.RelativePosition)var10.next();
               string4 = strings[0] + " " + relativePosition2.y + " " + relativePosition2.z;
               if (predicate.test(string4)) {
                  list.add(strings[0] + " " + relativePosition2.y);
                  list.add(string4);
               }
            }
         } else if (strings.length == 2) {
            var10 = candidates.iterator();

            while(var10.hasNext()) {
               relativePosition2 = (CommandSource.RelativePosition)var10.next();
               string4 = strings[0] + " " + strings[1] + " " + relativePosition2.z;
               if (predicate.test(string4)) {
                  list.add(string4);
               }
            }
         }
      }

      return suggestMatching((Iterable)list, builder);
   }

   static CompletableFuture<Suggestions> suggestColumnPositions(String string, Collection<CommandSource.RelativePosition> collection, SuggestionsBuilder suggestionsBuilder, Predicate<String> predicate) {
      List<String> list = Lists.newArrayList();
      if (Strings.isNullOrEmpty(string)) {
         Iterator var5 = collection.iterator();

         while(var5.hasNext()) {
            CommandSource.RelativePosition relativePosition = (CommandSource.RelativePosition)var5.next();
            String string2 = relativePosition.x + " " + relativePosition.z;
            if (predicate.test(string2)) {
               list.add(relativePosition.x);
               list.add(string2);
            }
         }
      } else {
         String[] strings = string.split(" ");
         if (strings.length == 1) {
            Iterator var10 = collection.iterator();

            while(var10.hasNext()) {
               CommandSource.RelativePosition relativePosition2 = (CommandSource.RelativePosition)var10.next();
               String string3 = strings[0] + " " + relativePosition2.z;
               if (predicate.test(string3)) {
                  list.add(string3);
               }
            }
         }
      }

      return suggestMatching((Iterable)list, suggestionsBuilder);
   }

   static CompletableFuture<Suggestions> suggestMatching(Iterable<String> iterable, SuggestionsBuilder suggestionsBuilder) {
      String string = suggestionsBuilder.getRemaining().toLowerCase(Locale.ROOT);
      Iterator var3 = iterable.iterator();

      while(var3.hasNext()) {
         String string2 = (String)var3.next();
         if (method_27136(string, string2.toLowerCase(Locale.ROOT))) {
            suggestionsBuilder.suggest(string2);
         }
      }

      return suggestionsBuilder.buildFuture();
   }

   static CompletableFuture<Suggestions> suggestMatching(Stream<String> stream, SuggestionsBuilder suggestionsBuilder) {
      String string = suggestionsBuilder.getRemaining().toLowerCase(Locale.ROOT);
      Stream var10000 = stream.filter((string2) -> {
         return method_27136(string, string2.toLowerCase(Locale.ROOT));
      });
      Objects.requireNonNull(suggestionsBuilder);
      var10000.forEach(suggestionsBuilder::suggest);
      return suggestionsBuilder.buildFuture();
   }

   static CompletableFuture<Suggestions> suggestMatching(String[] strings, SuggestionsBuilder suggestionsBuilder) {
      String string = suggestionsBuilder.getRemaining().toLowerCase(Locale.ROOT);
      String[] var3 = strings;
      int var4 = strings.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         String string2 = var3[var5];
         if (method_27136(string, string2.toLowerCase(Locale.ROOT))) {
            suggestionsBuilder.suggest(string2);
         }
      }

      return suggestionsBuilder.buildFuture();
   }

   static <T> CompletableFuture<Suggestions> method_35510(Iterable<T> iterable, SuggestionsBuilder suggestionsBuilder, Function<T, String> function, Function<T, Message> function2) {
      String string = suggestionsBuilder.getRemaining().toLowerCase(Locale.ROOT);
      Iterator var5 = iterable.iterator();

      while(var5.hasNext()) {
         T object = var5.next();
         String string2 = (String)function.apply(object);
         if (method_27136(string, string2.toLowerCase(Locale.ROOT))) {
            suggestionsBuilder.suggest(string2, (Message)function2.apply(object));
         }
      }

      return suggestionsBuilder.buildFuture();
   }

   static boolean method_27136(String string, String string2) {
      for(int i = 0; !string2.startsWith(string, i); ++i) {
         i = string2.indexOf(95, i);
         if (i < 0) {
            return false;
         }
      }

      return true;
   }

   public static class RelativePosition {
      public static final CommandSource.RelativePosition ZERO_LOCAL = new CommandSource.RelativePosition("^", "^", "^");
      public static final CommandSource.RelativePosition ZERO_WORLD = new CommandSource.RelativePosition("~", "~", "~");
      public final String x;
      public final String y;
      public final String z;

      public RelativePosition(String x, String y, String z) {
         this.x = x;
         this.y = y;
         this.z = z;
      }
   }
}
