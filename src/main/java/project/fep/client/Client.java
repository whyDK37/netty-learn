package project.fep.client;

import project.fep.MessageInfo;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

public class Client {
    private static Logger logger = LoggerFactory.getLogger(Client.class);
    private AtomicLong requestId = new AtomicLong();
    private ClientCnx clientCnx;

    private String orgCode;

    public Client(String orgCode, String ip, int port, int timeout, long idleTimeout, long connectTimeout) {
        this.orgCode = orgCode;
        clientCnx = new ClientCnx(ip, port, timeout, idleTimeout, connectTimeout);
        clientCnx.addConnectListener(channel -> authentication(channel, orgCode));
        clientCnx.connect();
    }

    public void authentication(Channel channel, String orgCode) {
        logger.info("发送认证信息:{}", orgCode);

        MessageInfo.Message message = MessageInfo.Message.newBuilder()
                .setDataType(MessageInfo.Message.DataType.AUTHENTICATION)
                .setId(requestId.getAndIncrement())
                .setAuthentication(MessageInfo.Authentication.newBuilder()
                        .setOrgCode(orgCode).build())
                .build();
        channel.writeAndFlush(message);
    }
}
