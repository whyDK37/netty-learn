package dubbo.mini.rpc;

import dubbo.mini.common.Node;
import dubbo.mini.exception.RpcException;

public interface Invoker<T> extends Node {

  Class<T> getInterface();

  Result invoke(Invocation invocation) throws RpcException;

}