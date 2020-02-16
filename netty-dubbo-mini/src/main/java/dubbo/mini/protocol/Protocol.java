package dubbo.mini.protocol;

import dubbo.mini.common.NetURL;
import dubbo.mini.exception.RpcException;
import dubbo.mini.rpc.Invoker;
import dubbo.mini.support.SPI;

@SPI("dubbo")
public interface Protocol {

  int getDefaultPort();

  <T> Exporter<T> export(Invoker<T> invoker) throws RpcException;

  <T> Invoker<T> refer(Class<T> type, NetURL url) throws RpcException;

  void destroy();

}