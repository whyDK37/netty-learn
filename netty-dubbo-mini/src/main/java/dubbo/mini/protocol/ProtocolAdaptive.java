package dubbo.mini.protocol;

import dubbo.mini.common.NetURL;
import dubbo.mini.exception.RpcException;
import dubbo.mini.rpc.Invoker;
import dubbo.mini.support.ExtensionLoader;

public class ProtocolAdaptive implements Protocol {

  private ProtocolAdaptive() {
  }

  private static final ProtocolAdaptive protocolAdaptive = new ProtocolAdaptive();

  public static ProtocolAdaptive getInstance() {
    return protocolAdaptive;
  }

  public void destroy() {
    throw new UnsupportedOperationException(
        "The method public abstract void Protocol.destroy() of interface Protocol is not adaptive method!");
  }

  public int getDefaultPort() {
    throw new UnsupportedOperationException(
        "The method public abstract int Protocol.getDefaultPort() of interface Protocol is not adaptive method!");
  }

  public Exporter export(Invoker arg0)
      throws RpcException {
      if (arg0 == null) {
          throw new IllegalArgumentException("Invoker argument == null");
      }
      if (arg0.getUrl() == null) {
          throw new IllegalArgumentException("Invoker argument getUrl() == null");
      }
    NetURL url = arg0.getUrl();
    String extName = (url.getProtocol() == null ? "dubbo" : url.getProtocol());
      if (extName == null) {
          throw new IllegalStateException("Failed to get extension (Protocol) name from url ("
              + url.toString() + ") use keys([protocol])");
      }
    Protocol extension = (Protocol) ExtensionLoader
        .getExtensionLoader(Protocol.class).getExtension(extName);
    return extension.export(arg0);
  }

  public Invoker refer(java.lang.Class type, NetURL url)
      throws RpcException {
      if (url == null) {
          throw new IllegalArgumentException("url == null");
      }
    String extName = (url.getProtocol() == null ? "dubbo" : url.getProtocol());
      if (extName == null) {
          throw new IllegalStateException("Failed to get extension (Protocol) name from url ("
              + url.toString() + ") use keys([protocol])");
      }
    Protocol extension = (Protocol) ExtensionLoader
        .getExtensionLoader(Protocol.class).getExtension(extName);
    return extension.refer(type, url);
  }
}
