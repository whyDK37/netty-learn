package dubbo.mini.netty4;

import dubbo.mini.common.NetURL;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
@io.netty.channel.ChannelHandler.Sharable
public class NettyClientHandler extends ChannelDuplexHandler {

  private static Logger logger = LoggerFactory.getLogger(NettyClientHandler.class);
  private NettyClient clientCnx;
  private NetURL url;

  NettyClientHandler(NetURL url, NettyClient clientCnx) {
    this.url = url;
    this.clientCnx = clientCnx;
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    logger.info("客户端与服务端建立连接");
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

    super.userEventTriggered(ctx, evt);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    logger.warn("channelInactive:{}", ctx);
    reConnect(ctx);
    super.channelInactive(ctx);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    logger.warn("exceptionCaught", cause);
    reConnect(ctx);
  }

  private void reConnect(ChannelHandlerContext ctx) {
    ctx.close();
    try {
      clientCnx.doConnect();
    } catch (Throwable throwable) {
      throwable.printStackTrace();
    }
  }
}
