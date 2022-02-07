package dubbo.mini.support;

import dubbo.mini.common.Constants;
import dubbo.mini.common.timer.HashedWheelTimer;
import dubbo.mini.common.timer.Timeout;
import dubbo.mini.common.timer.Timer;
import dubbo.mini.common.timer.TimerTask;
import dubbo.mini.exchange.Request;
import dubbo.mini.exchange.Response;
import dubbo.mini.remote.NetChannel;
import dubbo.mini.remote.RemotingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultFuture implements ResponseFuture {

  private static final Logger logger = LoggerFactory.getLogger(DefaultFuture.class);

  private static final Map<Long, DefaultFuture> FUTURES = new ConcurrentHashMap<>();
  private static final Map<Long, NetChannel> CHANNELS = new ConcurrentHashMap<>();

  private final long id;
  private final NetChannel channel;

  private final Lock lock = new ReentrantLock();

  private final Condition done = lock.newCondition();

  private final long start = System.currentTimeMillis();
  private final int timeout;

  private volatile long sent;

  private Request request;
  private Object response;


  public static final Timer TIME_OUT_TIMER = new HashedWheelTimer(
      new NamedThreadFactory("dubbo-future-timeout", true),
      30,
      TimeUnit.MILLISECONDS);


  private DefaultFuture(NetChannel channel, Request request, int timeout) {
    this.channel = channel;
    this.request = request;
    this.id = request.getId();
    this.timeout = timeout > 0 ? timeout
        : channel.getUrl().getPositiveParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
    // put into waiting map.
    FUTURES.put(id, this);
    CHANNELS.put(id, channel);
  }

  public static DefaultFuture newFuture(NetChannel channel, Request request, int timeout) {
    final DefaultFuture future = new DefaultFuture(channel, request, timeout);
    // timeout check
    timeoutCheck(future);
    return future;
  }

  private static void timeoutCheck(DefaultFuture future) {
    TimeoutCheckTask task = new TimeoutCheckTask(future);
    TIME_OUT_TIMER.newTimeout(task, future.getTimeout(), TimeUnit.MILLISECONDS);
  }

  public static void closeChannel(NetChannel channel) {
    for (Map.Entry<Long, NetChannel> entry : CHANNELS.entrySet()) {
      if (channel.equals(entry.getValue())) {
        DefaultFuture future = getFuture(entry.getKey());
        if (future != null && !future.isDone()) {
          Response disconnectResponse = new Response(future.getId());
          disconnectResponse.setStatus(Response.CHANNEL_INACTIVE);
          disconnectResponse.setErrorMessage("Channel " +
              channel +
              " is inactive. Directly return the unFinished request : " +
              future.getRequest());
          DefaultFuture.received(channel, disconnectResponse);
        }
      }
    }
  }

  public static boolean hasFuture(NetChannel channel) {
    return CHANNELS.containsValue(channel);
  }

  public Request getRequest() {
    return request;
  }

  private long getTimeout() {
    return timeout;
  }

  public static DefaultFuture getFuture(long id) {
    return FUTURES.get(id);
  }

  public static void sent(NetChannel channel, Request request) {
    DefaultFuture future = FUTURES.get(request.getId());
    if (future != null) {
      future.doSent();
    }
  }


  public static void received(NetChannel channel, Response response) {
    try {
      DefaultFuture future = FUTURES.remove(response.getId());
      if (future != null) {
        future.doReceived(response);
      } else {
        logger.warn("The timeout response finally returned at "
            + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()))
            + ", response " + response
            + (channel == null ? "" : ", channel: " + channel.getLocalAddress()
            + " -> " + channel.getRemoteAddress()));
      }
    } finally {
      CHANNELS.remove(response.getId());
    }
  }

  public static void received(NetChannel channel, long id, Object response) {
    DefaultFuture future = FUTURES.remove(id);
    if (future != null) {
      future.doReceived(response);
    } else {
      logger.warn("The timeout response finally returned at "
          + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()))
          + ", response " + response
          + (channel == null ? "" : ", channel: " + channel.getRemoteAddress()));
    }
  }

  @Override
  public <T> T get() throws RemotingException {
    return get(Constants.DEFAULT_TIMEOUT);
  }

  @Override
  public <T> T get(int timeout) throws RemotingException {
    if (timeout <= 0) {
      timeout = Constants.DEFAULT_TIMEOUT;
    }
    // 若未完成，等待
    // 判断是否完成。若未完成，基于 Lock + Condition 的方式，实现等待。
    // 而等待的唤醒，通过 ChannelHandler#received(channel, message) 方法，
    // 接收到请求时执行 DefaultFuture#received(channel, response) 方法。
    if (!isDone()) {
      // 注意，此处使用的不是 start 属性
      long start = System.currentTimeMillis();
      lock.lock();
      try {
        // 等待完成或超时
        while (!isDone()) {
          done.await(timeout, TimeUnit.MILLISECONDS);
          if (isDone() || System.currentTimeMillis() - start > timeout) {
            break;
          }
        }
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      } finally {
        lock.unlock();
      }
    }
    return (T) response;
  }

  public void cancel() {
    this.response = null;
    FUTURES.remove(id);
  }

  @Override
  public boolean isDone() {
    return this.response != null;
  }

  private long getId() {
    return id;
  }

  private NetChannel getChannel() {
    return channel;
  }

  private boolean isSent() {
    return sent > 0;
  }


  private long getStartTimestamp() {
    return start;
  }

  private void doSent() {
    sent = System.currentTimeMillis();
  }

  private void doReceived(Object res) {
    // 获得锁
    lock.lock();
    try {
      // 设置响应
      this.response = res;
      // 调用 Condition#signal() 方法，通知，唤醒 DefaultFuture#get(..) 方法的等待。
      done.signalAll();
    } finally {
      // 释放锁。
      lock.unlock();
    }
  }


  private static class TimeoutCheckTask implements TimerTask {

    private DefaultFuture future;

    TimeoutCheckTask(DefaultFuture future) {
      this.future = future;
    }

    @Override
    public void run(Timeout timeout) {
      if (future == null || future.isDone()) {
        return;
      }
      // create exception response.
      Response timeoutResponse = new Response(future.getId());
      // set timeout status.
      timeoutResponse
          .setStatus(future.isSent() ? Response.SERVER_TIMEOUT : Response.CLIENT_TIMEOUT);
      timeoutResponse.setErrorMessage(future.getTimeoutMessage(true));
      // handle response.
      DefaultFuture.received(future.getChannel(), timeoutResponse);

    }
  }

  private String getTimeoutMessage(boolean scan) {
    long nowTimestamp = System.currentTimeMillis();
    return (sent > 0 ? "Waiting server-side response timeout"
        : "Sending request timeout in client-side")
        + (scan ? " by scan timer" : "") + ". start time: "
        + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date(start))) + ", end time: "
        + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date())) + ","
        + (sent > 0 ? " client elapsed: " + (sent - start)
        + " ms, server elapsed: " + (nowTimestamp - sent)
        : " elapsed: " + (nowTimestamp - start)) + " ms, timeout: "
        + timeout + " ms, request: " + request + ", channel: " + channel.getLocalAddress()
        + " -> " + channel.getRemoteAddress();
  }
}