package dubbo.mini.codec;

import dubbo.mini.buffer.ChannelBuffer;
import dubbo.mini.buffer.ChannelBufferOutputStream;
import dubbo.mini.common.io.Bytes;
import dubbo.mini.common.io.StreamUtils;
import dubbo.mini.common.utils.StringUtils;
import dubbo.mini.exchange.Request;
import dubbo.mini.exchange.Response;
import dubbo.mini.remote.NetChannel;
import dubbo.mini.remote.RemotingException;
import dubbo.mini.serialize.Cleanable;
import dubbo.mini.serialize.ObjectInput;
import dubbo.mini.serialize.ObjectOutput;
import dubbo.mini.serialize.Serialization;
import dubbo.mini.support.DefaultFuture;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExchangeCodec extends TelnetCodec {

  // header length. Header 总长度，16 Bytes = 128 Bits 。
  protected static final int HEADER_LENGTH = 16;
  // magic header.
  protected static final short MAGIC = (short) 0xdabb;
  protected static final byte MAGIC_HIGH = Bytes.short2bytes(MAGIC)[0];
  protected static final byte MAGIC_LOW = Bytes.short2bytes(MAGIC)[1];
  // message flag.
  protected static final byte FLAG_REQUEST = (byte) 0x80;
  protected static final byte FLAG_TWOWAY = (byte) 0x40;
  protected static final byte FLAG_EVENT = (byte) 0x20;
  protected static final int SERIALIZATION_MASK = 0x1f;
  private static final Logger logger = LoggerFactory.getLogger(ExchangeCodec.class);

  public Short getMagicCode() {
    return MAGIC;
  }

  @Override
  public void encode(NetChannel channel, ChannelBuffer buffer, Object msg) throws IOException {
    // 请求
    if (msg instanceof Request) {
      encodeRequest(channel, buffer, (Request) msg);
    }
    // 响应
    else if (msg instanceof Response) {
      encodeResponse(channel, buffer, (Response) msg);
    }
    // 提交给父类( Telnet ) 处理，目前是 Telnet 命令的结果。
    else {
      super.encode(channel, buffer, msg);
    }
  }

  @Override
  public Object decode(NetChannel channel, ChannelBuffer buffer) throws IOException {
    // 读取 header 数组。注意，这里的 Math.min(readable, HEADER_LENGTH) ，优先考虑解析 Dubbo 协议。
    int readable = buffer.readableBytes();
    byte[] header = new byte[Math.min(readable, HEADER_LENGTH)];
    buffer.readBytes(header);
    // 解码
    return decode(channel, buffer, readable, header);
  }

  @Override
  protected Object decode(NetChannel channel, ChannelBuffer buffer, int readable, byte[] header)
      throws IOException {
    // check magic number.
    // 非 Dubbo 协议，目前是 Telnet 命令。
    if (readable > 0 && header[0] != MAGIC_HIGH
        || readable > 1 && header[1] != MAGIC_LOW) {
      // 将 Buffer 完全复制到 header 数组中。因为，上面的 #decode(channel, buffer) 方法，可能未读全。
      // 因为，【第 3 至 6 行】，是以 Dubbo 协议 为优先考虑解码的。
      int length = header.length;
      if (header.length < readable) {
        header = Bytes.copyOf(header, readable);
        buffer.readBytes(header, length, readable - length);
      }

      // fixme 【TODO 8026 】header[i] == MAGIC_HIGH && header[i + 1] == MAGIC_LOW ？搞不懂？
      for (int i = 1; i < header.length - 1; i++) {
        if (header[i] == MAGIC_HIGH && header[i + 1] == MAGIC_LOW) {
          buffer.readerIndex(buffer.readerIndex() - header.length + i);
          header = Bytes.copyOf(header, i);
          break;
        }
      }

      // 调用 Telnet#decode(channel, buffer, readable, header) 方法，解码 Telnet 。
      // 在 《精尽 Dubbo 源码分析 —— NIO 服务器（三）之 Telnet 层》 有详细解析。
      return super.decode(channel, buffer, readable, header);
    }
    // Header 长度不够，返回需要更多的输入
    // 基于消息长度的方式，拆包。
    // check length.
    if (readable < HEADER_LENGTH) {
      return DecodeResult.NEED_MORE_INPUT;
    }

    // `[96 - 127]`：Body 的**长度**。通过该长度，读取 Body 。
    // get data length.
    int len = Bytes.bytes2int(header, 12);
    checkPayload(channel, len);

    // 总长度不够，返回需要更多的输入
    int tt = len + HEADER_LENGTH;
    if (readable < tt) {
      return DecodeResult.NEED_MORE_INPUT;
    }

    // 解析 Header + Body
    // 调用 #decodeBody(channel, is, header) 方法，解析 Header + Body ，根据情况，返回 Request 或 Reponse 。
    // 🙂 逻辑上，是 #encodeRequest(...) 和 #encodeResponse(...) 方法的反向，所以，胖友就自己看啦。
    // limit input stream.
    ChannelBufferInputStream is = new ChannelBufferInputStream(buffer, len);
    try {
      return decodeBody(channel, is, header);
    } finally {
      // skip 未读完的流，并打印错误日志
      if (is.available() > 0) {
        try {
          if (logger.isWarnEnabled()) {
            logger.warn("Skip input stream " + is.available());
          }
          StreamUtils.skipUnusedStream(is);
        } catch (IOException e) {
          logger.warn(e.getMessage(), e);
        }
      }
    }
  }

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
            data = decodeResponseData(channel, in, getRequestData(id));
          }
          res.setResult(data);
        } else {
          res.setErrorMessage(in.readUTF());
        }
      } catch (Throwable t) {
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
        ObjectInput in = CodecSupport.deserialize(channel.getUrl(), is, proto);
        Object data;
        if (req.isHeartbeat()) {
          data = decodeHeartbeatData(channel, in);
        } else if (req.isEvent()) {
          data = decodeEventData(channel, in);
        } else {
          data = decodeRequestData(channel, in);
        }
        req.setData(data);
      } catch (Throwable t) {
        // bad request
        req.setBroken(true);
        req.setData(t);
      }
      return req;
    }
  }

  protected Object getRequestData(long id) {
    DefaultFuture future = DefaultFuture.getFuture(id);
    if (future == null) {
      return null;
    }
    Request req = future.getRequest();
    if (req == null) {
      return null;
    }
    return req.getData();
  }

  private void encodeRequest(NetChannel channel, ChannelBuffer buffer, Request req)
      throws IOException {
    Serialization serialization = getSerialization(channel);
    // `[0, 15]`：Magic Number
    // header.
    byte[] header = new byte[HEADER_LENGTH];
    // set magic number.
    Bytes.short2bytes(MAGIC, header);

    // `[16, 20]`：Serialization 编号 && `[23]`：请求。
    // set request and serialization flag.
    header[2] = (byte) (FLAG_REQUEST | serialization.getContentTypeId());

    // `[22]`：`twoWay` 是否需要响应。
    if (req.isTwoWay()) {
      header[2] |= FLAG_TWOWAY;
    }
    // `[21]`：`event` 是否为事件。
    if (req.isEvent()) {
      header[2] |= FLAG_EVENT;
    }

    // `[32 - 95]`：`id` 编号，Long 型。
    // set request id.
    Bytes.long2bytes(req.getId(), header, 4);

    // 编码 `Request.data` 到 Body ，并写入到 Buffer
    // encode request data.
    int savedWriteIndex = buffer.writerIndex();
    buffer.writerIndex(savedWriteIndex + HEADER_LENGTH);
    ChannelBufferOutputStream bos = new ChannelBufferOutputStream(buffer);
    ObjectOutput out = serialization.serialize(channel.getUrl(), bos);
    if (req.isEvent()) {
      encodeEventData(channel, out, req.getData());
    } else {
      encodeRequestData(channel, out, req.getData());
    }

    // 释放资源
    out.flushBuffer();
    if (out instanceof Cleanable) {
      ((Cleanable) out).cleanup();
    }
    bos.flush();
    bos.close();
    // 检查 Body 长度，是否超过消息上限。
    int len = bos.writtenBytes();
    // 会调用 #checkPayload(channel, len) 方法，校验 Body 内容的长度。笔者在这块纠结了很久，
    // 如果过长而抛出 ExceedPayloadLimitException 异常，那么 ChannelBuffer 是否重置下写入位置。
    // 后来发现自己煞笔了，每次 ChannelBuffer 都是新创建的，所以无需重置。
    // 为什么 Buffer 先写入了 Body ，再写入 Header 呢？因为 Header 中，里面 [96 - 127] 的 Body 长度，需要序列化后才得到。
    checkPayload(channel, len);
    // `[96 - 127]`：Body 的**长度**。
    Bytes.int2bytes(len, header, 12);

    // 写入 Header 到 Buffer
    // write
    buffer.writerIndex(savedWriteIndex);
    buffer.writeBytes(header); // write header.
    buffer.writerIndex(savedWriteIndex + HEADER_LENGTH + len);
  }

  protected void encodeResponse(NetChannel channel, ChannelBuffer buffer, Response res)
      throws IOException {
    int savedWriteIndex = buffer.writerIndex();
    try {
      Serialization serialization = getSerialization(channel);
      // header.
      byte[] header = new byte[HEADER_LENGTH];
      // set magic number.
      Bytes.short2bytes(MAGIC, header);
      // set request and serialization flag.
      header[2] = serialization.getContentTypeId();
      if (res.isHeartbeat()) {
        header[2] |= FLAG_EVENT;
      }
      // set response status.
      byte status = res.getStatus();
      header[3] = status;
      // set request id.
      Bytes.long2bytes(res.getId(), header, 4);

      buffer.writerIndex(savedWriteIndex + HEADER_LENGTH);
      ChannelBufferOutputStream bos = new ChannelBufferOutputStream(buffer);
      ObjectOutput out = serialization.serialize(channel.getUrl(), bos);
      // encode response data or error message.
      if (status == Response.OK) {
        if (res.isHeartbeat()) {
          encodeHeartbeatData(channel, out, res.getResult());
        } else {
          encodeResponseData(channel, out, res.getResult());
        }
      } else {
        out.writeUTF(res.getErrorMessage());
      }
      out.flushBuffer();
      if (out instanceof Cleanable) {
        ((Cleanable) out).cleanup();
      }
      bos.flush();
      bos.close();

      int len = bos.writtenBytes();
      checkPayload(channel, len);
      Bytes.int2bytes(len, header, 12);
      // write
      buffer.writerIndex(savedWriteIndex);
      buffer.writeBytes(header); // write header.
      buffer.writerIndex(savedWriteIndex + HEADER_LENGTH + len);
    } catch (Throwable t) {
      // clear buffer
      buffer.writerIndex(savedWriteIndex);
      // send error message to Consumer, otherwise, Consumer will wait till timeout.
      if (!res.isEvent() && res.getStatus() != Response.BAD_RESPONSE) {
        Response r = new Response(res.getId());
        r.setStatus(Response.BAD_RESPONSE);

        if (t instanceof ExceedPayloadLimitException) {
          logger.warn(t.getMessage(), t);
          try {
            r.setErrorMessage(t.getMessage());
            channel.send(r);
            return;
          } catch (RemotingException e) {
            logger.warn("Failed to send bad_response info back: " + t.getMessage() + ", cause: " + e
                .getMessage(), e);
          }
        } else {
          // FIXME log error message in Codec and handle in caught() of IoHanndler?
          logger.warn(
              "Fail to encode response: " + res + ", send bad_response info instead, cause: " + t
                  .getMessage(), t);
          try {
            r.setErrorMessage(
                "Failed to send response: " + res + ", cause: " + StringUtils.toString(t));
            channel.send(r);
            return;
          } catch (RemotingException e) {
            logger.warn(
                "Failed to send bad_response info back: " + res + ", cause: " + e.getMessage(), e);
          }
        }
      }

      // Rethrow exception
      if (t instanceof IOException) {
        throw (IOException) t;
      } else if (t instanceof RuntimeException) {
        throw (RuntimeException) t;
      } else if (t instanceof Error) {
        throw (Error) t;
      } else {
        throw new RuntimeException(t.getMessage(), t);
      }
    }
  }

  @Override
  protected Object decodeData(ObjectInput in) throws IOException {
    return decodeRequestData(in);
  }

  @Deprecated
  protected Object decodeHeartbeatData(ObjectInput in) throws IOException {
    try {
      return in.readObject();
    } catch (ClassNotFoundException e) {
      throw new IOException(StringUtils.toString("Read object failed.", e));
    }
  }

  protected Object decodeRequestData(ObjectInput in) throws IOException {
    try {
      return in.readObject();
    } catch (ClassNotFoundException e) {
      throw new IOException(StringUtils.toString("Read object failed.", e));
    }
  }

  protected Object decodeResponseData(ObjectInput in) throws IOException {
    try {
      return in.readObject();
    } catch (ClassNotFoundException e) {
      throw new IOException(StringUtils.toString("Read object failed.", e));
    }
  }

  @Override
  protected void encodeData(ObjectOutput out, Object data) throws IOException {
    encodeRequestData(out, data);
  }

  private void encodeEventData(ObjectOutput out, Object data) throws IOException {
    out.writeObject(data);
  }

  @Deprecated
  protected void encodeHeartbeatData(ObjectOutput out, Object data) throws IOException {
    encodeEventData(out, data);
  }

  protected void encodeRequestData(ObjectOutput out, Object data) throws IOException {
    out.writeObject(data);
  }

  protected void encodeResponseData(ObjectOutput out, Object data) throws IOException {
    out.writeObject(data);
  }

  @Override
  protected Object decodeData(NetChannel channel, ObjectInput in) throws IOException {
    return decodeRequestData(channel, in);
  }

  protected Object decodeEventData(NetChannel channel, ObjectInput in) throws IOException {
    try {
      return in.readObject();
    } catch (ClassNotFoundException e) {
      throw new IOException(StringUtils.toString("Read object failed.", e));
    }
  }

  @Deprecated
  protected Object decodeHeartbeatData(NetChannel channel, ObjectInput in) throws IOException {
    try {
      return in.readObject();
    } catch (ClassNotFoundException e) {
      throw new IOException(StringUtils.toString("Read object failed.", e));
    }
  }

  protected Object decodeRequestData(NetChannel channel, ObjectInput in) throws IOException {
    return decodeRequestData(in);
  }

  protected Object decodeResponseData(NetChannel channel, ObjectInput in) throws IOException {
    return decodeResponseData(in);
  }

  protected Object decodeResponseData(NetChannel channel, ObjectInput in, Object requestData)
      throws IOException {
    return decodeResponseData(channel, in);
  }

  @Override
  protected void encodeData(NetChannel channel, ObjectOutput out, Object data) throws IOException {
    encodeRequestData(channel, out, data);
  }

  private void encodeEventData(NetChannel channel, ObjectOutput out, Object data)
      throws IOException {
    encodeEventData(out, data);
  }

  @Deprecated
  protected void encodeHeartbeatData(NetChannel channel, ObjectOutput out, Object data)
      throws IOException {
    encodeHeartbeatData(out, data);
  }

  protected void encodeRequestData(NetChannel channel, ObjectOutput out, Object data)
      throws IOException {
    encodeRequestData(out, data);
  }

  protected void encodeResponseData(NetChannel channel, ObjectOutput out, Object data)
      throws IOException {
    encodeResponseData(out, data);
  }

}
