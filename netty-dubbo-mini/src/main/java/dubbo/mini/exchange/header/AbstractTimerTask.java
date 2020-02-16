package dubbo.mini.exchange.header;

import dubbo.mini.common.timer.Timeout;
import dubbo.mini.common.timer.Timer;
import dubbo.mini.common.timer.TimerTask;
import dubbo.mini.remote.NetChannel;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

public abstract class AbstractTimerTask implements TimerTask {

  private final ChannelProvider channelProvider;

  private final Long tick;

  protected volatile boolean cancel = false;

  AbstractTimerTask(ChannelProvider channelProvider, Long tick) {
    if (channelProvider == null || tick == null) {
      throw new IllegalArgumentException();
    }
    this.tick = tick;
    this.channelProvider = channelProvider;
  }

  static Long lastRead(NetChannel channel) {
    return (Long) channel.getAttribute(HeaderExchangeHandler.KEY_READ_TIMESTAMP);
  }

  static Long lastWrite(NetChannel channel) {
    return (Long) channel.getAttribute(HeaderExchangeHandler.KEY_WRITE_TIMESTAMP);
  }

  static Long now() {
    return System.currentTimeMillis();
  }

  public void cancel() {
    this.cancel = true;
  }

  private void reput(Timeout timeout, Long tick) {
    if (timeout == null || tick == null) {
      throw new IllegalArgumentException();
    }

    if (cancel) {
      return;
    }

    Timer timer = timeout.timer();
    if (timer.isStop() || timeout.isCancelled()) {
      return;
    }

    timer.newTimeout(timeout.task(), tick, TimeUnit.MILLISECONDS);
  }

  @Override
  public void run(Timeout timeout) throws Exception {
    Collection<NetChannel> c = channelProvider.getChannels();
    for (NetChannel channel : c) {
      if (channel.isClosed()) {
        continue;
      }
      doTask(channel);
    }
    reput(timeout, tick);
  }

  protected abstract void doTask(NetChannel channel);

  /**
   * 用于查询获得需要心跳的通道数组
   */
  interface ChannelProvider {

    Collection<NetChannel> getChannels();
  }
}
