package dubbo.mini.netty4;

import dubbo.mini.codec.Codec;
import dubbo.mini.common.Constants;
import dubbo.mini.common.NetURL;
import dubbo.mini.remote.ChannelEventHandler;
import dubbo.mini.remote.RemotingException;
import dubbo.mini.support.ExtensionLoader;
import java.net.InetSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Administrator
 */
public abstract class AbstractEndpoint extends AbstractPeer implements ChannelEventHandler {

  private Logger logger = LoggerFactory.getLogger(this.getClass());
  private int timeout;

  private int connectTimeout;

  private Codec codec;

  public AbstractEndpoint(NetURL url, ChannelEventHandler handler) {
    super(url, handler);
    this.codec = getChannelCodec(url);
    this.timeout = url.getPositiveParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
    this.connectTimeout = url
        .getPositiveParameter(Constants.CONNECT_TIMEOUT_KEY, Constants.DEFAULT_CONNECT_TIMEOUT);
  }


  protected static Codec getChannelCodec(NetURL url) {
    String codecName = url.getParameter(Constants.CODEC_KEY, "telnet");
    return ExtensionLoader.getExtensionLoader(Codec.class).getExtension(codecName);
  }

  public int getTimeout() {
    return timeout;
  }

  public long getConnectTimeout() {
    return connectTimeout;
  }

  @Override
  public NetURL getUrl() {
    return url;
  }

  @Override
  public ChannelEventHandler getChannelHandler() {
    return null;
  }

  @Override
  public InetSocketAddress getLocalAddress() {
    return null;
  }

  @Override
  public void send(Object message) throws RemotingException {

  }

  @Override
  public void send(Object message, boolean sent) throws RemotingException {

  }


  protected Codec getCodec() {
    return codec;
  }


  public void reset(NetURL url) {
    if (isClosed()) {
      throw new IllegalStateException("Failed to reset parameters "
          + url + ", cause: Channel closed. channel: " + getLocalAddress());
    }
    try {
      if (url.hasParameter(Constants.TIMEOUT_KEY)) {
        int t = url.getParameter(Constants.TIMEOUT_KEY, 0);
        if (t > 0) {
          this.timeout = t;
        }
      }
    } catch (Throwable t) {
      logger.error(t.getMessage(), t);
    }
    try {
      if (url.hasParameter(Constants.CONNECT_TIMEOUT_KEY)) {
        int t = url.getParameter(Constants.CONNECT_TIMEOUT_KEY, 0);
        if (t > 0) {
          this.connectTimeout = t;
        }
      }
    } catch (Throwable t) {
      logger.error(t.getMessage(), t);
    }
    try {
      if (url.hasParameter(Constants.CODEC_KEY)) {
        this.codec = getChannelCodec(url);
      }
    } catch (Throwable t) {
      logger.error(t.getMessage(), t);
    }
  }
}
