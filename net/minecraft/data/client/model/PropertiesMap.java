package net.minecraft.data.client.model;

import com.google.common.collect.ImmutableList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.state.property.Property;

/**
 * Represents a set of property to value pairs, used as conditions for model
 * application.
 * 
 * <p>This object is immutable.
 */
public final class PropertiesMap {
   private static final PropertiesMap EMPTY = new PropertiesMap(ImmutableList.of());
   private static final Comparator<Property.Value<?>> COMPARATOR = Comparator.comparing((value) -> {
      return value.getProperty().getName();
   });
   private final List<Property.Value<?>> values;

   public PropertiesMap withValue(Property.Value<?> value) {
      return new PropertiesMap(ImmutableList.builder().addAll((Iterable)this.values).add((Object)value).build());
   }

   public PropertiesMap copyOf(PropertiesMap propertiesMap) {
      return new PropertiesMap(ImmutableList.builder().addAll((Iterable)this.values).addAll((Iterable)propertiesMap.values).build());
   }

   private PropertiesMap(List<Property.Value<?>> values) {
      this.values = values;
   }

   public static PropertiesMap empty() {
      return EMPTY;
   }

   public static PropertiesMap withValues(Property.Value<?>... values) {
      return new PropertiesMap(ImmutableList.copyOf((Object[])values));
   }

   public boolean equals(Object o) {
      return this == o || o instanceof PropertiesMap && this.values.equals(((PropertiesMap)o).values);
   }

   public int hashCode() {
      return this.values.hashCode();
   }

   public String asString() {
      return (String)this.values.stream().sorted(COMPARATOR).map(Property.Value::toString).collect(Collectors.joining(","));
   }

   public String toString() {
      return this.asString();
   }
}
