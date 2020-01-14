package dubbo.mini.remote;

import dubbo.mini.ThreadPool.AbortPolicyWithReport;
import dubbo.mini.common.Constants;
import dubbo.mini.common.URL;
import dubbo.mini.support.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

public class WrappedChannelHandler implements ChannelHandlerDelegate {

    protected static final Logger logger = LoggerFactory.getLogger(WrappedChannelHandler.class);

    protected static final ExecutorService SHARED_EXECUTOR = Executors.newCachedThreadPool(new NamedThreadFactory("MIN-SharedHandler", true));

    protected final ExecutorService executor;

    protected final ChannelEventHandler handler;

    protected final URL url;

    public WrappedChannelHandler(ChannelEventHandler handler, URL url) {
        this.handler = handler;
        this.url = url;
        executor = getExecutor(url);
    }

    private ExecutorService getExecutor(URL url) {
        String name = url.getParameter(Constants.THREAD_NAME_KEY, Constants.DEFAULT_THREAD_NAME);
        int threads = url.getParameter(Constants.THREADS_KEY, Constants.DEFAULT_THREADS);
        int queues = url.getParameter(Constants.QUEUES_KEY, Constants.DEFAULT_QUEUES);
        return new ThreadPoolExecutor(threads, threads, 0, TimeUnit.MILLISECONDS,
                queues == 0 ? new SynchronousQueue<Runnable>() :
                        (queues < 0 ? new LinkedBlockingQueue<Runnable>()
                                : new LinkedBlockingQueue<Runnable>(queues)),
                new NamedThreadFactory(name, true), new AbortPolicyWithReport(name, url));
    }

    public void close() {
        try {
            if (executor != null) {
                executor.shutdown();
            }
        } catch (Throwable t) {
            logger.warn("fail to destroy thread pool of server: " + t.getMessage(), t);
        }
    }

    @Override
    public void connected(NetChannel channel) throws RemotingException {
        handler.connected(channel);
    }

    @Override
    public void disconnected(NetChannel channel) throws RemotingException {
        handler.disconnected(channel);
    }

    @Override
    public void sent(NetChannel channel, Object message) throws RemotingException {
        handler.sent(channel, message);
    }

    @Override
    public void received(NetChannel channel, Object message) throws RemotingException {
        handler.received(channel, message);
    }

    @Override
    public void caught(NetChannel channel, Throwable exception) throws RemotingException {
        handler.caught(channel, exception);
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    @Override
    public ChannelEventHandler getHandler() {
        if (handler instanceof ChannelHandlerDelegate) {
            return ((ChannelHandlerDelegate) handler).getHandler();
        } else {
            return handler;
        }
    }

    public URL getUrl() {
        return url;
    }

    public ExecutorService getExecutorService() {
        ExecutorService cexecutor = executor;
        if (cexecutor == null || cexecutor.isShutdown()) {
            cexecutor = SHARED_EXECUTOR;
        }
        return cexecutor;
    }

}
