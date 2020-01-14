package dubbo.mini.remote;

import dubbo.mini.common.URL;

public class ChannelHandlers {

    private static ChannelHandlers INSTANCE = new ChannelHandlers();

    protected ChannelHandlers() {
    }

    public static ChannelEventHandler wrap(ChannelEventHandler handler, URL url) {
        return ChannelHandlers.getInstance().wrapInternal(handler, url);
    }

    protected static ChannelHandlers getInstance() {
        return INSTANCE;
    }

    static void setTestingChannelHandlers(ChannelHandlers instance) {
        INSTANCE = instance;
    }

    protected ChannelEventHandler wrapInternal(ChannelEventHandler handler, URL url) {
        return new MultiMessageHandler(
                new HeartbeatHandler(
                        new AllChannelHandler(handler, url)));
    }
}
