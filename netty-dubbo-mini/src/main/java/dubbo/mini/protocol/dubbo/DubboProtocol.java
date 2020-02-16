package dubbo.mini.protocol.dubbo;

import dubbo.mini.common.Constants;
import dubbo.mini.common.NetURL;
import dubbo.mini.common.URLBuilder;
import dubbo.mini.common.utils.CollectionUtils;
import dubbo.mini.common.utils.ConfigUtils;
import dubbo.mini.common.utils.NetUtils;
import dubbo.mini.common.utils.StringUtils;
import dubbo.mini.exception.RpcException;
import dubbo.mini.exchange.ExchangeChannel;
import dubbo.mini.exchange.ExchangeClient;
import dubbo.mini.exchange.ExchangeHandler;
import dubbo.mini.exchange.ExchangeHandlerAdapter;
import dubbo.mini.exchange.ExchangeServer;
import dubbo.mini.exchange.Exchangers;
import dubbo.mini.protocol.AbstractProtocol;
import dubbo.mini.protocol.DubboExporter;
import dubbo.mini.protocol.DubboInvoker;
import dubbo.mini.protocol.Exporter;
import dubbo.mini.protocol.Protocol;
import dubbo.mini.remote.NetChannel;
import dubbo.mini.remote.RemotingException;
import dubbo.mini.remote.Transporter;
import dubbo.mini.rpc.Invocation;
import dubbo.mini.rpc.Invoker;
import dubbo.mini.rpc.Result;
import dubbo.mini.rpc.RpcContext;
import dubbo.mini.rpc.RpcInvocation;
import dubbo.mini.support.ExtensionLoader;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author why
 */
public class DubboProtocol extends AbstractProtocol {

  public static final String NAME = "dubbo";
  public static final String CODEC_NAME = "dubbo";

  private static DubboProtocol INSTANCE;

  private static final String IS_CALLBACK_SERVICE_INVOKE = "_isCallBackServiceInvoke";
  public static final int DEFAULT_PORT = 20880;
  private final Map<String, ExchangeServer> serverMap = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, Object> locks = new ConcurrentHashMap<>();

  public DubboProtocol() {
    INSTANCE = this;
  }

  public static DubboProtocol getDubboProtocol() {
    if (INSTANCE == null) {
      // load
      ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(DubboProtocol.NAME);
    }

    return INSTANCE;
  }


