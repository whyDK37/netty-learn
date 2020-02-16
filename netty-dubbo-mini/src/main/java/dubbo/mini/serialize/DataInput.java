package dubbo.mini.serialize;

import java.io.IOException;

public interface DataInput {

  boolean readBool() throws IOException;

  byte readByte() throws IOException;

  short readShort() throws IOException;

  int readInt() throws IOException;

  long readLong() throws IOException;

  float readFloat() throws IOException;

  double readDouble() throws IOException;

  String readUTF() throws IOException;

  byte[] readBytes() throws IOException;
}