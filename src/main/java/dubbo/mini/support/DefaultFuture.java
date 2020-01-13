package dubbo.mini.support;

import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dubbo.mini.MessageInfo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DefaultFuture implements ResponseFuture {

    private static final Logger logger = LoggerFactory.getLogger(DefaultFuture.class);

    /**
     * Future 集合
     * <p>
     * key：请求编号
     */
    private static final Map<Long, DefaultFuture> FUTURES = new ConcurrentHashMap<>();

    // invoke id.
    /**
     * 请求编号
     */
    private final long id;
    private final Channel channel;

    private final Lock lock = new ReentrantLock();
    /**
     * 请求是否完成条件
     */
    private final Condition done = lock.newCondition();
    /**
     * 创建开始时间
     */
    private final long start = System.currentTimeMillis();
    /**
     * 发送请求时间
     */
    private volatile long sent;

    private MessageInfo.Message request;
    private Object response;

    public static AtomicLong REQUEST_ID = new AtomicLong();

    private DefaultFuture(Channel channel, MessageInfo.Message request) {
        this.channel = channel;
        this.request = request;
        this.id = request.getId();
        // put into waiting map.
        FUTURES.put(id, this);
    }

    /**
     * init a DefaultFuture
     * 1.init a DefaultFuture
     * 2.timeout check
     *
     * @param channel channel
     * @param request the request
     * @return a new DefaultFuture
     */
    public static DefaultFuture newFuture(Channel channel, MessageInfo.Message request) {
        return new DefaultFuture(channel, request);
    }

    public static DefaultFuture getFuture(long id) {
        return FUTURES.get(id);
    }

    public static void sent(Channel channel, MessageInfo.Message request) {
        DefaultFuture future = FUTURES.get(request.getId());
        if (future != null) {
            future.doSent();
        }
    }


    public static void received(Channel channel, long id, Object response) {
        DefaultFuture future = FUTURES.remove(id);
        if (future != null) {
            future.doReceived(response);
        } else {
            logger.warn("The timeout response finally returned at "
                    + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()))
                    + ", response " + response
                    + (channel == null ? "" : ", channel: " + channel.remoteAddress()));
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

    private Channel getChannel() {
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
}