  private ExchangeHandler requestHandler = new ExchangeHandlerAdapter() {

    @Override
    public CompletableFuture<Object> reply(ExchangeChannel channel, Object message)
        throws RemotingException {

      if (!(message instanceof Invocation)) {
        throw new RemotingException(channel, "Unsupported request: "
            + (message == null ? null : (message.getClass().getName() + ": " + message))
            + ", channel: consumer: " + channel.getRemoteAddress() + " --> provider: " + channel
            .getLocalAddress());
      }

      Invocation inv = (Invocation) message;
      Invoker<?> invoker = getInvoker(channel, inv);
      // need to consider backward-compatibility if it's a callback
      if (Boolean.TRUE.toString().equals(inv.getAttachments().get(IS_CALLBACK_SERVICE_INVOKE))) {
        String methodsStr = invoker.getUrl().getParameters().get("methods");
        boolean hasMethod = false;
        if (methodsStr == null || !methodsStr.contains(",")) {
          hasMethod = inv.getMethodName().equals(methodsStr);
        } else {
          String[] methods = methodsStr.split(",");
          for (String method : methods) {
            if (inv.getMethodName().equals(method)) {
              hasMethod = true;
              break;
            }
          }
        }
        if (!hasMethod) {
          logger.warn(new IllegalStateException("The methodName " + inv.getMethodName()
              + " not found in callback service interface ,invoke will be ignored."
              + " please update the api interface. url is:"
              + invoker.getUrl()) + " ,invocation is :" + inv);
          return null;
        }
      }
      RpcContext rpcContext = RpcContext.getContext();
      rpcContext.setRemoteAddress(channel.getRemoteAddress());
      Result result = invoker.invoke(inv);

      return CompletableFuture.completedFuture(result);

    }

    @Override
    public void received(NetChannel channel, Object message) throws RemotingException {
      if (message instanceof Invocation) {
        reply((ExchangeChannel) channel, message);

      } else {
        super.received(channel, message);
      }
    }

    @Override
    public void connected(NetChannel channel) throws RemotingException {
      invoke(channel, Constants.ON_CONNECT_KEY);
    }

    @Override
    public void disconnected(NetChannel channel) throws RemotingException {
      if (logger.isDebugEnabled()) {
        logger
            .debug("disconnected from " + channel.getRemoteAddress() + ",url:" + channel.getUrl());
      }
      invoke(channel, Constants.ON_DISCONNECT_KEY);
    }

    private void invoke(NetChannel channel, String methodKey) {
      Invocation invocation = createInvocation(channel, channel.getUrl(), methodKey);
      if (invocation != null) {
        try {
          received(channel, invocation);
        } catch (Throwable t) {
          logger.warn(
              "Failed to invoke event method " + invocation.getMethodName() + "(), cause: " + t
                  .getMessage(), t);
        }
      }
    }

    private Invocation createInvocation(NetChannel channel, NetURL url, String methodKey) {
      String method = url.getParameter(methodKey);
      if (method == null || method.length() == 0) {
        return null;
      }

      RpcInvocation invocation = new RpcInvocation(method, new Class<?>[0], new Object[0]);
      invocation.setAttachment(Constants.PATH_KEY, url.getPath());
      invocation.setAttachment(Constants.GROUP_KEY, url.getParameter(Constants.GROUP_KEY));
      invocation.setAttachment(Constants.INTERFACE_KEY, url.getParameter(Constants.INTERFACE_KEY));
      invocation.setAttachment(Constants.VERSION_KEY, url.getParameter(Constants.VERSION_KEY));
      if (url.getParameter(Constants.STUB_EVENT_KEY, false)) {
        invocation.setAttachment(Constants.STUB_EVENT_KEY, Boolean.TRUE.toString());
      }

      return invocation;
    }
  };

  @Override
  public int getDefaultPort() {
    return DEFAULT_PORT;
  }

  @Override
  public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {

    NetURL url = invoker.getUrl();

    // export service.
    String key = serviceKey(url);
    DubboExporter<T> exporter = new DubboExporter<T>(invoker, key, exporterMap);
    exporterMap.put(key, exporter);

    openServer(url);

    return exporter;
  }


  private void openServer(NetURL url) {
    // find server.
    String key = url.getAddress();
    //client can export a service which's only for server to invoke
    boolean isServer = url.getParameter(Constants.IS_SERVER_KEY, true);
    if (isServer) {
      ExchangeServer server = serverMap.get(key);
      if (server == null) {
        synchronized (this) {
          server = serverMap.get(key);
          if (server == null) {
            serverMap.put(key, createServer(url));
          }
        }
      } else {
        // server supports reset, use together with override
        server.reset(url);
      }
    }
  }


  protected ExchangeServer createServer(NetURL url) {
    url = URLBuilder.from(url)
        // send readonly event when server closes, it's enabled by default
        .addParameterIfAbsent(Constants.CHANNEL_READONLYEVENT_SENT_KEY, Boolean.TRUE.toString())
        // enable heartbeat by default
        .addParameterIfAbsent(Constants.HEARTBEAT_KEY, String.valueOf(Constants.DEFAULT_HEARTBEAT))
        .addParameter(Constants.CODEC_KEY, CODEC_NAME)
        .build();
    String str = url.getParameter(Constants.SERVER_KEY, Constants.DEFAULT_REMOTING_SERVER);

    if (str != null && str.length() > 0 && !ExtensionLoader.getExtensionLoader(Transporter.class)
        .hasExtension(str)) {
      throw new RpcException("Unsupported server type: " + str + ", url: " + url);
    }

    ExchangeServer server;
    try {
      server = Exchangers.bind(url, requestHandler);
    } catch (RemotingException e) {
      throw new RpcException("Fail to start server(url: " + url + ") " + e.getMessage(), e);
    }

    str = url.getParameter(Constants.CLIENT_KEY);
    if (str != null && str.length() > 0) {
      Set<String> supportedTypes = ExtensionLoader.getExtensionLoader(Transporter.class)
          .getSupportedExtensions();
      if (!supportedTypes.contains(str)) {
        throw new RpcException("Unsupported client type: " + str);
      }
    }

    return server;
  }


