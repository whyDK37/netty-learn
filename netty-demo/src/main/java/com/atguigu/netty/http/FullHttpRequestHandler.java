package com.atguigu.netty.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import java.net.URI;
import java.util.List;

/*
说明
1. SimpleChannelInboundHandler 是 ChannelInboundHandlerAdapter
2. HttpObject 客户端和服务器端相互通讯的数据被封装成 HttpObject
 */
class FullHttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {


    //channelRead0 读取客户端数据
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {

        System.out.println("对应的channel=" + ctx.channel() + " pipeline=" + ctx
                .pipeline() + " 通过pipeline获取channel" + ctx.pipeline().channel());

        System.out.println("当前ctx的handler=" + ctx.handler());


        System.out.println("ctx 类型=" + ctx.getClass());
        System.out.println("request.toString() = " + request);
        ByteBuf content = request.content();
        byte[] dst = new byte[content.readableBytes()];
        content.readBytes(dst);
        System.out.println("content:" + new String(dst));

        System.out.println(
                "pipeline hashcode" + ctx.pipeline().hashCode() + " TestHttpServerHandler hash=" + this
                        .hashCode());

        System.out.println("msg 类型=" + request.getClass());
        System.out.println("客户端地址" + ctx.channel().remoteAddress());

        //获取到
        //获取uri, 过滤指定的资源
        URI uri = new URI(request.uri());
        if ("/favicon.ico".equals(uri.getPath())) {
            System.out.println("请求了 favicon.ico, 不做响应");
            return;
        }
        //回复信息给浏览器 [http协议]

        //构造一个http的相应，即 httpresponse
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK, Unpooled.copiedBuffer("hello, 我是服务器", CharsetUtil.UTF_8));

        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());

        //将构建好 response返回
        ctx.writeAndFlush(response);

    }
}
