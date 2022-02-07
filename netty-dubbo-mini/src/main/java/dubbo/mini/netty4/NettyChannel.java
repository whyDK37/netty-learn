package dubbo.mini.netty4;


import dubbo.mini.common.Constants;
import dubbo.mini.common.NetURL;
import dubbo.mini.remote.ChannelEventHandler;
import dubbo.mini.remote.NetChannel;
import dubbo.mini.remote.RemotingException;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NettyChannel extends AbstractPeer implements NetChannel {

  private static final Logger logger = LoggerFactory.getLogger(NettyChannel.class);

  private static final ConcurrentMap<Channel, NettyChannel> channelMap = new ConcurrentHashMap<Channel, NettyChannel>();

  private final Channel channel;

  private final Map<String, Object> attributes = new ConcurrentHashMap<String, Object>();

  private NettyChannel(Channel channel, NetURL url, ChannelEventHandler handler) {
    super(url, handler);
    if (channel == null) {
      throw new IllegalArgumentException("netty channel == null;");
    }
    this.channel = channel;
  }

  static NettyChannel getOrAddChannel(Channel ch, NetURL url, ChannelEventHandler handler) {
    if (ch == null) {
      return null;
    }
    NettyChannel ret = channelMap.get(ch);
    if (ret == null) {
      NettyChannel nettyChannel = new NettyChannel(ch, url, handler);
      if (ch.isActive()) {
        ret = channelMap.putIfAbsent(ch, nettyChannel);
      }
      if (ret == null) {
        ret = nettyChannel;
      }
    }
    return ret;
  }

  static void removeChannelIfDisconnected(Channel ch) {
    if (ch != null && !ch.isActive()) {
      channelMap.remove(ch);
    }
  }

  @Override
  public InetSocketAddress getLocalAddress() {
    return (InetSocketAddress) channel.localAddress();
  }

  @Override
  public InetSocketAddress getRemoteAddress() {
    return (InetSocketAddress) channel.remoteAddress();
  }

  @Override
  public boolean isConnected() {
    return !isClosed() && channel.isActive();
  }

  @Override
  public void send(Object message, boolean sent) throws RemotingException {
    if (isClosed()) {
      throw new RemotingException(this, "Failed to send message "
          + (message == null ? "" : message.getClass().getName()) + ":" + message
          + ", cause: Channel closed. channel: " + getLocalAddress() + " -> " + getRemoteAddress());
    }

    boolean success = true;
    int timeout = 0;
    try {
      ChannelFuture future = channel.writeAndFlush(message);
      if (sent) {
        timeout = getUrl().getPositiveParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
        success = future.await(timeout);
      }
      Throwable cause = future.cause();
      if (cause != null) {
        throw cause;
      }
    } catch (Throwable e) {
      throw new RemotingException(this,
          "Failed to send message " + message + " to " + getRemoteAddress() + ", cause: " + e
              .getMessage(), e);
    }

    if (!success) {
      throw new RemotingException(this,
          "Failed to send message " + message + " to " + getRemoteAddress()
              + "in timeout(" + timeout + "ms) limit");
    }
  }

  @Override
  public void close() {
    try {
      super.close();
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
    }
    try {
      removeChannelIfDisconnected(channel);
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
    }
    try {
      attributes.clear();
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
    }
    try {
      if (logger.isInfoEnabled()) {
        logger.info("Close netty channel " + channel);
      }
      channel.close();
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
    }
  }

  @Override
  public boolean hasAttribute(String key) {
    return attributes.containsKey(key);
  }

  @Override
  public Object getAttribute(String key) {
    return attributes.get(key);
  }

  @Override
  public void setAttribute(String key, Object value) {
    if (value == null) { // The null value unallowed in the ConcurrentHashMap.
      attributes.remove(key);
    } else {
      attributes.put(key, value);
    }
  }

  @Override
  public void removeAttribute(String key) {
    attributes.remove(key);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((channel == null) ? 0 : channel.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    NettyChannel other = (NettyChannel) obj;
    if (channel == null) {
      if (other.channel != null) {
        return false;
      }
    } else if (!channel.equals(other.channel)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "NettyChannel [channel=" + channel + "]";
  }

}