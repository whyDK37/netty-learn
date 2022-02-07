package netty.heartbeat;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;
import java.util.concurrent.ConcurrentHashMap;

public class MeServerHandler extends ChannelInboundHandlerAdapter {

  //  private static ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
  private static final ConcurrentHashMap map = new ConcurrentHashMap();

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    Channel channel = ctx.channel();
    //    channelGroup.add(channel);//将多个客户端的channle 添加channelGroup 组里
  }

  /**
   * 建立连接的时候
   *
   * @param ctx
   * @throws Exception
   */
  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    System.out.println("客户端连接成功,channel:" + ctx.channel().hashCode());
  }

  /**
   * 断开连接的时候
   *
   * @param ctx
   * @throws Exception
   */
  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    System.out.println("客户断开连接,channel:" + ctx.channel().hashCode());
  }

  /**
   * 读取数据的时候
   *
   * @param ctx
   * @throws Exception
   */
  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    // 用于获取客户端发来的数据信息
    SmartCarProtocol body = (SmartCarProtocol) msg;
    System.out.println("Server接受的客户端的信息 :" + body.toString());
    String result = new String(body.getContent(), CharsetUtil.UTF_8);
    if (msg != null && result.equals("PING")) {
      Channel channel = ctx.channel();
      String str = "PONG";
      SmartCarProtocol response = new SmartCarProtocol(str.getBytes().length,
          str.getBytes());
      System.out.println("客户端" + ctx.channel().remoteAddress() + "第 个PING");
      //ctx.writeAndFlush("PONG");
      ctx.writeAndFlush(response);
    }
       /* ByteBuf buf = (ByteBuf) msg;
        System.out.println("收到客户端的信息："+buf.toString(CharsetUtil.UTF_8)+",channel:"+ctx.channel().hashCode());*/
  }

  /**
   * 心跳检测
   *
   * @param ctx
   * @throws Exception
   */
  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    if (evt instanceof IdleStateEvent) {
      IdleStateEvent event = (IdleStateEvent) evt;
      if (event.state().equals(IdleState.READER_IDLE)) {
        // 空闲10s 没有收到客户端的发送数据，认为客户端断开连接
        ctx.channel().close().sync();
        System.out.println(
            "已与客户端" + ctx.channel().remoteAddress() + "断开连接，channel:" + ctx.channel().hashCode());
        /**
         * 此地可发送告警邮件或者短信去处理
         */
      }

    }
  }

  /**
   * 异常的时候，关闭此channel
   *
   * @param ctx
   * @param cause
   * @throws Exception
   */
  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    cause.printStackTrace();
    ctx.channel().close().sync();
  }
}
