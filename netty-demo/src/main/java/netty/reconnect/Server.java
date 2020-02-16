package netty.reconnect;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

public class Server {

  public static void main(String[] args) {
    int port = 6666;//端口
    ServerBootstrap serverBootstrap = new ServerBootstrap();//服务端入口
    serverBootstrap.group(new NioEventLoopGroup(), new NioEventLoopGroup());//两个线程池
    serverBootstrap.channel(NioServerSocketChannel.class);//Channel的类型
    serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
      @Override
      protected void initChannel(SocketChannel socketChannel) throws Exception {
        socketChannel.pipeline().addLast(new StringDecoder());//解码方式
        socketChannel.pipeline().addLast(new StringEncoder());//编码方式
        socketChannel.pipeline().addLast(new NettyServerHandler());//监听
      }
    });

    serverBootstrap.bind(port);//开服务

  }

}


class NettyServerHandler extends SimpleChannelInboundHandler<String> {

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
    System.out.println("有消息了,内容是:" + msg);
    ctx.writeAndFlush("来自服务端的消息");//有消息了回一个
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

  }

}