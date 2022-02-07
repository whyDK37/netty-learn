package dubbo.mini.exchange.header;

import dubbo.mini.remote.NetChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloseTimerTask extends AbstractTimerTask {

  private static final Logger logger = LoggerFactory.getLogger(CloseTimerTask.class);

  private final int idleTimeout;

  public CloseTimerTask(ChannelProvider channelProvider, Long heartbeatTimeoutTick,
      int idleTimeout) {
    super(channelProvider, heartbeatTimeoutTick);
    this.idleTimeout = idleTimeout;
  }

  @Override
  protected void doTask(NetChannel channel) {
    try {
      Long lastRead = lastRead(channel);
      Long lastWrite = lastWrite(channel);
      Long now = now();
      // check ping & pong at server
      if ((lastRead != null && now - lastRead > idleTimeout)
          || (lastWrite != null && now - lastWrite > idleTimeout)) {
        logger.warn("Close channel " + channel + ", because idleCheck timeout: "
            + idleTimeout + "ms");
        channel.close();
      }
    } catch (Throwable t) {
      logger.warn("Exception when close remote channel " + channel.getRemoteAddress(), t);
    }
  }
}
