package dubbo.mini.netty4;

import static dubbo.mini.common.Constants.DEFAULT_IO_THREADS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import dubbo.mini.common.Constants;
import dubbo.mini.common.NetURL;
import dubbo.mini.common.utils.NetUtils;
import dubbo.mini.common.utils.UrlUtils;
import dubbo.mini.remote.ChannelEventHandler;
import dubbo.mini.remote.NetChannel;
import dubbo.mini.remote.RemotingException;
import dubbo.mini.remote.Server;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 服务端
 */
public class NettyServer extends AbstractEndpoint implements Server {

  private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);

  private ServerBootstrap bootstrap;

  private Map<String, NetChannel> channels; // <ip:port, channel>

  private io.netty.channel.Channel channel;

  private EventLoopGroup bossGroup;
  private EventLoopGroup workerGroup;
  private InetSocketAddress bindAddress;
  private int accepts;
  private int idleTimeout;

  ExecutorService executor;

  public NettyServer(NetURL url, ChannelEventHandler handler) throws RemotingException {
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
        logger.info("Start " + getClass().getSimpleName() + " bind " + bindAddress + ", export "
            + getLocalAddress());
      }
    } catch (Throwable t) {
      throw new RemotingException(url.toInetSocketAddress(), null,
          "Failed to bind " + getClass().getSimpleName()
              + " on " + getLocalAddress() + ", cause: " + t.getMessage(), t);
    }
    //fixme replace this with better method
//        executor = (ExecutorService) dataStore.get(Constants.EXECUTOR_SERVICE_COMPONENT_KEY, Integer.toString(url.getPort()));
  }

  public void open() throws Throwable {
    bootstrap = new ServerBootstrap();
    bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("NettyServerBoss", true));
    workerGroup = new NioEventLoopGroup(DEFAULT_IO_THREADS,
        new DefaultThreadFactory("NettyServerWorker", true));
    NettyServerHandler nettyServerHandler = new NettyServerHandler(getUrl(), this);
    channels = nettyServerHandler.getChannels();

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
            NettyCodecAdapter adapter = new NettyCodecAdapter(getCodec(), getUrl(),
                NettyServer.this);
            int idleTimeout = UrlUtils.getIdleTimeout(getUrl());
            ch.pipeline()//.addLast("logging",new LoggingHandler(LogLevel.INFO))//for debug
                .addLast("decoder", adapter.getDecoder())
                .addLast("encoder", adapter.getEncoder())
                .addLast("server-idle-handler",
                    new IdleStateHandler(0, 0, idleTimeout, MILLISECONDS))
                .addLast("handler", nettyServerHandler);
          }
        });

    ChannelFuture channelFuture = bootstrap.bind(bindAddress);
//        channelFuture.syncUninterruptibly();
    channel = channelFuture.channel();
  }


  @Override
  public boolean isBound() {
    return false;
  }

  public Collection<NetChannel> getChannels() {
    Collection<NetChannel> chs = new HashSet<>();
    for (NetChannel channel : this.channels.values()) {
      if (channel.isConnected()) {
        chs.add(channel);
      } else {
        channels.remove(NetUtils.toAddressString(channel.getRemoteAddress()));
      }
    }
    return chs;
  }

  @Override
  public NetChannel getChannel(InetSocketAddress remoteAddress) {
    return null;
  }

  @Override
  public void reset(NetURL url) {
    if (url == null) {
      return;
    }
    try {
      if (url.hasParameter(Constants.ACCEPTS_KEY)) {
        int a = url.getParameter(Constants.ACCEPTS_KEY, 0);
        if (a > 0) {
          this.accepts = a;
        }
      }
    } catch (Throwable t) {
      logger.error(t.getMessage(), t);
    }
    try {
      if (url.hasParameter(Constants.IDLE_TIMEOUT_KEY)) {
        int t = url.getParameter(Constants.IDLE_TIMEOUT_KEY, 0);
        if (t > 0) {
          this.idleTimeout = t;
        }
      }
    } catch (Throwable t) {
      logger.error(t.getMessage(), t);
    }
    try {
      if (url.hasParameter(Constants.THREADS_KEY)
          && executor instanceof ThreadPoolExecutor && !executor.isShutdown()) {
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executor;
        int threads = url.getParameter(Constants.THREADS_KEY, 0);
        int max = threadPoolExecutor.getMaximumPoolSize();
        int core = threadPoolExecutor.getCorePoolSize();
        if (threads > 0 && (threads != max || threads != core)) {
          if (threads < core) {
            threadPoolExecutor.setCorePoolSize(threads);
            if (core == max) {
              threadPoolExecutor.setMaximumPoolSize(threads);
            }
          } else {
            threadPoolExecutor.setMaximumPoolSize(threads);
            if (core == max) {
              threadPoolExecutor.setCorePoolSize(threads);
            }
          }
        }
      }
    } catch (Throwable t) {
      logger.error(t.getMessage(), t);
    }
    super.setUrl(getUrl().addParameters(url.getParameters()));
  }


  @Override
  public boolean canHandleIdle() {
    return true;
  }
}
