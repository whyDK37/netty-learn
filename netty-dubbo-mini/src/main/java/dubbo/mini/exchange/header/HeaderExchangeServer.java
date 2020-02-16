package dubbo.mini.exchange.header;

import static java.util.Collections.unmodifiableCollection;

import dubbo.mini.common.Constants;
import dubbo.mini.common.NetURL;
import dubbo.mini.common.timer.HashedWheelTimer;
import dubbo.mini.common.utils.CollectionUtils;
import dubbo.mini.common.utils.UrlUtils;
import dubbo.mini.exchange.ExchangeChannel;
import dubbo.mini.exchange.ExchangeServer;
import dubbo.mini.exchange.Request;
import dubbo.mini.remote.ChannelEventHandler;
import dubbo.mini.remote.NetChannel;
import dubbo.mini.remote.RemotingException;
import dubbo.mini.remote.Server;
import dubbo.mini.support.NamedThreadFactory;
import dubbo.mini.util.Assert;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HeaderExchangeServer implements ExchangeServer {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * 服务器
   */
  private final Server server;
  private AtomicBoolean closed = new AtomicBoolean(false);

  /**
   * 定时器线程池
   */
  private static final HashedWheelTimer IDLE_CHECK_TIMER = new HashedWheelTimer(
      new NamedThreadFactory("dubbo-server-idleCheck", true), 1,
      TimeUnit.SECONDS, Constants.TICKS_PER_WHEEL);

  private CloseTimerTask closeTimerTask;

  public HeaderExchangeServer(Server server) {
    Assert.notNull(server, "server == null");
    this.server = server;
    startIdleCheckTask(getUrl());
  }

  public Server getServer() {
    return server;
  }

  @Override
  public boolean isClosed() {
    return server.isClosed();
  }

  private boolean isRunning() {
    Collection<NetChannel> channels = getChannels();
    for (NetChannel channel : channels) {

      /**
       *  If there are any client connections,
       *  our server should be running.
       */

      if (channel.isConnected()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void close() {
    doClose();
    server.close();
  }

  @Override
  public void close(final int timeout) {
    startClose();
    if (timeout > 0) {
      final long max = (long) timeout;
      final long start = System.currentTimeMillis();
      if (getUrl().getParameter(Constants.CHANNEL_SEND_READONLYEVENT_KEY, true)) {
        sendChannelReadOnlyEvent();
      }
      while (HeaderExchangeServer.this.isRunning()
          && System.currentTimeMillis() - start < max) {
        try {
          Thread.sleep(10);
        } catch (InterruptedException e) {
          logger.warn(e.getMessage(), e);
        }
      }
    }
    doClose();
    server.close(timeout);
  }

  @Override
  public void startClose() {
    server.startClose();
  }

  /**
   * 发送 READONLY 事件给所有 Client ，表示 Server 不再接收新的消息，避免不断有新的消息接收到。 广播客户端，READONLY_EVENT 事件。
   */
  private void sendChannelReadOnlyEvent() {
    Request request = new Request();
    request.setEvent(Request.READONLY_EVENT);
    request.setTwoWay(false);

    Collection<NetChannel> channels = getChannels();
    for (NetChannel channel : channels) {
      try {
        if (channel.isConnected()) {
          channel
              .send(request, getUrl().getParameter(Constants.CHANNEL_READONLYEVENT_SENT_KEY, true));
        }
      } catch (RemotingException e) {
        logger.warn("send cannot write message error.", e);
      }
    }
  }

  private void doClose() {
    if (!closed.compareAndSet(false, true)) {
      return;
    }
    cancelCloseTask();
  }

  private void cancelCloseTask() {
    if (closeTimerTask != null) {
      closeTimerTask.cancel();
    }
  }

  @Override
  public Collection<ExchangeChannel> getExchangeChannels() {
    Collection<ExchangeChannel> exchangeChannels = new ArrayList<ExchangeChannel>();
    Collection<NetChannel> channels = server.getChannels();
    if (CollectionUtils.isNotEmpty(channels)) {
      for (NetChannel channel : channels) {
        exchangeChannels.add(HeaderExchangeChannel.getOrAddChannel(channel));
      }
    }
    return exchangeChannels;
  }

  @Override
  public ExchangeChannel getExchangeChannel(InetSocketAddress remoteAddress) {
    NetChannel channel = server.getChannel(remoteAddress);
    return HeaderExchangeChannel.getOrAddChannel(channel);
  }

  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  public Collection<NetChannel> getChannels() {
    return (Collection) getExchangeChannels();
  }

  @Override
  public NetChannel getChannel(InetSocketAddress remoteAddress) {
    return getExchangeChannel(remoteAddress);
  }

  @Override
  public boolean isBound() {
    return server.isBound();
  }

  @Override
  public InetSocketAddress getLocalAddress() {
    return server.getLocalAddress();
  }

  @Override
  public NetURL getUrl() {
    return server.getUrl();
  }

  @Override
  public ChannelEventHandler getChannelHandler() {
    return server.getChannelHandler();
  }

  @Override
  public void reset(NetURL url) {
    server.reset(url);
    try {
      int currHeartbeat = UrlUtils.getHeartbeat(getUrl());
      int currIdleTimeout = UrlUtils.getIdleTimeout(getUrl());
      int heartbeat = UrlUtils.getHeartbeat(url);
      int idleTimeout = UrlUtils.getIdleTimeout(url);
      if (currHeartbeat != heartbeat || currIdleTimeout != idleTimeout) {
        cancelCloseTask();
        startIdleCheckTask(url);
      }
    } catch (Throwable t) {
      logger.error(t.getMessage(), t);
    }
  }

  @Override
  public void send(Object message) throws RemotingException {
    if (closed.get()) {
      throw new RemotingException(this.getLocalAddress(), null, "Failed to send message " + message
          + ", cause: The server " + getLocalAddress() + " is closed!");
    }
    server.send(message);
  }

  @Override
  public void send(Object message, boolean sent) throws RemotingException {
    if (closed.get()) {
      throw new RemotingException(this.getLocalAddress(), null, "Failed to send message " + message
          + ", cause: The server " + getLocalAddress() + " is closed!");
    }
    server.send(message, sent);
  }

  /**
   * Each interval cannot be less than 1000ms.
   */
  private long calculateLeastDuration(int time) {
    if (time / Constants.HEARTBEAT_CHECK_TICK <= 0) {
      return Constants.LEAST_HEARTBEAT_DURATION;
    } else {
      return time / Constants.HEARTBEAT_CHECK_TICK;
    }
  }

  private void startIdleCheckTask(NetURL url) {
    if (!server.canHandleIdle()) {
      AbstractTimerTask.ChannelProvider cp = () -> unmodifiableCollection(
          HeaderExchangeServer.this.getChannels());
      int idleTimeout = UrlUtils.getIdleTimeout(url);
      long idleTimeoutTick = calculateLeastDuration(idleTimeout);
      CloseTimerTask closeTimerTask = new CloseTimerTask(cp, idleTimeoutTick, idleTimeout);
      this.closeTimerTask = closeTimerTask;

      // init task and start timer.
      IDLE_CHECK_TIMER.newTimeout(closeTimerTask, idleTimeoutTick, TimeUnit.MILLISECONDS);
    }
  }
}