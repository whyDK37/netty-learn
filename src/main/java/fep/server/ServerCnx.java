package fep.server;

import fep.MessageInfo;
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

import java.util.Collection;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static netty.heartbeat.ConstantValue.DEFAULT_IO_THREADS;

/**
 * 服務端
 */
public class ServerCnx extends AbstractServer {

    private static final Logger logger = LoggerFactory.getLogger(ServerCnx.class);


    private ServerBootstrap bootstrap;

    private io.netty.channel.Channel channel;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public ServerCnx(String bindIp, int bindPort, int timeout, long allIdleTime, long connectTimeout) {
        super(bindIp, bindPort, timeout, allIdleTime, connectTimeout);
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
                        ch.pipeline()//.addLast("logging",new LoggingHandler(LogLevel.INFO))//for debug
//                                .addLast("decoder", adapter.getDecoder())
//                                .addLast("encoder", adapter.getEncoder())
                                .addLast("encoder", new ProtobufEncoder())
                                .addLast("decoder", new ProtobufDecoder(MessageInfo.Message.getDefaultInstance()))
                                .addLast("server-idle-handler", new IdleStateHandler(0, 0, getIdleTimeout(), MILLISECONDS))
                                .addLast("handler", new ServerHandler());
                    }
                });

        ChannelFuture channelFuture = bootstrap.bind(getBindAddress());
//        channelFuture.syncUninterruptibly();
        channel = channelFuture.channel();
    }


    protected void close() throws Throwable {
        try {
            if (channel != null) {
                // unbind.
                channel.close();
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
        try {
            Collection<Channel> channels = getChannels();
            if (channels != null && channels.size() > 0) {
                for (Channel channel : channels) {
                    try {
                        channel.close();
                    } catch (Throwable e) {
                        logger.warn(e.getMessage(), e);
                    }
                }
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
        try {
            if (bootstrap != null) {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
        try {
            SessionManager.getInstance().clear();
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
    }

    public Collection<Channel> getChannels() {
        return SessionManager.getInstance().getChannels();
//        Collection<Channel> chs = new HashSet<Channel>();
//        for (Channel channel : SessionManager.getInstance().getChannels()) {
//            if (channel.isActive()) {
//                chs.add(channel);
//            } else {
//                channels.remove(NetUtils.toAddressString((InetSocketAddress) channel.remoteAddress()));
//            }
//        }
//        return chs;
    }
//
//    public Channel getChannel(InetSocketAddress remoteAddress) {
//        return channels.get(NetUtils.toAddressString(remoteAddress));
//    }

    public boolean isBound() {
        return channel.isActive();
    }

}
