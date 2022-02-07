package netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;
import java.nio.charset.StandardCharsets;

public class NettyClient {

  public static void main(String[] args) {
    EventLoopGroup group = new NioEventLoopGroup();
    try {
      Bootstrap bootstrap = new Bootstrap();

      bootstrap.group(group)
          .channel(NioSocketChannel.class)
          .option(ChannelOption.TCP_NODELAY, true)
          .handler(new ChannelInitializer<Channel>() {

            @Override
            protected void initChannel(Channel channel) throws Exception {
              channel.pipeline().addLast(new NettyClientHandler());
            }

          });

      ChannelFuture channelFuture = bootstrap.connect("127.0.0.1", 50070).sync();

      channelFuture.channel().closeFuture().sync();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      group.shutdownGracefully();
    }
  }


  public static class NettyClientHandler extends ChannelInboundHandlerAdapter {

    private final ByteBuf requestBuffer;

    public NettyClientHandler() {
      byte[] requestBytes = "hello, this is a message from client".getBytes();
      requestBuffer = Unpooled.buffer(requestBytes.length);
      requestBuffer.writeBytes(requestBytes);
    }


    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
      ctx.writeAndFlush(requestBuffer);
      super.handlerAdded(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
      ctx.writeAndFlush(Unpooled.copiedBuffer("hello, server", CharsetUtil.UTF_8));
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      ByteBuf responseBuffer = (ByteBuf) msg;
      byte[] responseBytes = new byte[responseBuffer.readableBytes()];
      responseBuffer.readBytes(responseBytes);

      String response = new String(responseBytes, StandardCharsets.UTF_8);
      System.out.println("receive message from server" + response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
      cause.printStackTrace();
      ctx.close();
    }

  }
}
