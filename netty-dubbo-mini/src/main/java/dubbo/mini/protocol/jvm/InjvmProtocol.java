package dubbo.mini.protocol.jvm;

import dubbo.mini.common.Constants;
import dubbo.mini.common.NetURL;
import dubbo.mini.common.utils.CollectionUtils;
import dubbo.mini.common.utils.UrlUtils;
import dubbo.mini.exception.RpcException;
import dubbo.mini.protocol.AbstractProtocol;
import dubbo.mini.protocol.Exporter;
import dubbo.mini.protocol.Protocol;
import dubbo.mini.rpc.Invoker;
import dubbo.mini.support.ExtensionLoader;
import java.util.Map;

public class InjvmProtocol extends AbstractProtocol implements Protocol {

  public static final String NAME = Constants.LOCAL_PROTOCOL;

  public static final int DEFAULT_PORT = 0;
  private static InjvmProtocol INSTANCE;

  public InjvmProtocol() {
    INSTANCE = this;
  }

  public static InjvmProtocol getInjvmProtocol() {
    if (INSTANCE == null) {
      ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(InjvmProtocol.NAME); // load
    }
    return INSTANCE;
  }

  static Exporter<?> getExporter(Map<String, Exporter<?>> map, NetURL key) {
    Exporter<?> result = null;

    if (!key.getServiceKey().contains("*")) {
      result = map.get(key.getServiceKey());
    } else {
      if (CollectionUtils.isNotEmptyMap(map)) {
        for (Exporter<?> exporter : map.values()) {
          if (UrlUtils.isServiceKeyMatch(key, exporter.getInvoker().getUrl())) {
            result = exporter;
            break;
          }
        }
      }
    }

    if (result == null) {
      return null;
    } else {
      return result;
    }
  }

  @Override
  public int getDefaultPort() {
    return DEFAULT_PORT;
  }

  @Override
  public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
    return new InjvmExporter<T>(invoker, invoker.getUrl().getServiceKey(), exporterMap);
  }

  @Override
  public <T> Invoker<T> refer(Class<T> serviceType, NetURL url) throws RpcException {
    return new InjvmInvoker<T>(serviceType, url, url.getServiceKey(), exporterMap);
  }

  public boolean isInjvmRefer(NetURL url) {
    String scope = url.getParameter(Constants.SCOPE_KEY);
    // Since injvm protocol is configured explicitly, we don't need to set any extra flag, use normal refer process.
    if (Constants.SCOPE_LOCAL.equals(scope) || (url
        .getParameter(Constants.LOCAL_PROTOCOL, false))) {
      // if it's declared as local reference
      // 'scope=local' is equivalent to 'injvm=true', injvm will be deprecated in the future release
      return true;
    } else if (Constants.SCOPE_REMOTE.equals(scope)) {
      // it's declared as remote reference
      return false;
    } else if (url.getParameter(Constants.GENERIC_KEY, false)) {
      // generic invocation is not local reference
      return false;
    } else if (getExporter(exporterMap, url) != null) {
      // by default, go through local reference if there's the service exposed locally
      return true;
    } else {
      return false;
    }
  }
}