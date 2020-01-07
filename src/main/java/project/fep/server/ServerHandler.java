package project.fep.server;

import project.fep.MessageInfo;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author why
 */
public class ServerHandler extends ChannelDuplexHandler {

    private static Logger logger = LoggerFactory.getLogger(ServerHandler.class);

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("channelActive");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("channelInactive");
        SessionManager.getInstance().remove(ctx.channel());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        logger.info("channelRead");
        MessageInfo.Message message = (MessageInfo.Message) msg;
        if (message.getDataType() == MessageInfo.Message.DataType.PING) {
            logger.info("ping...pong");
            MessageInfo.Message ping = MessageInfo.Message.newBuilder().setDataType(MessageInfo.Message.DataType.PONG)
                    .setPong(MessageInfo.Pong.newBuilder().setMsg("pong").build())
                    .build();
            ctx.writeAndFlush(ping);
            SessionManager.getInstance().touch(ctx.channel());
        }
        // 认证信息
        else if (message.getDataType() == MessageInfo.Message.DataType.AUTHENTICATION) {
            logger.info("认证信息:{}", message.getAuthentication());
            MessageInfo.Authentication authentication = message.getAuthentication();
            String orgCode = authentication.getOrgCode();
            Channel channel = ctx.channel();
            SessionManager.getInstance().registerChannel(orgCode, channel);

            // response
            MessageInfo.Message response = MessageInfo.Message.newBuilder().setDataType(MessageInfo.Message.DataType.AUTHENTICATION)
                    .setAuthentication(MessageInfo.Authentication.newBuilder()
                            .setSuccess(true)
                            .build())
                    .build();
            ctx.writeAndFlush(response);
        }
    }


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        logger.info("userEventTriggered:{}", evt);
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        logger.warn("exceptionCaught", cause);
    }
}
