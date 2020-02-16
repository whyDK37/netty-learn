package dubbo.mini.exchange.header;

import dubbo.mini.common.Constants;
import dubbo.mini.common.NetURL;
import dubbo.mini.exchange.ExchangeChannel;
import dubbo.mini.exchange.ExchangeHandler;
import dubbo.mini.exchange.Request;
import dubbo.mini.exchange.Response;
import dubbo.mini.remote.ChannelEventHandler;
import dubbo.mini.remote.NetChannel;
import dubbo.mini.remote.RemotingException;
import dubbo.mini.support.DefaultFuture;
import dubbo.mini.support.ResponseFuture;
import java.net.InetSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class HeaderExchangeChannel implements ExchangeChannel {

  private static final Logger logger = LoggerFactory.getLogger(HeaderExchangeChannel.class);

  private static final String CHANNEL_KEY = HeaderExchangeChannel.class.getName() + ".CHANNEL";

  /**
   * 通道 通道。HeaderExchangeChannel 是传入 channel 属性的装饰器， 每个实现的方法，都会调用 channel 。如下是该属性的一个例子：
   */
  private final NetChannel channel;

  /**
   * 是否关闭
   */
  private volatile boolean closed = false;

  HeaderExchangeChannel(NetChannel channel) {
    if (channel == null) {
      throw new IllegalArgumentException("channel == null");
    }
    this.channel = channel;
  }

  /**
   * 创建 HeaderExchangeChannel 对象
   *
   * @param ch 传入的 ch 属性，实际就是 HeaderExchangeChanel.channel 属性。
   * @return
   */
  static HeaderExchangeChannel getOrAddChannel(NetChannel ch) {
    if (ch == null) {
      return null;
    }
    // 通过 ch.attribute 的 CHANNEL_KEY 键值，
    // 保证有且仅有为 ch 属性，创建唯一的 HeaderExchangeChannel 对象。
    HeaderExchangeChannel ret = (HeaderExchangeChannel) ch.getAttribute(CHANNEL_KEY);
    if (ret == null) {
      ret = new HeaderExchangeChannel(ch);
      // 要求已连接。
      if (ch.isConnected()) {
        ch.setAttribute(CHANNEL_KEY, ret);
      }
    }
    return ret;
  }

  /**
   * 移除 HeaderExchangeChannel 对象。
   *
   * @param ch
   */
  static void removeChannelIfDisconnected(NetChannel ch) {
    if (ch != null && !ch.isConnected()) {
      ch.removeAttribute(CHANNEL_KEY);
    }
  }

  @Override
  public void send(Object message) throws RemotingException {
    send(message, false);
  }

  @Override
  public void send(Object message, boolean sent) throws RemotingException {
    if (closed) {
      throw new RemotingException(this.getLocalAddress(), null,
          "Failed to send message " + message + ", cause: The channel " + this + " is closed!");
    }
    if (message instanceof Request
        || message instanceof Response
        || message instanceof String) {
      channel.send(message, sent);
    } else {
      Request request = new Request();
      request.setTwoWay(false);
      request.setData(message);
      channel.send(request, sent);
    }
  }

  @Override
  public ResponseFuture request(Object request) throws RemotingException {
    return request(request,
        channel.getUrl().getPositiveParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT));
  }

  @Override
  public ResponseFuture request(Object request, int timeout) throws RemotingException {
    // 若已经关闭，不再允许发起新的请求。
    if (closed) {
      throw new RemotingException(this.getLocalAddress(), null,
          "Failed to send request " + request + ", cause: The channel " + this + " is closed!");
    }
    // create request.
    Request req = new Request();
    // 需要响应
    req.setTwoWay(true);
    // 具体数据
    req.setData(request);
    // 创建 DefaultFuture 对象
    DefaultFuture future = DefaultFuture.newFuture(channel, req, timeout);
    try {
      // 发送请求
      channel.send(req);
    } catch (RemotingException e) {
      // 发生异常，取消 DefaultFuture
      future.cancel();
      throw e;
    }
    return future;
  }

  @Override
  public boolean isClosed() {
    return closed;
  }

  @Override
  public void close() {
    try {
      channel.close();
    } catch (Throwable e) {
      logger.warn(e.getMessage(), e);
    }
  }

  // graceful close

  /**
   * 优雅关闭
   *
   * @param timeout
   */
  @Override
  public void close(int timeout) {
    if (closed) {
      return;
    }
    closed = true;
    if (timeout > 0) {
      long start = System.currentTimeMillis();
      // 等待请求完成， 如果有未完成的请求的话
      while (DefaultFuture.hasFuture(channel)
          && System.currentTimeMillis() - start < timeout) {
        try {
          Thread.sleep(10);
        } catch (InterruptedException e) {
          logger.warn(e.getMessage(), e);
        }
      }
    }
    // 关闭通道
    close();
  }

  @Override
  public void startClose() {
    channel.startClose();
  }

  @Override
  public InetSocketAddress getLocalAddress() {
    return channel.getLocalAddress();
  }

  @Override
  public InetSocketAddress getRemoteAddress() {
    return channel.getRemoteAddress();
  }

  @Override
  public NetURL getUrl() {
    return channel.getUrl();
  }

  @Override
  public boolean isConnected() {
    return channel.isConnected();
  }

  @Override
  public ChannelEventHandler getChannelHandler() {
    return channel.getChannelHandler();
  }

  @Override
  public ExchangeHandler getExchangeHandler() {
    return (ExchangeHandler) channel.getChannelHandler();
  }

  @Override
  public Object getAttribute(String key) {
    return channel.getAttribute(key);
  }

  @Override
  public void setAttribute(String key, Object value) {
    channel.setAttribute(key, value);
  }

  @Override
  public void removeAttribute(String key) {
    channel.removeAttribute(key);
  }

  @Override
  public boolean hasAttribute(String key) {
    return channel.hasAttribute(key);
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
    HeaderExchangeChannel other = (HeaderExchangeChannel) obj;
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
    return channel.toString();
  }

}
