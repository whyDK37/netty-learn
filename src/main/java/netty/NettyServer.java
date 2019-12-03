package netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class NettyServer {
	
	public static void main(String[] args) {
		EventLoopGroup parentGroup = new NioEventLoopGroup(); // thread group -> Acceptor thread
		EventLoopGroup childGroup = new NioEventLoopGroup(); // thread group -> Processor / Handler
		
		try {
			ServerBootstrap serverBootstrap = new ServerBootstrap(); // Netty server
			
			serverBootstrap
					.group(parentGroup, childGroup)
					.channel(NioServerSocketChannel.class)  // ServerChannel , ServerSocketChannel
					.option(ChannelOption.SO_BACKLOG, 1024)
					.childHandler(new ChannelInitializer<SocketChannel>() { // 处理每个连接的SocketChannel

						@Override
						protected void initChannel(SocketChannel socketChannel) throws Exception {
							socketChannel.pipeline().addLast(new NettyServerHandler()); // business code
						}
						
					});
			
			ChannelFuture bindFuture = serverBootstrap.bind(50070).sync(); // waiting for server startup
			
			bindFuture.channel().closeFuture().sync(); // waiting for server shutdown
		} catch (Exception e) {
			e.printStackTrace();  
		} finally {
			parentGroup.shutdownGracefully();
			childGroup.shutdownGracefully();
		}

		System.out.println("server start");
	}
	
}
