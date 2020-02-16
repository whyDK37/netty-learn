package dubbo.mini.codec;

import dubbo.mini.common.Constants;
import dubbo.mini.common.NetURL;
import dubbo.mini.common.utils.NetUtils;
import dubbo.mini.remote.NetChannel;
import dubbo.mini.serialize.Serialization;
import java.io.IOException;
import java.net.InetSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AbstractCodec
 */
public abstract class AbstractCodec implements Codec {

  private static final Logger logger = LoggerFactory.getLogger(AbstractCodec.class);

  /**
   * 消息长度是否大于负载
   *
   * @param channel
   * @param size    消息长度
   * @throws IOException
   */
  protected static void checkPayload(NetChannel channel, long size) throws IOException {
    int payload = Constants.DEFAULT_PAYLOAD;
    if (channel != null && channel.getUrl() != null) {
      payload = channel.getUrl().getParameter(Constants.PAYLOAD_KEY, Constants.DEFAULT_PAYLOAD);
    }
    if (payload > 0 && size > payload) {
      ExceedPayloadLimitException e = new ExceedPayloadLimitException(
          "Data length too large: " + size + ", max payload: " + payload + ", channel: " + channel);
      logger.error("", e);
      throw e;
    }
  }

  protected Serialization getSerialization(NetChannel channel) {
    return CodecSupport.getSerialization(channel.getUrl());
  }

  protected boolean isClientSide(NetChannel channel) {
    String side = (String) channel.getAttribute(Constants.SIDE_KEY);
    if ("client".equals(side)) {
      return true;
    } else if ("server".equals(side)) {
      return false;
    } else {
      InetSocketAddress address = channel.getRemoteAddress();
      NetURL url = channel.getUrl();
      boolean client = url.getPort() == address.getPort()
          && NetUtils.filterLocalHost(url.getIp()).equals(
          NetUtils.filterLocalHost(address.getAddress()
              .getHostAddress()));
      channel.setAttribute(Constants.SIDE_KEY, client ? "client"
          : "server");
      return client;
    }
  }

  protected boolean isServerSide(NetChannel channel) {
    return !isClientSide(channel);
  }

}
