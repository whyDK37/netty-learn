package netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

public class NettyServerHandler extends ChannelHandlerAdapter {

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		ByteBuf requestBuffer = (ByteBuf) msg;
		byte[] requestBytes = new byte[requestBuffer.readableBytes()];
		requestBuffer.readBytes(requestBytes);
		
		String request = new String(requestBytes, "UTF-8");
		System.out.println("message from client :" + request);
		
		String response = "this is a response from server";
		ByteBuf responseBuffer = Unpooled.copiedBuffer(response.getBytes());
		ctx.write(responseBuffer);
		
		// 这个东西类似对应着我们之前说的那个Processor线程，负责读取请求，返回响应
		// 具体底层的源码还没看，这个东西也可以理解为我们之前说的那个Handler线程
		// Netty底层就有类似Processor的东西，负责从网络连接中读取请求
		// 然后把读取出来的请求交给我们的Handler线程来处理，处理完以后把响应返回回去
		// 但是可能在底层响应是由Processor线程来发送回去的
	}
	
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		ctx.close();
	}
	
}