  Invoker<?> getInvoker(NetChannel channel, Invocation inv) throws RemotingException {
    boolean isCallBackServiceInvoke = false;
    boolean isStubServiceInvoke = false;
    int port = channel.getLocalAddress().getPort();
    String path = inv.getAttachments().get(Constants.PATH_KEY);

    // if it's callback service on client side
    isStubServiceInvoke = Boolean.TRUE.toString()
        .equals(inv.getAttachments().get(Constants.STUB_EVENT_KEY));
    if (isStubServiceInvoke) {
      port = channel.getRemoteAddress().getPort();
    }

    //callback
    isCallBackServiceInvoke = isClientSide(channel) && !isStubServiceInvoke;
    if (isCallBackServiceInvoke) {
      path += "." + inv.getAttachments().get(Constants.CALLBACK_SERVICE_KEY);
      inv.getAttachments().put(IS_CALLBACK_SERVICE_INVOKE, Boolean.TRUE.toString());
    }

    String serviceKey = serviceKey(port, path, inv.getAttachments().get(Constants.VERSION_KEY),
        inv.getAttachments().get(Constants.GROUP_KEY));
    DubboExporter<?> exporter = (DubboExporter<?>) exporterMap.get(serviceKey);

    if (exporter == null) {
      throw new RemotingException(channel,
          "Not found exported service: " + serviceKey + " in " + exporterMap.keySet()
              + ", may be version or group mismatch " +
              ", channel: consumer: " + channel.getRemoteAddress() + " --> provider: " + channel
              .getLocalAddress() + ", message:" + inv);
    }

    return exporter.getInvoker();
  }


  private boolean isClientSide(NetChannel channel) {
    InetSocketAddress address = channel.getRemoteAddress();
    NetURL url = channel.getUrl();
    return url.getPort() == address.getPort() &&
        NetUtils.filterLocalHost(channel.getUrl().getIp())
            .equals(NetUtils.filterLocalHost(address.getAddress().getHostAddress()));
  }

  @Override
  public <T> Invoker<T> refer(Class<T> serviceType, NetURL url) throws RpcException {
    // create rpc invoker.
    DubboInvoker<T> invoker = new DubboInvoker<T>(serviceType, url, getClients(url), invokers);
    invokers.add(invoker);

    return invoker;
  }


  private ExchangeClient[] getClients(NetURL url) {
    // whether to share connection

    boolean useShareConnect = false;

    int connections = url.getParameter(Constants.CONNECTIONS_KEY, 0);
    List<ReferenceCountExchangeClient> shareClients = null;
    // if not configured, connection is shared, otherwise, one connection for one service
    if (connections == 0) {
      useShareConnect = true;

      /**
       * The xml configuration should have a higher priority than properties.
       */
      String shareConnectionsStr = url.getParameter(Constants.SHARE_CONNECTIONS_KEY, (String) null);
      connections = Integer.parseInt(StringUtils.isEmpty(shareConnectionsStr) ? ConfigUtils
          .getProperty(Constants.SHARE_CONNECTIONS_KEY,
              Constants.DEFAULT_SHARE_CONNECTIONS) : shareConnectionsStr);
      shareClients = getSharedClient(url, connections);
    }

    ExchangeClient[] clients = new ExchangeClient[connections];
    for (int i = 0; i < clients.length; i++) {
      if (useShareConnect) {
        clients[i] = shareClients.get(i);

      } else {
        clients[i] = initClient(url);
      }
    }

    return clients;
  }

  private final Map<String, List<ReferenceCountExchangeClient>> referenceClientMap = new ConcurrentHashMap<>();

