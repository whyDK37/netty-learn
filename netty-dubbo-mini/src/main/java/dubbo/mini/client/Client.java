//package dubbo.mini.client;
//
//import dubbo.mini.netty4.NettyClient;
//import io.netty.channel.Channel;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import dubbo.mini.support.DefaultFuture;
//
//public class Client {
//    private static Logger logger = LoggerFactory.getLogger(Client.class);
//    private NettyClient clientCnx;
//
//    private String orgCode;
//
//    public Client(String orgCode, String ip, int port, int timeout, long idleTimeout, long connectTimeout) {
//        this.orgCode = orgCode;
////        clientCnx = new NettyClient(ip, port, timeout, idleTimeout, connectTimeout);
////        clientCnx.addConnectListener(channel -> authentication(channel, orgCode));
////        clientCnx.connect();
//    }
//
//    public void authentication(Channel channel, String orgCode) {
//        logger.info("发送认证信息:{}", orgCode);
//
//        DefaultFuture defaultFuture = DefaultFuture.newFuture(channel, MessageInfo.Message.newBuilder()
//                .setDataType(MessageInfo.Message.DataType.AUTHENTICATION)
//                .setId(DefaultFuture.REQUEST_ID.incrementAndGet())
//                .setAuthentication(MessageInfo.Authentication.newBuilder()
//                        .setOrgCode(orgCode).build())
//                .build());
//    }
//}
