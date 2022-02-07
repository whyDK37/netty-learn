package dubbo.mini.exchange.header;

import dubbo.mini.remote.Client;
import dubbo.mini.remote.NetChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReconnectTimerTask extends AbstractTimerTask {

  private static final Logger logger = LoggerFactory.getLogger(ReconnectTimerTask.class);

  private final int idleTimeout;

  public ReconnectTimerTask(ChannelProvider channelProvider, Long heartbeatTimeoutTick,
      int idleTimeout) {
    super(channelProvider, heartbeatTimeoutTick);
    this.idleTimeout = idleTimeout;
  }

  @Override
  protected void doTask(NetChannel channel) {
    try {
      Long lastRead = lastRead(channel);
      Long now = now();

      // Rely on reconnect timer to reconnect when AbstractClient.doConnect fails to init the connection
      if (!channel.isConnected()) {
        try {
          logger.info("Initial connection to " + channel);
          ((Client) channel).reconnect();
        } catch (Exception e) {
          logger.error("Fail to connect to " + channel, e);
        }
        // check pong at client
      } else if (lastRead != null && now - lastRead > idleTimeout) {
        logger.warn("Reconnect to channel " + channel + ", because heartbeat read idle time out: "
            + idleTimeout + "ms");
        try {
          ((Client) channel).reconnect();
        } catch (Exception e) {
          logger.error(channel + "reconnect failed during idle time.", e);
        }
      }
    } catch (Throwable t) {
      logger.warn("Exception when reconnect to remote channel " + channel.getRemoteAddress(), t);
    }
  }
}
