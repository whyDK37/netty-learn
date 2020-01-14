package dubbo.mini.netty4;

import dubbo.mini.MessageInfo;
import dubbo.mini.common.Constants;
import dubbo.mini.common.URL;
import dubbo.mini.common.utils.NetUtils;
import dubbo.mini.common.utils.UrlUtils;
import dubbo.mini.remote.ChannelEventHandler;
import dubbo.mini.remote.RemotingException;
import dubbo.mini.server.SessionManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static netty.heartbeat.ConstantValue.DEFAULT_IO_THREADS;

/**
 * 服务端
 */
public class NettyServer extends AbstractEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);

    private ServerBootstrap bootstrap;

    private io.netty.channel.Channel channel;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private InetSocketAddress bindAddress;
    private int accepts;
    private int idleTimeout;

    ExecutorService executor;

    public NettyServer(URL url, ChannelEventHandler handler) throws RemotingException {
        super(url, handler);
        String bindIp = getUrl().getParameter(Constants.BIND_IP_KEY, getUrl().getHost());
        int bindPort = getUrl().getParameter(Constants.BIND_PORT_KEY, getUrl().getPort());
        if (url.getParameter(Constants.ANYHOST_KEY, false) || NetUtils.isInvalidLocalHost(bindIp)) {
            bindIp = Constants.ANYHOST_VALUE;
        }
        bindAddress = new InetSocketAddress(bindIp, bindPort);
        this.accepts = url.getParameter(Constants.ACCEPTS_KEY, Constants.DEFAULT_ACCEPTS);
        this.idleTimeout = url.getParameter(Constants.IDLE_TIMEOUT_KEY, Constants.DEFAULT_IDLE_TIMEOUT);
        try {
            open();
            if (logger.isInfoEnabled()) {
                logger.info("Start " + getClass().getSimpleName() + " bind " + bindAddress + ", export " + getLocalAddress());
            }
        } catch (Throwable t) {
            throw new RemotingException(url.toInetSocketAddress(), null, "Failed to bind " + getClass().getSimpleName()
                    + " on " + getLocalAddress() + ", cause: " + t.getMessage(), t);
        }
        //fixme replace this with better method
//        executor = (ExecutorService) dataStore.get(Constants.EXECUTOR_SERVICE_COMPONENT_KEY, Integer.toString(url.getPort()));
    }

    public void open() throws Throwable {
        bootstrap = new ServerBootstrap();
        bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("NettyServerBoss", true));
        workerGroup = new NioEventLoopGroup(DEFAULT_IO_THREADS, new DefaultThreadFactory("NettyServerWorker", true));

        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE)
                .childOption(ChannelOption.SO_REUSEADDR, Boolean.TRUE)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        // FIXME: should we use getTimeout()?
//                        int idleTimeout = UrlUtils.getIdleTimeout(getUrl());
//                        NettyCodecAdapter adapter = new NettyCodecAdapter(getCodec(), getUrl(), NettyServer.this);
                        int idleTimeout = UrlUtils.getIdleTimeout(getUrl());
                        ch.pipeline()//.addLast("logging",new LoggingHandler(LogLevel.INFO))//for debug
//                                .addLast("decoder", adapter.getDecoder())
//                                .addLast("encoder", adapter.getEncoder())
                                .addLast("encoder", new ProtobufEncoder())
                                .addLast("decoder", new ProtobufDecoder(MessageInfo.Message.getDefaultInstance()))
                                .addLast("server-idle-handler", new IdleStateHandler(0, 0, idleTimeout, MILLISECONDS))
                                .addLast("handler", new NettyServerHandler());
                    }
                });

        ChannelFuture channelFuture = bootstrap.bind(bindAddress);
//        channelFuture.syncUninterruptibly();
        channel = channelFuture.channel();
    }


    public Collection<Channel> getChannels() {
        return SessionManager.getInstance().getChannels();
    }


//    public boolean isBound() {
//        return channel.isActive();
//    }
}
