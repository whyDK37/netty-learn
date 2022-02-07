package dubbo.mini.common.utils;

import dubbo.mini.common.Constants;
import dubbo.mini.common.NetURL;
import dubbo.mini.rpc.Invocation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcUtils {

  private static final Logger logger = LoggerFactory.getLogger(RpcUtils.class);
  private static final AtomicLong INVOKE_ID = new AtomicLong(0);

  public static Class<?> getReturnType(Invocation invocation) {
    try {
      if (invocation != null && invocation.getInvoker() != null
          && invocation.getInvoker().getUrl() != null
          && !invocation.getMethodName().startsWith("$")) {
        String service = invocation.getInvoker().getUrl().getServiceInterface();
        if (!StringUtils.isEmpty(service)) {
          Class<?> invokerInterface = invocation.getInvoker().getInterface();
          Class<?> cls = invokerInterface != null ? ReflectUtils
              .forName(invokerInterface.getClassLoader(), service)
              : ReflectUtils.forName(service);
          Method method = cls.getMethod(invocation.getMethodName(), invocation.getParameterTypes());
          if (method.getReturnType() == void.class) {
            return null;
          }
          return method.getReturnType();
        }
      }
    } catch (Throwable t) {
      logger.warn(t.getMessage(), t);
    }
    return null;
  }


  public static boolean hasFutureReturnType(Method method) {
    return CompletableFuture.class.isAssignableFrom(method.getReturnType());
  }

  public static boolean isReturnTypeFuture(Invocation inv) {
    return Boolean.TRUE.toString().equals(inv.getAttachment(Constants.FUTURE_RETURNTYPE_KEY));
  }

  public static String getMethodName(Invocation invocation) {
    if (Constants.$INVOKE.equals(invocation.getMethodName())
        && invocation.getArguments() != null
        && invocation.getArguments().length > 0
        && invocation.getArguments()[0] instanceof String) {
      return (String) invocation.getArguments()[0];
    }
    return invocation.getMethodName();
  }


  public static boolean isAsync(NetURL url, Invocation inv) {
    boolean isAsync;
    if (Boolean.TRUE.toString().equals(inv.getAttachment(Constants.ASYNC_KEY))) {
      isAsync = true;
    } else {
      isAsync = url.getMethodParameter(getMethodName(inv), Constants.ASYNC_KEY, false);
    }
    return isAsync;
  }

  public static boolean isOneway(NetURL url, Invocation inv) {
    boolean isOneway;
    if (Boolean.FALSE.toString().equals(inv.getAttachment(Constants.RETURN_KEY))) {
      isOneway = true;
    } else {
      isOneway = !url.getMethodParameter(getMethodName(inv), Constants.RETURN_KEY, true);
    }
    return isOneway;
  }

  // TODO why not get return type when initialize Invocation?
  public static Type[] getReturnTypes(Invocation invocation) {
    try {
      if (invocation != null && invocation.getInvoker() != null
          && invocation.getInvoker().getUrl() != null
          && !invocation.getMethodName().startsWith("$")) {
        String service = invocation.getInvoker().getUrl().getServiceInterface();
        if (StringUtils.isNotEmpty(service)) {
          Class<?> invokerInterface = invocation.getInvoker().getInterface();
          Class<?> cls = invokerInterface != null ? ReflectUtils
              .forName(invokerInterface.getClassLoader(), service)
              : ReflectUtils.forName(service);
          Method method = cls.getMethod(invocation.getMethodName(), invocation.getParameterTypes());
          if (method.getReturnType() == void.class) {
            return null;
          }
          Class<?> returnType = method.getReturnType();
          Type genericReturnType = method.getGenericReturnType();
          if (Future.class.isAssignableFrom(returnType)) {
            if (genericReturnType instanceof ParameterizedType) {
              Type actualArgType = ((ParameterizedType) genericReturnType)
                  .getActualTypeArguments()[0];
              if (actualArgType instanceof ParameterizedType) {
                returnType = (Class<?>) ((ParameterizedType) actualArgType).getRawType();
                genericReturnType = actualArgType;
              } else {
                returnType = (Class<?>) actualArgType;
                genericReturnType = returnType;
              }
            } else {
              returnType = null;
              genericReturnType = null;
            }
          }
          return new Type[]{returnType, genericReturnType};
        }
      }
    } catch (Throwable t) {
      logger.warn(t.getMessage(), t);
    }
    return null;
  }


  public static Map<String, String> getNecessaryAttachments(Invocation inv) {
    Map<String, String> attachments = new HashMap<>(inv.getAttachments());
    attachments.remove(Constants.ASYNC_KEY);
    attachments.remove(Constants.FUTURE_GENERATED_KEY);
    return attachments;
  }


}
