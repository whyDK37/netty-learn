package netty.heartbeat;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import java.util.concurrent.TimeUnit;

public class MeCilent {

  public static void main(String[] args) {
    start();
  }

  public static void start() {
    EventLoopGroup group = new NioEventLoopGroup();
    try {
      Bootstrap b = new Bootstrap();
      b.group(group)
          .option(ChannelOption.SO_KEEPALIVE, true)
          .channel(NioSocketChannel.class)
          .handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                            /*ch.pipeline().addLast(new StringEncoder());
                            ch.pipeline().addLast(new StringDecoder());*/
              ch.pipeline().addLast(new SmartCarEncoder());
              ch.pipeline().addLast(new SmartCarDecoder());
              ch.pipeline().addLast(new IdleStateHandler(0, 5, 0, TimeUnit.SECONDS));
              ch.pipeline().addLast(new ClientHandler());
            }
          });

      ChannelFuture f = b.connect("127.0.0.1", 7000);
      //断线重连
      f.addListener(new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture channelFuture) throws Exception {
          if (!channelFuture.isSuccess()) {
            final EventLoop loop = channelFuture.channel().eventLoop();
            loop.schedule(new Runnable() {
              @Override
              public void run() {
                System.out.println("连接不上服务端，开始重连操作...");
                start();
              }
            }, 3L, TimeUnit.SECONDS);
          } else {
            Channel channel = channelFuture.channel();
            System.out.println("连接到服务端成功...");
          }
        }
      });

    } catch (Exception e) {
      e.printStackTrace();
    }
  }


}
