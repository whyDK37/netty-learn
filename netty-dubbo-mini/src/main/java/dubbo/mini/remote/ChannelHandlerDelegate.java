package dubbo.mini.remote;

public interface ChannelHandlerDelegate extends ChannelEventHandler {

  ChannelEventHandler getHandler();
}
