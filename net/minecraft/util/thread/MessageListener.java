package net.minecraft.util.thread;

import com.mojang.datafixers.util.Either;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public interface MessageListener<Msg> extends AutoCloseable {
   String getName();

   void send(Msg message);

   default void close() {
   }

   /**
    * Asks a message provider for a message.
    * 
    * The {@link CompletableFuture} returned from this function will never complete exceptionally.
    * 
    * @return CompletableFuture future that completes with the received message
    */
   default <Source> CompletableFuture<Source> ask(Function<? super MessageListener<Source>, ? extends Msg> messageProvider) {
      CompletableFuture<Source> completableFuture = new CompletableFuture();
      Objects.requireNonNull(completableFuture);
      Msg object = messageProvider.apply(create("ask future procesor handle", completableFuture::complete));
      this.send(object);
      return completableFuture;
   }

   /**
    * Asks a fallible message provider for a message.
    * 
    * The provider is given a MessageListener that accepts a {@link Either} representing either
    * a valid response (generic parameter Source) or an Exception, which decides whether the
    * future completes successfully or exceptionally.
    * 
    * @return CompletableFuture that may either complete successfully or exceptionally
    */
   default <Source> CompletableFuture<Source> askFallible(Function<? super MessageListener<Either<Source, Exception>>, ? extends Msg> messageProvider) {
      CompletableFuture<Source> completableFuture = new CompletableFuture();
      Msg object = messageProvider.apply(create("ask future procesor handle", (either) -> {
         Objects.requireNonNull(completableFuture);
         either.ifLeft(completableFuture::complete);
         Objects.requireNonNull(completableFuture);
         either.ifRight(completableFuture::completeExceptionally);
      }));
      this.send(object);
      return completableFuture;
   }

   static <Msg> MessageListener<Msg> create(final String name, final Consumer<Msg> action) {
      return new MessageListener<Msg>() {
         public String getName() {
            return name;
         }

         public void send(Msg message) {
            action.accept(message);
         }

         public String toString() {
            return name;
         }
      };
   }
}
