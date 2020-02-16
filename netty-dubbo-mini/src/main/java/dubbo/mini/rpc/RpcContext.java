package dubbo.mini.rpc;

import dubbo.mini.common.Constants;
import dubbo.mini.common.NetURL;
import dubbo.mini.common.utils.CollectionUtils;
import dubbo.mini.common.utils.NetUtils;
import dubbo.mini.common.utils.StringUtils;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class RpcContext {

  // FIXME REQUEST_CONTEXT
  private static final ThreadLocal<RpcContext> LOCAL = new ThreadLocal<RpcContext>() {
    @Override
    protected RpcContext initialValue() {
      return new RpcContext();
    }
  };

  // FIXME RESPONSE_CONTEXT
  private static final ThreadLocal<RpcContext> SERVER_LOCAL = new ThreadLocal<RpcContext>() {
    @Override
    protected RpcContext initialValue() {
      return new RpcContext();
    }
  };

  private final Map<String, String> attachments = new HashMap<String, String>();
  private final Map<String, Object> values = new HashMap<String, Object>();
  private Future<?> future;

  private List<NetURL> urls;

  private NetURL url;

  private String methodName;

  private Class<?>[] parameterTypes;

  private Object[] arguments;

  private InetSocketAddress localAddress;

  private InetSocketAddress remoteAddress;

  private String remoteApplicationName;

  @Deprecated
  private List<Invoker<?>> invokers;
  @Deprecated
  private Invoker<?> invoker;
  @Deprecated
  private Invocation invocation;

  // now we don't use the 'values' map to hold these objects
  // we want these objects to be as generic as possible
  private Object request;
  private Object response;

  protected RpcContext() {
  }

  /**
   * get server side context.
   *
   * @return server context
   */
  public static RpcContext getServerContext() {
    return SERVER_LOCAL.get();
  }

  public static void restoreServerContext(RpcContext oldServerContext) {
    SERVER_LOCAL.set(oldServerContext);
  }

  /**
   * get context.
   *
   * @return context
   */
  public static RpcContext getContext() {
    return LOCAL.get();
  }

  public static void restoreContext(RpcContext oldContext) {
    LOCAL.set(oldContext);
  }


  public RpcContext copyOf() {
    RpcContext copy = new RpcContext();
    copy.attachments.putAll(this.attachments);
    copy.values.putAll(this.values);
    copy.future = this.future;
    copy.urls = this.urls;
    copy.url = this.url;
    copy.methodName = this.methodName;
    copy.parameterTypes = this.parameterTypes;
    copy.arguments = this.arguments;
    copy.localAddress = this.localAddress;
    copy.remoteAddress = this.remoteAddress;
    copy.remoteApplicationName = this.remoteApplicationName;
    copy.invokers = this.invokers;
    copy.invoker = this.invoker;
    copy.invocation = this.invocation;

    copy.request = this.request;
    copy.response = this.response;

    return copy;
  }


  /**
   * remove context.
   */
  public static void removeContext() {
    LOCAL.remove();
  }

  /**
   * Get the request object of the underlying RPC protocol, e.g. HttpServletRequest
   *
   * @return null if the underlying protocol doesn't provide support for getting request
   */
  public Object getRequest() {
    return request;
  }

  public void setRequest(Object request) {
    this.request = request;
  }

  /**
   * Get the request object of the underlying RPC protocol, e.g. HttpServletRequest
   *
   * @return null if the underlying protocol doesn't provide support for getting request or the
   * request is not of the specified type
   */
  @SuppressWarnings("unchecked")
  public <T> T getRequest(Class<T> clazz) {
    return (request != null && clazz.isAssignableFrom(request.getClass())) ? (T) request : null;
  }

  /**
   * Get the response object of the underlying RPC protocol, e.g. HttpServletResponse
   *
   * @return null if the underlying protocol doesn't provide support for getting response
   */
  public Object getResponse() {
    return response;
  }

  public void setResponse(Object response) {
    this.response = response;
  }

  /**
   * Get the response object of the underlying RPC protocol, e.g. HttpServletResponse
   *
   * @return null if the underlying protocol doesn't provide support for getting response or the
   * response is not of the specified type
   */
  @SuppressWarnings("unchecked")
  public <T> T getResponse(Class<T> clazz) {
    return (response != null && clazz.isAssignableFrom(response.getClass())) ? (T) response : null;
  }

  /**
   * is provider side.
   *
   * @return provider side.
   */
  public boolean isProviderSide() {
    return !isConsumerSide();
  }

  /**
   * is consumer side.
   *
   * @return consumer side.
   */
  public boolean isConsumerSide() {
    return getUrl().getParameter(Constants.SIDE_KEY, Constants.PROVIDER_SIDE)
        .equals(Constants.CONSUMER_SIDE);
  }

  /**
   * get CompletableFuture.
   *
   * @param <T>
   * @return future
   */
  @SuppressWarnings("unchecked")
  public <T> CompletableFuture<T> getCompletableFuture() {
    return (CompletableFuture<T>) future;
  }

  /**
   * get future.
   *
   * @param <T>
   * @return future
   */
  @SuppressWarnings("unchecked")
  public <T> Future<T> getFuture() {
    return (Future<T>) future;
  }

  /**
   * set future.
   *
   * @param future
   */
  public void setFuture(Future<?> future) {
    this.future = future;
  }

  public List<NetURL> getUrls() {
    return urls == null && url != null ? (List<NetURL>) Arrays.asList(url) : urls;
  }

  public void setUrls(List<NetURL> urls) {
    this.urls = urls;
  }

  public NetURL getUrl() {
    return url;
  }

  public void setUrl(NetURL url) {
    this.url = url;
  }

  /**
   * get method name.
   *
   * @return method name.
   */
  public String getMethodName() {
    return methodName;
  }

  public void setMethodName(String methodName) {
    this.methodName = methodName;
  }

  /**
   * get parameter types.
   *
   * @serial
   */
  public Class<?>[] getParameterTypes() {
    return parameterTypes;
  }

  public void setParameterTypes(Class<?>[] parameterTypes) {
    this.parameterTypes = parameterTypes;
  }

  /**
   * get arguments.
   *
   * @return arguments.
   */
  public Object[] getArguments() {
    return arguments;
  }

  public void setArguments(Object[] arguments) {
    this.arguments = arguments;
  }

  /**
   * set local address.
   *
   * @param host
   * @param port
   * @return context
   */
  public RpcContext setLocalAddress(String host, int port) {
    if (port < 0) {
      port = 0;
    }
    this.localAddress = InetSocketAddress.createUnresolved(host, port);
    return this;
  }

  /**
   * get local address.
   *
   * @return local address
   */
  public InetSocketAddress getLocalAddress() {
    return localAddress;
  }

  /**
   * set local address.
   *
   * @param address
   * @return context
   */
  public RpcContext setLocalAddress(InetSocketAddress address) {
    this.localAddress = address;
    return this;
  }

  public String getLocalAddressString() {
    return getLocalHost() + ":" + getLocalPort();
  }

  /**
   * get local host name.
   *
   * @return local host name
   */
  public String getLocalHostName() {
    String host = localAddress == null ? null : localAddress.getHostName();
    if (StringUtils.isEmpty(host)) {
      return getLocalHost();
    }
    return host;
  }

  /**
   * set remote address.
   *
   * @param host
   * @param port
   * @return context
   */
  public RpcContext setRemoteAddress(String host, int port) {
    if (port < 0) {
      port = 0;
    }
    this.remoteAddress = InetSocketAddress.createUnresolved(host, port);
    return this;
  }

  /**
   * get remote address.
   *
   * @return remote address
   */
  public InetSocketAddress getRemoteAddress() {
    return remoteAddress;
  }

  /**
   * set remote address.
   *
   * @param address
   * @return context
   */
  public RpcContext setRemoteAddress(InetSocketAddress address) {
    this.remoteAddress = address;
    return this;
  }

  public String getRemoteApplicationName() {
    return remoteApplicationName;
  }

  public RpcContext setRemoteApplicationName(String remoteApplicationName) {
    this.remoteApplicationName = remoteApplicationName;
    return this;
  }

  /**
   * get remote address string.
   *
   * @return remote address string.
   */
  public String getRemoteAddressString() {
    return getRemoteHost() + ":" + getRemotePort();
  }

  /**
   * get remote host name.
   *
   * @return remote host name
   */
  public String getRemoteHostName() {
    return remoteAddress == null ? null : remoteAddress.getHostName();
  }

  /**
   * get local host.
   *
   * @return local host
   */
  public String getLocalHost() {
    String host = localAddress == null ? null :
        localAddress.getAddress() == null ? localAddress.getHostName()
            : NetUtils.filterLocalHost(localAddress.getAddress().getHostAddress());
    if (host == null || host.length() == 0) {
      return NetUtils.getLocalHost();
    }
    return host;
  }

  /**
   * get local port.
   *
   * @return port
   */
  public int getLocalPort() {
    return localAddress == null ? 0 : localAddress.getPort();
  }

  /**
   * get remote host.
   *
   * @return remote host
   */
  public String getRemoteHost() {
    return remoteAddress == null ? null :
        remoteAddress.getAddress() == null ? remoteAddress.getHostName()
            : NetUtils.filterLocalHost(remoteAddress.getAddress().getHostAddress());
  }

  /**
   * get remote port.
   *
   * @return remote port
   */
  public int getRemotePort() {
    return remoteAddress == null ? 0 : remoteAddress.getPort();
  }

  /**
   * get attachment.
   *
   * @param key
   * @return attachment
   */
  public String getAttachment(String key) {
    return attachments.get(key);
  }

  /**
   * set attachment.
   *
   * @param key
   * @param value
   * @return context
   */
  public RpcContext setAttachment(String key, String value) {
    if (value == null) {
      attachments.remove(key);
    } else {
      attachments.put(key, value);
    }
    return this;
  }

  /**
   * remove attachment.
   *
   * @param key
   * @return context
   */
  public RpcContext removeAttachment(String key) {
    attachments.remove(key);
    return this;
  }

  /**
   * get attachments.
   *
   * @return attachments
   */
  public Map<String, String> getAttachments() {
    return attachments;
  }

  /**
   * set attachments
   *
   * @param attachment
   * @return context
   */
  public RpcContext setAttachments(Map<String, String> attachment) {
    this.attachments.clear();
    if (attachment != null && attachment.size() > 0) {
      this.attachments.putAll(attachment);
    }
    return this;
  }

  public void clearAttachments() {
    this.attachments.clear();
  }

  /**
   * get values.
   *
   * @return values
   */
  public Map<String, Object> get() {
    return values;
  }

  /**
   * set value.
   *
   * @param key
   * @param value
   * @return context
   */
  public RpcContext set(String key, Object value) {
    if (value == null) {
      values.remove(key);
    } else {
      values.put(key, value);
    }
    return this;
  }

  /**
   * remove value.
   *
   * @param key
   * @return value
   */
  public RpcContext remove(String key) {
    values.remove(key);
    return this;
  }

  /**
   * get value.
   *
   * @param key
   * @return value
   */
  public Object get(String key) {
    return values.get(key);
  }

  /**
   * @deprecated Replace to isProviderSide()
   */
  @Deprecated
  public boolean isServerSide() {
    return isProviderSide();
  }

  /**
   * @deprecated Replace to isConsumerSide()
   */
  @Deprecated
  public boolean isClientSide() {
    return isConsumerSide();
  }

  /**
   * @deprecated Replace to getUrls()
   */
  @Deprecated
  @SuppressWarnings({"unchecked", "rawtypes"})
  public List<Invoker<?>> getInvokers() {
    return invokers == null && invoker != null ? (List) Arrays.asList(invoker) : invokers;
  }

  public RpcContext setInvokers(List<Invoker<?>> invokers) {
    this.invokers = invokers;
    if (CollectionUtils.isNotEmpty(invokers)) {
      List<NetURL> urls = new ArrayList<NetURL>(invokers.size());
      for (Invoker<?> invoker : invokers) {
        urls.add(invoker.getUrl());
      }
      setUrls(urls);
    }
    return this;
  }

  /**
   * @deprecated Replace to getUrl()
   */
  @Deprecated
  public Invoker<?> getInvoker() {
    return invoker;
  }

  public RpcContext setInvoker(Invoker<?> invoker) {
    this.invoker = invoker;
    if (invoker != null) {
      setUrl(invoker.getUrl());
    }
    return this;
  }

  /**
   * @deprecated Replace to getMethodName(), getParameterTypes(), getArguments()
   */
  @Deprecated
  public Invocation getInvocation() {
    return invocation;
  }

  public RpcContext setInvocation(Invocation invocation) {
    this.invocation = invocation;
    if (invocation != null) {
      setMethodName(invocation.getMethodName());
      setParameterTypes(invocation.getParameterTypes());
      setArguments(invocation.getArguments());
    }
    return this;
  }

}
