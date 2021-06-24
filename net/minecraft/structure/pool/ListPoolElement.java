package net.minecraft.structure.pool;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.structure.Structure;
import net.minecraft.structure.StructureManager;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

public class ListPoolElement extends StructurePoolElement {
   public static final Codec<ListPoolElement> CODEC = RecordCodecBuilder.create((instance) -> {
      return instance.group(StructurePoolElement.CODEC.listOf().fieldOf("elements").forGetter((listPoolElement) -> {
         return listPoolElement.elements;
      }), method_28883()).apply(instance, (BiFunction)(ListPoolElement::new));
   });
   private final List<StructurePoolElement> elements;

   public ListPoolElement(List<StructurePoolElement> elements, StructurePool.Projection projection) {
      super(projection);
      if (elements.isEmpty()) {
         throw new IllegalArgumentException("Elements are empty");
      } else {
         this.elements = elements;
         this.setAllElementsProjection(projection);
      }
   }

   public Vec3i getStart(StructureManager structureManager, BlockRotation rotation) {
      int i = 0;
      int j = 0;
      int k = 0;

      Vec3i vec3i;
      for(Iterator var6 = this.elements.iterator(); var6.hasNext(); k = Math.max(k, vec3i.getZ())) {
         StructurePoolElement structurePoolElement = (StructurePoolElement)var6.next();
         vec3i = structurePoolElement.getStart(structureManager, rotation);
         i = Math.max(i, vec3i.getX());
         j = Math.max(j, vec3i.getY());
      }

      return new Vec3i(i, j, k);
   }

   public List<Structure.StructureBlockInfo> getStructureBlockInfos(StructureManager structureManager, BlockPos pos, BlockRotation rotation, Random random) {
      return ((StructurePoolElement)this.elements.get(0)).getStructureBlockInfos(structureManager, pos, rotation, random);
   }

   public BlockBox getBoundingBox(StructureManager structureManager, BlockPos pos, BlockRotation rotation) {
      Stream<BlockBox> stream = this.elements.stream().filter((structurePoolElement) -> {
         return structurePoolElement != EmptyPoolElement.INSTANCE;
      }).map((structurePoolElement) -> {
         return structurePoolElement.getBoundingBox(structureManager, pos, rotation);
      });
      Objects.requireNonNull(stream);
      return (BlockBox)BlockBox.encompass(stream::iterator).orElseThrow(() -> {
         return new IllegalStateException("Unable to calculate boundingbox for ListPoolElement");
      });
   }

   public boolean generate(StructureManager structureManager, StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator, BlockPos pos, BlockPos blockPos, BlockRotation rotation, BlockBox box, Random random, boolean keepJigsaws) {
      Iterator var11 = this.elements.iterator();

      StructurePoolElement structurePoolElement;
      do {
         if (!var11.hasNext()) {
            return true;
         }

         structurePoolElement = (StructurePoolElement)var11.next();
      } while(structurePoolElement.generate(structureManager, world, structureAccessor, chunkGenerator, pos, blockPos, rotation, box, random, keepJigsaws));

      return false;
   }

   public StructurePoolElementType<?> getType() {
      return StructurePoolElementType.LIST_POOL_ELEMENT;
   }

   public StructurePoolElement setProjection(StructurePool.Projection projection) {
      super.setProjection(projection);
      this.setAllElementsProjection(projection);
      return this;
   }

   public String toString() {
      Stream var10000 = this.elements.stream().map(Object::toString);
      return "List[" + (String)var10000.collect(Collectors.joining(", ")) + "]";
   }

   private void setAllElementsProjection(StructurePool.Projection projection) {
      this.elements.forEach((structurePoolElement) -> {
         structurePoolElement.setProjection(projection);
      });
   }
}
