package dubbo.mini.codec;

import dubbo.mini.buffer.ChannelBuffer;
import dubbo.mini.buffer.ChannelBufferOutputStream;
import dubbo.mini.common.utils.StringUtils;
import dubbo.mini.remote.NetChannel;
import dubbo.mini.serialize.Cleanable;
import dubbo.mini.serialize.ObjectInput;
import dubbo.mini.serialize.ObjectOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * TransportCodec
 */
public class TransportCodec extends AbstractCodec {

  @Override
  public void encode(NetChannel channel, ChannelBuffer buffer, Object message) throws IOException {
    OutputStream output = new ChannelBufferOutputStream(buffer);
    ObjectOutput objectOutput = getSerialization(channel).serialize(channel.getUrl(), output);
    encodeData(channel, objectOutput, message);
    objectOutput.flushBuffer();
    if (objectOutput instanceof Cleanable) {
      ((Cleanable) objectOutput).cleanup();
    }
  }

  @Override
  public Object decode(NetChannel channel, ChannelBuffer buffer) throws IOException {
    InputStream input = new ChannelBufferInputStream(buffer);
    ObjectInput objectInput = getSerialization(channel).deserialize(channel.getUrl(), input);
    Object object = decodeData(channel, objectInput);
    if (objectInput instanceof Cleanable) {
      ((Cleanable) objectInput).cleanup();
    }
    return object;
  }

  protected void encodeData(NetChannel channel, ObjectOutput output, Object message)
      throws IOException {
    encodeData(output, message);
  }

  protected Object decodeData(NetChannel channel, ObjectInput input) throws IOException {
    return decodeData(input);
  }

  protected void encodeData(ObjectOutput output, Object message) throws IOException {
    output.writeObject(message);
  }

  protected Object decodeData(ObjectInput input) throws IOException {
    try {
      return input.readObject();
    } catch (ClassNotFoundException e) {
      throw new IOException("ClassNotFoundException: " + StringUtils.toString(e));
    }
  }
}
