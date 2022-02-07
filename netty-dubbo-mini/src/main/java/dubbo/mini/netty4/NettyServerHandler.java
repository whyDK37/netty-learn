package dubbo.mini.netty4;

import dubbo.mini.common.NetURL;
import dubbo.mini.common.utils.NetUtils;
import dubbo.mini.remote.ChannelEventHandler;
import dubbo.mini.remote.NetChannel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.timeout.IdleStateEvent;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author why
 */
public class NettyServerHandler extends ChannelDuplexHandler {

  private static Logger logger = LoggerFactory.getLogger(NettyServerHandler.class);
  private final Map<String, NetChannel> channels = new ConcurrentHashMap<>(); // <ip:port, channel>

  private final NetURL url;

  private final ChannelEventHandler handler;

  public NettyServerHandler(NetURL url, ChannelEventHandler handler) {
    if (url == null) {
      throw new IllegalArgumentException("url == null");
    }
    if (handler == null) {
      throw new IllegalArgumentException("handler == null");
    }
    this.url = url;
    this.handler = handler;
  }

  public Map<String, NetChannel> getChannels() {
    return channels;
  }


  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    NettyChannel channel = NettyChannel.getOrAddChannel(ctx.channel(), url, handler);
    try {
      if (channel != null) {
        channels.put(NetUtils.toAddressString((InetSocketAddress) ctx.channel().remoteAddress()),
            channel);
      }
      handler.connected(channel);
    } finally {
      NettyChannel.removeChannelIfDisconnected(ctx.channel());
    }
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    NettyChannel channel = NettyChannel.getOrAddChannel(ctx.channel(), url, handler);
    try {
      channels.remove(NetUtils.toAddressString((InetSocketAddress) ctx.channel().remoteAddress()));
      handler.disconnected(channel);
    } finally {
      NettyChannel.removeChannelIfDisconnected(ctx.channel());
    }
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    NettyChannel channel = NettyChannel.getOrAddChannel(ctx.channel(), url, handler);
    try {
      handler.received(channel, msg);
    } finally {
      NettyChannel.removeChannelIfDisconnected(ctx.channel());
    }
  }


  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
      throws Exception {
    super.write(ctx, msg, promise);
    NettyChannel channel = NettyChannel.getOrAddChannel(ctx.channel(), url, handler);
    try {
      handler.sent(channel, msg);
    } finally {
      NettyChannel.removeChannelIfDisconnected(ctx.channel());
    }
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    if (evt instanceof IdleStateEvent) {
      NettyChannel channel = NettyChannel.getOrAddChannel(ctx.channel(), url, handler);
      try {
        logger.info("IdleStateEvent triggered, close channel " + channel);
        channel.close();
      } finally {
        NettyChannel.removeChannelIfDisconnected(ctx.channel());
      }
    }
    super.userEventTriggered(ctx, evt);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
      throws Exception {
    NettyChannel channel = NettyChannel.getOrAddChannel(ctx.channel(), url, handler);
    try {
      handler.caught(channel, cause);
    } finally {
      NettyChannel.removeChannelIfDisconnected(ctx.channel());
    }
  }
}
