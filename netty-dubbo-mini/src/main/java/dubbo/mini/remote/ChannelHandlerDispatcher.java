package dubbo.mini.remote;

import dubbo.mini.common.utils.CollectionUtils;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelHandlerDispatcher implements ChannelEventHandler {

  private static final Logger logger = LoggerFactory.getLogger(ChannelHandlerDispatcher.class);

  private final Collection<ChannelEventHandler> channelHandlers = new CopyOnWriteArraySet<ChannelEventHandler>();

  public ChannelHandlerDispatcher() {
  }

  public ChannelHandlerDispatcher(ChannelEventHandler... handlers) {
    this(handlers == null ? null : Arrays.asList(handlers));
  }

  public ChannelHandlerDispatcher(Collection<ChannelEventHandler> handlers) {
    if (CollectionUtils.isNotEmpty(handlers)) {
      this.channelHandlers.addAll(handlers);
    }
  }

  public Collection<ChannelEventHandler> getChannelHandlers() {
    return channelHandlers;
  }

  public ChannelHandlerDispatcher addChannelHandler(ChannelEventHandler handler) {
    this.channelHandlers.add(handler);
    return this;
  }

  public ChannelHandlerDispatcher removeChannelHandler(ChannelEventHandler handler) {
    this.channelHandlers.remove(handler);
    return this;
  }

  @Override
  public void connected(NetChannel channel) {
    for (ChannelEventHandler listener : channelHandlers) {
      try {
        listener.connected(channel);
      } catch (Throwable t) {
        logger.error(t.getMessage(), t);
      }
    }
  }

  @Override
  public void disconnected(NetChannel channel) {
    for (ChannelEventHandler listener : channelHandlers) {
      try {
        listener.disconnected(channel);
      } catch (Throwable t) {
        logger.error(t.getMessage(), t);
      }
    }
  }

  @Override
  public void sent(NetChannel channel, Object message) {
    for (ChannelEventHandler listener : channelHandlers) {
      try {
        listener.sent(channel, message);
      } catch (Throwable t) {
        logger.error(t.getMessage(), t);
      }
    }
  }

  @Override
  public void received(NetChannel channel, Object message) {
    for (ChannelEventHandler listener : channelHandlers) {
      try {
        listener.received(channel, message);
      } catch (Throwable t) {
        logger.error(t.getMessage(), t);
      }
    }
  }

  @Override
  public void caught(NetChannel channel, Throwable exception) {
    for (ChannelEventHandler listener : channelHandlers) {
      try {
        listener.caught(channel, exception);
      } catch (Throwable t) {
        logger.error(t.getMessage(), t);
      }
    }
  }

}
