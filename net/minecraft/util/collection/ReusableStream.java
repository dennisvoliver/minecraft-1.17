package net.minecraft.util.collection;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A wrapper that automatically flattens the original stream and provides duplicates iterating a copy of that stream's output.
 */
public class ReusableStream<T> {
   final List<T> collectedElements = Lists.newArrayList();
   final Spliterator<T> source;

   public ReusableStream(Stream<T> stream) {
      this.source = stream.spliterator();
   }

   public Stream<T> stream() {
      return StreamSupport.stream(new AbstractSpliterator<T>(Long.MAX_VALUE, 0) {
         private int pos;

         public boolean tryAdvance(Consumer<? super T> consumer) {
            while(true) {
               if (this.pos >= ReusableStream.this.collectedElements.size()) {
                  Spliterator var10000 = ReusableStream.this.source;
                  List var10001 = ReusableStream.this.collectedElements;
                  Objects.requireNonNull(var10001);
                  if (var10000.tryAdvance(var10001::add)) {
                     continue;
                  }

                  return false;
               }

               consumer.accept(ReusableStream.this.collectedElements.get(this.pos++));
               return true;
            }
         }
      }, false);
   }
}
