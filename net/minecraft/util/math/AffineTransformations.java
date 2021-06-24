package net.minecraft.util.math;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AffineTransformations {
   private static final Logger LOGGER = LogManager.getLogger();
   public static final Map<Direction, AffineTransformation> DIRECTION_ROTATIONS = (Map)Util.make(Maps.newEnumMap(Direction.class), (enumMap) -> {
      enumMap.put(Direction.SOUTH, AffineTransformation.identity());
      enumMap.put(Direction.EAST, new AffineTransformation((Vec3f)null, Vec3f.POSITIVE_Y.getDegreesQuaternion(90.0F), (Vec3f)null, (Quaternion)null));
      enumMap.put(Direction.WEST, new AffineTransformation((Vec3f)null, Vec3f.POSITIVE_Y.getDegreesQuaternion(-90.0F), (Vec3f)null, (Quaternion)null));
      enumMap.put(Direction.NORTH, new AffineTransformation((Vec3f)null, Vec3f.POSITIVE_Y.getDegreesQuaternion(180.0F), (Vec3f)null, (Quaternion)null));
      enumMap.put(Direction.UP, new AffineTransformation((Vec3f)null, Vec3f.POSITIVE_X.getDegreesQuaternion(-90.0F), (Vec3f)null, (Quaternion)null));
      enumMap.put(Direction.DOWN, new AffineTransformation((Vec3f)null, Vec3f.POSITIVE_X.getDegreesQuaternion(90.0F), (Vec3f)null, (Quaternion)null));
   });
   public static final Map<Direction, AffineTransformation> INVERTED_DIRECTION_ROTATIONS = (Map)Util.make(Maps.newEnumMap(Direction.class), (enumMap) -> {
      Direction[] var1 = Direction.values();
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         Direction direction = var1[var3];
         enumMap.put(direction, ((AffineTransformation)DIRECTION_ROTATIONS.get(direction)).invert());
      }

   });

   public static AffineTransformation setupUvLock(AffineTransformation affineTransformation) {
      Matrix4f matrix4f = Matrix4f.translate(0.5F, 0.5F, 0.5F);
      matrix4f.multiply(affineTransformation.getMatrix());
      matrix4f.multiply(Matrix4f.translate(-0.5F, -0.5F, -0.5F));
      return new AffineTransformation(matrix4f);
   }

   public static AffineTransformation method_35829(AffineTransformation affineTransformation) {
      Matrix4f matrix4f = Matrix4f.translate(-0.5F, -0.5F, -0.5F);
      matrix4f.multiply(affineTransformation.getMatrix());
      matrix4f.multiply(Matrix4f.translate(0.5F, 0.5F, 0.5F));
      return new AffineTransformation(matrix4f);
   }

   public static AffineTransformation uvLock(AffineTransformation affineTransformation, Direction direction, Supplier<String> supplier) {
      Direction direction2 = Direction.transform(affineTransformation.getMatrix(), direction);
      AffineTransformation affineTransformation2 = affineTransformation.invert();
      if (affineTransformation2 == null) {
         LOGGER.warn((String)supplier.get());
         return new AffineTransformation((Vec3f)null, (Quaternion)null, new Vec3f(0.0F, 0.0F, 0.0F), (Quaternion)null);
      } else {
         AffineTransformation affineTransformation3 = ((AffineTransformation)INVERTED_DIRECTION_ROTATIONS.get(direction)).multiply(affineTransformation2).multiply((AffineTransformation)DIRECTION_ROTATIONS.get(direction2));
         return setupUvLock(affineTransformation3);
      }
   }
}
