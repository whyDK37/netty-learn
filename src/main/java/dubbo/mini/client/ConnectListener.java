package dubbo.mini.client;

import io.netty.channel.Channel;

public interface ConnectListener {

    void connect(Channel channel);
}
