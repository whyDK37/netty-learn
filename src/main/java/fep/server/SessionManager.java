package fep.server;

import io.netty.channel.Channel;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * session 管理器
 *
 * @author why
 */
public class SessionManager {
    private SessionManager() {
    }

    private static final SessionManager SESSION_MANAGER = new SessionManager();


    public static SessionManager getInstance() {
        return SESSION_MANAGER;
    }

    /**
     * key: endpoint, ip:port
     * value: channel
     */
    private Map<String, Channel> channels = new ConcurrentHashMap<>();
    /**
     * key: org
     * value: endpoint
     */
    private Map<String, String> orgMap = new ConcurrentHashMap<>();

    public void clear() {
        channels.clear();
        orgMap.clear();
    }

    public void registerChannel(String orgCode, Channel channel) {
        SocketAddress socketAddress = channel.remoteAddress();;
        channels.put(socketAddress.toString(), channel);
        orgMap.put(orgCode, socketAddress.toString());
    }

    public Collection<Channel> getChannels() {
        return channels.values();
    }
}
