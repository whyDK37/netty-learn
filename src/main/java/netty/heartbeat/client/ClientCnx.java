package netty.heartbeat.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import netty.heartbeat.AbstractEndpoint;
import netty.heartbeat.SmartCarDecoder;
import netty.heartbeat.SmartCarEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    // closing closed means the process is being closed and close is finished
    private volatile boolean closing;

    private volatile boolean closed;

    private final Lock connectLock = new ReentrantLock();

    public ClientCnx(String ip, int port, int timeout, long idleTimeout, long connectTimeout) {
        super(timeout, idleTimeout, connectTimeout);
        this.ip = ip;
        this.port = port;
    }

    public void connect() throws Throwable {
        this.group = new NioEventLoopGroup();
        this.bootstrap = new Bootstrap();
        bootstrap.group(group)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new SmartCarEncoder());
                        ch.pipeline().addLast(new SmartCarDecoder());
                        ch.pipeline().addLast(new IdleStateHandler(0, 5, 0, TimeUnit.SECONDS));
                        ch.pipeline().addLast(new ClientHandler());
                    }
                });

        doConnect();
    }

    protected void doConnect() throws Throwable {
        long start = System.currentTimeMillis();

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
            }

        } finally {
            if (!isConnected()) {

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

}
