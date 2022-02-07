package netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import java.nio.charset.StandardCharsets;

public class NettyServer {

  public static void main(String[] args) {
    EventLoopGroup parentGroup = new NioEventLoopGroup(); // thread group -> Acceptor thread
    EventLoopGroup childGroup = new NioEventLoopGroup(); // thread group -> Processor / Handler

    EventExecutorGroup eventExecutors = new DefaultEventLoopGroup(
        Runtime.getRuntime().availableProcessors(), new DefaultThreadFactory("worker"));
    try {
      ServerBootstrap serverBootstrap = new ServerBootstrap(); // Netty server

      serverBootstrap
          .group(parentGroup, childGroup)
          .channel(NioServerSocketChannel.class)  // ServerChannel , ServerSocketChannel
          .option(ChannelOption.SO_BACKLOG, 1024)
          .childHandler(new ChannelInitializer<SocketChannel>() { // 处理每个连接的SocketChannel

            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {

              socketChannel.pipeline()
                  .addLast(eventExecutors, new NettyServerHandler()); // business code
            }

          });

      ChannelFuture bindFuture = serverBootstrap.bind(50070).sync(); // waiting for server startup

      bindFuture.channel().closeFuture().sync(); // waiting for server shutdown
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      parentGroup.shutdownGracefully();
      childGroup.shutdownGracefully();
    }

    System.out.println("server start");
  }


  public static class NettyServerHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
      System.out.println("handlerAdded");
      super.handlerAdded(ctx);
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      ByteBuf requestBuffer = (ByteBuf) msg;
      byte[] requestBytes = new byte[requestBuffer.readableBytes()];
      requestBuffer.readBytes(requestBytes);

      String request = new String(requestBytes, StandardCharsets.UTF_8);
      System.out.println("message from client :" + request);

      String response = "this is a response from server";
      ByteBuf responseBuffer = Unpooled.copiedBuffer(response.getBytes());
      ctx.write(responseBuffer);

      // 这个东西类似对应着我们之前说的那个Processor线程，负责读取请求，返回响应
      // 具体底层的源码还没看，这个东西也可以理解为我们之前说的那个Handler线程
      // Netty底层就有类似Processor的东西，负责从网络连接中读取请求
      // 然后把读取出来的请求交给我们的Handler线程来处理，处理完以后把响应返回回去
      // 但是可能在底层响应是由Processor线程来发送回去的
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
      ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
      ctx.close();
    }

  }
}
