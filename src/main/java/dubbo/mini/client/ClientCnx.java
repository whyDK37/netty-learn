package dubbo.mini.client;

import dubbo.mini.AbstractEndpoint;
import dubbo.mini.MessageInfo;
import dubbo.mini.support.DefaultFuture;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class ClientCnx extends AbstractEndpoint {

    private static Logger logger = LoggerFactory.getLogger(ClientCnx.class);

    private String ip;
    private int port;
    private EventLoopGroup group;
    private Bootstrap bootstrap;

    private Channel channel;

    private volatile boolean closed = false;

    private final Lock connectLock = new ReentrantLock();

    List<ConnectListener> connectListeners = new CopyOnWriteArrayList<>();

    public void addConnectListener(ConnectListener listener) {
        connectListeners.add(listener);
    }

    public ClientCnx(String ip, int port, int timeout, long idleTimeout, long connectTimeout) {
        super(timeout, idleTimeout, connectTimeout);
        this.ip = ip;
        this.port = port;
    }

    public void connect() {
        this.group = new NioEventLoopGroup();
        this.bootstrap = new Bootstrap();
        ClientCnx clientCnx = this;
        bootstrap.group(group)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast("encoder", new ProtobufEncoder());
                        ch.pipeline().addLast("decoder", new ProtobufDecoder(MessageInfo.Message.getDefaultInstance()));
                        ch.pipeline().addLast(new IdleStateHandler(0, 5, 0, TimeUnit.SECONDS));
                        ch.pipeline().addLast(new ClientHandler(clientCnx));
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
            ChannelFuture future = bootstrap.connect(ip, port);
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
                                    logger.info("Close old netty channel " + oldChannel + " on create new netty channel " + newChannel);
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
                                    logger.info("Close new netty channel " + newChannel + ", because the client closed.");
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
        if (isConnected()) {
            for (ConnectListener connectListener : this.connectListeners) {
                connectListener.connect(channel);
            }
        }
    }

    private boolean isClosed() {
        return closed;
    }


    public boolean isConnected() {
        Channel channel = getChannel();
        if (channel == null) {
            return false;
        }
        return channel.isOpen();
    }

    private Channel getChannel() {
        return this.channel;
    }

    public void closed() {
        this.closed = true;
        this.channel = null;
    }

    public <T> T send(MessageInfo.Message message) throws Throwable {
        if (!isConnected()) {
            doConnect();
        }

        Channel channel = getChannel();
        DefaultChannelPromise promise = new DefaultChannelPromise(channel);
        DefaultFuture defaultFuture = DefaultFuture.newFuture(channel, message);
        channel.writeAndFlush(message, promise);
        return defaultFuture.get(60 * 1000);
    }
}
