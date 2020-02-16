package dubbo.mini.server;

import io.netty.channel.Channel;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
   * key: endpoint, ip:port value: channel
   */
  private Map<String, Machine> channels = new ConcurrentHashMap<>();
  /**
   * key: org value: endpoint
   */
  private Map<String, Machine> orgMap = new ConcurrentHashMap<>();

  public void clear() {
    channels.clear();
    orgMap.clear();
  }

  public void registerChannel(String orgCode, Channel channel) {
    SocketAddress socketAddress = channel.remoteAddress();
    Machine machine = new Machine(channel, orgCode, socketAddress.toString());
    channels.put(socketAddress.toString(), machine);
    orgMap.put(orgCode, machine);
  }

  public void touch(Channel channel) {
    Machine machine = channels.get(channel.remoteAddress().toString());
    if (machine != null) {
      machine.touch();
    }
  }


  public Collection<Channel> getChannels() {
    return channels.values().stream().map(machine -> machine.channel).collect(Collectors.toList());
  }

  public Collection<Machine> getMachines() {
    return channels.values();
  }

  public void remove(Channel channel) {
    Machine machine = channels.remove(channel.remoteAddress().toString());
    if (machine != null) {
      orgMap.remove(machine.orgCode);
    }
  }


}
