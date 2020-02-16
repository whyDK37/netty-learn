package dubbo.mini.rpc;

import dubbo.mini.common.NetURL;
import dubbo.mini.exception.RpcException;
import java.lang.reflect.InvocationTargetException;

public abstract class AbstractProxyInvoker<T> implements Invoker<T> {

  private final T proxy;

  private final Class<T> type;

  private final NetURL url;

  public AbstractProxyInvoker(T proxy, Class<T> type, NetURL url) {
    if (proxy == null) {
      throw new IllegalArgumentException("proxy == null");
    }
    if (type == null) {
      throw new IllegalArgumentException("interface == null");
    }
    if (!type.isInstance(proxy)) {
      throw new IllegalArgumentException(
          proxy.getClass().getName() + " not implement interface " + type);
    }
    this.proxy = proxy;
    this.type = type;
    this.url = url;
  }

  @Override
  public Class<T> getInterface() {
    return type;
  }

  @Override
  public NetURL getUrl() {
    return url;
  }

  @Override
  public boolean isAvailable() {
    return true;
  }

  @Override
  public void destroy() {
  }

  // TODO Unified to AsyncResult?
  @Override
  public Result invoke(Invocation invocation) throws RpcException {
    try {
      Object obj = doInvoke(proxy, invocation.getMethodName(), invocation.getParameterTypes(),
          invocation.getArguments());
      return new RpcResult(obj);
    } catch (InvocationTargetException e) {
      return new RpcResult(e.getTargetException());
    } catch (Throwable e) {
      throw new RpcException(
          "Failed to invoke remote proxy method " + invocation.getMethodName() + " to " + getUrl()
              + ", cause: " + e.getMessage(), e);
    }
  }

  protected abstract Object doInvoke(T proxy, String methodName, Class<?>[] parameterTypes,
      Object[] arguments) throws Throwable;

  @Override
  public String toString() {
    return getInterface() + " -> " + (getUrl() == null ? " " : getUrl().toString());
  }


}