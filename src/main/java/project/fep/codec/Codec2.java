package project.fep.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import project.fep.buffer.ChannelBuffer;

import java.io.IOException;

public interface Codec2 {

    void encode(Channel channel, ChannelBuffer buffer, Object message) throws IOException;

    Object decode(Channel channel, ChannelBuffer buffer) throws IOException;


    enum DecodeResult {
        NEED_MORE_INPUT, SKIP_SOME_INPUT
    }

}