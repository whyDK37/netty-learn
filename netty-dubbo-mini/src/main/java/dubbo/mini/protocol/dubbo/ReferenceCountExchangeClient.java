package dubbo.mini.protocol.dubbo;

import dubbo.mini.common.Constants;
import dubbo.mini.common.NetURL;
import dubbo.mini.common.URLBuilder;
import dubbo.mini.exchange.ExchangeClient;
import dubbo.mini.exchange.ExchangeHandler;
import dubbo.mini.remote.ChannelEventHandler;
import dubbo.mini.remote.RemotingException;
import dubbo.mini.support.ResponseFuture;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

final class ReferenceCountExchangeClient implements ExchangeClient {

  private final NetURL url;
  private final AtomicInteger referenceCount = new AtomicInteger(0);

  private ExchangeClient client;

  public ReferenceCountExchangeClient(ExchangeClient client) {
    this.client = client;
    referenceCount.incrementAndGet();
    this.url = client.getUrl();
  }

  @Override
  public void reset(NetURL url) {
    client.reset(url);
  }

  @Override
  public ResponseFuture request(Object request) throws RemotingException {
    return client.request(request);
  }

  @Override
  public NetURL getUrl() {
    return client.getUrl();
  }

  @Override
  public InetSocketAddress getRemoteAddress() {
    return client.getRemoteAddress();
  }

  @Override
  public ChannelEventHandler getChannelHandler() {
    return client.getChannelHandler();
  }

  @Override
  public ResponseFuture request(Object request, int timeout) throws RemotingException {
    return client.request(request, timeout);
  }

  @Override
  public boolean isConnected() {
    return client.isConnected();
  }

  @Override
  public void reconnect() throws RemotingException {
    client.reconnect();
  }

  @Override
  public InetSocketAddress getLocalAddress() {
    return client.getLocalAddress();
  }

  @Override
  public boolean hasAttribute(String key) {
    return client.hasAttribute(key);
  }

  @Override
  public void send(Object message) throws RemotingException {
    client.send(message);
  }

  @Override
  public ExchangeHandler getExchangeHandler() {
    return client.getExchangeHandler();
  }

  @Override
  public Object getAttribute(String key) {
    return client.getAttribute(key);
  }

  @Override
  public void send(Object message, boolean sent) throws RemotingException {
    client.send(message, sent);
  }

  @Override
  public void setAttribute(String key, Object value) {
    client.setAttribute(key, value);
  }

  @Override
  public void removeAttribute(String key) {
    client.removeAttribute(key);
  }

  /**
   * close() is not idempotent any longer
   */
  @Override
  public void close() {
    close(0);
  }

  @Override
  public void close(int timeout) {
    if (referenceCount.decrementAndGet() <= 0) {
      if (timeout == 0) {
        client.close();

      } else {
        client.close(timeout);
      }

      replaceWithLazyClient();
    }
  }

  @Override
  public void startClose() {
    client.startClose();
  }

  /**
   * when closing the client, the client needs to be set to LazyConnectExchangeClient, and if a new
   * call is made, the client will "resurrect".
   *
   * @return
   */
  private void replaceWithLazyClient() {
    // this is a defensive operation to avoid client is closed by accident, the initial state of the client is false
    NetURL lazyUrl = URLBuilder.from(url)
        .addParameter(Constants.LAZY_CONNECT_INITIAL_STATE_KEY, Boolean.FALSE)
        .addParameter(Constants.RECONNECT_KEY, Boolean.FALSE)
        .addParameter(Constants.SEND_RECONNECT_KEY, Boolean.TRUE.toString())
        .addParameter("warning", Boolean.TRUE.toString())
        .addParameter(LazyConnectExchangeClient.REQUEST_WITH_WARNING_KEY, true)
        .addParameter("_client_memo", "referencecounthandler.replacewithlazyclient")
        .build();

    /**
     * the order of judgment in the if statement cannot be changed.
     */
    if (!(client instanceof LazyConnectExchangeClient) || client.isClosed()) {
      client = new LazyConnectExchangeClient(lazyUrl, client.getExchangeHandler());
    }
  }

  @Override
  public boolean isClosed() {
    return client.isClosed();
  }

  public void incrementAndGetCount() {
    referenceCount.incrementAndGet();
  }
}

