package net.minecraft.client.realms.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.util.UUIDTypeAdapter;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

@Environment(EnvType.CLIENT)
public class RealmsUtil {
   private static final YggdrasilAuthenticationService authenticationService = new YggdrasilAuthenticationService(MinecraftClient.getInstance().getNetworkProxy());
   static final MinecraftSessionService sessionService;
   public static LoadingCache<String, GameProfile> gameProfileCache;
   private static final int SECONDS_PER_MINUTE = 60;
   private static final int SECONDS_PER_HOUR = 3600;
   private static final int SECONDS_PER_DAY = 86400;

   public static String uuidToName(String uuid) throws Exception {
      GameProfile gameProfile = (GameProfile)gameProfileCache.get(uuid);
      return gameProfile.getName();
   }

   public static Map<Type, MinecraftProfileTexture> getTextures(String uuid) {
      try {
         GameProfile gameProfile = (GameProfile)gameProfileCache.get(uuid);
         return sessionService.getTextures(gameProfile, false);
      } catch (Exception var2) {
         return Maps.newHashMap();
      }
   }

   public static String convertToAgePresentation(long milliseconds) {
      if (milliseconds < 0L) {
         return "right now";
      } else {
         long l = milliseconds / 1000L;
         String var10000;
         if (l < 60L) {
            var10000 = l == 1L ? "1 second" : l + " seconds";
            return var10000 + " ago";
         } else {
            long o;
            if (l < 3600L) {
               o = l / 60L;
               var10000 = o == 1L ? "1 minute" : o + " minutes";
               return var10000 + " ago";
            } else if (l < 86400L) {
               o = l / 3600L;
               var10000 = o == 1L ? "1 hour" : o + " hours";
               return var10000 + " ago";
            } else {
               o = l / 86400L;
               var10000 = o == 1L ? "1 day" : o + " days";
               return var10000 + " ago";
            }
         }
      }
   }

   public static String convertToAgePresentation(Date date) {
      return convertToAgePresentation(System.currentTimeMillis() - date.getTime());
   }

   static {
      sessionService = authenticationService.createMinecraftSessionService();
      gameProfileCache = CacheBuilder.newBuilder().expireAfterWrite(60L, TimeUnit.MINUTES).build(new CacheLoader<String, GameProfile>() {
         public GameProfile load(String string) throws Exception {
            GameProfile gameProfile = RealmsUtil.sessionService.fillProfileProperties(new GameProfile(UUIDTypeAdapter.fromString(string), (String)null), false);
            if (gameProfile == null) {
               throw new Exception("Couldn't get profile");
            } else {
               return gameProfile;
            }
         }
      });
   }
}
