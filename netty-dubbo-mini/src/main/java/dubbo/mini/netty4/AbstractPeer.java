package dubbo.mini.netty4;

import dubbo.mini.common.Constants;
import dubbo.mini.common.NetURL;
import dubbo.mini.remote.ChannelEventHandler;
import dubbo.mini.remote.ChannelHandlerDelegate;
import dubbo.mini.remote.Endpoint;
import dubbo.mini.remote.NetChannel;
import dubbo.mini.remote.RemotingException;

public abstract class AbstractPeer implements Endpoint, ChannelEventHandler {

  private final ChannelEventHandler handler;

  volatile NetURL url;

  // closing closed means the process is being closed and close is finished
  private volatile boolean closing;

  private volatile boolean closed;

  public AbstractPeer(NetURL url, ChannelEventHandler handler) {
    if (url == null) {
      throw new IllegalArgumentException("url == null");
    }
    if (handler == null) {
      throw new IllegalArgumentException("handler == null");
    }
    this.url = url;
    this.handler = handler;
  }

  @Override
  public void send(Object message) throws RemotingException {
    send(message, url.getParameter(Constants.SENT_KEY, false));
  }

  @Override
  public void close() {
    closed = true;
  }

  @Override
  public void close(int timeout) {
    close();
  }

  @Override
  public void startClose() {
    if (isClosed()) {
      return;
    }
    closing = true;
  }

  @Override
  public NetURL getUrl() {
    return url;
  }

  protected void setUrl(NetURL url) {
    if (url == null) {
      throw new IllegalArgumentException("url == null");
    }
    this.url = url;
  }

  @Override
  public ChannelEventHandler getChannelHandler() {
    if (handler instanceof ChannelHandlerDelegate) {
      return ((ChannelHandlerDelegate) handler).getHandler();
    } else {
      return handler;
    }
  }

  /**
   * @return ChannelHandler
   */
  @Deprecated
  public ChannelEventHandler getHandler() {
    return getDelegateHandler();
  }

  /**
   * Return the final handler (which may have been wrapped). This method should be distinguished
   * with getChannelHandler() method
   *
   * @return ChannelHandler
   */
  public ChannelEventHandler getDelegateHandler() {
    return handler;
  }

  @Override
  public boolean isClosed() {
    return closed;
  }

  public boolean isClosing() {
    return closing && !closed;
  }

  @Override
  public void connected(NetChannel ch) throws RemotingException {
    if (closed) {
      return;
    }
    handler.connected(ch);
  }

  @Override
  public void disconnected(NetChannel ch) throws RemotingException {
    handler.disconnected(ch);
  }

  @Override
  public void sent(NetChannel ch, Object msg) throws RemotingException {
    if (closed) {
      return;
    }
    handler.sent(ch, msg);
  }

  @Override
  public void received(NetChannel ch, Object msg) throws RemotingException {
    if (closed) {
      return;
    }
    handler.received(ch, msg);
  }

  @Override
  public void caught(NetChannel ch, Throwable ex) throws RemotingException {
    handler.caught(ch, ex);
  }
}