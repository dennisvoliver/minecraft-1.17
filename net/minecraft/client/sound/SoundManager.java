package net.minecraft.client.sound;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.Camera;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SinglePreparationResourceReloader;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class SoundManager extends SinglePreparationResourceReloader<SoundManager.SoundList> {
   public static final Sound MISSING_SOUND;
   static final Logger LOGGER;
   private static final String SOUNDS_JSON = "sounds.json";
   private static final Gson GSON;
   private static final TypeToken<Map<String, SoundEntry>> TYPE;
   private final Map<Identifier, WeightedSoundSet> sounds = Maps.newHashMap();
   private final SoundSystem soundSystem;

   public SoundManager(ResourceManager resourceManager, GameOptions gameOptions) {
      this.soundSystem = new SoundSystem(this, gameOptions, resourceManager);
   }

   protected SoundManager.SoundList prepare(ResourceManager resourceManager, Profiler profiler) {
      SoundManager.SoundList soundList = new SoundManager.SoundList();
      profiler.startTick();

      for(Iterator var4 = resourceManager.getAllNamespaces().iterator(); var4.hasNext(); profiler.pop()) {
         String string = (String)var4.next();
         profiler.push(string);

         try {
            List<Resource> list = resourceManager.getAllResources(new Identifier(string, "sounds.json"));

            for(Iterator var7 = list.iterator(); var7.hasNext(); profiler.pop()) {
               Resource resource = (Resource)var7.next();
               profiler.push(resource.getResourcePackName());

               try {
                  InputStream inputStream = resource.getInputStream();

                  try {
                     InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);

                     try {
                        profiler.push("parse");
                        Map<String, SoundEntry> map = (Map)JsonHelper.deserialize(GSON, (Reader)reader, (TypeToken)TYPE);
                        profiler.swap("register");
                        Iterator var12 = map.entrySet().iterator();

                        while(var12.hasNext()) {
                           Entry<String, SoundEntry> entry = (Entry)var12.next();
                           soundList.register(new Identifier(string, (String)entry.getKey()), (SoundEntry)entry.getValue(), resourceManager);
                        }

                        profiler.pop();
                     } catch (Throwable var16) {
                        try {
                           reader.close();
                        } catch (Throwable var15) {
                           var16.addSuppressed(var15);
                        }

                        throw var16;
                     }

                     reader.close();
                  } catch (Throwable var17) {
                     if (inputStream != null) {
                        try {
                           inputStream.close();
                        } catch (Throwable var14) {
                           var17.addSuppressed(var14);
                        }
                     }

                     throw var17;
                  }

                  if (inputStream != null) {
                     inputStream.close();
                  }
               } catch (RuntimeException var18) {
                  LOGGER.warn((String)"Invalid {} in resourcepack: '{}'", (Object)"sounds.json", resource.getResourcePackName(), var18);
               }
            }
         } catch (IOException var19) {
         }
      }

      profiler.endTick();
      return soundList;
   }

   protected void apply(SoundManager.SoundList soundList, ResourceManager resourceManager, Profiler profiler) {
      soundList.addTo(this.sounds, this.soundSystem);
      Iterator var4;
      Identifier identifier2;
      if (SharedConstants.isDevelopment) {
         var4 = this.sounds.keySet().iterator();

         while(var4.hasNext()) {
            identifier2 = (Identifier)var4.next();
            WeightedSoundSet weightedSoundSet = (WeightedSoundSet)this.sounds.get(identifier2);
            if (weightedSoundSet.getSubtitle() instanceof TranslatableText) {
               String string = ((TranslatableText)weightedSoundSet.getSubtitle()).getKey();
               if (!I18n.hasTranslation(string) && Registry.SOUND_EVENT.containsId(identifier2)) {
                  LOGGER.error((String)"Missing subtitle {} for sound event: {}", (Object)string, (Object)identifier2);
               }
            }
         }
      }

      if (LOGGER.isDebugEnabled()) {
         var4 = this.sounds.keySet().iterator();

         while(var4.hasNext()) {
            identifier2 = (Identifier)var4.next();
            if (!Registry.SOUND_EVENT.containsId(identifier2)) {
               LOGGER.debug((String)"Not having sound event for: {}", (Object)identifier2);
            }
         }
      }

      this.soundSystem.reloadSounds();
   }

   static boolean isSoundResourcePresent(Sound sound, Identifier id, ResourceManager resourceManager) {
      Identifier identifier = sound.getLocation();
      if (!resourceManager.containsResource(identifier)) {
         LOGGER.warn((String)"File {} does not exist, cannot add it to event {}", (Object)identifier, (Object)id);
         return false;
      } else {
         return true;
      }
   }

   @Nullable
   public WeightedSoundSet get(Identifier id) {
      return (WeightedSoundSet)this.sounds.get(id);
   }

   public Collection<Identifier> getKeys() {
      return this.sounds.keySet();
   }

   public void playNextTick(TickableSoundInstance sound) {
      this.soundSystem.playNextTick(sound);
   }

   public void play(SoundInstance sound) {
      this.soundSystem.play(sound);
   }

   public void play(SoundInstance sound, int delay) {
      this.soundSystem.play(sound, delay);
   }

   public void updateListenerPosition(Camera camera) {
      this.soundSystem.updateListenerPosition(camera);
   }

   public void pauseAll() {
      this.soundSystem.pauseAll();
   }

   public void stopAll() {
      this.soundSystem.stopAll();
   }

   public void close() {
      this.soundSystem.stop();
   }

   public void tick(boolean bl) {
      this.soundSystem.tick(bl);
   }

   public void resumeAll() {
      this.soundSystem.resumeAll();
   }

   public void updateSoundVolume(SoundCategory category, float volume) {
      if (category == SoundCategory.MASTER && volume <= 0.0F) {
         this.stopAll();
      }

      this.soundSystem.updateSoundVolume(category, volume);
   }

   public void stop(SoundInstance sound) {
      this.soundSystem.stop(sound);
   }

   public boolean isPlaying(SoundInstance sound) {
      return this.soundSystem.isPlaying(sound);
   }

   public void registerListener(SoundInstanceListener listener) {
      this.soundSystem.registerListener(listener);
   }

   public void unregisterListener(SoundInstanceListener listener) {
      this.soundSystem.unregisterListener(listener);
   }

   public void stopSounds(@Nullable Identifier id, @Nullable SoundCategory soundCategory) {
      this.soundSystem.stopSounds(id, soundCategory);
   }

   public String getDebugString() {
      return this.soundSystem.getDebugString();
   }

   static {
      MISSING_SOUND = new Sound("meta:missing_sound", 1.0F, 1.0F, 1, Sound.RegistrationType.FILE, false, false, 16);
      LOGGER = LogManager.getLogger();
      GSON = (new GsonBuilder()).registerTypeHierarchyAdapter(Text.class, new Text.Serializer()).registerTypeAdapter(SoundEntry.class, new SoundEntryDeserializer()).create();
      TYPE = new TypeToken<Map<String, SoundEntry>>() {
      };
   }

   @Environment(EnvType.CLIENT)
   protected static class SoundList {
      final Map<Identifier, WeightedSoundSet> loadedSounds = Maps.newHashMap();

      void register(Identifier id, SoundEntry entry, ResourceManager resourceManager) {
         WeightedSoundSet weightedSoundSet = (WeightedSoundSet)this.loadedSounds.get(id);
         boolean bl = weightedSoundSet == null;
         if (bl || entry.canReplace()) {
            if (!bl) {
               SoundManager.LOGGER.debug((String)"Replaced sound event location {}", (Object)id);
            }

            weightedSoundSet = new WeightedSoundSet(id, entry.getSubtitle());
            this.loadedSounds.put(id, weightedSoundSet);
         }

         Iterator var6 = entry.getSounds().iterator();

         while(var6.hasNext()) {
            final Sound sound = (Sound)var6.next();
            final Identifier identifier = sound.getIdentifier();
            Object soundContainer3;
            switch(sound.getRegistrationType()) {
            case FILE:
               if (!SoundManager.isSoundResourcePresent(sound, id, resourceManager)) {
                  continue;
               }

               soundContainer3 = sound;
               break;
            case SOUND_EVENT:
               soundContainer3 = new SoundContainer<Sound>() {
                  public int getWeight() {
                     WeightedSoundSet weightedSoundSet = (WeightedSoundSet)SoundList.this.loadedSounds.get(identifier);
                     return weightedSoundSet == null ? 0 : weightedSoundSet.getWeight();
                  }

                  public Sound getSound() {
                     WeightedSoundSet weightedSoundSet = (WeightedSoundSet)SoundList.this.loadedSounds.get(identifier);
                     if (weightedSoundSet == null) {
                        return SoundManager.MISSING_SOUND;
                     } else {
                        Sound soundx = weightedSoundSet.getSound();
                        return new Sound(soundx.getIdentifier().toString(), soundx.getVolume() * sound.getVolume(), soundx.getPitch() * sound.getPitch(), sound.getWeight(), Sound.RegistrationType.FILE, soundx.isStreamed() || sound.isStreamed(), soundx.isPreloaded(), soundx.getAttenuation());
                     }
                  }

                  public void preload(SoundSystem soundSystem) {
                     WeightedSoundSet weightedSoundSet = (WeightedSoundSet)SoundList.this.loadedSounds.get(identifier);
                     if (weightedSoundSet != null) {
                        weightedSoundSet.preload(soundSystem);
                     }
                  }
               };
               break;
            default:
               throw new IllegalStateException("Unknown SoundEventRegistration type: " + sound.getRegistrationType());
            }

            weightedSoundSet.add((SoundContainer)soundContainer3);
         }

      }

      public void addTo(Map<Identifier, WeightedSoundSet> map, SoundSystem soundSystem) {
         map.clear();
         Iterator var3 = this.loadedSounds.entrySet().iterator();

         while(var3.hasNext()) {
            Entry<Identifier, WeightedSoundSet> entry = (Entry)var3.next();
            map.put((Identifier)entry.getKey(), (WeightedSoundSet)entry.getValue());
            ((WeightedSoundSet)entry.getValue()).preload(soundSystem);
         }

      }
   }
}
