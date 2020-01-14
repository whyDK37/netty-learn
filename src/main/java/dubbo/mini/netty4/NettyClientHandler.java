package dubbo.mini.netty4;

import dubbo.mini.MessageInfo;
import dubbo.mini.common.URL;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class NettyClientHandler extends ChannelDuplexHandler {

    private static Logger logger = LoggerFactory.getLogger(NettyClientHandler.class);
    private NettyClient clientCnx;
    private URL url;

    NettyClientHandler(URL url, NettyClient clientCnx) {
        this.url = url;
        this.clientCnx = clientCnx;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("客户端与服务端建立连接");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 用于获取客户端发来的数据信息
        MessageInfo.Message message = (MessageInfo.Message) msg;
        if (message.getDataType() == MessageInfo.Message.DataType.PING) {
            logger.info("ping 消息");
            long id = message.getId();
            MessageInfo.Message ping = MessageInfo.Message.newBuilder().setDataType(MessageInfo.Message.DataType.PONG)
                    .setId(id)
                    .setPing(MessageInfo.Ping.newBuilder().setMsg("ping").build())
                    .build();
            ctx.writeAndFlush(ping);
        } else if (message.getDataType() == MessageInfo.Message.DataType.PONG) {
            logger.info("收到 pong 消息");
        } else if (message.getDataType() == MessageInfo.Message.DataType.AUTHENTICATION) {
//            DefaultFuture.received(ctx.channel(), message.getId(), message.getAuthentication());
            logger.info("认证成功...");
        } else if (message.getDataType() == MessageInfo.Message.DataType.QUERY) {
            logger.info("query...");
            MessageInfo.Message query = MessageInfo.Message.newBuilder()
                    .setDataType(MessageInfo.Message.DataType.QUERY)
                    .setId(message.getId())
                    .setQuery(MessageInfo.Query.newBuilder()
                            .setSuccess(true)
                            .build())
                    .build();
            ctx.writeAndFlush(query);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            if (state == IdleState.WRITER_IDLE) {
                MessageInfo.Message ping = MessageInfo.Message.newBuilder().setDataType(MessageInfo.Message.DataType.PING)
                        .setPing(MessageInfo.Ping.newBuilder().setMsg("ping").build())
                        .setId(0)
                        .build();
                ctx.writeAndFlush(ping);
                logger.info("userEventTriggered send PING...");
            }
        }
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
