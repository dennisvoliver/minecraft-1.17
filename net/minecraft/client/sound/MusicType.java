package net.minecraft.client.sound;

import net.minecraft.sound.MusicSound;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

public class MusicType {
   private static final int field_29804 = 20;
   private static final int field_29805 = 600;
   private static final int field_29806 = 12000;
   private static final int field_29807 = 24000;
   private static final int field_29808 = 6000;
   public static final MusicSound MENU;
   public static final MusicSound CREATIVE;
   public static final MusicSound CREDITS;
   public static final MusicSound DRAGON;
   public static final MusicSound END;
   public static final MusicSound UNDERWATER;
   public static final MusicSound GAME;

   public static MusicSound createIngameMusic(SoundEvent event) {
      return new MusicSound(event, 12000, 24000, false);
   }

   static {
      MENU = new MusicSound(SoundEvents.MUSIC_MENU, 20, 600, true);
      CREATIVE = new MusicSound(SoundEvents.MUSIC_CREATIVE, 12000, 24000, false);
      CREDITS = new MusicSound(SoundEvents.MUSIC_CREDITS, 0, 0, true);
      DRAGON = new MusicSound(SoundEvents.MUSIC_DRAGON, 0, 0, true);
      END = new MusicSound(SoundEvents.MUSIC_END, 6000, 24000, true);
      UNDERWATER = createIngameMusic(SoundEvents.MUSIC_UNDER_WATER);
      GAME = createIngameMusic(SoundEvents.MUSIC_GAME);
   }
}
