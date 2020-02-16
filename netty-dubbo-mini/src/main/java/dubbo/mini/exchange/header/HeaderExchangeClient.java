package dubbo.mini.exchange.header;

import static dubbo.mini.common.utils.UrlUtils.getHeartbeat;
import static dubbo.mini.common.utils.UrlUtils.getIdleTimeout;

import dubbo.mini.common.Constants;
import dubbo.mini.common.NetURL;
import dubbo.mini.common.timer.HashedWheelTimer;
import dubbo.mini.exchange.ExchangeChannel;
import dubbo.mini.exchange.ExchangeClient;
import dubbo.mini.exchange.ExchangeHandler;
import dubbo.mini.remote.ChannelEventHandler;
import dubbo.mini.remote.Client;
import dubbo.mini.remote.RemotingException;
import dubbo.mini.support.NamedThreadFactory;
import dubbo.mini.support.ResponseFuture;
import dubbo.mini.util.Assert;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class HeaderExchangeClient implements ExchangeClient {

  /**
   * 客户端
   */
  private final Client client;
  /**
   * 信息交换通道
   */
  private final ExchangeChannel channel;

  private static final HashedWheelTimer IDLE_CHECK_TIMER = new HashedWheelTimer(
      new NamedThreadFactory("dubbo-client-idleCheck", true), 1, TimeUnit.SECONDS,
      Constants.TICKS_PER_WHEEL);

  /**
   * 心跳定时器
   */
  private HeartbeatTimerTask heartBeatTimerTask;
  private ReconnectTimerTask reconnectTimerTask;

  public HeaderExchangeClient(Client client, boolean startTimer) {
    Assert.notNull(client, "Client can't be null");
    this.client = client;
    // 创建 HeaderExchangeChannel 对象
    this.channel = new HeaderExchangeChannel(client);

    if (startTimer) {
      NetURL url = client.getUrl();
      startReconnectTask(url);
      startHeartBeatTask(url);
    }
  }

  @Override
  public ResponseFuture request(Object request) throws RemotingException {
    return channel.request(request);
  }

  @Override
  public NetURL getUrl() {
    return channel.getUrl();
  }

  @Override
  public InetSocketAddress getRemoteAddress() {
    return channel.getRemoteAddress();
  }

  @Override
  public ResponseFuture request(Object request, int timeout) throws RemotingException {
    return channel.request(request, timeout);
  }

  @Override
  public ChannelEventHandler getChannelHandler() {
    return channel.getChannelHandler();
  }

  @Override
  public boolean isConnected() {
    return channel.isConnected();
  }

  @Override
  public InetSocketAddress getLocalAddress() {
    return channel.getLocalAddress();
  }

  @Override
  public ExchangeHandler getExchangeHandler() {
    return channel.getExchangeHandler();
  }

  @Override
  public void send(Object message) throws RemotingException {
    channel.send(message);
  }

  @Override
  public void send(Object message, boolean sent) throws RemotingException {
    channel.send(message, sent);
  }

  @Override
  public boolean isClosed() {
    return channel.isClosed();
  }

  @Override
  public void close() {
    doClose();
    channel.close();
  }

  @Override
  public void close(int timeout) {
    // Mark the client into the closure process
    startClose();
    doClose();
    channel.close(timeout);
  }

  @Override
  public void startClose() {
    channel.startClose();
  }

  @Override
  public void reset(NetURL url) {
    client.reset(url);
    // FIXME, should cancel and restart timer tasks if parameters in the new URL are different?
  }


  @Override
  public void reconnect() throws RemotingException {
    client.reconnect();
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

  private void startHeartBeatTask(NetURL url) {
    if (!client.canHandleIdle()) {
      AbstractTimerTask.ChannelProvider cp = () -> Collections
          .singletonList(HeaderExchangeClient.this);
      int heartbeat = getHeartbeat(url);
      long heartbeatTick = calculateLeastDuration(heartbeat);
      this.heartBeatTimerTask = new HeartbeatTimerTask(cp, heartbeatTick, heartbeat);
      IDLE_CHECK_TIMER.newTimeout(heartBeatTimerTask, heartbeatTick, TimeUnit.MILLISECONDS);
    }
  }

  private void startReconnectTask(NetURL url) {
    if (shouldReconnect(url)) {
      AbstractTimerTask.ChannelProvider cp = () -> Collections
          .singletonList(HeaderExchangeClient.this);
      int idleTimeout = getIdleTimeout(url);
      long heartbeatTimeoutTick = calculateLeastDuration(idleTimeout);
      this.reconnectTimerTask = new ReconnectTimerTask(cp, heartbeatTimeoutTick, idleTimeout);
      IDLE_CHECK_TIMER.newTimeout(reconnectTimerTask, heartbeatTimeoutTick, TimeUnit.MILLISECONDS);
    }
  }

  private void doClose() {
    if (heartBeatTimerTask != null) {
      heartBeatTimerTask.cancel();
    }

    if (reconnectTimerTask != null) {
      reconnectTimerTask.cancel();
    }
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

  private boolean shouldReconnect(NetURL url) {
    return url.getParameter(Constants.RECONNECT_KEY, true);
  }

  @Override
  public String toString() {
    return "HeaderExchangeClient [channel=" + channel + "]";
  }
}