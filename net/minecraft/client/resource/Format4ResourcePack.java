package net.minecraft.client.resource;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.metadata.ResourceMetadataReader;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class Format4ResourcePack implements ResourcePack {
   private static final Map<String, Pair<ChestType, Identifier>> NEW_TO_OLD_CHEST_TEXTURES = (Map)Util.make(Maps.newHashMap(), (map) -> {
      map.put("textures/entity/chest/normal_left.png", new Pair(ChestType.LEFT, new Identifier("textures/entity/chest/normal_double.png")));
      map.put("textures/entity/chest/normal_right.png", new Pair(ChestType.RIGHT, new Identifier("textures/entity/chest/normal_double.png")));
      map.put("textures/entity/chest/normal.png", new Pair(ChestType.SINGLE, new Identifier("textures/entity/chest/normal.png")));
      map.put("textures/entity/chest/trapped_left.png", new Pair(ChestType.LEFT, new Identifier("textures/entity/chest/trapped_double.png")));
      map.put("textures/entity/chest/trapped_right.png", new Pair(ChestType.RIGHT, new Identifier("textures/entity/chest/trapped_double.png")));
      map.put("textures/entity/chest/trapped.png", new Pair(ChestType.SINGLE, new Identifier("textures/entity/chest/trapped.png")));
      map.put("textures/entity/chest/christmas_left.png", new Pair(ChestType.LEFT, new Identifier("textures/entity/chest/christmas_double.png")));
      map.put("textures/entity/chest/christmas_right.png", new Pair(ChestType.RIGHT, new Identifier("textures/entity/chest/christmas_double.png")));
      map.put("textures/entity/chest/christmas.png", new Pair(ChestType.SINGLE, new Identifier("textures/entity/chest/christmas.png")));
      map.put("textures/entity/chest/ender.png", new Pair(ChestType.SINGLE, new Identifier("textures/entity/chest/ender.png")));
   });
   private static final List<String> BANNER_PATTERN_TYPES = Lists.newArrayList((Object[])("base", "border", "bricks", "circle", "creeper", "cross", "curly_border", "diagonal_left", "diagonal_right", "diagonal_up_left", "diagonal_up_right", "flower", "globe", "gradient", "gradient_up", "half_horizontal", "half_horizontal_bottom", "half_vertical", "half_vertical_right", "mojang", "rhombus", "skull", "small_stripes", "square_bottom_left", "square_bottom_right", "square_top_left", "square_top_right", "straight_cross", "stripe_bottom", "stripe_center", "stripe_downleft", "stripe_downright", "stripe_left", "stripe_middle", "stripe_right", "stripe_top", "triangle_bottom", "triangle_top", "triangles_bottom", "triangles_top"));
   private static final Set<String> SHIELD_PATTERN_TEXTURES;
   private static final Set<String> BANNER_PATTERN_TEXTURES;
   public static final Identifier OLD_SHIELD_BASE_TEXTURE;
   public static final Identifier OLD_BANNER_BASE_TEXTURE;
   public static final int field_32966 = 64;
   public static final int field_32967 = 64;
   public static final int field_32968 = 64;
   public static final Identifier IRON_GOLEM_TEXTURE;
   public static final String IRON_GOLEM_TEXTURE_PATH = "textures/entity/iron_golem/iron_golem.png";
   private final ResourcePack parent;

   public Format4ResourcePack(ResourcePack parent) {
      this.parent = parent;
   }

   public InputStream openRoot(String fileName) throws IOException {
      return this.parent.openRoot(fileName);
   }

   public boolean contains(ResourceType type, Identifier id) {
      if (!"minecraft".equals(id.getNamespace())) {
         return this.parent.contains(type, id);
      } else {
         String string = id.getPath();
         if ("textures/misc/enchanted_item_glint.png".equals(string)) {
            return false;
         } else if ("textures/entity/iron_golem/iron_golem.png".equals(string)) {
            return this.parent.contains(type, IRON_GOLEM_TEXTURE);
         } else if (!"textures/entity/conduit/wind.png".equals(string) && !"textures/entity/conduit/wind_vertical.png".equals(string)) {
            if (SHIELD_PATTERN_TEXTURES.contains(string)) {
               return this.parent.contains(type, OLD_SHIELD_BASE_TEXTURE) && this.parent.contains(type, id);
            } else if (!BANNER_PATTERN_TEXTURES.contains(string)) {
               Pair<ChestType, Identifier> pair = (Pair)NEW_TO_OLD_CHEST_TEXTURES.get(string);
               return pair != null && this.parent.contains(type, (Identifier)pair.getSecond()) ? true : this.parent.contains(type, id);
            } else {
               return this.parent.contains(type, OLD_BANNER_BASE_TEXTURE) && this.parent.contains(type, id);
            }
         } else {
            return false;
         }
      }
   }

   public InputStream open(ResourceType type, Identifier id) throws IOException {
      if (!"minecraft".equals(id.getNamespace())) {
         return this.parent.open(type, id);
      } else {
         String string = id.getPath();
         if ("textures/entity/iron_golem/iron_golem.png".equals(string)) {
            return this.parent.open(type, IRON_GOLEM_TEXTURE);
         } else {
            InputStream inputStream2;
            if (SHIELD_PATTERN_TEXTURES.contains(string)) {
               inputStream2 = openCroppedStream(this.parent.open(type, OLD_SHIELD_BASE_TEXTURE), this.parent.open(type, id), 64, 2, 2, 12, 22);
               if (inputStream2 != null) {
                  return inputStream2;
               }
            } else if (BANNER_PATTERN_TEXTURES.contains(string)) {
               inputStream2 = openCroppedStream(this.parent.open(type, OLD_BANNER_BASE_TEXTURE), this.parent.open(type, id), 64, 0, 0, 42, 41);
               if (inputStream2 != null) {
                  return inputStream2;
               }
            } else {
               if ("textures/entity/enderdragon/dragon.png".equals(string) || "textures/entity/enderdragon/dragon_exploding.png".equals(string)) {
                  NativeImage nativeImage = NativeImage.read(this.parent.open(type, id));

                  ByteArrayInputStream var14;
                  try {
                     int i = nativeImage.getWidth() / 256;
                     int j = 88 * i;

                     while(true) {
                        if (j >= 200 * i) {
                           var14 = new ByteArrayInputStream(nativeImage.getBytes());
                           break;
                        }

                        for(int k = 56 * i; k < 112 * i; ++k) {
                           nativeImage.setPixelColor(k, j, 0);
                        }

                        ++j;
                     }
                  } catch (Throwable var9) {
                     if (nativeImage != null) {
                        try {
                           nativeImage.close();
                        } catch (Throwable var8) {
                           var9.addSuppressed(var8);
                        }
                     }

                     throw var9;
                  }

                  if (nativeImage != null) {
                     nativeImage.close();
                  }

                  return var14;
               }

               if ("textures/entity/conduit/closed_eye.png".equals(string) || "textures/entity/conduit/open_eye.png".equals(string)) {
                  return method_24199(this.parent.open(type, id));
               }

               Pair<ChestType, Identifier> pair = (Pair)NEW_TO_OLD_CHEST_TEXTURES.get(string);
               if (pair != null) {
                  ChestType chestType = (ChestType)pair.getFirst();
                  InputStream inputStream3 = this.parent.open(type, (Identifier)pair.getSecond());
                  if (chestType == ChestType.SINGLE) {
                     return cropSingleChestTexture(inputStream3);
                  }

                  if (chestType == ChestType.LEFT) {
                     return cropLeftChestTexture(inputStream3);
                  }

                  if (chestType == ChestType.RIGHT) {
                     return cropRightChestTexture(inputStream3);
                  }
               }
            }

            return this.parent.open(type, id);
         }
      }
   }

   @Nullable
   public static InputStream openCroppedStream(InputStream inputStream, InputStream inputStream2, int i, int j, int k, int l, int m) throws IOException {
      NativeImage nativeImage = NativeImage.read(inputStream);

      ByteArrayInputStream var23;
      label105: {
         try {
            NativeImage nativeImage2;
            label107: {
               nativeImage2 = NativeImage.read(inputStream2);

               try {
                  int n = nativeImage.getWidth();
                  int o = nativeImage.getHeight();
                  if (n != nativeImage2.getWidth() || o != nativeImage2.getHeight()) {
                     break label107;
                  }

                  NativeImage nativeImage3 = new NativeImage(n, o, true);

                  try {
                     int p = n / i;
                     int q = k * p;

                     while(true) {
                        if (q >= m * p) {
                           var23 = new ByteArrayInputStream(nativeImage3.getBytes());
                           break;
                        }

                        for(int r = j * p; r < l * p; ++r) {
                           int s = NativeImage.getRed(nativeImage2.getPixelColor(r, q));
                           int t = nativeImage.getPixelColor(r, q);
                           nativeImage3.setPixelColor(r, q, NativeImage.getAbgrColor(s, NativeImage.getBlue(t), NativeImage.getGreen(t), NativeImage.getRed(t)));
                        }

                        ++q;
                     }
                  } catch (Throwable var20) {
                     try {
                        nativeImage3.close();
                     } catch (Throwable var19) {
                        var20.addSuppressed(var19);
                     }

                     throw var20;
                  }

                  nativeImage3.close();
               } catch (Throwable var21) {
                  if (nativeImage2 != null) {
                     try {
                        nativeImage2.close();
                     } catch (Throwable var18) {
                        var21.addSuppressed(var18);
                     }
                  }

                  throw var21;
               }

               if (nativeImage2 != null) {
                  nativeImage2.close();
               }
               break label105;
            }

            if (nativeImage2 != null) {
               nativeImage2.close();
            }
         } catch (Throwable var22) {
            if (nativeImage != null) {
               try {
                  nativeImage.close();
               } catch (Throwable var17) {
                  var22.addSuppressed(var17);
               }
            }

            throw var22;
         }

         if (nativeImage != null) {
            nativeImage.close();
         }

         return null;
      }

      if (nativeImage != null) {
         nativeImage.close();
      }

      return var23;
   }

   public static InputStream method_24199(InputStream inputStream) throws IOException {
      NativeImage nativeImage = NativeImage.read(inputStream);

      ByteArrayInputStream var5;
      try {
         int i = nativeImage.getWidth();
         int j = nativeImage.getHeight();
         NativeImage nativeImage2 = new NativeImage(2 * i, 2 * j, true);

         try {
            loadBytes(nativeImage, nativeImage2, 0, 0, 0, 0, i, j, 1, false, false);
            var5 = new ByteArrayInputStream(nativeImage2.getBytes());
         } catch (Throwable var9) {
            try {
               nativeImage2.close();
            } catch (Throwable var8) {
               var9.addSuppressed(var8);
            }

            throw var9;
         }

         nativeImage2.close();
      } catch (Throwable var10) {
         if (nativeImage != null) {
            try {
               nativeImage.close();
            } catch (Throwable var7) {
               var10.addSuppressed(var7);
            }
         }

         throw var10;
      }

      if (nativeImage != null) {
         nativeImage.close();
      }

      return var5;
   }

   public static InputStream cropLeftChestTexture(InputStream inputStream) throws IOException {
      NativeImage nativeImage = NativeImage.read(inputStream);

      ByteArrayInputStream var6;
      try {
         int i = nativeImage.getWidth();
         int j = nativeImage.getHeight();
         NativeImage nativeImage2 = new NativeImage(i / 2, j, true);

         try {
            int k = j / 64;
            loadBytes(nativeImage, nativeImage2, 29, 0, 29, 0, 15, 14, k, false, true);
            loadBytes(nativeImage, nativeImage2, 59, 0, 14, 0, 15, 14, k, false, true);
            loadBytes(nativeImage, nativeImage2, 29, 14, 43, 14, 15, 5, k, true, true);
            loadBytes(nativeImage, nativeImage2, 44, 14, 29, 14, 14, 5, k, true, true);
            loadBytes(nativeImage, nativeImage2, 58, 14, 14, 14, 15, 5, k, true, true);
            loadBytes(nativeImage, nativeImage2, 29, 19, 29, 19, 15, 14, k, false, true);
            loadBytes(nativeImage, nativeImage2, 59, 19, 14, 19, 15, 14, k, false, true);
            loadBytes(nativeImage, nativeImage2, 29, 33, 43, 33, 15, 10, k, true, true);
            loadBytes(nativeImage, nativeImage2, 44, 33, 29, 33, 14, 10, k, true, true);
            loadBytes(nativeImage, nativeImage2, 58, 33, 14, 33, 15, 10, k, true, true);
            loadBytes(nativeImage, nativeImage2, 2, 0, 2, 0, 1, 1, k, false, true);
            loadBytes(nativeImage, nativeImage2, 4, 0, 1, 0, 1, 1, k, false, true);
            loadBytes(nativeImage, nativeImage2, 2, 1, 3, 1, 1, 4, k, true, true);
            loadBytes(nativeImage, nativeImage2, 3, 1, 2, 1, 1, 4, k, true, true);
            loadBytes(nativeImage, nativeImage2, 4, 1, 1, 1, 1, 4, k, true, true);
            var6 = new ByteArrayInputStream(nativeImage2.getBytes());
         } catch (Throwable var9) {
            try {
               nativeImage2.close();
            } catch (Throwable var8) {
               var9.addSuppressed(var8);
            }

            throw var9;
         }

         nativeImage2.close();
      } catch (Throwable var10) {
         if (nativeImage != null) {
            try {
               nativeImage.close();
            } catch (Throwable var7) {
               var10.addSuppressed(var7);
            }
         }

         throw var10;
      }

      if (nativeImage != null) {
         nativeImage.close();
      }

      return var6;
   }

   public static InputStream cropRightChestTexture(InputStream inputStream) throws IOException {
      NativeImage nativeImage = NativeImage.read(inputStream);

      ByteArrayInputStream var6;
      try {
         int i = nativeImage.getWidth();
         int j = nativeImage.getHeight();
         NativeImage nativeImage2 = new NativeImage(i / 2, j, true);

         try {
            int k = j / 64;
            loadBytes(nativeImage, nativeImage2, 14, 0, 29, 0, 15, 14, k, false, true);
            loadBytes(nativeImage, nativeImage2, 44, 0, 14, 0, 15, 14, k, false, true);
            loadBytes(nativeImage, nativeImage2, 0, 14, 0, 14, 14, 5, k, true, true);
            loadBytes(nativeImage, nativeImage2, 14, 14, 43, 14, 15, 5, k, true, true);
            loadBytes(nativeImage, nativeImage2, 73, 14, 14, 14, 15, 5, k, true, true);
            loadBytes(nativeImage, nativeImage2, 14, 19, 29, 19, 15, 14, k, false, true);
            loadBytes(nativeImage, nativeImage2, 44, 19, 14, 19, 15, 14, k, false, true);
            loadBytes(nativeImage, nativeImage2, 0, 33, 0, 33, 14, 10, k, true, true);
            loadBytes(nativeImage, nativeImage2, 14, 33, 43, 33, 15, 10, k, true, true);
            loadBytes(nativeImage, nativeImage2, 73, 33, 14, 33, 15, 10, k, true, true);
            loadBytes(nativeImage, nativeImage2, 1, 0, 2, 0, 1, 1, k, false, true);
            loadBytes(nativeImage, nativeImage2, 3, 0, 1, 0, 1, 1, k, false, true);
            loadBytes(nativeImage, nativeImage2, 0, 1, 0, 1, 1, 4, k, true, true);
            loadBytes(nativeImage, nativeImage2, 1, 1, 3, 1, 1, 4, k, true, true);
            loadBytes(nativeImage, nativeImage2, 5, 1, 1, 1, 1, 4, k, true, true);
            var6 = new ByteArrayInputStream(nativeImage2.getBytes());
         } catch (Throwable var9) {
            try {
               nativeImage2.close();
            } catch (Throwable var8) {
               var9.addSuppressed(var8);
            }

            throw var9;
         }

         nativeImage2.close();
      } catch (Throwable var10) {
         if (nativeImage != null) {
            try {
               nativeImage.close();
            } catch (Throwable var7) {
               var10.addSuppressed(var7);
            }
         }

         throw var10;
      }

      if (nativeImage != null) {
         nativeImage.close();
      }

      return var6;
   }

   public static InputStream cropSingleChestTexture(InputStream inputStream) throws IOException {
      NativeImage nativeImage = NativeImage.read(inputStream);

      ByteArrayInputStream var6;
      try {
         int i = nativeImage.getWidth();
         int j = nativeImage.getHeight();
         NativeImage nativeImage2 = new NativeImage(i, j, true);

         try {
            int k = j / 64;
            loadBytes(nativeImage, nativeImage2, 14, 0, 28, 0, 14, 14, k, false, true);
            loadBytes(nativeImage, nativeImage2, 28, 0, 14, 0, 14, 14, k, false, true);
            loadBytes(nativeImage, nativeImage2, 0, 14, 0, 14, 14, 5, k, true, true);
            loadBytes(nativeImage, nativeImage2, 14, 14, 42, 14, 14, 5, k, true, true);
            loadBytes(nativeImage, nativeImage2, 28, 14, 28, 14, 14, 5, k, true, true);
            loadBytes(nativeImage, nativeImage2, 42, 14, 14, 14, 14, 5, k, true, true);
            loadBytes(nativeImage, nativeImage2, 14, 19, 28, 19, 14, 14, k, false, true);
            loadBytes(nativeImage, nativeImage2, 28, 19, 14, 19, 14, 14, k, false, true);
            loadBytes(nativeImage, nativeImage2, 0, 33, 0, 33, 14, 10, k, true, true);
            loadBytes(nativeImage, nativeImage2, 14, 33, 42, 33, 14, 10, k, true, true);
            loadBytes(nativeImage, nativeImage2, 28, 33, 28, 33, 14, 10, k, true, true);
            loadBytes(nativeImage, nativeImage2, 42, 33, 14, 33, 14, 10, k, true, true);
            loadBytes(nativeImage, nativeImage2, 1, 0, 3, 0, 2, 1, k, false, true);
            loadBytes(nativeImage, nativeImage2, 3, 0, 1, 0, 2, 1, k, false, true);
            loadBytes(nativeImage, nativeImage2, 0, 1, 0, 1, 1, 4, k, true, true);
            loadBytes(nativeImage, nativeImage2, 1, 1, 4, 1, 2, 4, k, true, true);
            loadBytes(nativeImage, nativeImage2, 3, 1, 3, 1, 1, 4, k, true, true);
            loadBytes(nativeImage, nativeImage2, 4, 1, 1, 1, 2, 4, k, true, true);
            var6 = new ByteArrayInputStream(nativeImage2.getBytes());
         } catch (Throwable var9) {
            try {
               nativeImage2.close();
            } catch (Throwable var8) {
               var9.addSuppressed(var8);
            }

            throw var9;
         }

         nativeImage2.close();
      } catch (Throwable var10) {
         if (nativeImage != null) {
            try {
               nativeImage.close();
            } catch (Throwable var7) {
               var10.addSuppressed(var7);
            }
         }

         throw var10;
      }

      if (nativeImage != null) {
         nativeImage.close();
      }

      return var6;
   }

   public Collection<Identifier> findResources(ResourceType type, String namespace, String prefix, int maxDepth, Predicate<String> pathFilter) {
      return this.parent.findResources(type, namespace, prefix, maxDepth, pathFilter);
   }

   public Set<String> getNamespaces(ResourceType type) {
      return this.parent.getNamespaces(type);
   }

   @Nullable
   public <T> T parseMetadata(ResourceMetadataReader<T> metaReader) throws IOException {
      return this.parent.parseMetadata(metaReader);
   }

   public String getName() {
      return this.parent.getName();
   }

   public void close() {
      this.parent.close();
   }

   private static void loadBytes(NativeImage source, NativeImage target, int i, int j, int k, int l, int m, int n, int o, boolean bl, boolean bl2) {
      n *= o;
      m *= o;
      k *= o;
      l *= o;
      i *= o;
      j *= o;

      for(int p = 0; p < n; ++p) {
         for(int q = 0; q < m; ++q) {
            target.setPixelColor(k + q, l + p, source.getPixelColor(i + (bl ? m - 1 - q : q), j + (bl2 ? n - 1 - p : p)));
         }
      }

   }

   static {
      SHIELD_PATTERN_TEXTURES = (Set)BANNER_PATTERN_TYPES.stream().map((string) -> {
         return "textures/entity/shield/" + string + ".png";
      }).collect(Collectors.toSet());
      BANNER_PATTERN_TEXTURES = (Set)BANNER_PATTERN_TYPES.stream().map((string) -> {
         return "textures/entity/banner/" + string + ".png";
      }).collect(Collectors.toSet());
      OLD_SHIELD_BASE_TEXTURE = new Identifier("textures/entity/shield_base.png");
      OLD_BANNER_BASE_TEXTURE = new Identifier("textures/entity/banner_base.png");
      IRON_GOLEM_TEXTURE = new Identifier("textures/entity/iron_golem.png");
   }
}
