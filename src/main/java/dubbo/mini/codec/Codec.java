package dubbo.mini.codec;

import io.netty.channel.Channel;
import dubbo.mini.buffer.ChannelBuffer;

import java.io.IOException;

public interface Codec {

    void encode(Channel channel, ChannelBuffer buffer, Object message) throws IOException;

    Object decode(Channel channel, ChannelBuffer buffer) throws IOException;


    enum DecodeResult {
        NEED_MORE_INPUT, SKIP_SOME_INPUT
    }

}