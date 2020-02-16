package dubbo.mini.transport;

import dubbo.mini.common.NetURL;
import dubbo.mini.remote.ChannelEventHandler;
import dubbo.mini.remote.ChannelHandlerAdapter;
import dubbo.mini.remote.ChannelHandlerDispatcher;
import dubbo.mini.remote.Client;
import dubbo.mini.remote.RemotingException;
import dubbo.mini.remote.Server;
import dubbo.mini.remote.Transporter;
import dubbo.mini.support.ExtensionLoader;

public class Transporters {


  private Transporters() {
  }

  public static Server bind(String url, ChannelEventHandler... handler) throws RemotingException {
    return bind(NetURL.valueOf(url), handler);
  }

  public static Server bind(NetURL url, ChannelEventHandler... handlers) throws RemotingException {
    if (url == null) {
      throw new IllegalArgumentException("url == null");
    }
    if (handlers == null || handlers.length == 0) {
      throw new IllegalArgumentException("handlers == null");
    }
    ChannelEventHandler handler;
    if (handlers.length == 1) {
      handler = handlers[0];
    } else {
      handler = new ChannelHandlerDispatcher(handlers);
    }
    return getTransporter().bind(url, handler);
  }

  public static Client connect(String url, ChannelEventHandler... handler)
      throws RemotingException {
    return connect(NetURL.valueOf(url), handler);
  }

  public static Client connect(NetURL url, ChannelEventHandler... handlers)
      throws RemotingException {
    if (url == null) {
      throw new IllegalArgumentException("url == null");
    }
    ChannelEventHandler handler;
    if (handlers == null || handlers.length == 0) {
      handler = new ChannelHandlerAdapter();
    } else if (handlers.length == 1) {
      handler = handlers[0];
    } else {
      handler = new ChannelHandlerDispatcher(handlers);
    }
    return getTransporter().connect(url, handler);
  }

  public static Transporter getTransporter() {
    return ExtensionLoader.getExtensionLoader(Transporter.class).getDefaultExtension();
  }

}