package net.minecraft.test;

import net.minecraft.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FailureLoggingTestCompletionListener implements TestCompletionListener {
   private static final Logger LOGGER = LogManager.getLogger();

   public void onTestFailed(GameTestState test) {
      if (test.isRequired()) {
         LOGGER.error((String)"{} failed! {}", (Object)test.getStructurePath(), (Object)Util.getInnermostMessage(test.getThrowable()));
      } else {
         LOGGER.warn((String)"(optional) {} failed. {}", (Object)test.getStructurePath(), (Object)Util.getInnermostMessage(test.getThrowable()));
      }

   }

   public void onTestPassed(GameTestState test) {
   }
}
