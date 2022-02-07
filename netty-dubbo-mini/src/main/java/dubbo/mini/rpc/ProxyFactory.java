package dubbo.mini.rpc;

import dubbo.mini.common.NetURL;
import dubbo.mini.exception.RpcException;
import dubbo.mini.support.SPI;

/**
 * @author why
 */
@SPI("javassist")
public interface ProxyFactory {

  <T> T getProxy(Invoker<T> invoker) throws RpcException;

  <T> Invoker<T> getInvoker(T proxy, Class<T> type, NetURL url) throws RpcException;
}
