package dubbo.mini.protocol.dubbo;

import dubbo.mini.codec.CodecSupport;
import dubbo.mini.common.utils.ArrayUtils;
import dubbo.mini.common.utils.RpcUtils;
import dubbo.mini.common.utils.StringUtils;
import dubbo.mini.exchange.Response;
import dubbo.mini.remote.Decodeable;
import dubbo.mini.remote.NetChannel;
import dubbo.mini.rpc.Invocation;
import dubbo.mini.rpc.RpcResult;
import dubbo.mini.serialize.Cleanable;
import dubbo.mini.serialize.ObjectInput;
import dubbo.mini.util.Assert;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DecodeableRpcResult extends RpcResult implements Decodeable {

  private static final Logger log = LoggerFactory.getLogger(DecodeableRpcResult.class);

  private NetChannel channel;

  private byte serializationType;

  private InputStream inputStream;

  private Response response;

  private Invocation invocation;

  private volatile boolean hasDecoded;

  public DecodeableRpcResult(NetChannel channel, Response response, InputStream is,
      Invocation invocation, byte id) {
    Assert.notNull(channel, "channel == null");
    Assert.notNull(response, "response == null");
    Assert.notNull(is, "inputStream == null");
    this.channel = channel;
    this.response = response;
    this.inputStream = is;
    this.invocation = invocation;
    this.serializationType = id;
  }

  public void encode(NetChannel channel, OutputStream output, Object message) throws IOException {
    throw new UnsupportedOperationException();
  }

  public Object decode(NetChannel channel, InputStream input) throws IOException {
    ObjectInput in = CodecSupport.getSerialization(channel.getUrl(), serializationType)
        .deserialize(channel.getUrl(), input);

    byte flag = in.readByte();
    switch (flag) {
      case DubboCodec.RESPONSE_NULL_VALUE:
        break;
      case DubboCodec.RESPONSE_VALUE:
        handleValue(in);
        break;
      case DubboCodec.RESPONSE_WITH_EXCEPTION:
        handleException(in);
        break;
      case DubboCodec.RESPONSE_NULL_VALUE_WITH_ATTACHMENTS:
        handleAttachment(in);
        break;
      case DubboCodec.RESPONSE_VALUE_WITH_ATTACHMENTS:
        handleValue(in);
        handleAttachment(in);
        break;
      case DubboCodec.RESPONSE_WITH_EXCEPTION_WITH_ATTACHMENTS:
        handleException(in);
        handleAttachment(in);
        break;
      default:
        throw new IOException("Unknown result flag, expect '0' '1' '2', get " + flag);
    }
    if (in instanceof Cleanable) {
      ((Cleanable) in).cleanup();
    }
    return this;
  }

  @Override
  public void decode() throws Exception {
    if (!hasDecoded && channel != null && inputStream != null) {
      try {
        decode(channel, inputStream);
      } catch (Throwable e) {
        if (log.isWarnEnabled()) {
          log.warn("Decode rpc result failed: " + e.getMessage(), e);
        }
        response.setStatus(Response.CLIENT_ERROR);
        response.setErrorMessage(StringUtils.toString(e));
      } finally {
        hasDecoded = true;
      }
    }
  }

  private void handleValue(ObjectInput in) throws IOException {
    try {
      Type[] returnTypes = RpcUtils.getReturnTypes(invocation);
      Object value = null;
      if (ArrayUtils.isEmpty(returnTypes)) {
        value = in.readObject();
      } else if (returnTypes.length == 1) {
        value = in.readObject((Class<?>) returnTypes[0]);
      } else {
        value = in.readObject((Class<?>) returnTypes[0], returnTypes[1]);
      }
      setValue(value);
    } catch (ClassNotFoundException e) {
      rethrow(e);
    }
  }

  private void handleException(ObjectInput in) throws IOException {
    try {
      Object obj = in.readObject();
      if (!(obj instanceof Throwable)) {
        throw new IOException("Response data error, expect Throwable, but get " + obj);
      }
      setException((Throwable) obj);
    } catch (ClassNotFoundException e) {
      rethrow(e);
    }
  }

  private void handleAttachment(ObjectInput in) throws IOException {
    try {
      setAttachments((Map<String, String>) in.readObject(Map.class));
    } catch (ClassNotFoundException e) {
      rethrow(e);
    }
  }

  private void rethrow(Exception e) throws IOException {
    throw new IOException(StringUtils.toString("Read response data failed.", e));
  }
}
