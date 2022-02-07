package dubbo.mini.buffer;

import java.nio.ByteBuffer;

public interface ChannelBufferFactory {

  ChannelBuffer getBuffer(int capacity);

  ChannelBuffer getBuffer(byte[] array, int offset, int length);

  ChannelBuffer getBuffer(ByteBuffer nioBuffer);

}
