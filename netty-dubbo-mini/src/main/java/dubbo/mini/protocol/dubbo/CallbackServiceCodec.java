package dubbo.mini.protocol.dubbo;

import dubbo.mini.common.Constants;
import dubbo.mini.common.NetURL;
import dubbo.mini.common.bytecode.Wrapper;
import dubbo.mini.common.utils.ConcurrentHashSet;
import dubbo.mini.common.utils.StringUtils;
import dubbo.mini.protocol.Exporter;
import dubbo.mini.remote.NetChannel;
import dubbo.mini.remote.RemotingException;
import dubbo.mini.rpc.Invocation;
import dubbo.mini.rpc.Invoker;
import dubbo.mini.rpc.ProxyFactory;
import dubbo.mini.rpc.RpcInvocation;
import dubbo.mini.support.ExtensionLoader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CallbackServiceCodec {

  private static final Logger logger = LoggerFactory.getLogger(CallbackServiceCodec.class);

  private static final ProxyFactory proxyFactory = ExtensionLoader
      .getExtensionLoader(ProxyFactory.class).getDefaultExtension();
  private static final DubboProtocol protocol = DubboProtocol.getDubboProtocol();
  private static final byte CALLBACK_NONE = 0x0;
  private static final byte CALLBACK_CREATE = 0x1;
  private static final byte CALLBACK_DESTROY = 0x2;
  private static final String INV_ATT_CALLBACK_KEY = "sys_callback_arg-";


  private static byte isCallBack(NetURL url, String methodName, int argIndex) {
    // parameter callback rule: method-name.parameter-index(starting from 0).callback
    byte isCallback = CALLBACK_NONE;
    if (url != null) {
      String callback = url.getParameter(methodName + "." + argIndex + ".callback");
      if (callback != null) {
        if (callback.equalsIgnoreCase("true")) {
          isCallback = CALLBACK_CREATE;
        } else if (callback.equalsIgnoreCase("false")) {
          isCallback = CALLBACK_DESTROY;
        }
      }
    }
    return isCallback;
  }

  public static Object encodeInvocationArgument(NetChannel channel, RpcInvocation inv,
      int paraIndex) throws IOException {
    // get URL directly
    NetURL url = inv.getInvoker() == null ? null : inv.getInvoker().getUrl();
    byte callbackStatus = isCallBack(url, inv.getMethodName(), paraIndex);
    Object[] args = inv.getArguments();
    Class<?>[] pts = inv.getParameterTypes();
    switch (callbackStatus) {
      case CallbackServiceCodec.CALLBACK_NONE:
        return args[paraIndex];
      case CallbackServiceCodec.CALLBACK_CREATE:
        inv.setAttachment(INV_ATT_CALLBACK_KEY + paraIndex,
            exportOrUnexportCallbackService(channel, url, pts[paraIndex], args[paraIndex], true));
        return null;
      case CallbackServiceCodec.CALLBACK_DESTROY:
        inv.setAttachment(INV_ATT_CALLBACK_KEY + paraIndex,
            exportOrUnexportCallbackService(channel, url, pts[paraIndex], args[paraIndex], false));
        return null;
      default:
        return args[paraIndex];
    }
  }


  private static String exportOrUnexportCallbackService(NetChannel channel, NetURL url,
      Class clazz, Object inst, Boolean export) throws IOException {
    int instid = System.identityHashCode(inst);

    Map<String, String> params = new HashMap<>(3);
    // no need to new client again
    params.put(Constants.IS_SERVER_KEY, Boolean.FALSE.toString());
    // mark it's a callback, for troubleshooting
    params.put(Constants.IS_CALLBACK_SERVICE, Boolean.TRUE.toString());
    String group = (url == null ? null : url.getParameter(Constants.GROUP_KEY));
    if (group != null && group.length() > 0) {
      params.put(Constants.GROUP_KEY, group);
    }
    // add method, for verifying against method, automatic fallback (see dubbo protocol)
    params.put(Constants.METHODS_KEY,
        StringUtils.join(Wrapper.getWrapper(clazz).getDeclaredMethodNames(), ","));

    Map<String, String> tmpMap = new HashMap<>(url.getParameters());
    tmpMap.putAll(params);
    tmpMap.remove(Constants.VERSION_KEY);// doesn't need to distinguish version for callback
    tmpMap.put(Constants.INTERFACE_KEY, clazz.getName());
    NetURL exportUrl = new NetURL(DubboProtocol.NAME,
        channel.getLocalAddress().getAddress().getHostAddress(),
        channel.getLocalAddress().getPort(), clazz.getName() + "." + instid, tmpMap);

    // no need to generate multiple exporters for different channel in the same JVM, cache key cannot collide.
    String cacheKey = getClientSideCallbackServiceCacheKey(instid);
    String countKey = getClientSideCountKey(clazz.getName());
    if (export) {
      // one channel can have multiple callback instances, no need to re-export for different instance.
      if (!channel.hasAttribute(cacheKey)) {
        if (!isInstancesOverLimit(channel, url, clazz.getName(), instid, false)) {
          Invoker<?> invoker = proxyFactory.getInvoker(inst, clazz, exportUrl);
          // should destroy resource?
          Exporter<?> exporter = protocol.export(invoker);
          // this is used for tracing if instid has published service or not.
          channel.setAttribute(cacheKey, exporter);
          logger.info(
              "Export a callback service :" + exportUrl + ", on " + channel + ", url is: " + url);
          increaseInstanceCount(channel, countKey);
        }
      }
    } else {
      if (channel.hasAttribute(cacheKey)) {
        Exporter<?> exporter = (Exporter<?>) channel.getAttribute(cacheKey);
        exporter.unexport();
        channel.removeAttribute(cacheKey);
        decreaseInstanceCount(channel, countKey);
      }
    }
    return String.valueOf(instid);
  }

  private static String getClientSideCallbackServiceCacheKey(int instid) {
    return Constants.CALLBACK_SERVICE_KEY + "." + instid;
  }

  private static String getClientSideCountKey(String interfaceClass) {
    return Constants.CALLBACK_SERVICE_KEY + "." + interfaceClass + ".COUNT";
  }

  private static String getServerSideCountKey(NetChannel channel, String interfaceClass) {
    return Constants.CALLBACK_SERVICE_PROXY_KEY + "." + System.identityHashCode(channel) + "."
        + interfaceClass + ".COUNT";
  }

  private static boolean isInstancesOverLimit(NetChannel channel, NetURL url, String interfaceClass,
      int instid, boolean isServer) {
    Integer count = (Integer) channel.getAttribute(
        isServer ? getServerSideCountKey(channel, interfaceClass)
            : getClientSideCountKey(interfaceClass));
    int limit = url
        .getParameter(Constants.CALLBACK_INSTANCES_LIMIT_KEY, Constants.DEFAULT_CALLBACK_INSTANCES);
    if (count != null && count >= limit) {
      //client side error
      throw new IllegalStateException(
          "interface " + interfaceClass + " `s callback instances num exceed providers limit :"
              + limit
              + " ,current num: " + (count + 1)
              + ". The new callback service will not work !!! you can cancle the callback service which exported before. channel :"
              + channel);
    } else {
      return false;
    }
  }

  private static void increaseInstanceCount(NetChannel channel, String countkey) {
    try {
      //ignore concurrent problem?
      Integer count = (Integer) channel.getAttribute(countkey);
      if (count == null) {
        count = 1;
      } else {
        count++;
      }
      channel.setAttribute(countkey, count);
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
  }


  private static void decreaseInstanceCount(NetChannel channel, String countkey) {
    try {
      Integer count = (Integer) channel.getAttribute(countkey);
      if (count == null || count <= 0) {
        return;
      } else {
        count--;
      }
      channel.setAttribute(countkey, count);
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
  }


  public static Object decodeInvocationArgument(NetChannel channel, RpcInvocation inv,
      Class<?>[] pts, int paraIndex, Object inObject) throws IOException {
    // if it's a callback, create proxy on client side, callback interface on client side can be invoked through channel
    // need get URL from channel and env when decode
    NetURL url = null;
    try {
      url = DubboProtocol.getDubboProtocol().getInvoker(channel, inv).getUrl();
    } catch (RemotingException e) {
      if (logger.isInfoEnabled()) {
        logger.info(e.getMessage(), e);
      }
      return inObject;
    }
    byte callbackstatus = isCallBack(url, inv.getMethodName(), paraIndex);
    switch (callbackstatus) {
      case CallbackServiceCodec.CALLBACK_NONE:
        return inObject;
      case CallbackServiceCodec.CALLBACK_CREATE:
        try {
          return referOrDestroyCallbackService(channel, url, pts[paraIndex], inv,
              Integer.parseInt(inv.getAttachment(INV_ATT_CALLBACK_KEY + paraIndex)), true);
        } catch (Exception e) {
          logger.error(e.getMessage(), e);
          throw new IOException(StringUtils.toString(e));
        }
      case CallbackServiceCodec.CALLBACK_DESTROY:
        try {
          return referOrDestroyCallbackService(channel, url, pts[paraIndex], inv,
              Integer.parseInt(inv.getAttachment(INV_ATT_CALLBACK_KEY + paraIndex)), false);
        } catch (Exception e) {
          throw new IOException(StringUtils.toString(e));
        }
      default:
        return inObject;
    }
  }

  private static String getServerSideCallbackInvokerCacheKey(NetChannel channel,
      String interfaceClass, int instid) {
    return getServerSideCallbackServiceCacheKey(channel, interfaceClass, instid) + "." + "invoker";
  }

  private static String getServerSideCallbackServiceCacheKey(NetChannel channel,
      String interfaceClass, int instid) {
    return Constants.CALLBACK_SERVICE_PROXY_KEY + "." + System.identityHashCode(channel) + "."
        + interfaceClass + "." + instid;
  }

  private static Object referOrDestroyCallbackService(NetChannel channel, NetURL url,
      Class<?> clazz, Invocation inv, int instid, boolean isRefer) {
    Object proxy = null;
    String invokerCacheKey = getServerSideCallbackInvokerCacheKey(channel, clazz.getName(), instid);
    String proxyCacheKey = getServerSideCallbackServiceCacheKey(channel, clazz.getName(), instid);
    proxy = channel.getAttribute(proxyCacheKey);
    String countkey = getServerSideCountKey(channel, clazz.getName());
    if (isRefer) {
      if (proxy == null) {
        NetURL referurl = NetURL.valueOf(
            "callback://" + url.getAddress() + "/" + clazz.getName() + "?" + Constants.INTERFACE_KEY
                + "=" + clazz.getName());
        referurl = referurl.addParametersIfAbsent(url.getParameters())
            .removeParameter(Constants.METHODS_KEY);
        if (!isInstancesOverLimit(channel, referurl, clazz.getName(), instid, true)) {
          @SuppressWarnings("rawtypes")
          Invoker<?> invoker = new ChannelWrappedInvoker(clazz, channel, referurl,
              String.valueOf(instid));
          proxy = proxyFactory.getProxy(invoker);
          channel.setAttribute(proxyCacheKey, proxy);
          channel.setAttribute(invokerCacheKey, invoker);
          increaseInstanceCount(channel, countkey);

          //convert error fail fast .
          //ignore concurrent problem.
          Set<Invoker<?>> callbackInvokers = (Set<Invoker<?>>) channel
              .getAttribute(Constants.CHANNEL_CALLBACK_KEY);
          if (callbackInvokers == null) {
            callbackInvokers = new ConcurrentHashSet<>(1);
            callbackInvokers.add(invoker);
            channel.setAttribute(Constants.CHANNEL_CALLBACK_KEY, callbackInvokers);
          }
          logger.info(
              "method " + inv.getMethodName() + " include a callback service :" + invoker.getUrl()
                  + ", a proxy :" + invoker + " has been created.");
        }
      }
    } else {
      if (proxy != null) {
        Invoker<?> invoker = (Invoker<?>) channel.getAttribute(invokerCacheKey);
        try {
          Set<Invoker<?>> callbackInvokers = (Set<Invoker<?>>) channel
              .getAttribute(Constants.CHANNEL_CALLBACK_KEY);
          if (callbackInvokers != null) {
            callbackInvokers.remove(invoker);
          }
          invoker.destroy();
        } catch (Exception e) {
          logger.error(e.getMessage(), e);
        }
        // cancel refer, directly remove from the map
        channel.removeAttribute(proxyCacheKey);
        channel.removeAttribute(invokerCacheKey);
        decreaseInstanceCount(channel, countkey);
      }
    }
    return proxy;
  }
}