package netty.heartbeat;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoop;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;
import java.util.concurrent.TimeUnit;

public class ClientHandler extends ChannelInboundHandlerAdapter {

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    System.out.println("客户端与服务端建立连接");
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    System.out.println("掉线了...");
    //使用过程中断线重连
    final EventLoop eventLoop = ctx.channel().eventLoop();
    eventLoop.schedule(() -> {
      try {
        MeCilent.start();
      } catch (Exception e) {

      }
    }, 1L, TimeUnit.SECONDS);
    super.channelInactive(ctx);
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    // 用于获取客户端发来的数据信息
    SmartCarProtocol body = (SmartCarProtocol) msg;
    System.out.println("Client接受的客户端的信息 :" + body.toString());
    String result = new String(body.getContent(), CharsetUtil.UTF_8);
    if (result.equals("PONG")) {
      System.out.println("receive form server PONG");
    }
    System.out.println("获取到的信息为：msg：" + msg);
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    if (evt instanceof IdleStateEvent) {
      IdleState state = ((IdleStateEvent) evt).state();
      if (state == IdleState.WRITER_IDLE) {
        // 要发送的信息
        String data = "PING";
        // 获得要发送信息的字节数组
        byte[] content = data.getBytes();
        // 要发送信息的长度
        int contentLength = content.length;
        SmartCarProtocol protocol = new SmartCarProtocol(contentLength, content);
        ctx.writeAndFlush(protocol);
        //ctx.writeAndFlush("PING");

        System.out.println("send PING");
      }
    }
    super.userEventTriggered(ctx, evt);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    cause.printStackTrace();
    ctx.close();
  }
}
