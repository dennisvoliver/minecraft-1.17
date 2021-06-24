package net.minecraft.world.chunk;

public class ColumnChunkNibbleArray extends ChunkNibbleArray {
   public static final int field_31707 = 128;

   public ColumnChunkNibbleArray() {
      super(128);
   }

   public ColumnChunkNibbleArray(ChunkNibbleArray chunkNibbleArray, int offset) {
      super(128);
      System.arraycopy(chunkNibbleArray.asByteArray(), offset * 128, this.bytes, 0, 128);
   }

   protected int getIndex(int x, int y, int z) {
      return z << 4 | x;
   }

   public byte[] asByteArray() {
      byte[] bs = new byte[2048];

      for(int i = 0; i < 16; ++i) {
         System.arraycopy(this.bytes, 0, bs, i * 128, 128);
      }

      return bs;
   }
}
