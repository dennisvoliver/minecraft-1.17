package net.minecraft.client.realms.gui;

import com.google.common.annotations.VisibleForTesting;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;

/**
 * An operational rate limiter.
 */
@Environment(EnvType.CLIENT)
public class BasicFetchRateLimiter implements FetchRateLimiter {
   private final Duration period;
   private final Supplier<Clock> clock;
   @Nullable
   private Instant lastRun;

   public BasicFetchRateLimiter(Duration period) {
      this.period = period;
      this.clock = Clock::systemUTC;
   }

   @VisibleForTesting
   protected BasicFetchRateLimiter(Duration period, Supplier<Clock> clock) {
      this.period = period;
      this.clock = clock;
   }

   public void onRun() {
      this.lastRun = Instant.now((Clock)this.clock.get());
   }

   public long getRemainingPeriod() {
      return this.lastRun == null ? 0L : Math.max(0L, Duration.between(Instant.now((Clock)this.clock.get()), this.lastRun.plus(this.period)).toMillis());
   }
}
