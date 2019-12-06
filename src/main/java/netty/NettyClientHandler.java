package netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class NettyClientHandler extends SimpleChannelInboundHandler {

    private ByteBuf requestBuffer;

    public NettyClientHandler() {
        byte[] requestBytes = "hello, this is a message from client".getBytes();
        requestBuffer = Unpooled.buffer(requestBytes.length);
        requestBuffer.writeBytes(requestBytes);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object message) throws Exception {
        System.out.println("channelRead0");
    }

//	@Override
//	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
//		ctx.writeAndFlush(requestBuffer);
//		super.handlerAdded(ctx);
//	}
//
////	@Override
////	public void channelActive(ChannelHandlerContext ctx) throws Exception {
////	}
//
//
//	@Override
//	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
//		ByteBuf responseBuffer = (ByteBuf) msg;
//		byte[] responseBytes = new byte[responseBuffer.readableBytes()];
//		responseBuffer.readBytes(responseBytes);
//
//		String response = new String(responseBytes, "UTF-8");
//		System.out.println("receive message from server" + response);
//	}
//
//	@Override
//	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//		cause.printStackTrace();
//		ctx.close();
//	}

}
