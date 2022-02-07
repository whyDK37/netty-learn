package dubbo.mini.server;

import io.netty.channel.Channel;

/**
 * @author why
 */
public class Machine {

  Channel channel;
  String orgCode;
  String endpoint;

  private Lease lease;

  public Machine(Channel channel, String orgCode, String endpoint) {
    this.channel = channel;
    this.orgCode = orgCode;
    this.endpoint = endpoint;
    lease = new Lease();
  }

  public void touch() {
    lease.touch();
  }


  public Channel getChannel() {
    return channel;
  }

  public String getOrgCode() {
    return orgCode;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public Lease getLease() {
    return lease;
  }

  @Override
  public String toString() {
    return "Machine{" +
        "channel=" + channel +
        ", orgCode='" + orgCode + '\'' +
        ", endpoint='" + endpoint + '\'' +
        ", lease=" + lease +
        '}';
  }
}