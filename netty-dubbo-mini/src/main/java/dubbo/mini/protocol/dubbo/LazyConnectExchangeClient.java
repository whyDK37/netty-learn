package dubbo.mini.protocol.dubbo;

import dubbo.mini.common.Constants;
import dubbo.mini.common.NetURL;
import dubbo.mini.common.utils.NetUtils;
import dubbo.mini.exchange.ExchangeClient;
import dubbo.mini.exchange.ExchangeHandler;
import dubbo.mini.exchange.Exchangers;
import dubbo.mini.remote.ChannelEventHandler;
import dubbo.mini.remote.RemotingException;
import dubbo.mini.support.ResponseFuture;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class LazyConnectExchangeClient implements ExchangeClient {

  /**
   * when this warning rises from invocation, program probably have bug.
   */
  protected static final String REQUEST_WITH_WARNING_KEY = "lazyclient_request_with_warning";
  private final static Logger logger = LoggerFactory.getLogger(LazyConnectExchangeClient.class);
  protected final boolean requestWithWarning;
  private final NetURL url;
  private final ExchangeHandler requestHandler;
  private final Lock connectLock = new ReentrantLock();
  private final int warning_period = 5000;
  /**
   * lazy connect, initial state for connection
   */
  private final boolean initialState;
  private volatile ExchangeClient client;
  private AtomicLong warningcount = new AtomicLong(0);

  public LazyConnectExchangeClient(NetURL url, ExchangeHandler requestHandler) {
    // lazy connect, need set send.reconnect = true, to avoid channel bad status.
    this.url = url.addParameter(Constants.SEND_RECONNECT_KEY, Boolean.TRUE.toString());
    this.requestHandler = requestHandler;
    this.initialState = url.getParameter(Constants.LAZY_CONNECT_INITIAL_STATE_KEY,
        Constants.DEFAULT_LAZY_CONNECT_INITIAL_STATE);
    this.requestWithWarning = url.getParameter(REQUEST_WITH_WARNING_KEY, false);
  }

  private void initClient() throws RemotingException {
    if (client != null) {
      return;
    }
    if (logger.isInfoEnabled()) {
      logger.info("Lazy connect to " + url);
    }
    connectLock.lock();
    try {
      if (client != null) {
        return;
      }
      this.client = Exchangers.connect(url, requestHandler);
    } finally {
      connectLock.unlock();
    }
  }

  @Override
  public ResponseFuture request(Object request) throws RemotingException {
    warning();
    initClient();
    return client.request(request);
  }

  @Override
  public NetURL getUrl() {
    return url;
  }

  @Override
  public InetSocketAddress getRemoteAddress() {
    if (client == null) {
      return InetSocketAddress.createUnresolved(url.getHost(), url.getPort());
    } else {
      return client.getRemoteAddress();
    }
  }

  @Override
  public ResponseFuture request(Object request, int timeout) throws RemotingException {
    warning();
    initClient();
    return client.request(request, timeout);
  }

  /**
   * If {@link #REQUEST_WITH_WARNING_KEY} is configured, then warn once every 5000 invocations.
   */
  private void warning() {
    if (requestWithWarning) {
      if (warningcount.get() % warning_period == 0) {
        logger.warn("", new IllegalStateException(
            "safe guard client , should not be called ,must have a bug."));
      }
      warningcount.incrementAndGet();
    }
  }

  @Override
  public ChannelEventHandler getChannelHandler() {
    checkClient();
    return client.getChannelHandler();
  }

  @Override
  public boolean isConnected() {
    if (client == null) {
      return initialState;
    } else {
      return client.isConnected();
    }
  }

  @Override
  public InetSocketAddress getLocalAddress() {
    if (client == null) {
      return InetSocketAddress.createUnresolved(NetUtils.getLocalHost(), 0);
    } else {
      return client.getLocalAddress();
    }
  }

  @Override
  public ExchangeHandler getExchangeHandler() {
    return requestHandler;
  }

  @Override
  public void send(Object message) throws RemotingException {
    initClient();
    client.send(message);
  }

  @Override
  public void send(Object message, boolean sent) throws RemotingException {
    initClient();
    client.send(message, sent);
  }

  @Override
  public boolean isClosed() {
    if (client != null) {
      return client.isClosed();
    } else {
      return true;
    }
  }

  @Override
  public void close() {
    if (client != null) {
      client.close();
    }
  }

  @Override
  public void close(int timeout) {
    if (client != null) {
      client.close(timeout);
    }
  }

  @Override
  public void startClose() {
    if (client != null) {
      client.startClose();
    }
  }

  @Override
  public void reset(NetURL url) {
    checkClient();
    client.reset(url);
  }

  @Override
  public void reconnect() throws RemotingException {
    checkClient();
    client.reconnect();
  }

  @Override
  public Object getAttribute(String key) {
    if (client == null) {
      return null;
    } else {
      return client.getAttribute(key);
    }
  }

  @Override
  public void setAttribute(String key, Object value) {
    checkClient();
    client.setAttribute(key, value);
  }

  @Override
  public void removeAttribute(String key) {
    checkClient();
    client.removeAttribute(key);
  }

  @Override
  public boolean hasAttribute(String key) {
    if (client == null) {
      return false;
    } else {
      return client.hasAttribute(key);
    }
  }

  private void checkClient() {
    if (client == null) {
      throw new IllegalStateException(
          "LazyConnectExchangeClient state error. the client has not be init .url:" + url);
    }
  }
}
