package dubbo.mini.rpc;

import dubbo.mini.common.NetURL;
import dubbo.mini.exception.RpcException;
import dubbo.mini.support.SPI;

/**
 * @author why
 */
@SPI("javassist")
public interface ProxyFactory {

    /**
     * create proxy.
     *
     * @param invoker
     * @return proxy
     */
    <T> T getProxy(Invoker<T> invoker) throws RpcException;

    /**
     * create invoker.
     *
     * @param <T>
     * @param proxy
     * @param type
     * @param url
     * @return invoker
     */
    <T> Invoker<T> getInvoker(T proxy, Class<T> type, NetURL url) throws RpcException;
}
