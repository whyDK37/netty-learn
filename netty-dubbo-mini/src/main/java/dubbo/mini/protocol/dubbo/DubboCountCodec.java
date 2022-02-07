package dubbo.mini.protocol.dubbo;

import dubbo.mini.buffer.ChannelBuffer;
import dubbo.mini.codec.Codec;
import dubbo.mini.common.Constants;
import dubbo.mini.exchange.Request;
import dubbo.mini.exchange.Response;
import dubbo.mini.remote.NetChannel;
import dubbo.mini.remote.support.MultiMessage;
import dubbo.mini.rpc.RpcInvocation;
import dubbo.mini.rpc.RpcResult;
import java.io.IOException;

public final class DubboCountCodec implements Codec {

  private DubboCodec codec = new DubboCodec();

  @Override
  public void encode(NetChannel channel, ChannelBuffer buffer, Object msg) throws IOException {
    codec.encode(channel, buffer, msg);
  }

  @Override
  public Object decode(NetChannel channel, ChannelBuffer buffer) throws IOException {
    int save = buffer.readerIndex();
    MultiMessage result = MultiMessage.create();
    do {
      Object obj = codec.decode(channel, buffer);
      if (Codec.DecodeResult.NEED_MORE_INPUT == obj) {
        buffer.readerIndex(save);
        break;
      } else {
        result.addMessage(obj);
        logMessageLength(obj, buffer.readerIndex() - save);
        save = buffer.readerIndex();
      }
    } while (true);
    if (result.isEmpty()) {
      return Codec.DecodeResult.NEED_MORE_INPUT;
    }
    if (result.size() == 1) {
      return result.get(0);
    }
    return result;
  }

  private void logMessageLength(Object result, int bytes) {
    if (bytes <= 0) {
      return;
    }
    if (result instanceof Request) {
      try {
        ((RpcInvocation) ((Request) result).getData()).setAttachment(
            Constants.INPUT_KEY, String.valueOf(bytes));
      } catch (Throwable e) {
        /* ignore */
      }
    } else if (result instanceof Response) {
      try {
        ((RpcResult) ((Response) result).getResult()).setAttachment(
            Constants.OUTPUT_KEY, String.valueOf(bytes));
      } catch (Throwable e) {
        /* ignore */
      }
    }
  }

}
