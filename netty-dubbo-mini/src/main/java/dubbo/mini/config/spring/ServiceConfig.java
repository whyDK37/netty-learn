package dubbo.mini.config.spring;

import dubbo.mini.common.NetURL;
import dubbo.mini.protocol.Exporter;
import dubbo.mini.protocol.Protocol;
import dubbo.mini.support.ExtensionLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author why
 */
public class ServiceConfig {

  private final Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class)
      .getDefaultExtension();

  private final List<NetURL> urls = new ArrayList<NetURL>();

  private final List<Exporter<?>> exporters = new ArrayList<Exporter<?>>();

  protected String interfaceName;
  protected Class<?> interfaceClass;

  public Protocol getProtocol() {
    return protocol;
  }
}
