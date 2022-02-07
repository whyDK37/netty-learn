package com.atguigu.netty.proto;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class NettyUserEventHandler extends SimpleChannelInboundHandler<UserEvent> {

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, UserEvent msg) throws Exception {
    System.out.println("user event : " + msg);
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    System.out.println("user event : " + evt);
  }
}
