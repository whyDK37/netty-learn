package dubbo.mini.remote;

import java.net.InetSocketAddress;

public class ExecutionException extends RemotingException {

  private static final long serialVersionUID = -2531085236111056860L;

  private final Object request;

  public ExecutionException(Object request, NetChannel channel, String message, Throwable cause) {
    super(channel, message, cause);
    this.request = request;
  }

  public ExecutionException(Object request, NetChannel channel, String msg) {
    super(channel, msg);
    this.request = request;
  }

  public ExecutionException(Object request, NetChannel channel, Throwable cause) {
    super(channel, cause);
    this.request = request;
  }

  public ExecutionException(Object request, InetSocketAddress localAddress,
      InetSocketAddress remoteAddress, String message,
      Throwable cause) {
    super(localAddress, remoteAddress, message, cause);
    this.request = request;
  }

  public ExecutionException(Object request, InetSocketAddress localAddress,
      InetSocketAddress remoteAddress, String message) {
    super(localAddress, remoteAddress, message);
    this.request = request;
  }

  public ExecutionException(Object request, InetSocketAddress localAddress,
      InetSocketAddress remoteAddress, Throwable cause) {
    super(localAddress, remoteAddress, cause);
    this.request = request;
  }


  public Object getRequest() {
    return request;
  }

}