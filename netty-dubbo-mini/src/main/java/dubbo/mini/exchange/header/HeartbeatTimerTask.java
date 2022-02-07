package dubbo.mini.exchange.header;

import dubbo.mini.exchange.Request;
import dubbo.mini.remote.NetChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HeartbeatTimerTask extends AbstractTimerTask {

  private static final Logger logger = LoggerFactory.getLogger(HeartbeatTimerTask.class);

  private final int heartbeat;

  HeartbeatTimerTask(ChannelProvider channelProvider, Long heartbeatTick, int heartbeat) {
    super(channelProvider, heartbeatTick);
    this.heartbeat = heartbeat;
  }

  @Override
  protected void doTask(NetChannel channel) {
    try {
      Long lastRead = lastRead(channel);
      Long lastWrite = lastWrite(channel);
      if ((lastRead != null && now() - lastRead > heartbeat)
          || (lastWrite != null && now() - lastWrite > heartbeat)) {
        Request req = new Request();
        req.setTwoWay(true);
        req.setEvent(Request.HEARTBEAT_EVENT);
        channel.send(req);
        if (logger.isDebugEnabled()) {
          logger.debug("Send heartbeat to remote channel " + channel.getRemoteAddress()
              + ", cause: The channel has no data-transmission exceeds a heartbeat period: "
              + heartbeat + "ms");
        }
      }
    } catch (Throwable t) {
      logger.warn("Exception when heartbeat to remote channel " + channel.getRemoteAddress(), t);
    }
  }
}
