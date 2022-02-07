package dubbo.mini.protocol.dubbo;

import static dubbo.mini.protocol.dubbo.CallbackServiceCodec.decodeInvocationArgument;

import dubbo.mini.codec.CodecSupport;
import dubbo.mini.common.Constants;
import dubbo.mini.common.utils.ReflectUtils;
import dubbo.mini.common.utils.StringUtils;
import dubbo.mini.exchange.Request;
import dubbo.mini.remote.Decodeable;
import dubbo.mini.remote.NetChannel;
import dubbo.mini.rpc.RpcInvocation;
import dubbo.mini.serialize.Cleanable;
import dubbo.mini.serialize.ObjectInput;
import dubbo.mini.util.Assert;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channel;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DecodeableRpcInvocation extends RpcInvocation implements Decodeable {

  private static final Logger log = LoggerFactory.getLogger(DecodeableRpcInvocation.class);

  private NetChannel channel;

  private byte serializationType;

  private InputStream inputStream;

  private Request request;

  private volatile boolean hasDecoded;

  public DecodeableRpcInvocation(NetChannel channel, Request request, InputStream is, byte id) {
    Assert.notNull(channel, "channel == null");
    Assert.notNull(request, "request == null");
    Assert.notNull(is, "inputStream == null");
    this.channel = channel;
    this.request = request;
    this.inputStream = is;
    this.serializationType = id;
  }

  @Override
  public void decode() throws Exception {
    if (!hasDecoded && channel != null && inputStream != null) {
      try {
        decode(channel, inputStream);
      } catch (Throwable e) {
        if (log.isWarnEnabled()) {
          log.warn("Decode rpc invocation failed: " + e.getMessage(), e);
        }
        request.setBroken(true);
        request.setData(e);
      } finally {
        hasDecoded = true;
      }
    }
  }

  public void encode(Channel channel, OutputStream output, Object message) throws IOException {
    throw new UnsupportedOperationException();
  }

  public Object decode(NetChannel channel, InputStream input) throws IOException {
    ObjectInput in = CodecSupport.getSerialization(channel.getUrl(), serializationType)
        .deserialize(channel.getUrl(), input);

    setAttachment(Constants.PATH_KEY, in.readUTF());
    setAttachment(Constants.VERSION_KEY, in.readUTF());

    setMethodName(in.readUTF());
    try {
      Object[] args;
      Class<?>[] pts;
      String desc = in.readUTF();
      if (desc.length() == 0) {
        pts = DubboCodec.EMPTY_CLASS_ARRAY;
        args = DubboCodec.EMPTY_OBJECT_ARRAY;
      } else {
        pts = ReflectUtils.desc2classArray(desc);
        args = new Object[pts.length];
        for (int i = 0; i < args.length; i++) {
          try {
            args[i] = in.readObject(pts[i]);
          } catch (Exception e) {
            if (log.isWarnEnabled()) {
              log.warn("Decode argument failed: " + e.getMessage(), e);
            }
          }
        }
      }
      setParameterTypes(pts);

      Map<String, String> map = (Map<String, String>) in.readObject(Map.class);
      if (map != null && map.size() > 0) {
        Map<String, String> attachment = getAttachments();
        if (attachment == null) {
          attachment = new HashMap<>();
        }
        attachment.putAll(map);
        setAttachments(attachment);
      }
      //decode argument ,may be callback
      for (int i = 0; i < args.length; i++) {
        args[i] = decodeInvocationArgument(channel, this, pts, i, args[i]);
      }

      setArguments(args);

    } catch (ClassNotFoundException e) {
      throw new IOException(StringUtils.toString("Read invocation data failed.", e));
    } finally {
      if (in instanceof Cleanable) {
        ((Cleanable) in).cleanup();
      }
    }
    return this;
  }

}
