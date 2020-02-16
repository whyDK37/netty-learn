package dubbo.mini.netty4;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import dubbo.mini.common.NetURL;
import dubbo.mini.common.utils.NetUtils;
import dubbo.mini.remote.ChannelEventHandler;
import dubbo.mini.remote.ChannelHandlers;
import dubbo.mini.remote.Client;
import dubbo.mini.remote.NetChannel;
import dubbo.mini.remote.RemotingException;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyClient extends AbstractEndpoint implements NetChannel, Client {

  private static Logger logger = LoggerFactory.getLogger(NettyClient.class);

  private EventLoopGroup group;
  private Bootstrap bootstrap;

  private Channel channel;

  private volatile boolean closed = false;

  private volatile boolean closing = false;

  private final Lock connectLock = new ReentrantLock();

  public NettyClient(NetURL url, ChannelEventHandler channelEventHandler) {
    super(url, ChannelHandlers.wrap(channelEventHandler, url));
    connect();
  }

  public void connect() {
    final NettyClientHandler nettyClientHandler = new NettyClientHandler(getUrl(), this);
    this.group = new NioEventLoopGroup();
    this.bootstrap = new Bootstrap();
    bootstrap.group(group)
        .option(ChannelOption.SO_KEEPALIVE, true)
        .channel(NioSocketChannel.class)
        .handler(new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) throws Exception {
            NettyCodecAdapter adapter = new NettyCodecAdapter(getCodec(), getUrl(),
                NettyClient.this);

            ch.pipeline().addLast("encoder", adapter.getEncoder());
            ch.pipeline().addLast("decoder", adapter.getDecoder());
            ch.pipeline().addLast(new IdleStateHandler(0, 5, 0, TimeUnit.SECONDS));
            ch.pipeline().addLast(nettyClientHandler);
          }

          @Override
          public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            super.channelInactive(ctx);
          }
        });

    doConnect();
  }

  protected void doConnect() {
    long start = System.currentTimeMillis();
    connectLock.lock();
    try {
      if (isConnected()) {
        return;
      }
      ChannelFuture future = bootstrap.connect(getConnectAddress());
      try {
        boolean ret = future.awaitUninterruptibly(getConnectTimeout(), MILLISECONDS);
        if (ret && future.isSuccess()) {
          Channel newChannel = future.channel();

          try {
            // Close old channel
            Channel oldChannel = this.channel; // copy reference
            if (oldChannel != null) {
              try {
                if (logger.isInfoEnabled()) {
                  logger.info(
                      "Close old netty channel " + oldChannel + " on create new netty channel "
                          + newChannel);
                }
                oldChannel.close();
              } finally {
                // fixme 增加注冊表刪除邏輯
              }
            }
          } finally {
            if (this.isClosed()) {
              try {
                if (logger.isInfoEnabled()) {
                  logger.info(
                      "Close new netty channel " + newChannel + ", because the client closed.");
                }
                newChannel.close();
              } finally {
                this.channel = null;
                // 增加注冊表注冊邏輯
//                            NettyChannel.removeChannelIfDisconnected(newChannel);
              }
            } else {
              channel = newChannel;
            }
          }
        } else {
          logger.warn("reconnect after {} seconds.", 3);
          future.channel().eventLoop().schedule(() -> {
            try {
              doConnect();
            } catch (Throwable throwable) {
              throwable.printStackTrace();
            }
          }, 3, TimeUnit.SECONDS);
        }

      } finally {
        if (!isConnected()) {
          //
        }
      }
    } finally {
      connectLock.unlock();
    }
    logger.info("connect time:{}", (System.currentTimeMillis() - start));
  }


  @Override
  public boolean isConnected() {
    NetChannel channel = getChannel();
    if (channel == null) {
      return false;
    }
    return channel.isConnected();
  }

  public InetSocketAddress getConnectAddress() {
    return new InetSocketAddress(NetUtils.filterLocalHost(getUrl().getHost()), getUrl().getPort());
  }

  @Override
  public boolean isClosed() {
    return closed;
  }

  public boolean isClosing() {
    return closing && !closed;
  }

  protected NetChannel getChannel() {
    Channel c = channel;
    if (c == null || !c.isActive()) {
      return null;
    }
    return NettyChannel.getOrAddChannel(c, getUrl(), this);
  }

  @Override
  public InetSocketAddress getRemoteAddress() {
    return null;
  }

  @Override
  public boolean hasAttribute(String key) {
    return false;
  }

  @Override
  public Object getAttribute(String key) {
    return null;
  }

  @Override
  public void setAttribute(String key, Object value) {

  }

  @Override
  public void removeAttribute(String key) {

  }

  @Override
  public void reconnect() throws RemotingException {

  }

  @Override
  public boolean canHandleIdle() {
    return true;
  }
}
