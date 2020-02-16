package dubbo.mini.protocol.dubbo;

import dubbo.mini.common.Constants;
import dubbo.mini.common.NetURL;
import dubbo.mini.exception.RpcException;
import dubbo.mini.exchange.ExchangeClient;
import dubbo.mini.exchange.header.HeaderExchangeClient;
import dubbo.mini.remote.ChannelEventHandler;
import dubbo.mini.remote.NetChannel;
import dubbo.mini.remote.RemotingException;
import dubbo.mini.remote.TimeoutException;
import dubbo.mini.rpc.AbstractInvoker;
import dubbo.mini.rpc.Invocation;
import dubbo.mini.rpc.Result;
import dubbo.mini.rpc.RpcInvocation;
import dubbo.mini.rpc.RpcResult;
import dubbo.mini.transport.ClientDelegate;
import java.net.InetSocketAddress;

class ChannelWrappedInvoker<T> extends AbstractInvoker<T> {

  private final NetChannel channel;
  private final String serviceKey;
  private final ExchangeClient currentClient;

  ChannelWrappedInvoker(Class<T> serviceType, NetChannel channel, NetURL url, String serviceKey) {
    super(serviceType, url,
        new String[]{Constants.GROUP_KEY, Constants.TOKEN_KEY, Constants.TIMEOUT_KEY});
    this.channel = channel;
    this.serviceKey = serviceKey;
    this.currentClient = new HeaderExchangeClient(new ChannelWrapper(this.channel), false);
  }

  @Override
  protected Result doInvoke(Invocation invocation) throws Throwable {
    RpcInvocation inv = (RpcInvocation) invocation;
    // use interface's name as service path to export if it's not found on client side
    inv.setAttachment(Constants.PATH_KEY, getInterface().getName());
    inv.setAttachment(Constants.CALLBACK_SERVICE_KEY, serviceKey);

    try {
      if (getUrl().getMethodParameter(invocation.getMethodName(), Constants.ASYNC_KEY,
          false)) { // may have concurrency issue
        currentClient.send(inv,
            getUrl().getMethodParameter(invocation.getMethodName(), Constants.SENT_KEY, false));
        return new RpcResult();
      }
      int timeout = getUrl().getMethodParameter(invocation.getMethodName(), Constants.TIMEOUT_KEY,
          Constants.DEFAULT_TIMEOUT);
      if (timeout > 0) {
        return (Result) currentClient.request(inv, timeout).get();
      } else {
        return (Result) currentClient.request(inv).get();
      }
    } catch (RpcException e) {
      throw e;
    } catch (TimeoutException e) {
      throw new RpcException(RpcException.TIMEOUT_EXCEPTION, e.getMessage(), e);
    } catch (RemotingException e) {
      throw new RpcException(RpcException.NETWORK_EXCEPTION, e.getMessage(), e);
    } catch (Throwable e) { // here is non-biz exception, wrap it.
      throw new RpcException(e.getMessage(), e);
    }
  }

  @Override
  public void destroy() {
//        super.destroy();
//        try {
//            channel.close();
//        } catch (Throwable t) {
//            logger.warn(t.getMessage(), t);
//        }
  }

  public static class ChannelWrapper extends ClientDelegate {

    private final NetChannel channel;
    private final NetURL url;

    ChannelWrapper(NetChannel channel) {
      this.channel = channel;
      this.url = channel.getUrl().addParameter("codec", DubboCodec.NAME);
    }

    @Override
    public NetURL getUrl() {
      return url;
    }

    @Override
    public ChannelEventHandler getChannelHandler() {
      return channel.getChannelHandler();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
      return channel.getLocalAddress();
    }

    @Override
    public void close() {
      channel.close();
    }

    @Override
    public boolean isClosed() {
      return channel == null || channel.isClosed();
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
      return channel.getLocalAddress();
    }

    @Override
    public boolean isConnected() {
      return channel != null && channel.isConnected();
    }

    @Override
    public boolean hasAttribute(String key) {
      return channel.hasAttribute(key);
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
    public void reconnect() throws RemotingException {

    }

    @Override
    public void send(Object message) throws RemotingException {
      channel.send(message);
    }

    @Override
    public void send(Object message, boolean sent) throws RemotingException {
      channel.send(message, sent);
    }
  }
}
