package dubbo.mini.exchange.header;

import dubbo.mini.common.Constants;
import dubbo.mini.exchange.Request;
import dubbo.mini.exchange.Response;
import dubbo.mini.remote.AbstractChannelHandlerDelegate;
import dubbo.mini.remote.ChannelEventHandler;
import dubbo.mini.remote.NetChannel;
import dubbo.mini.remote.RemotingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HeartbeatHandler extends AbstractChannelHandlerDelegate {

  private static final Logger logger = LoggerFactory.getLogger(HeartbeatHandler.class);

  public static final String KEY_READ_TIMESTAMP = "READ_TIMESTAMP";

  public static final String KEY_WRITE_TIMESTAMP = "WRITE_TIMESTAMP";

  public HeartbeatHandler(ChannelEventHandler handler) {
    super(handler);
  }

  @Override
  public void connected(NetChannel channel) throws RemotingException {
    setReadTimestamp(channel);
    setWriteTimestamp(channel);
    handler.connected(channel);
  }

  @Override
  public void disconnected(NetChannel channel) throws RemotingException {
    clearReadTimestamp(channel);
    clearWriteTimestamp(channel);
    handler.disconnected(channel);
  }

  @Override
  public void sent(NetChannel channel, Object message) throws RemotingException {
    setWriteTimestamp(channel);
    handler.sent(channel, message);
  }

  @Override
  public void received(NetChannel channel, Object message) throws RemotingException {
    setReadTimestamp(channel);
    if (isHeartbeatRequest(message)) {
      Request req = (Request) message;
      if (req.isTwoWay()) {
        Response res = new Response(req.getId());
        res.setEvent(Response.HEARTBEAT_EVENT);
        channel.send(res);
        if (logger.isInfoEnabled()) {
          int heartbeat = channel.getUrl().getParameter(Constants.HEARTBEAT_KEY, 0);
          if (logger.isDebugEnabled()) {
            logger.debug("Received heartbeat from remote channel " + channel.getRemoteAddress()
                + ", cause: The channel has no data-transmission exceeds a heartbeat period"
                + (heartbeat > 0 ? ": " + heartbeat + "ms" : ""));
          }
        }
      }
      return;
    }
    if (isHeartbeatResponse(message)) {
      if (logger.isDebugEnabled()) {
        logger.debug("Receive heartbeat response in thread " + Thread.currentThread().getName());
      }
      return;
    }
    handler.received(channel, message);
  }

  private void setReadTimestamp(NetChannel channel) {
    channel.setAttribute(KEY_READ_TIMESTAMP, System.currentTimeMillis());
  }

  private void setWriteTimestamp(NetChannel channel) {
    channel.setAttribute(KEY_WRITE_TIMESTAMP, System.currentTimeMillis());
  }

  private void clearReadTimestamp(NetChannel channel) {
    channel.removeAttribute(KEY_READ_TIMESTAMP);
  }

  private void clearWriteTimestamp(NetChannel channel) {
    channel.removeAttribute(KEY_WRITE_TIMESTAMP);
  }

  private boolean isHeartbeatRequest(Object message) {
    return message instanceof Request && ((Request) message).isHeartbeat();
  }

  private boolean isHeartbeatResponse(Object message) {
    return message instanceof Response && ((Response) message).isHeartbeat();
  }
}
