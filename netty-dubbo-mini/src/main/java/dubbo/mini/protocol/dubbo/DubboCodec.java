package dubbo.mini.protocol.dubbo;

import static dubbo.mini.protocol.dubbo.CallbackServiceCodec.encodeInvocationArgument;

import dubbo.mini.codec.CodecSupport;
import dubbo.mini.codec.ExchangeCodec;
import dubbo.mini.common.Constants;
import dubbo.mini.common.io.Bytes;
import dubbo.mini.common.io.UnsafeByteArrayInputStream;
import dubbo.mini.common.utils.ReflectUtils;
import dubbo.mini.common.utils.RpcUtils;
import dubbo.mini.common.utils.StringUtils;
import dubbo.mini.exchange.Request;
import dubbo.mini.exchange.Response;
import dubbo.mini.remote.NetChannel;
import dubbo.mini.rpc.Invocation;
import dubbo.mini.rpc.Result;
import dubbo.mini.rpc.RpcInvocation;
import dubbo.mini.serialize.ObjectInput;
import dubbo.mini.serialize.ObjectOutput;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DubboCodec extends ExchangeCodec {

  public static final String NAME = "dubbo";

  public static final byte RESPONSE_WITH_EXCEPTION = 0;
  public static final byte RESPONSE_VALUE = 1;
  public static final byte RESPONSE_NULL_VALUE = 2;
  public static final byte RESPONSE_WITH_EXCEPTION_WITH_ATTACHMENTS = 3;
  public static final byte RESPONSE_VALUE_WITH_ATTACHMENTS = 4;
  public static final byte RESPONSE_NULL_VALUE_WITH_ATTACHMENTS = 5;
  public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
  public static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];
  private static final Logger log = LoggerFactory.getLogger(DubboCodec.class);

  @Override
  protected Object decodeBody(NetChannel channel, InputStream is, byte[] header)
      throws IOException {
    byte flag = header[2], proto = (byte) (flag & SERIALIZATION_MASK);
    // get request id.
    long id = Bytes.bytes2long(header, 4);
    if ((flag & FLAG_REQUEST) == 0) {
      // decode response.
      Response res = new Response(id);
      if ((flag & FLAG_EVENT) != 0) {
        res.setEvent(true);
      }
      // get status.
      byte status = header[3];
      res.setStatus(status);
      try {
        ObjectInput in = CodecSupport.deserialize(channel.getUrl(), is, proto);
        if (status == Response.OK) {
          Object data;
          if (res.isHeartbeat()) {
            data = decodeHeartbeatData(channel, in);
          } else if (res.isEvent()) {
            data = decodeEventData(channel, in);
          } else {
            DecodeableRpcResult result;
            if (channel.getUrl().getParameter(
                Constants.DECODE_IN_IO_THREAD_KEY,
                Constants.DEFAULT_DECODE_IN_IO_THREAD)) {
              result = new DecodeableRpcResult(channel, res, is,
                  (Invocation) getRequestData(id), proto);
              result.decode();
            } else {
              result = new DecodeableRpcResult(channel, res,
                  new UnsafeByteArrayInputStream(readMessageData(is)),
                  (Invocation) getRequestData(id), proto);
            }
            data = result;
          }
          res.setResult(data);
        } else {
          res.setErrorMessage(in.readUTF());
        }
      } catch (Throwable t) {
        if (log.isWarnEnabled()) {
          log.warn("Decode response failed: " + t.getMessage(), t);
        }
        res.setStatus(Response.CLIENT_ERROR);
        res.setErrorMessage(StringUtils.toString(t));
      }
      return res;
    } else {
      // decode request.
      Request req = new Request(id);
      req.setTwoWay((flag & FLAG_TWOWAY) != 0);
      if ((flag & FLAG_EVENT) != 0) {
        req.setEvent(true);
      }
      try {
        Object data;
        ObjectInput in = CodecSupport.deserialize(channel.getUrl(), is, proto);
        if (req.isHeartbeat()) {
          data = decodeHeartbeatData(channel, in);
        } else if (req.isEvent()) {
          data = decodeEventData(channel, in);
        } else {
          DecodeableRpcInvocation inv;
          if (channel.getUrl().getParameter(
              Constants.DECODE_IN_IO_THREAD_KEY,
              Constants.DEFAULT_DECODE_IN_IO_THREAD)) {
            inv = new DecodeableRpcInvocation(channel, req, is, proto);
            inv.decode();
          } else {
            inv = new DecodeableRpcInvocation(channel, req,
                new UnsafeByteArrayInputStream(readMessageData(is)), proto);
          }
          data = inv;
        }
        req.setData(data);
      } catch (Throwable t) {
        if (log.isWarnEnabled()) {
          log.warn("Decode request failed: " + t.getMessage(), t);
        }
        // bad request
        req.setBroken(true);
        req.setData(t);
      }

      return req;
    }
  }

  private byte[] readMessageData(InputStream is) throws IOException {
    if (is.available() > 0) {
      byte[] result = new byte[is.available()];
      is.read(result);
      return result;
    }
    return new byte[]{};
  }

  @Override
  protected void encodeRequestData(NetChannel channel, ObjectOutput out, Object data)
      throws IOException {
    RpcInvocation inv = (RpcInvocation) data;

    out.writeUTF(inv.getAttachment(Constants.PATH_KEY));
    out.writeUTF(inv.getAttachment(Constants.VERSION_KEY));

    out.writeUTF(inv.getMethodName());
    out.writeUTF(ReflectUtils.getDesc(inv.getParameterTypes()));
    Object[] args = inv.getArguments();
    if (args != null) {
      for (int i = 0; i < args.length; i++) {
        out.writeObject(encodeInvocationArgument(channel, inv, i));
      }
    }
    out.writeObject(RpcUtils.getNecessaryAttachments(inv));
  }

  @Override
  protected void encodeResponseData(NetChannel channel, ObjectOutput out, Object data)
      throws IOException {
    Result result = (Result) data;
    // currently, the version value in Response records the version of Request
    Throwable th = result.getException();
    if (th == null) {
      Object ret = result.getValue();
      if (ret == null) {
      } else {
        out.writeObject(ret);
      }
    } else {
      out.writeObject(th);
    }
  }
}
