package dubbo.mini.codec;

import dubbo.mini.buffer.ChannelBuffer;
import dubbo.mini.remote.NetChannel;
import dubbo.mini.support.SPI;
import java.io.IOException;

@SPI
public interface Codec {

  void encode(NetChannel channel, ChannelBuffer buffer, Object message) throws IOException;

  Object decode(NetChannel channel, ChannelBuffer buffer) throws IOException;

  enum DecodeResult {
    NEED_MORE_INPUT, SKIP_SOME_INPUT
  }

}