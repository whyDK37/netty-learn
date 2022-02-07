package dubbo.mini.exchange;

import dubbo.mini.common.Constants;
import dubbo.mini.common.NetURL;
import dubbo.mini.remote.ChannelEventHandler;
import dubbo.mini.remote.ChannelHandlerAdapter;
import dubbo.mini.remote.RemotingException;
import dubbo.mini.support.ExtensionLoader;

public class Exchangers {

  private Exchangers() {
  }

  public static ExchangeServer bind(String url, Replier<?> replier) throws RemotingException {
    return bind(NetURL.valueOf(url), replier);
  }

  public static ExchangeServer bind(NetURL url, Replier<?> replier) throws RemotingException {
    return bind(url, new ChannelHandlerAdapter(), replier);
  }

  public static ExchangeServer bind(String url, ChannelEventHandler handler, Replier<?> replier)
      throws RemotingException {
    return bind(NetURL.valueOf(url), handler, replier);
  }

  public static ExchangeServer bind(NetURL url, ChannelEventHandler handler, Replier<?> replier)
      throws RemotingException {
    return bind(url, new ExchangeHandlerDispatcher(replier, handler));
  }

  public static ExchangeServer bind(String url, ExchangeHandler handler) throws RemotingException {
    return bind(NetURL.valueOf(url), handler);
  }

  public static ExchangeServer bind(NetURL url, ExchangeHandler handler) throws RemotingException {
    if (url == null) {
      throw new IllegalArgumentException("url == null");
    }
    if (handler == null) {
      throw new IllegalArgumentException("handler == null");
    }
    url = url.addParameterIfAbsent(Constants.CODEC_KEY, "exchange");
    return getExchanger(url).bind(url, handler);
  }

  public static ExchangeClient connect(String url) throws RemotingException {
    return connect(NetURL.valueOf(url));
  }

  public static ExchangeClient connect(NetURL url) throws RemotingException {
    return connect(url, new ChannelHandlerAdapter(), null);
  }

  public static ExchangeClient connect(String url, Replier<?> replier) throws RemotingException {
    return connect(NetURL.valueOf(url), new ChannelHandlerAdapter(), replier);
  }

  public static ExchangeClient connect(NetURL url, Replier<?> replier) throws RemotingException {
    return connect(url, new ChannelHandlerAdapter(), replier);
  }

  public static ExchangeClient connect(String url, ChannelEventHandler handler, Replier<?> replier)
      throws RemotingException {
    return connect(NetURL.valueOf(url), handler, replier);
  }

  public static ExchangeClient connect(NetURL url, ChannelEventHandler handler, Replier<?> replier)
      throws RemotingException {
    return connect(url, new ExchangeHandlerDispatcher(replier, handler));
  }

  public static ExchangeClient connect(String url, ExchangeHandler handler)
      throws RemotingException {
    return connect(NetURL.valueOf(url), handler);
  }

  public static ExchangeClient connect(NetURL url, ExchangeHandler handler)
      throws RemotingException {
    if (url == null) {
      throw new IllegalArgumentException("url == null");
    }
    if (handler == null) {
      throw new IllegalArgumentException("handler == null");
    }
    url = url.addParameterIfAbsent(Constants.CODEC_KEY, "exchange");
    return getExchanger(url).connect(url, handler);
  }

  public static Exchanger getExchanger(NetURL url) {
    String type = url.getParameter(Constants.EXCHANGER_KEY, Constants.DEFAULT_EXCHANGER);
    return getExchanger(type);
  }

  public static Exchanger getExchanger(String type) {
    return ExtensionLoader.getExtensionLoader(Exchanger.class).getExtension(type);
  }

}