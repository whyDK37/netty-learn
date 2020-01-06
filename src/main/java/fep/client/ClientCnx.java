package fep.client;

import fep.AbstractEndpoint;
import fep.MessageInfo;
import fep.support.DefaultFuture;
import fep.support.RemotingException;
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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
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

    // closing closed means the process is being closed and close is finished
    private volatile boolean closing;

    private volatile boolean closed;

    AtomicLong requestId = new AtomicLong();


    private final Lock connectLock = new ReentrantLock();

    public ClientCnx(String ip, int port, int timeout, long idleTimeout, long connectTimeout) {
        super(timeout, idleTimeout, connectTimeout);
        this.ip = ip;
        this.port = port;
    }

    public void connect() throws Throwable {
        this.group = new NioEventLoopGroup();
        this.bootstrap = new Bootstrap();
        ClientCnx clientCnx = this;
        bootstrap.group(group)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
//                        ch.pipeline().addLast(new SmartCarEncoder());
//                        ch.pipeline().addLast(new SmartCarDecoder());

                        ch.pipeline().addLast("encoder", new ProtobufEncoder());
                        ch.pipeline().addLast("decoder", new ProtobufDecoder(MessageInfo.Message.getDefaultInstance()));
                        ch.pipeline().addLast(new IdleStateHandler(0, 5, 0, TimeUnit.SECONDS));
                        ch.pipeline().addLast(new ClientHandler(clientCnx));
                    }
                });

        doConnect();
    }

    protected void doConnect() throws Throwable {
        long start = System.currentTimeMillis();

        ChannelFuture future = bootstrap.connect(ip, port);
        future.addListener((ChannelFuture futureListener) -> {
            final EventLoop eventLoop = futureListener.channel().eventLoop();
            if (!futureListener.isSuccess()) {
                logger.warn("客户端与服务端建立连接失败，10s之后尝试重连...");
                // 10s秒之后重连
                eventLoop.schedule(() -> {
                    while (!isConnected()) {
                        try {
                            doConnect();
                        } catch (Throwable throwable) {
                            logger.warn("", throwable);
                        }
                    }
                }, 10, TimeUnit.SECONDS);
            }
//            else {
//                if (retryConnectFlag) {
//                    // 如果连接成功后，再次断开，则尝试重连。
//                    retryConnectFlag = false;
//                    log.info("客户端重连成功，port：{}", appConfig.getPort());
//                } else {
//                    // 客户端首次成功连接服务端后，这里有个验证登录服务端的动作（不要验证可以取消）
//                    log.info("客户端与服务器连接成功，port：{}，开始登录服务端...", appConfig.getPort());
//                }
//            }
        });
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
            }

        } finally {
            if (!isConnected()) {

            }
        }
        logger.info("connect time:{}", (System.currentTimeMillis() - start));
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

    public MessageInfo.Authentication authentication(String orgCode) throws RemotingException {
        logger.info("发送认证信息:{}", orgCode);

        MessageInfo.Message message = MessageInfo.Message.newBuilder()
                .setDataType(MessageInfo.Message.DataType.AUTHENTICATION)
                .setId(requestId.getAndIncrement())
                .setAuthentication(MessageInfo.Authentication.newBuilder()
                        .setOrgCode(orgCode).build())
                .build();
        DefaultChannelPromise promise = new DefaultChannelPromise(channel);
        DefaultFuture defaultFuture = DefaultFuture.newFuture(channel, message);
        channel.writeAndFlush(message, promise);
        return defaultFuture.get(60 * 1000);
    }

    public long getRequestId() {
        return requestId.incrementAndGet();
    }
}
