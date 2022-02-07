package netty.reconnect;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import java.util.concurrent.TimeUnit;

public class Client {

  private static Client client;
  int port = 6666;//端口
  private final Bootstrap bootstrap;
  private ChannelFuture channelFuture;

  private Client() {
    bootstrap = new Bootstrap();//客户端入口
    bootstrap.group(new NioEventLoopGroup());//开个线程
    bootstrap.channel(NioSocketChannel.class);//Channel的类型
    bootstrap.remoteAddress("127.0.0.1", port);//往哪连
    bootstrap.handler(new ChannelInitializer<SocketChannel>() {
      @Override
      protected void initChannel(SocketChannel socketChannel) throws Exception {
        socketChannel.pipeline().addLast(new StringDecoder());//解码方式
        socketChannel.pipeline().addLast(new StringEncoder());//编码方式
        socketChannel.pipeline().addLast(new MyClientHandler());
      }
    });
  }

  public static void main(String[] args) {
    Client client = Client.instance();
    client.doConnect();

    while (true) {
      try {
        TimeUnit.SECONDS.sleep(3);
        if (client.getChannelFuture().isSuccess()) {
          client.getChannelFuture().channel().writeAndFlush("来自客户端的消息");
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

  }

  public static Client instance() {
    if (client == null) {
      client = new Client();
    }
    return client;
  }


  public void doConnect() {
    this.channelFuture = bootstrap.connect().addListener((ChannelFutureListener) channelFuture ->
    {
      if (channelFuture.isSuccess()) {
        System.out.println("connect server  success-----");
      } else {
        System.out.println("connect server  fail---------3秒后尝试重新连接");
        channelFuture.channel().eventLoop().schedule(() -> doConnect(), 3, TimeUnit.SECONDS);
      }
    });
  }


  private ChannelFuture getChannelFuture() {
    return channelFuture;
  }
}


class MyClientHandler extends SimpleChannelInboundHandler<String> {

  protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
    System.out.println("有新消息了,内容是:" + msg);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    System.out.println("与服务器断开了,发起重连...");
    ctx.channel().close();
    Client.instance().doConnect();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
  }
}