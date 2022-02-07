package dubbo.mini.rpc.proxy;

import dubbo.mini.common.Constants;
import dubbo.mini.common.utils.RpcUtils;
import dubbo.mini.rpc.Invoker;
import dubbo.mini.rpc.RpcInvocation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InvokerInvocationHandler implements InvocationHandler {

  private static final Logger logger = LoggerFactory.getLogger(InvokerInvocationHandler.class);
  private final Invoker<?> invoker;

  public InvokerInvocationHandler(Invoker<?> handler) {
    this.invoker = handler;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    String methodName = method.getName();
    Class<?>[] parameterTypes = method.getParameterTypes();
    if (method.getDeclaringClass() == Object.class) {
      return method.invoke(invoker, args);
    }
    if ("toString".equals(methodName) && parameterTypes.length == 0) {
      return invoker.toString();
    }
    if ("hashCode".equals(methodName) && parameterTypes.length == 0) {
      return invoker.hashCode();
    }
    if ("equals".equals(methodName) && parameterTypes.length == 1) {
      return invoker.equals(args[0]);
    }

    return invoker.invoke(createInvocation(method, args)).recreate();
  }

  private RpcInvocation createInvocation(Method method, Object[] args) {
    RpcInvocation invocation = new RpcInvocation(method, args);
    if (RpcUtils.hasFutureReturnType(method)) {
      invocation.setAttachment(Constants.FUTURE_RETURNTYPE_KEY, "true");
      invocation.setAttachment(Constants.ASYNC_KEY, "true");
    }
    return invocation;
  }

}
