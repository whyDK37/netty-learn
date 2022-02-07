package dubbo.mini.rpc;

import dubbo.mini.common.NetURL;
import dubbo.mini.config.spring.ServiceBean;
import dubbo.mini.exception.RpcException;

public class DelegateProviderMetaDataInvoker<T> implements Invoker {

  protected final Invoker<T> invoker;
  private ServiceBean metadata;

  public DelegateProviderMetaDataInvoker(Invoker<T> invoker, ServiceBean metadata) {
    this.invoker = invoker;
    this.metadata = metadata;
  }

  @Override
  public Class<T> getInterface() {
    return invoker.getInterface();
  }

  @Override
  public NetURL getUrl() {
    return invoker.getUrl();
  }

  @Override
  public boolean isAvailable() {
    return invoker.isAvailable();
  }

  @Override
  public Result invoke(Invocation invocation) throws RpcException {
    return invoker.invoke(invocation);
  }

  @Override
  public void destroy() {
    invoker.destroy();
  }

  public ServiceBean getMetadata() {
    return metadata;
  }
}
