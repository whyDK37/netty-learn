package dubbo.mini.rpc;

import dubbo.mini.common.Constants;
import dubbo.mini.common.utils.ReflectUtils;
import dubbo.mini.exception.RpcException;

public abstract class AbstractProxyFactory implements ProxyFactory {

  @Override
  public <T> T getProxy(Invoker<T> invoker) throws RpcException {
    Class<?>[] interfaces = null;
    String config = invoker.getUrl().getParameter(Constants.INTERFACES);
    if (config != null && config.length() > 0) {
      String[] types = Constants.COMMA_SPLIT_PATTERN.split(config);
      if (types != null && types.length > 0) {
        interfaces = new Class<?>[types.length + 1];
        interfaces[0] = invoker.getInterface();
        for (int i = 0; i < types.length; i++) {
          // TODO can we load successfully for a different classloader?.
          interfaces[i + 1] = ReflectUtils.forName(types[i]);
        }
      }
    }

    return getProxy(invoker, interfaces);
  }

  public abstract <T> T getProxy(Invoker<T> invoker, Class<?>[] types);

}