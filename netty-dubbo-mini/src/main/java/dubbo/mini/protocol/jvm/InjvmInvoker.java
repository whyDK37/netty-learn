package dubbo.mini.protocol.jvm;

import dubbo.mini.common.Constants;
import dubbo.mini.common.NetURL;
import dubbo.mini.exception.RpcException;
import dubbo.mini.protocol.Exporter;
import dubbo.mini.rpc.AbstractInvoker;
import dubbo.mini.rpc.Invocation;
import dubbo.mini.rpc.Result;
import dubbo.mini.rpc.RpcContext;
import java.util.Map;

public class InjvmInvoker<T> extends AbstractInvoker<T> {

  private final String key;

  private final Map<String, Exporter<?>> exporterMap;

  public InjvmInvoker(Class<T> type, NetURL url, String key, Map<String, Exporter<?>> exporterMap) {
    super(type, url);
    this.key = key;
    this.exporterMap = exporterMap;
  }

  @Override
  public boolean isAvailable() {
    InjvmExporter<?> exporter = (InjvmExporter<?>) exporterMap.get(key);
    if (exporter == null) {
      return false;
    } else {
      return super.isAvailable();
    }
  }

  @Override
  public Result doInvoke(Invocation invocation) throws Throwable {
    Exporter<?> exporter = InjvmProtocol.getExporter(exporterMap, getUrl());
    if (exporter == null) {
      throw new RpcException("Service [" + key + "] not found.");
    }
    RpcContext.getContext().setRemoteAddress(Constants.LOCALHOST_VALUE, 0);
    return exporter.getInvoker().invoke(invocation);
  }
}
