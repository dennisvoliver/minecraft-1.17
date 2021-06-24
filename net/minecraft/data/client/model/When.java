package net.minecraft.data.client.model;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;

public interface When extends Supplier<JsonElement> {
   void validate(StateManager<?, ?> stateManager);

   static When.PropertyCondition create() {
      return new When.PropertyCondition();
   }

   static When allOf(When... conditions) {
      return new When.LogicalCondition(When.LogicalOperator.AND, Arrays.asList(conditions));
   }

   static When anyOf(When... conditions) {
      return new When.LogicalCondition(When.LogicalOperator.OR, Arrays.asList(conditions));
   }

   public static class PropertyCondition implements When {
      private final Map<Property<?>, String> properties = Maps.newHashMap();

      private static <T extends Comparable<T>> String name(Property<T> property, Stream<T> valueStream) {
         Objects.requireNonNull(property);
         return (String)valueStream.map(property::name).collect(Collectors.joining("|"));
      }

      private static <T extends Comparable<T>> String name(Property<T> property, T value, T[] otherValues) {
         return name(property, Stream.concat(Stream.of(value), Stream.of(otherValues)));
      }

      private <T extends Comparable<T>> void set(Property<T> property, String value) {
         String string = (String)this.properties.put(property, value);
         if (string != null) {
            throw new IllegalStateException("Tried to replace " + property + " value from " + string + " to " + value);
         }
      }

      public final <T extends Comparable<T>> When.PropertyCondition set(Property<T> property, T value) {
         this.set(property, property.name(value));
         return this;
      }

      @SafeVarargs
      public final <T extends Comparable<T>> When.PropertyCondition set(Property<T> property, T value, T... otherValues) {
         this.set(property, name(property, value, otherValues));
         return this;
      }

      public final <T extends Comparable<T>> When.PropertyCondition method_35871(Property<T> property, T comparable) {
         String var10002 = property.name(comparable);
         this.set(property, "!" + var10002);
         return this;
      }

      @SafeVarargs
      public final <T extends Comparable<T>> When.PropertyCondition method_35872(Property<T> property, T comparable, T... comparables) {
         String var10002 = name(property, comparable, comparables);
         this.set(property, "!" + var10002);
         return this;
      }

      public JsonElement get() {
         JsonObject jsonObject = new JsonObject();
         this.properties.forEach((property, string) -> {
            jsonObject.addProperty(property.getName(), string);
         });
         return jsonObject;
      }

      public void validate(StateManager<?, ?> stateManager) {
         List<Property<?>> list = (List)this.properties.keySet().stream().filter((property) -> {
            return stateManager.getProperty(property.getName()) != property;
         }).collect(Collectors.toList());
         if (!list.isEmpty()) {
            throw new IllegalStateException("Properties " + list + " are missing from " + stateManager);
         }
      }
   }

   public static class LogicalCondition implements When {
      private final When.LogicalOperator operator;
      private final List<When> components;

      LogicalCondition(When.LogicalOperator logicalOperator, List<When> list) {
         this.operator = logicalOperator;
         this.components = list;
      }

      public void validate(StateManager<?, ?> stateManager) {
         this.components.forEach((when) -> {
            when.validate(stateManager);
         });
      }

      public JsonElement get() {
         JsonArray jsonArray = new JsonArray();
         Stream var10000 = this.components.stream().map(Supplier::get);
         Objects.requireNonNull(jsonArray);
         var10000.forEach(jsonArray::add);
         JsonObject jsonObject = new JsonObject();
         jsonObject.add(this.operator.name, jsonArray);
         return jsonObject;
      }
   }

   public static enum LogicalOperator {
      AND("AND"),
      OR("OR");

      final String name;

      private LogicalOperator(String name) {
         this.name = name;
      }
   }
}
