package dubbo.mini.remote;

import java.net.SocketAddress;

public class RemotingException extends Exception {

  private static final long serialVersionUID = -3160452149606778709L;

  private SocketAddress localAddress;

  private SocketAddress remoteAddress;

  public RemotingException(NetChannel channel, String msg) {
    this(channel == null ? null : channel.getRemoteAddress(),
        channel == null ? null : channel.getRemoteAddress(),
        msg);
  }

  public RemotingException(SocketAddress localAddress, SocketAddress remoteAddress,
      String message) {
    super(message);

    this.localAddress = localAddress;
    this.remoteAddress = remoteAddress;
  }

  public RemotingException(NetChannel channel, Throwable cause) {
    this(channel == null ? null : channel.getRemoteAddress(),
        channel == null ? null : channel.getRemoteAddress(),
        cause);
  }

  public RemotingException(SocketAddress localAddress, SocketAddress remoteAddress,
      Throwable cause) {
    super(cause);

    this.localAddress = localAddress;
    this.remoteAddress = remoteAddress;
  }

  public RemotingException(NetChannel channel, String message, Throwable cause) {
    this(channel == null ? null : channel.getRemoteAddress(),
        channel == null ? null : channel.getRemoteAddress(),
        message, cause);
  }

  public RemotingException(SocketAddress localAddress, SocketAddress remoteAddress, String message,
      Throwable cause) {
    super(message, cause);

    this.localAddress = localAddress;
    this.remoteAddress = remoteAddress;
  }

  public SocketAddress getLocalAddress() {
    return localAddress;
  }

  public SocketAddress getRemoteAddress() {
    return remoteAddress;
  }
}