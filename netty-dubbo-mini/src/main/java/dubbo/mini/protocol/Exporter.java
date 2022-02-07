package dubbo.mini.protocol;

import dubbo.mini.rpc.Invoker;

public interface Exporter<T> {

  Invoker<T> getInvoker();


  void unexport();

}