  private List<ReferenceCountExchangeClient> getSharedClient(NetURL url, int connectNum) {
    String key = url.getAddress();
    List<ReferenceCountExchangeClient> clients = referenceClientMap.get(key);

    if (checkClientCanUse(clients)) {
      batchClientRefIncr(clients);
      return clients;
    }

    locks.putIfAbsent(key, new Object());
    synchronized (locks.get(key)) {
      clients = referenceClientMap.get(key);
      // dubbo check
      if (checkClientCanUse(clients)) {
        batchClientRefIncr(clients);
        return clients;
      }

      // connectNum must be greater than or equal to 1
      connectNum = Math.max(connectNum, 1);

      // If the clients is empty, then the first initialization is
      if (CollectionUtils.isEmpty(clients)) {
        clients = buildReferenceCountExchangeClientList(url, connectNum);
        referenceClientMap.put(key, clients);

      } else {
        for (int i = 0; i < clients.size(); i++) {
          ReferenceCountExchangeClient referenceCountExchangeClient = clients.get(i);
          // If there is a client in the list that is no longer available, create a new one to replace him.
          if (referenceCountExchangeClient == null || referenceCountExchangeClient.isClosed()) {
            clients.set(i, buildReferenceCountExchangeClient(url));
            continue;
          }

          referenceCountExchangeClient.incrementAndGetCount();
        }
      }

      /**
       * I understand that the purpose of the remove operation here is to avoid the expired url key
       * always occupying this memory space.
       */
      locks.remove(key);

      return clients;
    }
  }


  private boolean checkClientCanUse(
      List<ReferenceCountExchangeClient> referenceCountExchangeClients) {
    if (CollectionUtils.isEmpty(referenceCountExchangeClients)) {
      return false;
    }

    for (ReferenceCountExchangeClient referenceCountExchangeClient : referenceCountExchangeClients) {
      // As long as one client is not available, you need to replace the unavailable client with the available one.
      if (referenceCountExchangeClient == null || referenceCountExchangeClient.isClosed()) {
        return false;
      }
    }

    return true;
  }


  private void batchClientRefIncr(
      List<ReferenceCountExchangeClient> referenceCountExchangeClients) {
    if (CollectionUtils.isEmpty(referenceCountExchangeClients)) {
      return;
    }

    for (ReferenceCountExchangeClient referenceCountExchangeClient : referenceCountExchangeClients) {
      if (referenceCountExchangeClient != null) {
        referenceCountExchangeClient.incrementAndGetCount();
      }
    }
  }


  private List<ReferenceCountExchangeClient> buildReferenceCountExchangeClientList(NetURL url,
      int connectNum) {
    List<ReferenceCountExchangeClient> clients = new CopyOnWriteArrayList<>();

    for (int i = 0; i < connectNum; i++) {
      clients.add(buildReferenceCountExchangeClient(url));
    }

    return clients;
  }


  private ReferenceCountExchangeClient buildReferenceCountExchangeClient(NetURL url) {
    ExchangeClient exchangeClient = initClient(url);

    return new ReferenceCountExchangeClient(exchangeClient);
  }


  private ExchangeClient initClient(NetURL url) {

    // client type setting.
    String str = url.getParameter(Constants.CLIENT_KEY,
        url.getParameter(Constants.SERVER_KEY, Constants.DEFAULT_REMOTING_CLIENT));

    url = url.addParameter(Constants.CODEC_KEY, DubboCodec.NAME);
    // enable heartbeat by default
    url = url
        .addParameterIfAbsent(Constants.HEARTBEAT_KEY, String.valueOf(Constants.DEFAULT_HEARTBEAT));

    // BIO is not allowed since it has severe performance issue.
    if (str != null && str.length() > 0 && !ExtensionLoader.getExtensionLoader(Transporter.class)
        .hasExtension(str)) {
      throw new RpcException("Unsupported client type: " + str + "," +
          " supported client type is " + StringUtils
          .join(ExtensionLoader.getExtensionLoader(Transporter.class).getSupportedExtensions(),
              " "));
    }

    ExchangeClient client;
    try {
      // connection should be lazy
      if (url.getParameter(Constants.LAZY_CONNECT_KEY, false)) {
        client = new LazyConnectExchangeClient(url, requestHandler);

      } else {
        client = Exchangers.connect(url, requestHandler);
      }

    } catch (RemotingException e) {
      throw new RpcException(
          "Fail to create remoting client for service(" + url + "): " + e.getMessage(), e);
    }

    return client;
  }
}
