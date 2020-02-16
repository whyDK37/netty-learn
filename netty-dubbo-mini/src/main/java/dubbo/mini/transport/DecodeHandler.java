package dubbo.mini.transport;

import dubbo.mini.exchange.Request;
import dubbo.mini.exchange.Response;
import dubbo.mini.remote.AbstractChannelHandlerDelegate;
import dubbo.mini.remote.ChannelEventHandler;
import dubbo.mini.remote.Decodeable;
import dubbo.mini.remote.NetChannel;
import dubbo.mini.remote.RemotingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DecodeHandler extends AbstractChannelHandlerDelegate {

  private static final Logger log = LoggerFactory.getLogger(DecodeHandler.class);

  public DecodeHandler(ChannelEventHandler handler) {
    super(handler);
  }

  @Override
  public void received(NetChannel channel, Object message) throws RemotingException {
    if (message instanceof Decodeable) {
      decode(message);
    }

    if (message instanceof Request) {
      decode(((Request) message).getData());
    }

    if (message instanceof Response) {
      decode(((Response) message).getResult());
    }

    handler.received(channel, message);
  }

  private void decode(Object message) {
    if (message != null && message instanceof Decodeable) {
      try {
        ((Decodeable) message).decode();
        if (log.isDebugEnabled()) {
          log.debug("Decode decodeable message " + message.getClass().getName());
        }
      } catch (Throwable e) {
        if (log.isWarnEnabled()) {
          log.warn("Call Decodeable.decode failed: " + e.getMessage(), e);
        }
      } // ~ end of catch
    } // ~ end of if
  } // ~ end of method decode

}