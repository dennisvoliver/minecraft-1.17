package net.minecraft.test;

import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;
import net.minecraft.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TeamCityTestCompletionListener implements TestCompletionListener {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final Escaper ESCAPER = Escapers.builder().addEscape('\'', "|'").addEscape('\n', "|n").addEscape('\r', "|r").addEscape('|', "||").addEscape('[', "|[").addEscape(']', "|]").build();

   public void onTestFailed(GameTestState test) {
      String string = ESCAPER.escape(test.getStructurePath());
      String string2 = ESCAPER.escape(test.getThrowable().getMessage());
      String string3 = ESCAPER.escape(Util.getInnermostMessage(test.getThrowable()));
      LOGGER.info((String)"##teamcity[testStarted name='{}']", (Object)string);
      if (test.isRequired()) {
         LOGGER.info((String)"##teamcity[testFailed name='{}' message='{}' details='{}']", (Object)string, string2, string3);
      } else {
         LOGGER.info((String)"##teamcity[testIgnored name='{}' message='{}' details='{}']", (Object)string, string2, string3);
      }

      LOGGER.info((String)"##teamcity[testFinished name='{}' duration='{}']", (Object)string, (Object)test.getElapsedMilliseconds());
   }

   public void onTestPassed(GameTestState test) {
      String string = ESCAPER.escape(test.getStructurePath());
      LOGGER.info((String)"##teamcity[testStarted name='{}']", (Object)string);
      LOGGER.info((String)"##teamcity[testFinished name='{}' duration='{}']", (Object)string, (Object)test.getElapsedMilliseconds());
   }
}
