package dubbo.mini.buffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public interface ChannelBuffer extends Comparable<ChannelBuffer> {

  int capacity();

  void clear();

  ChannelBuffer copy();

  ChannelBuffer copy(int index, int length);

  void discardReadBytes();


  void ensureWritableBytes(int writableBytes);


  @Override
  public boolean equals(Object o);


  ChannelBufferFactory factory();


  byte getByte(int index);


  void getBytes(int index, byte[] dst);

  void getBytes(int index, byte[] dst, int dstIndex, int length);

  void getBytes(int index, ByteBuffer dst);

  void getBytes(int index, ChannelBuffer dst);

  void getBytes(int index, ChannelBuffer dst, int length);

  void getBytes(int index, ChannelBuffer dst, int dstIndex, int length);

  void getBytes(int index, OutputStream dst, int length) throws IOException;

  boolean isDirect();

  void markReaderIndex();

  void markWriterIndex();

  boolean readable();

  int readableBytes();

  byte readByte();

  void readBytes(byte[] dst);

  void readBytes(byte[] dst, int dstIndex, int length);

  void readBytes(ByteBuffer dst);

  void readBytes(ChannelBuffer dst);


  void readBytes(ChannelBuffer dst, int length);

  void readBytes(ChannelBuffer dst, int dstIndex, int length);

  ChannelBuffer readBytes(int length);

  void resetReaderIndex();

  void resetWriterIndex();

  int readerIndex();

  void readerIndex(int readerIndex);

  void readBytes(OutputStream dst, int length) throws IOException;

  void setByte(int index, int value);

  void setBytes(int index, byte[] src);

  void setBytes(int index, byte[] src, int srcIndex, int length);

  void setBytes(int index, ByteBuffer src);

  void setBytes(int index, ChannelBuffer src);

  void setBytes(int index, ChannelBuffer src, int length);

  void setBytes(int index, ChannelBuffer src, int srcIndex, int length);

  int setBytes(int index, InputStream src, int length) throws IOException;

  void setIndex(int readerIndex, int writerIndex);


  void skipBytes(int length);


  ByteBuffer toByteBuffer();


  ByteBuffer toByteBuffer(int index, int length);


  boolean writable();

  int writableBytes();

  void writeByte(int value);

  void writeBytes(byte[] src);

  void writeBytes(byte[] src, int index, int length);

  void writeBytes(ByteBuffer src);

  void writeBytes(ChannelBuffer src);

  void writeBytes(ChannelBuffer src, int length);


  void writeBytes(ChannelBuffer src, int srcIndex, int length);

  int writeBytes(InputStream src, int length) throws IOException;

  int writerIndex();


  void writerIndex(int writerIndex);


  byte[] array();

  boolean hasArray();

  int arrayOffset();
}
