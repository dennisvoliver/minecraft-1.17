package net.minecraft.client.realms.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * A fetch rate limiter that does nothing.
 */
@Environment(EnvType.CLIENT)
public class DummyFetchRateLimiter implements FetchRateLimiter {
   public void onRun() {
   }

   public long getRemainingPeriod() {
      return 0L;
   }
}
