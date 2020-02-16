package com.atguigu.netty.http;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;
import java.net.URI;

public class TestHTTPServer {

  public static void main(String[] args) throws Exception {

    EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    EventLoopGroup workerGroup = new NioEventLoopGroup();

    try {
      ServerBootstrap serverBootstrap = new ServerBootstrap();

      serverBootstrap.group(bossGroup, workerGroup)
          .channel(NioServerSocketChannel.class)
//                    .handler(null)
          .childHandler(new TestServerInitializer())
      ;

      ChannelFuture channelFuture = serverBootstrap.bind(6668).sync();

      channelFuture.channel().closeFuture().sync();

    } finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }
}

class TestServerInitializer extends ChannelInitializer<SocketChannel> {

  @Override
  protected void initChannel(SocketChannel ch) throws Exception {

    //向管道加入处理器

    //得到管道
    ChannelPipeline pipeline = ch.pipeline();

    //加入一个netty 提供的httpServerCodec codec =>[coder - decoder]
    //HttpServerCodec 说明
    //1. HttpServerCodec 是netty 提供的处理http的 编-解码器
    pipeline.addLast("MyHttpServerCodec", new HttpServerCodec());
    //2. 增加一个自定义的handler
    pipeline.addLast("MyTestHttpServerHandler", new TestHttpServerHandler());

    System.out.println("ok~~~~");

  }
}


/*
说明
1. SimpleChannelInboundHandler 是 ChannelInboundHandlerAdapter
2. HttpObject 客户端和服务器端相互通讯的数据被封装成 HttpObject
 */
class TestHttpServerHandler extends SimpleChannelInboundHandler<HttpObject> {


  //channelRead0 读取客户端数据
  @Override
  protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {

    System.out.println("对应的channel=" + ctx.channel() + " pipeline=" + ctx
        .pipeline() + " 通过pipeline获取channel" + ctx.pipeline().channel());

    System.out.println("当前ctx的handler=" + ctx.handler());

    //判断 msg 是不是 httprequest请求
    if (msg instanceof HttpRequest) {

      System.out.println("ctx 类型=" + ctx.getClass());

      System.out.println(
          "pipeline hashcode" + ctx.pipeline().hashCode() + " TestHttpServerHandler hash=" + this
              .hashCode());

      System.out.println("msg 类型=" + msg.getClass());
      System.out.println("客户端地址" + ctx.channel().remoteAddress());

      //获取到
      HttpRequest httpRequest = (HttpRequest) msg;
      //获取uri, 过滤指定的资源
      URI uri = new URI(httpRequest.uri());
      if ("/favicon.ico".equals(uri.getPath())) {
        System.out.println("请求了 favicon.ico, 不做响应");
        return;
      }
      //回复信息给浏览器 [http协议]

      ByteBuf content = Unpooled.copiedBuffer("hello, 我是服务器", CharsetUtil.UTF_8);

      //构造一个http的相应，即 httpresponse
      FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
          HttpResponseStatus.OK, content);

      response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
      response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());

      //将构建好 response返回
      ctx.writeAndFlush(response);

    }
  }


}