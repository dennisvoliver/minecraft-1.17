package net.minecraft.network;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import io.netty.util.ByteProcessor;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtTagSizeTracker;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * A packet byte buf is a specialized byte buf with utility methods adapted
 * to Minecraft's protocol. It has serialization and deserialization of
 * custom objects.
 * 
 * <div class="fabric"><table border=1>
 * <caption>Custom object handling</caption>
 * <tr>
 *  <th><b>Object Type</b></th> <th><b>read method</b></th> <th><b>write method</b></th>
 * </tr>
 * <tr>
 *  <td>Codec-based</td><td>{@link #decode(Codec)}</td><td>{@link #encode(Codec, Object)}</td>
 * </tr>
 * <tr>
 *  <td>{@link Collection}</td><td>{@link #readCollection(IntFunction, Function)}</td><td>{@link #writeCollection(Collection, BiConsumer)}</td>
 * </tr>
 * <tr>
 *  <td>{@link IntList}</td><td>{@link #readIntList()}</td><td>{@link #writeIntList(IntList)}</td>
 * </tr>
 * <tr>
 *  <td>{@link Map}</td><td>{@link #readMap(IntFunction, Function, Function)}</td><td>{@link #writeMap(Map, BiConsumer, BiConsumer)}</td>
 * </tr>
 * <tr>
 *  <td>{@code byte[]}</td><td>{@link #readByteArray()}</td><td>{@link #writeByteArray(byte[])}</td>
 * </tr>
 * <tr>
 *  <td>{@code int[]}</td><td>{@link #readIntArray()}</td><td>{@link #writeIntArray(int[])}</td>
 * </tr>
 * <tr>
 *  <td>{@code long[]}</td><td>{@link #readLongArray()}</td><td>{@link #writeLongArray(long[])}</td>
 * </tr>
 * <tr>
 *  <td>{@link BlockPos}</td><td>{@link #readBlockPos()}</td><td>{@link #writeBlockPos(BlockPos)}</td>
 * </tr>
 * <tr>
 *  <td>{@link ChunkPos}</td><td>{@link #readChunkPos()}</td><td>{@link #writeChunkPos(ChunkPos)}</td>
 * </tr>
 * <tr>
 *  <td>{@link ChunkSectionPos}</td><td>{@link #readChunkSectionPos()}</td><td>{@link #writeChunkSectionPos(ChunkSectionPos)}</td>
 * </tr>
 * <tr>
 *  <td>{@link Text}</td><td>{@link #readText()}</td><td>{@link #writeText(Text)}</td>
 * </tr>
 * <tr>
 *  <td>{@link Enum}</td><td>{@link #readEnumConstant(Class)}</td><td>{@link #writeEnumConstant(Enum)}</td>
 * </tr>
 * <tr>
 *  <td>{@index VarInt}</td><td>{@link #readVarInt()}</td><td>{@link #writeVarInt(int)}</td>
 * </tr>
 * <tr>
 *  <td>{@index VarLong}</td><td>{@link #readVarLong()}</td><td>{@link #writeVarLong(long)}</td>
 * </tr>
 * <tr>
 *  <td>{@link UUID}</td><td>{@link #readUuid()}</td><td>{@link #writeUuid(UUID)}</td>
 * </tr>
 * <tr>
 *  <td>{@link NbtCompound}</td><td>{@link #readNbt()}</td><td>{@link #writeNbt(NbtCompound)}</td>
 * </tr>
 * <tr>
 *  <td>{@link ItemStack}</td><td>{@link #readItemStack()}</td><td>{@link #writeItemStack(ItemStack)}</td>
 * </tr>
 * <tr>
 *  <td>{@link String}</td><td>{@link #readString()}</td><td>{@link #writeString(String)}</td>
 * </tr>
 * <tr>
 *  <td>{@link Identifier}</td><td>{@link #readIdentifier()}</td><td>{@link #writeIdentifier(Identifier)}</td>
 * </tr>
 * <tr>
 *  <td>{@link Date}</td><td>{@link #readDate()}</td><td>{@link #writeDate(Date)}</td>
 * </tr>
 * <tr>
 *  <td>{@link BlockHitResult}</td><td>{@link #readBlockHitResult()}</td><td>{@link #writeBlockHitResult(BlockHitResult)}</td>
 * </tr>
 * <tr>
 *  <td>{@link BitSet}</td><td>{@link #readBitSet()}</td><td>{@link #writeBitSet(BitSet)}</td>
 * </tr>
 * </table></div>
 * 
 * <p>All {@code read} and {@code write} methods throw {@link
 * IndexOutOfBoundsException} if there is not enough bytes to be read or
 * not enough space to write.
 */
public class PacketByteBuf extends ByteBuf {
   /**
    * The max number of bytes an encoded var int value may use.
    * 
    * <p>Its value is {@value}. A regular int value always use 4 bytes in contrast.
    * 
    * @see #getVarIntLength(int)
    */
   private static final int MAX_VAR_INT_LENGTH = 5;
   /**
    * The max number of bytes an encoded var long value may use.
    * 
    * <p>Its value is {@value}. A regular long value always use 8 bytes in contrast.
    * 
    * @see #getVarLongLength(long)
    */
   private static final int MAX_VAR_LONG_LENGTH = 10;
   /**
    * The maximum size, in number of bytes, allowed of the NBT compound read by
    * {@link #readNbt()}.
    */
   private static final int MAX_READ_NBT_SIZE = 2097152;
   private final ByteBuf parent;
   /**
    * The default max length of strings {@linkplain #readString() read} or {@linkplain
    * #writeString(String) written}. This is also the max length of identifiers
    * {@linkplain #readIdentifier() read} or {@linkplain #writeIdentifier(Identifier)
    * written} in their string form.
    */
   public static final short DEFAULT_MAX_STRING_LENGTH = 32767;
   /**
    * The maximum size, in terms of JSON string length, allowed of the text read by
    * {@link #readText()} or written by {@link #writeText(Text)}.
    */
   public static final int MAX_TEXT_LENGTH = 262144;

   /**
    * Creates a packet byte buf that delegates its operations to the {@code
    * parent} buf.
    * 
    * @param parent the parent, or delegate, buf
    */
   public PacketByteBuf(ByteBuf parent) {
      this.parent = parent;
   }

   /**
    * Returns the number of bytes needed to encode {@code value} as a
    * {@linkplain #writeVarInt(int) var int}. Guaranteed to be between {@code
    * 1} and {@value #MAX_VAR_INT_LENGTH}.
    * 
    * @return the number of bytes a var int {@code value} uses
    * 
    * @param value the value to encode
    */
   public static int getVarIntLength(int value) {
      for(int i = 1; i < MAX_VAR_INT_LENGTH; ++i) {
         if ((value & -1 << i * 7) == 0) {
            return i;
         }
      }

      return MAX_VAR_INT_LENGTH;
   }

   /**
    * Returns the number of bytes needed to encode {@code value} as a
    * {@linkplain #writeVarLong(int) var long}. Guaranteed to be between {@code
    * 1} and {@value #MAX_VAR_LONG_LENGTH}.
    * 
    * @return the number of bytes a var long {@code value} uses
    * 
    * @param value the value to encode
    */
   public static int getVarLongLength(long value) {
      for(int i = 1; i < MAX_VAR_LONG_LENGTH; ++i) {
         if ((value & -1L << i * 7) == 0L) {
            return i;
         }
      }

      return MAX_VAR_LONG_LENGTH;
   }

   /**
    * Reads an object from this buf as a compound NBT with the given codec.
    * 
    * @param <T> the decoded object's type
    * @return the read object
    * @throws io.netty.handler.codec.EncoderException if the {@code codec} fails
    * to decode the compound NBT
    * @see #encode(Codec, Object)
    * 
    * @param codec the codec to decode the object
    */
   public <T> T decode(Codec<T> codec) {
      NbtCompound nbtCompound = this.readUnlimitedNbt();
      DataResult<T> dataResult = codec.parse(NbtOps.INSTANCE, nbtCompound);
      dataResult.error().ifPresent((partialResult) -> {
         String var10002 = partialResult.message();
         throw new EncoderException("Failed to decode: " + var10002 + " " + nbtCompound);
      });
      return dataResult.result().get();
   }

   /**
    * Writes an object to this buf as a compound NBT with the given codec.
    * 
    * @param <T> the encoded object's type
    * @throws io.netty.handler.codec.EncoderException if the {@code codec} fails
    * to encode the compound NBT
    * @see #decode(Codec)
    * 
    * @param codec the codec to encode the object
    * @param object the object to write to this buf
    */
   public <T> void encode(Codec<T> codec, T object) {
      DataResult<NbtElement> dataResult = codec.encodeStart(NbtOps.INSTANCE, object);
      dataResult.error().ifPresent((partialResult) -> {
         String var10002 = partialResult.message();
         throw new EncoderException("Failed to encode: " + var10002 + " " + object);
      });
      this.writeNbt((NbtCompound)dataResult.result().get());
   }

   /**
    * Reads a collection from this buf. The collection is stored as a leading
    * {@linkplain #readVarInt() var int} size followed by the entries
    * sequentially.
    * 
    * @param <T> the collection's entry type
    * @param <C> the collection's type
    * @return the read collection
    * @see #writeCollection(Collection, BiConsumer)
    * @see #readList(Function)
    * 
    * @param collectionFactory a factory that creates a collection with a given size
    * @param entryParser a parser that parses each entry for the collection given this buf
    */
   public <T, C extends Collection<T>> C readCollection(IntFunction<C> collectionFactory, Function<PacketByteBuf, T> entryParser) {
      int i = this.readVarInt();
      C collection = (Collection)collectionFactory.apply(i);

      for(int j = 0; j < i; ++j) {
         collection.add(entryParser.apply(this));
      }

      return collection;
   }

   /**
    * Writes a collection to this buf. The collection is stored as a leading
    * {@linkplain #readVarInt() var int} size followed by the entries
    * sequentially.
    * 
    * @param <T> the list's entry type
    * @see #readCollection(IntFunction, Function)
    * 
    * @param collection the collection to write
    * @param entrySerializer a serializer that writes each entry to this buf
    */
   public <T> void writeCollection(Collection<T> collection, BiConsumer<PacketByteBuf, T> entrySerializer) {
      this.writeVarInt(collection.size());
      Iterator var3 = collection.iterator();

      while(var3.hasNext()) {
         T object = var3.next();
         entrySerializer.accept(this, object);
      }

   }

   /**
    * Reads a collection from this buf as an array list.
    * 
    * @param <T> the list's entry type
    * @return the read list
    * @see #readCollection(IntFunction, Function)
    * 
    * @param entryParser a parser that parses each entry for the collection given this buf
    */
   public <T> List<T> readList(Function<PacketByteBuf, T> entryParser) {
      return (List)this.readCollection(Lists::newArrayListWithCapacity, entryParser);
   }

   /**
    * Reads a list of primitive ints from this buf. The ints are stored as var
    * ints, with an extra var int in the beginning indicating the size.
    * 
    * @apiNote To limit the length of the list or array read, use
    * {@link #readIntArray(int)}.
    * 
    * @implNote A list of ints has the same format as an int array.
    * 
    * @return the read list
    * @see #writeIntList(IntList)
    * @see #readIntArray()
    */
   public IntList readIntList() {
      int i = this.readVarInt();
      IntList intList = new IntArrayList();

      for(int j = 0; j < i; ++j) {
         intList.add(this.readVarInt());
      }

      return intList;
   }

   /**
    * Writes a list of primitive ints from this buf. The ints are stored as var
    * ints, with an extra var int in the beginning indicating the size.
    * 
    * @implNote A list of ints has the same format as an int array.
    * 
    * @see #readIntList()
    * @see #writeIntArray(int[])
    * 
    * @param list the list to write
    */
   public void writeIntList(IntList list) {
      this.writeVarInt(list.size());
      list.forEach(this::writeVarInt);
   }

   /**
    * Reads a map from this buf. The map is stored as a leading
    * {@linkplain #readVarInt() var int} size followed by each key and value
    * pair.
    * 
    * @param <K> the key type
    * @param <V> the value type
    * @param <M> the map type
    * @return the read map
    * @see #writeMap(Map, BiConsumer, BiConsumer)
    * @see #readMap(Function, Function)
    * 
    * @param mapFactory a factory that creates a map with a given size
    * @param keyParser a parser that parses each key for the map given this buf
    * @param valueParser a parser that parses each value for the map given this buf
    */
   public <K, V, M extends Map<K, V>> M readMap(IntFunction<M> mapFactory, Function<PacketByteBuf, K> keyParser, Function<PacketByteBuf, V> valueParser) {
      int i = this.readVarInt();
      M map = (Map)mapFactory.apply(i);

      for(int j = 0; j < i; ++j) {
         K object = keyParser.apply(this);
         V object2 = valueParser.apply(this);
         map.put(object, object2);
      }

      return map;
   }

   /**
    * Reads a map from this buf as a hash map.
    * 
    * @param <K> the key type
    * @param <V> the value type
    * @return the read map
    * @see #readMap(IntFunction, Function, Function)
    * 
    * @param keyParser a parser that parses each key for the map given this buf
    * @param valueParser a parser that parses each value for the map given this buf
    */
   public <K, V> Map<K, V> readMap(Function<PacketByteBuf, K> keyParser, Function<PacketByteBuf, V> valueParser) {
      return this.readMap(Maps::newHashMapWithExpectedSize, keyParser, valueParser);
   }

   /**
    * Writes a map to this buf. The map is stored as a leading
    * {@linkplain #readVarInt() var int} size followed by each key and value
    * pair.
    * 
    * @param <K> the key type
    * @param <V> the value type
    * @see #readMap(IntFunction, Function, Function)
    * 
    * @param map the map to write
    * @param keySerializer a serializer that writes each key in the map to this buf
    * @param valueSerializer a serializer that writes each value in the map to this buf
    */
   public <K, V> void writeMap(Map<K, V> map, BiConsumer<PacketByteBuf, K> keySerializer, BiConsumer<PacketByteBuf, V> valueSerializer) {
      this.writeVarInt(map.size());
      map.forEach((key, value) -> {
         keySerializer.accept(this, key);
         valueSerializer.accept(this, value);
      });
   }

   /**
    * Iterates a collection from this buf. The collection is stored as a leading
    * {@linkplain #readVarInt() var int} {@code size} followed by the entries
    * sequentially. The {@code consumer} will be called {@code size} times.
    * 
    * @see #readCollection(IntFunction, Function)
    * 
    * @param consumer the consumer to read entries
    */
   public void forEachInCollection(Consumer<PacketByteBuf> consumer) {
      int i = this.readVarInt();

      for(int j = 0; j < i; ++j) {
         consumer.accept(this);
      }

   }

   /**
    * Reads an array of primitive bytes from this buf. The array first has a
    * var int indicating its length, followed by the actual bytes. The array
    * does not have a length limit.
    * 
    * @see #readByteArray(int)
    * @see #writeByteArray(byte[])
    * @return the read byte array
    */
   public byte[] readByteArray() {
      return this.readByteArray(this.readableBytes());
   }

   /**
    * Writes an array of primitive bytes to this buf. The array first has a
    * var int indicating its length, followed by the actual bytes.
    * 
    * @see #readByteArray()
    * @return this buf, for chaining
    * 
    * @param array the array to write
    */
   public PacketByteBuf writeByteArray(byte[] array) {
      this.writeVarInt(array.length);
      this.writeBytes(array);
      return this;
   }

   /**
    * Reads an array of primitive bytes from this buf. The array first has a
    * var int indicating its length, followed by the actual bytes. The array
    * has a length limit given by {@code maxSize}.
    * 
    * @see #readByteArray()
    * @see #writeByteArray(byte[])
    * @return the read byte array
    * @throws io.netty.handler.codec.DecoderException if the read array has a
    * length over {@code maxSize}
    * 
    * @param maxSize the max length of the read array
    */
   public byte[] readByteArray(int maxSize) {
      int i = this.readVarInt();
      if (i > maxSize) {
         throw new DecoderException("ByteArray with size " + i + " is bigger than allowed " + maxSize);
      } else {
         byte[] bs = new byte[i];
         this.readBytes(bs);
         return bs;
      }
   }

   /**
    * Writes an array of primitive ints to this buf. The array first has a
    * var int indicating its length, followed by the var int entries.
    * 
    * @implNote An int array has the same format as a list of ints.
    * 
    * @see #readIntArray(int)
    * @see #writeIntArray(int[])
    * @see #writeIntList(IntList)
    * @return this buf, for chaining
    * 
    * @param array the array to write
    */
   public PacketByteBuf writeIntArray(int[] array) {
      this.writeVarInt(array.length);
      int[] var2 = array;
      int var3 = array.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         int i = var2[var4];
         this.writeVarInt(i);
      }

      return this;
   }

   /**
    * Reads an array of primitive ints from this buf. The array first has a
    * var int indicating its length, followed by the var int entries. The array
    * does not have a length limit.
    * 
    * @implNote An int array has the same format as a list of ints.
    * 
    * @see #readIntArray(int)
    * @see #writeIntArray(int[])
    * @see #readIntList()
    * @return the read byte array
    */
   public int[] readIntArray() {
      return this.readIntArray(this.readableBytes());
   }

   /**
    * Reads an array of primitive ints from this buf. The array first has a
    * var int indicating its length, followed by the var int entries. The array
    * has a length limit given by {@code maxSize}.
    * 
    * @implNote An int array has the same format as a list of ints.
    * 
    * @see #readIntArray()
    * @see #writeIntArray(int[])
    * @return the read byte array
    * @throws io.netty.handler.codec.DecoderException if the read array has a
    * length over {@code maxSize}
    * 
    * @param maxSize the max length of the read array
    */
   public int[] readIntArray(int maxSize) {
      int i = this.readVarInt();
      if (i > maxSize) {
         throw new DecoderException("VarIntArray with size " + i + " is bigger than allowed " + maxSize);
      } else {
         int[] is = new int[i];

         for(int j = 0; j < is.length; ++j) {
            is[j] = this.readVarInt();
         }

         return is;
      }
   }

   /**
    * Writes an array of primitive longs to this buf. The array first has a
    * var int indicating its length, followed by the regular long (not var
    * long) values.
    * 
    * @see #readLongArray()
    * @return this buf, for chaining
    * 
    * @param array the array to write
    */
   public PacketByteBuf writeLongArray(long[] array) {
      this.writeVarInt(array.length);
      long[] var2 = array;
      int var3 = array.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         long l = var2[var4];
         this.writeLong(l);
      }

      return this;
   }

   /**
    * Reads an array of primitive longs from this buf. The array first has a
    * var int indicating its length, followed by the regular long (not var
    * long) values. The array does not have a length limit.
    * 
    * @see #writeLongArray(long[])
    * @see #readLongArray(long[])
    * @see #readLongArray(long[], int)
    * @return the read long array
    */
   public long[] readLongArray() {
      return this.readLongArray((long[])null);
   }

   /**
    * Reads an array of primitive longs from this buf. The array first has a
    * var int indicating its length, followed by the regular long (not var
    * long) values. The array does not have a length limit.
    * 
    * <p>Only when {@code toArray} is not {@code null} and {@code
    * toArray.length} equals to the length var int read will the {@code
    * toArray} be reused and returned; otherwise, a new array
    * of proper size is created.
    * 
    * @see #writeLongArray(long[])
    * @see #readLongArray()
    * @see #readLongArray(long[], int)
    * @return the read long array
    * 
    * @param toArray the array to reuse
    */
   public long[] readLongArray(@Nullable long[] toArray) {
      return this.readLongArray(toArray, this.readableBytes() / 8);
   }

   /**
    * Reads an array of primitive longs from this buf. The array first has a
    * var int indicating its length, followed by the regular long (not var
    * long) values. The array has a length limit of {@code maxSize}.
    * 
    * <p>Only when {@code toArray} is not {@code null} and {@code
    * toArray.length} equals to the length var int read will the {@code
    * toArray} be reused and returned; otherwise, a new array
    * of proper size is created.
    * 
    * @see #writeLongArray(long[])
    * @see #readLongArray()
    * @see #readLongArray(long[])
    * @return the read long array
    * @throws io.netty.handler.codec.DecoderException if the read array has a
    * length over {@code maxSize}
    * 
    * @param toArray the array to reuse
    * @param maxSize the max length of the read array
    */
   public long[] readLongArray(@Nullable long[] toArray, int maxSize) {
      int i = this.readVarInt();
      if (toArray == null || toArray.length != i) {
         if (i > maxSize) {
            throw new DecoderException("LongArray with size " + i + " is bigger than allowed " + maxSize);
         }

         toArray = new long[i];
      }

      for(int j = 0; j < toArray.length; ++j) {
         toArray[j] = this.readLong();
      }

      return toArray;
   }

   /**
    * Returns an array of bytes of contents in this buf between index {@code 0} and
    * the {@link #writerIndex()}.
    */
   @VisibleForTesting
   public byte[] getWrittenBytes() {
      int i = this.writerIndex();
      byte[] bs = new byte[i];
      this.getBytes(0, (byte[])bs);
      return bs;
   }

   /**
    * Reads a block position from this buf. A block position is represented by
    * a regular long.
    * 
    * @return the read block pos
    * @see #writeBlockPos(BlockPos)
    */
   public BlockPos readBlockPos() {
      return BlockPos.fromLong(this.readLong());
   }

   /**
    * Writes a block position to this buf. A block position is represented by
    * a regular long.
    * 
    * @return this buf, for chaining
    * @see #readBlockPos()
    * 
    * @param pos the pos to write
    */
   public PacketByteBuf writeBlockPos(BlockPos pos) {
      this.writeLong(pos.asLong());
      return this;
   }

   /**
    * Reads a chunk position from this buf. A chunk position is represented by
    * a regular long.
    * 
    * @return the read chunk position
    * @see #writeChunkPos(ChunkPos)
    */
   public ChunkPos readChunkPos() {
      return new ChunkPos(this.readLong());
   }

   /**
    * Writes a chunk position to this buf. A chunk position is represented by
    * a regular long.
    * 
    * @return this buf, for chaining
    * @see #readChunkPos()
    * 
    * @param pos the chunk position to write
    */
   public PacketByteBuf writeChunkPos(ChunkPos pos) {
      this.writeLong(pos.toLong());
      return this;
   }

   /**
    * Reads a chunk section position from this buf. A chunk section position is
    * represented by a regular long.
    * 
    * @return the read chunk section pos
    * @see #writeChunkSectionPos(ChunkSectionPos)
    */
   public ChunkSectionPos readChunkSectionPos() {
      return ChunkSectionPos.from(this.readLong());
   }

   /**
    * Reads a chunk section position from this buf. A chunk section position is
    * represented by a regular long.
    * 
    * @return this buf, for chaining
    * @see #readChunkSectionPos()
    * 
    * @param pos the section position to write
    */
   public PacketByteBuf writeChunkSectionPos(ChunkSectionPos pos) {
      this.writeLong(pos.asLong());
      return this;
   }

   /**
    * Reads a text from this buf. A text is represented by a JSON string with
    * max length {@value #MAX_TEXT_LENGTH}.
    * 
    * @return the read text
    * @throws io.netty.handler.codec.DecoderException if the JSON string read
    * exceeds {@value #MAX_TEXT_LENGTH} in length
    * @see #writeText(Text)
    * @see #MAX_TEXT_LENGTH
    */
   public Text readText() {
      return Text.Serializer.fromJson(this.readString(MAX_TEXT_LENGTH));
   }

   /**
    * Writes a text to this buf. A text is represented by a JSON string with
    * max length {@value #MAX_TEXT_LENGTH}.
    * 
    * @return this buf, for chaining
    * @throws io.netty.handler.codec.EncoderException if the JSON string
    * written exceeds {@value #MAX_TEXT_LENGTH} in length
    * @see #readText()
    * @see #MAX_TEXT_LENGTH
    * 
    * @param text the text to write
    */
   public PacketByteBuf writeText(Text text) {
      return this.writeString(Text.Serializer.toJson(text), MAX_TEXT_LENGTH);
   }

   /**
    * Reads an enum constant from this buf. An enum constant is represented
    * by a var int indicating its ordinal.
    * 
    * @return the read enum constant
    * @see #writeEnumConstant(Enum)
    * 
    * @param enumClass the enum class, for constant lookup
    */
   public <T extends Enum<T>> T readEnumConstant(Class<T> enumClass) {
      return ((Enum[])enumClass.getEnumConstants())[this.readVarInt()];
   }

   /**
    * Writes an enum constant to this buf. An enum constant is represented
    * by a var int indicating its ordinal.
    * 
    * @return this buf, for chaining
    * @see #readEnumConstant(Class)
    * 
    * @param instance the enum constant to write
    */
   public PacketByteBuf writeEnumConstant(Enum<?> instance) {
      return this.writeVarInt(instance.ordinal());
   }

   /**
    * Reads a single var int from this buf.
    * 
    * @return the value read
    * @see #writeVarInt(int)
    */
   public int readVarInt() {
      int i = 0;
      int j = 0;

      byte b;
      do {
         b = this.readByte();
         i |= (b & 127) << j++ * 7;
         if (j > 5) {
            throw new RuntimeException("VarInt too big");
         }
      } while((b & 128) == 128);

      return i;
   }

   /**
    * Reads a single var long from this buf.
    * 
    * @return the value read
    * @see #writeVarLong(long)
    */
   public long readVarLong() {
      long l = 0L;
      int i = 0;

      byte b;
      do {
         b = this.readByte();
         l |= (long)(b & 127) << i++ * 7;
         if (i > 10) {
            throw new RuntimeException("VarLong too big");
         }
      } while((b & 128) == 128);

      return l;
   }

   /**
    * Writes a UUID (universally unique identifier) to this buf. A UUID is
    * represented by two regular longs.
    * 
    * @return this buf, for chaining
    * @see #readUuid()
    * 
    * @param uuid the UUID to write
    */
   public PacketByteBuf writeUuid(UUID uuid) {
      this.writeLong(uuid.getMostSignificantBits());
      this.writeLong(uuid.getLeastSignificantBits());
      return this;
   }

   /**
    * Reads a UUID (universally unique identifier) from this buf. A UUID is
    * represented by two regular longs.
    * 
    * @return the read UUID
    * @see #writeUuid(UUID)
    */
   public UUID readUuid() {
      return new UUID(this.readLong(), this.readLong());
   }

   /**
    * Writes a single var int to this buf.
    * 
    * <p>Compared to regular ints, var ints may use less bytes (ranging from 1
    * to 5, where regular ints use 4) when representing smaller positive
    * numbers.
    * 
    * @return this buf, for chaining
    * @see #readVarInt()
    * @see #getVarIntLength(int)
    * 
    * @param value the value to write
    */
   public PacketByteBuf writeVarInt(int value) {
      while((value & -128) != 0) {
         this.writeByte(value & 127 | 128);
         value >>>= 7;
      }

      this.writeByte(value);
      return this;
   }

   /**
    * Writes a single var long to this buf.
    * 
    * <p>Compared to regular longs, var longs may use less bytes when
    * representing smaller positive numbers.
    * 
    * @return this buf, for chaining
    * @see #readVarLong()
    * @see #getVarLongLength(long)
    * 
    * @param value the value to write
    */
   public PacketByteBuf writeVarLong(long value) {
      while((value & -128L) != 0L) {
         this.writeByte((int)(value & 127L) | 128);
         value >>>= 7;
      }

      this.writeByte((int)value);
      return this;
   }

   /**
    * Writes an NBT compound to this buf. The binary representation of NBT is
    * handled by {@link net.minecraft.nbt.NbtIo}. If {@code compound} is {@code
    * null}, it is treated as an NBT null.
    * 
    * @return this buf, for chaining
    * @throws io.netty.handler.codec.EncoderException if the NBT cannot be
    * written
    * @see #readNbt()
    * @see #readUnlimitedNbt()
    * @see #readNbt(NbtTagSizeTracker)
    * 
    * @param compound the compound to write
    */
   public PacketByteBuf writeNbt(@Nullable NbtCompound compound) {
      if (compound == null) {
         this.writeByte(0);
      } else {
         try {
            NbtIo.write((NbtCompound)compound, (DataOutput)(new ByteBufOutputStream(this)));
         } catch (IOException var3) {
            throw new EncoderException(var3);
         }
      }

      return this;
   }

   /**
    * Reads an NBT compound from this buf. The binary representation of NBT is
    * handled by {@link net.minecraft.nbt.NbtIo}. If an NBT null is encountered,
    * this method returns {@code null}. The compound can have a maximum size of
    * {@value #MAX_READ_NBT_SIZE} bytes.
    * 
    * @return the read compound, may be {@code null}
    * @throws io.netty.handler.codec.EncoderException if the NBT cannot be read
    * @throws RuntimeException if the compound exceeds the allowed maximum size
    * @see #writeNbt(NbtCompound)
    * @see #readUnlimitedNbt()
    * @see #readNbt(NbtTagSizeTracker)
    * @see #MAX_READ_NBT_SIZE
    */
   @Nullable
   public NbtCompound readNbt() {
      return this.readNbt(new NbtTagSizeTracker(2097152L));
   }

   /**
    * Reads an NBT compound from this buf. The binary representation of NBT is
    * handled by {@link net.minecraft.nbt.NbtIo}. If an NBT null is encountered,
    * this method returns {@code null}. The compound does not have a size limit.
    * 
    * @apiNote Since this version does not have a size limit, it may be
    * vulnerable to malicious NBT spam attacks.
    * 
    * @return the read compound, may be {@code null}
    * @throws io.netty.handler.codec.EncoderException if the NBT cannot be read
    * @see #writeNbt(NbtCompound)
    * @see #readNbt()
    * @see #readNbt(NbtTagSizeTracker)
    */
   @Nullable
   public NbtCompound readUnlimitedNbt() {
      return this.readNbt(NbtTagSizeTracker.EMPTY);
   }

   /**
    * Reads an NBT compound from this buf. The binary representation of NBT is
    * handled by {@link net.minecraft.nbt.NbtIo}. If an NBT null is encountered,
    * this method returns {@code null}. The compound can have a maximum size
    * controlled by the {@code sizeTracker}.
    * 
    * @return the read compound, may be {@code null}
    * @throws io.netty.handler.codec.EncoderException if the NBT cannot be read
    * @throws RuntimeException if the compound exceeds the allowed maximum size
    * @see #writeNbt(NbtCompound)
    * @see #readNbt()
    * @see #readUnlimitedNbt()
    */
   @Nullable
   public NbtCompound readNbt(NbtTagSizeTracker sizeTracker) {
      int i = this.readerIndex();
      byte b = this.readByte();
      if (b == 0) {
         return null;
      } else {
         this.readerIndex(i);

         try {
            return NbtIo.read(new ByteBufInputStream(this), sizeTracker);
         } catch (IOException var5) {
            throw new EncoderException(var5);
         }
      }
   }

   /**
    * Writes an item stack to this buf. An item stack is represented by a
    * boolean indicating whether it exists; if it exists, it is followed by
    * a var int for its raw id, a byte for its count, and an NBT compound for
    * its tag.
    * 
    * @return this buf, for chaining
    * @see #readItemStack()
    * 
    * @param stack the stack to write
    */
   public PacketByteBuf writeItemStack(ItemStack stack) {
      if (stack.isEmpty()) {
         this.writeBoolean(false);
      } else {
         this.writeBoolean(true);
         Item item = stack.getItem();
         this.writeVarInt(Item.getRawId(item));
         this.writeByte(stack.getCount());
         NbtCompound nbtCompound = null;
         if (item.isDamageable() || item.shouldSyncTagToClient()) {
            nbtCompound = stack.getTag();
         }

         this.writeNbt(nbtCompound);
      }

      return this;
   }

   /**
    * Reads an item stack from this buf. An item stack is represented by a
    * boolean indicating whether it exists; if it exists, it is followed by
    * a var int for its raw id, a byte for its count, and an NBT compound for
    * its tag.
    * 
    * @return the read item stack
    * @see #writeItemStack(ItemStack)
    */
   public ItemStack readItemStack() {
      if (!this.readBoolean()) {
         return ItemStack.EMPTY;
      } else {
         int i = this.readVarInt();
         int j = this.readByte();
         ItemStack itemStack = new ItemStack(Item.byRawId(i), j);
         itemStack.setTag(this.readNbt());
         return itemStack;
      }
   }

   /**
    * Reads a string from this buf. A string is represented by a byte array of
    * its UTF-8 data. The string can have a maximum length of {@value
    * #DEFAULT_MAX_STRING_LENGTH}.
    * 
    * @return the string read
    * @throws io.netty.handler.codec.DecoderException if the string read
    * exceeds the maximum length
    * @see #readString(int)
    * @see #writeString(String)
    * @see #writeString(String, int)
    */
   public String readString() {
      return this.readString(DEFAULT_MAX_STRING_LENGTH);
   }

   /**
    * Reads a string from this buf. A string is represented by a byte array of
    * its UTF-8 data. The string can have a maximum length of {@code maxLength}.
    * 
    * @return the string read
    * @throws io.netty.handler.codec.DecoderException if the string read
    * is longer than {@code maxLength}
    * @see #readString()
    * @see #writeString(String)
    * @see #writeString(String, int)
    * 
    * @param maxLength the maximum length of the string read
    */
   public String readString(int maxLength) {
      int i = this.readVarInt();
      if (i > maxLength * 4) {
         throw new DecoderException("The received encoded string buffer length is longer than maximum allowed (" + i + " > " + maxLength * 4 + ")");
      } else if (i < 0) {
         throw new DecoderException("The received encoded string buffer length is less than zero! Weird string!");
      } else {
         String string = this.toString(this.readerIndex(), i, StandardCharsets.UTF_8);
         this.readerIndex(this.readerIndex() + i);
         if (string.length() > maxLength) {
            throw new DecoderException("The received string length is longer than maximum allowed (" + i + " > " + maxLength + ")");
         } else {
            return string;
         }
      }
   }

   /**
    * Writes a string to this buf. A string is represented by a byte array of
    * its UTF-8 data. That byte array can have a maximum length of
    * {@value #DEFAULT_MAX_STRING_LENGTH}.
    * 
    * @return this buf, for chaining
    * @throws io.netty.handler.codec.EncoderException if the byte array of the
    * string to write is longer than {@value #DEFAULT_MAX_STRING_LENGTH}
    * @see #readString()
    * @see #readString(int)
    * @see #writeString(String, int)
    * 
    * @param string the string to write
    */
   public PacketByteBuf writeString(String string) {
      return this.writeString(string, DEFAULT_MAX_STRING_LENGTH);
   }

   /**
    * Writes a string to this buf. A string is represented by a byte array of
    * its UTF-8 data. That byte array can have a maximum length of
    * {@code maxLength}.
    * 
    * @return this buf, for chaining
    * @throws io.netty.handler.codec.EncoderException if the byte array of the
    * string to write is longer than {@code maxLength}
    * @see #readString()
    * @see #readString(int)
    * @see #writeString(String)
    * 
    * @param string the string to write
    * @param maxLength the max length of the byte array
    */
   public PacketByteBuf writeString(String string, int maxLength) {
      byte[] bs = string.getBytes(StandardCharsets.UTF_8);
      if (bs.length > maxLength) {
         throw new EncoderException("String too big (was " + bs.length + " bytes encoded, max " + maxLength + ")");
      } else {
         this.writeVarInt(bs.length);
         this.writeBytes(bs);
         return this;
      }
   }

   /**
    * Reads an identifier from this buf. An identifier is represented by its
    * string form. The read identifier's string form can have a max length of
    * {@value #DEFAULT_MAX_STRING_LENGTH}.
    * 
    * @return the read identifier
    * @throws io.netty.handler.codec.DecoderException if the identifier's
    * string form is longer than {@value #DEFAULT_MAX_STRING_LENGTH}
    * @see #writeIdentifier(Identifier)
    */
   public Identifier readIdentifier() {
      return new Identifier(this.readString(DEFAULT_MAX_STRING_LENGTH));
   }

   /**
    * Writes an identifier to this buf. An identifier is represented by its
    * string form. The written identifier's byte array can have a max length of
    * {@value #DEFAULT_MAX_STRING_LENGTH}.
    * 
    * @return the read identifier
    * @throws io.netty.handler.codec.EncoderException if the {@code id}'s
    * byte array is longer than {@value #DEFAULT_MAX_STRING_LENGTH}
    * @see #readIdentifier()
    * 
    * @param id the identifier to write
    */
   public PacketByteBuf writeIdentifier(Identifier id) {
      this.writeString(id.toString());
      return this;
   }

   /**
    * Reads a date from this buf. A date is represented by its time, a regular
    * long.
    * 
    * @return the read date
    * @see #writeDate(Date)
    */
   public Date readDate() {
      return new Date(this.readLong());
   }

   /**
    * Writes a date to this buf. A date is represented by its time, a regular
    * long.
    * 
    * @return this buf, for chaining
    * @see #readDate()
    * 
    * @param date the date to write
    */
   public PacketByteBuf writeDate(Date date) {
      this.writeLong(date.getTime());
      return this;
   }

   /**
    * Reads a block hit result from this buf. A block hit result is represented
    * by a block position, a direction enum constant, 3 floats for the hit
    * offset position, and a boolean for whether the hit was inside a block.
    * 
    * @return the read block hit result
    * @see #writeBlockHitResult(BlockHitResult)
    */
   public BlockHitResult readBlockHitResult() {
      BlockPos blockPos = this.readBlockPos();
      Direction direction = (Direction)this.readEnumConstant(Direction.class);
      float f = this.readFloat();
      float g = this.readFloat();
      float h = this.readFloat();
      boolean bl = this.readBoolean();
      return new BlockHitResult(new Vec3d((double)blockPos.getX() + (double)f, (double)blockPos.getY() + (double)g, (double)blockPos.getZ() + (double)h), direction, blockPos, bl);
   }

   /**
    * Writes a block hit result to this buf. A block hit result is represented
    * by a block position, a direction enum constant, 3 floats for the hit
    * offset position, and a boolean for whether the hit was inside a block.
    * 
    * @see #readBlockHitResult()
    * 
    * @param hitResult the block hit result to write
    */
   public void writeBlockHitResult(BlockHitResult hitResult) {
      BlockPos blockPos = hitResult.getBlockPos();
      this.writeBlockPos(blockPos);
      this.writeEnumConstant(hitResult.getSide());
      Vec3d vec3d = hitResult.getPos();
      this.writeFloat((float)(vec3d.x - (double)blockPos.getX()));
      this.writeFloat((float)(vec3d.y - (double)blockPos.getY()));
      this.writeFloat((float)(vec3d.z - (double)blockPos.getZ()));
      this.writeBoolean(hitResult.isInsideBlock());
   }

   /**
    * Reads a bit set from this buf. A bit set is represented by a long array.
    * 
    * @return the read bit set
    * @see #writeBitSet(BitSet)
    */
   public BitSet readBitSet() {
      return BitSet.valueOf(this.readLongArray());
   }

   /**
    * Writes a bit set to this buf. A bit set is represented by a long array.
    * 
    * @see #readBitSet()
    * 
    * @param bitSet the bit set to write
    */
   public void writeBitSet(BitSet bitSet) {
      this.writeLongArray(bitSet.toLongArray());
   }

   public int capacity() {
      return this.parent.capacity();
   }

   public ByteBuf capacity(int i) {
      return this.parent.capacity(i);
   }

   public int maxCapacity() {
      return this.parent.maxCapacity();
   }

   public ByteBufAllocator alloc() {
      return this.parent.alloc();
   }

   public ByteOrder order() {
      return this.parent.order();
   }

   public ByteBuf order(ByteOrder byteOrder) {
      return this.parent.order(byteOrder);
   }

   public ByteBuf unwrap() {
      return this.parent.unwrap();
   }

   public boolean isDirect() {
      return this.parent.isDirect();
   }

   public boolean isReadOnly() {
      return this.parent.isReadOnly();
   }

   public ByteBuf asReadOnly() {
      return this.parent.asReadOnly();
   }

   public int readerIndex() {
      return this.parent.readerIndex();
   }

   public ByteBuf readerIndex(int i) {
      return this.parent.readerIndex(i);
   }

   public int writerIndex() {
      return this.parent.writerIndex();
   }

   public ByteBuf writerIndex(int i) {
      return this.parent.writerIndex(i);
   }

   public ByteBuf setIndex(int i, int j) {
      return this.parent.setIndex(i, j);
   }

   public int readableBytes() {
      return this.parent.readableBytes();
   }

   public int writableBytes() {
      return this.parent.writableBytes();
   }

   public int maxWritableBytes() {
      return this.parent.maxWritableBytes();
   }

   public boolean isReadable() {
      return this.parent.isReadable();
   }

   public boolean isReadable(int i) {
      return this.parent.isReadable(i);
   }

   public boolean isWritable() {
      return this.parent.isWritable();
   }

   public boolean isWritable(int i) {
      return this.parent.isWritable(i);
   }

   public ByteBuf clear() {
      return this.parent.clear();
   }

   public ByteBuf markReaderIndex() {
      return this.parent.markReaderIndex();
   }

   public ByteBuf resetReaderIndex() {
      return this.parent.resetReaderIndex();
   }

   public ByteBuf markWriterIndex() {
      return this.parent.markWriterIndex();
   }

   public ByteBuf resetWriterIndex() {
      return this.parent.resetWriterIndex();
   }

   public ByteBuf discardReadBytes() {
      return this.parent.discardReadBytes();
   }

   public ByteBuf discardSomeReadBytes() {
      return this.parent.discardSomeReadBytes();
   }

   public ByteBuf ensureWritable(int i) {
      return this.parent.ensureWritable(i);
   }

   public int ensureWritable(int i, boolean bl) {
      return this.parent.ensureWritable(i, bl);
   }

   public boolean getBoolean(int i) {
      return this.parent.getBoolean(i);
   }

   public byte getByte(int i) {
      return this.parent.getByte(i);
   }

   public short getUnsignedByte(int i) {
      return this.parent.getUnsignedByte(i);
   }

   public short getShort(int i) {
      return this.parent.getShort(i);
   }

   public short getShortLE(int i) {
      return this.parent.getShortLE(i);
   }

   public int getUnsignedShort(int i) {
      return this.parent.getUnsignedShort(i);
   }

   public int getUnsignedShortLE(int i) {
      return this.parent.getUnsignedShortLE(i);
   }

   public int getMedium(int i) {
      return this.parent.getMedium(i);
   }

   public int getMediumLE(int i) {
      return this.parent.getMediumLE(i);
   }

   public int getUnsignedMedium(int i) {
      return this.parent.getUnsignedMedium(i);
   }

   public int getUnsignedMediumLE(int i) {
      return this.parent.getUnsignedMediumLE(i);
   }

   public int getInt(int i) {
      return this.parent.getInt(i);
   }

   public int getIntLE(int i) {
      return this.parent.getIntLE(i);
   }

   public long getUnsignedInt(int i) {
      return this.parent.getUnsignedInt(i);
   }

   public long getUnsignedIntLE(int i) {
      return this.parent.getUnsignedIntLE(i);
   }

   public long getLong(int i) {
      return this.parent.getLong(i);
   }

   public long getLongLE(int i) {
      return this.parent.getLongLE(i);
   }

   public char getChar(int i) {
      return this.parent.getChar(i);
   }

   public float getFloat(int i) {
      return this.parent.getFloat(i);
   }

   public double getDouble(int i) {
      return this.parent.getDouble(i);
   }

   public ByteBuf getBytes(int i, ByteBuf byteBuf) {
      return this.parent.getBytes(i, byteBuf);
   }

   public ByteBuf getBytes(int i, ByteBuf byteBuf, int j) {
      return this.parent.getBytes(i, byteBuf, j);
   }

   public ByteBuf getBytes(int i, ByteBuf byteBuf, int j, int k) {
      return this.parent.getBytes(i, byteBuf, j, k);
   }

   public ByteBuf getBytes(int i, byte[] bs) {
      return this.parent.getBytes(i, bs);
   }

   public ByteBuf getBytes(int i, byte[] bs, int j, int k) {
      return this.parent.getBytes(i, bs, j, k);
   }

   public ByteBuf getBytes(int i, ByteBuffer byteBuffer) {
      return this.parent.getBytes(i, byteBuffer);
   }

   public ByteBuf getBytes(int i, OutputStream outputStream, int j) throws IOException {
      return this.parent.getBytes(i, outputStream, j);
   }

   public int getBytes(int i, GatheringByteChannel gatheringByteChannel, int j) throws IOException {
      return this.parent.getBytes(i, gatheringByteChannel, j);
   }

   public int getBytes(int i, FileChannel fileChannel, long l, int j) throws IOException {
      return this.parent.getBytes(i, fileChannel, l, j);
   }

   public CharSequence getCharSequence(int i, int j, Charset charset) {
      return this.parent.getCharSequence(i, j, charset);
   }

   public ByteBuf setBoolean(int i, boolean bl) {
      return this.parent.setBoolean(i, bl);
   }

   public ByteBuf setByte(int i, int j) {
      return this.parent.setByte(i, j);
   }

   public ByteBuf setShort(int i, int j) {
      return this.parent.setShort(i, j);
   }

   public ByteBuf setShortLE(int i, int j) {
      return this.parent.setShortLE(i, j);
   }

   public ByteBuf setMedium(int i, int j) {
      return this.parent.setMedium(i, j);
   }

   public ByteBuf setMediumLE(int i, int j) {
      return this.parent.setMediumLE(i, j);
   }

   public ByteBuf setInt(int i, int j) {
      return this.parent.setInt(i, j);
   }

   public ByteBuf setIntLE(int i, int j) {
      return this.parent.setIntLE(i, j);
   }

   public ByteBuf setLong(int i, long l) {
      return this.parent.setLong(i, l);
   }

   public ByteBuf setLongLE(int i, long l) {
      return this.parent.setLongLE(i, l);
   }

   public ByteBuf setChar(int i, int j) {
      return this.parent.setChar(i, j);
   }

   public ByteBuf setFloat(int i, float f) {
      return this.parent.setFloat(i, f);
   }

   public ByteBuf setDouble(int i, double d) {
      return this.parent.setDouble(i, d);
   }

   public ByteBuf setBytes(int i, ByteBuf byteBuf) {
      return this.parent.setBytes(i, byteBuf);
   }

   public ByteBuf setBytes(int i, ByteBuf byteBuf, int j) {
      return this.parent.setBytes(i, byteBuf, j);
   }

   public ByteBuf setBytes(int i, ByteBuf byteBuf, int j, int k) {
      return this.parent.setBytes(i, byteBuf, j, k);
   }

   public ByteBuf setBytes(int i, byte[] bs) {
      return this.parent.setBytes(i, bs);
   }

   public ByteBuf setBytes(int i, byte[] bs, int j, int k) {
      return this.parent.setBytes(i, bs, j, k);
   }

   public ByteBuf setBytes(int i, ByteBuffer byteBuffer) {
      return this.parent.setBytes(i, byteBuffer);
   }

   public int setBytes(int i, InputStream inputStream, int j) throws IOException {
      return this.parent.setBytes(i, inputStream, j);
   }

   public int setBytes(int i, ScatteringByteChannel scatteringByteChannel, int j) throws IOException {
      return this.parent.setBytes(i, scatteringByteChannel, j);
   }

   public int setBytes(int i, FileChannel fileChannel, long l, int j) throws IOException {
      return this.parent.setBytes(i, fileChannel, l, j);
   }

   public ByteBuf setZero(int i, int j) {
      return this.parent.setZero(i, j);
   }

   public int setCharSequence(int i, CharSequence charSequence, Charset charset) {
      return this.parent.setCharSequence(i, charSequence, charset);
   }

   public boolean readBoolean() {
      return this.parent.readBoolean();
   }

   public byte readByte() {
      return this.parent.readByte();
   }

   public short readUnsignedByte() {
      return this.parent.readUnsignedByte();
   }

   public short readShort() {
      return this.parent.readShort();
   }

   public short readShortLE() {
      return this.parent.readShortLE();
   }

   public int readUnsignedShort() {
      return this.parent.readUnsignedShort();
   }

   public int readUnsignedShortLE() {
      return this.parent.readUnsignedShortLE();
   }

   public int readMedium() {
      return this.parent.readMedium();
   }

   public int readMediumLE() {
      return this.parent.readMediumLE();
   }

   public int readUnsignedMedium() {
      return this.parent.readUnsignedMedium();
   }

   public int readUnsignedMediumLE() {
      return this.parent.readUnsignedMediumLE();
   }

   public int readInt() {
      return this.parent.readInt();
   }

   public int readIntLE() {
      return this.parent.readIntLE();
   }

   public long readUnsignedInt() {
      return this.parent.readUnsignedInt();
   }

   public long readUnsignedIntLE() {
      return this.parent.readUnsignedIntLE();
   }

   public long readLong() {
      return this.parent.readLong();
   }

   public long readLongLE() {
      return this.parent.readLongLE();
   }

   public char readChar() {
      return this.parent.readChar();
   }

   public float readFloat() {
      return this.parent.readFloat();
   }

   public double readDouble() {
      return this.parent.readDouble();
   }

   public ByteBuf readBytes(int i) {
      return this.parent.readBytes(i);
   }

   public ByteBuf readSlice(int i) {
      return this.parent.readSlice(i);
   }

   public ByteBuf readRetainedSlice(int i) {
      return this.parent.readRetainedSlice(i);
   }

   public ByteBuf readBytes(ByteBuf byteBuf) {
      return this.parent.readBytes(byteBuf);
   }

   public ByteBuf readBytes(ByteBuf byteBuf, int i) {
      return this.parent.readBytes(byteBuf, i);
   }

   public ByteBuf readBytes(ByteBuf byteBuf, int i, int j) {
      return this.parent.readBytes(byteBuf, i, j);
   }

   public ByteBuf readBytes(byte[] bs) {
      return this.parent.readBytes(bs);
   }

   public ByteBuf readBytes(byte[] bs, int i, int j) {
      return this.parent.readBytes(bs, i, j);
   }

   public ByteBuf readBytes(ByteBuffer byteBuffer) {
      return this.parent.readBytes(byteBuffer);
   }

   public ByteBuf readBytes(OutputStream outputStream, int i) throws IOException {
      return this.parent.readBytes(outputStream, i);
   }

   public int readBytes(GatheringByteChannel gatheringByteChannel, int i) throws IOException {
      return this.parent.readBytes(gatheringByteChannel, i);
   }

   public CharSequence readCharSequence(int i, Charset charset) {
      return this.parent.readCharSequence(i, charset);
   }

   public int readBytes(FileChannel fileChannel, long l, int i) throws IOException {
      return this.parent.readBytes(fileChannel, l, i);
   }

   public ByteBuf skipBytes(int i) {
      return this.parent.skipBytes(i);
   }

   public ByteBuf writeBoolean(boolean bl) {
      return this.parent.writeBoolean(bl);
   }

   public ByteBuf writeByte(int i) {
      return this.parent.writeByte(i);
   }

   public ByteBuf writeShort(int i) {
      return this.parent.writeShort(i);
   }

   public ByteBuf writeShortLE(int i) {
      return this.parent.writeShortLE(i);
   }

   public ByteBuf writeMedium(int i) {
      return this.parent.writeMedium(i);
   }

   public ByteBuf writeMediumLE(int i) {
      return this.parent.writeMediumLE(i);
   }

   public ByteBuf writeInt(int i) {
      return this.parent.writeInt(i);
   }

   public ByteBuf writeIntLE(int i) {
      return this.parent.writeIntLE(i);
   }

   public ByteBuf writeLong(long l) {
      return this.parent.writeLong(l);
   }

   public ByteBuf writeLongLE(long l) {
      return this.parent.writeLongLE(l);
   }

   public ByteBuf writeChar(int i) {
      return this.parent.writeChar(i);
   }

   public ByteBuf writeFloat(float f) {
      return this.parent.writeFloat(f);
   }

   public ByteBuf writeDouble(double d) {
      return this.parent.writeDouble(d);
   }

   public ByteBuf writeBytes(ByteBuf byteBuf) {
      return this.parent.writeBytes(byteBuf);
   }

   public ByteBuf writeBytes(ByteBuf byteBuf, int i) {
      return this.parent.writeBytes(byteBuf, i);
   }

   public ByteBuf writeBytes(ByteBuf byteBuf, int i, int j) {
      return this.parent.writeBytes(byteBuf, i, j);
   }

   public ByteBuf writeBytes(byte[] bs) {
      return this.parent.writeBytes(bs);
   }

   public ByteBuf writeBytes(byte[] bs, int i, int j) {
      return this.parent.writeBytes(bs, i, j);
   }

   public ByteBuf writeBytes(ByteBuffer byteBuffer) {
      return this.parent.writeBytes(byteBuffer);
   }

   public int writeBytes(InputStream inputStream, int i) throws IOException {
      return this.parent.writeBytes(inputStream, i);
   }

   public int writeBytes(ScatteringByteChannel scatteringByteChannel, int i) throws IOException {
      return this.parent.writeBytes(scatteringByteChannel, i);
   }

   public int writeBytes(FileChannel fileChannel, long l, int i) throws IOException {
      return this.parent.writeBytes(fileChannel, l, i);
   }

   public ByteBuf writeZero(int i) {
      return this.parent.writeZero(i);
   }

   public int writeCharSequence(CharSequence charSequence, Charset charset) {
      return this.parent.writeCharSequence(charSequence, charset);
   }

   public int indexOf(int i, int j, byte b) {
      return this.parent.indexOf(i, j, b);
   }

   public int bytesBefore(byte b) {
      return this.parent.bytesBefore(b);
   }

   public int bytesBefore(int i, byte b) {
      return this.parent.bytesBefore(i, b);
   }

   public int bytesBefore(int i, int j, byte b) {
      return this.parent.bytesBefore(i, j, b);
   }

   public int forEachByte(ByteProcessor byteProcessor) {
      return this.parent.forEachByte(byteProcessor);
   }

   public int forEachByte(int i, int j, ByteProcessor byteProcessor) {
      return this.parent.forEachByte(i, j, byteProcessor);
   }

   public int forEachByteDesc(ByteProcessor byteProcessor) {
      return this.parent.forEachByteDesc(byteProcessor);
   }

   public int forEachByteDesc(int i, int j, ByteProcessor byteProcessor) {
      return this.parent.forEachByteDesc(i, j, byteProcessor);
   }

   public ByteBuf copy() {
      return this.parent.copy();
   }

   public ByteBuf copy(int i, int j) {
      return this.parent.copy(i, j);
   }

   public ByteBuf slice() {
      return this.parent.slice();
   }

   public ByteBuf retainedSlice() {
      return this.parent.retainedSlice();
   }

   public ByteBuf slice(int i, int j) {
      return this.parent.slice(i, j);
   }

   public ByteBuf retainedSlice(int i, int j) {
      return this.parent.retainedSlice(i, j);
   }

   public ByteBuf duplicate() {
      return this.parent.duplicate();
   }

   public ByteBuf retainedDuplicate() {
      return this.parent.retainedDuplicate();
   }

   public int nioBufferCount() {
      return this.parent.nioBufferCount();
   }

   public ByteBuffer nioBuffer() {
      return this.parent.nioBuffer();
   }

   public ByteBuffer nioBuffer(int i, int j) {
      return this.parent.nioBuffer(i, j);
   }

   public ByteBuffer internalNioBuffer(int i, int j) {
      return this.parent.internalNioBuffer(i, j);
   }

   public ByteBuffer[] nioBuffers() {
      return this.parent.nioBuffers();
   }

   public ByteBuffer[] nioBuffers(int i, int j) {
      return this.parent.nioBuffers(i, j);
   }

   public boolean hasArray() {
      return this.parent.hasArray();
   }

   public byte[] array() {
      return this.parent.array();
   }

   public int arrayOffset() {
      return this.parent.arrayOffset();
   }

   public boolean hasMemoryAddress() {
      return this.parent.hasMemoryAddress();
   }

   public long memoryAddress() {
      return this.parent.memoryAddress();
   }

   public String toString(Charset charset) {
      return this.parent.toString(charset);
   }

   public String toString(int i, int j, Charset charset) {
      return this.parent.toString(i, j, charset);
   }

   public int hashCode() {
      return this.parent.hashCode();
   }

   public boolean equals(Object o) {
      return this.parent.equals(o);
   }

   public int compareTo(ByteBuf byteBuf) {
      return this.parent.compareTo(byteBuf);
   }

   public String toString() {
      return this.parent.toString();
   }

   public ByteBuf retain(int i) {
      return this.parent.retain(i);
   }

   public ByteBuf retain() {
      return this.parent.retain();
   }

   public ByteBuf touch() {
      return this.parent.touch();
   }

   public ByteBuf touch(Object object) {
      return this.parent.touch(object);
   }

   public int refCnt() {
      return this.parent.refCnt();
   }

   public boolean release() {
      return this.parent.release();
   }

   public boolean release(int i) {
      return this.parent.release(i);
   }
}
