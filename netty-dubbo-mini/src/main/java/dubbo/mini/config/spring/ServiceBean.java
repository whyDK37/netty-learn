package dubbo.mini.config.spring;

import dubbo.mini.common.Constants;
import dubbo.mini.common.NetURL;
import dubbo.mini.common.utils.StringUtils;
import dubbo.mini.config.ConfigManager;
import dubbo.mini.config.ProtocolConfig;
import dubbo.mini.config.RegistryConfig;
import dubbo.mini.protocol.Exporter;
import dubbo.mini.protocol.Protocol;
import dubbo.mini.rpc.DelegateProviderMetaDataInvoker;
import dubbo.mini.rpc.Invoker;
import dubbo.mini.rpc.ProxyFactory;
import dubbo.mini.rpc.model.ApplicationModel;
import dubbo.mini.rpc.model.ProviderModel;
import dubbo.mini.support.ExtensionLoader;
import dubbo.mini.support.NamedThreadFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * @author why
 */
public class ServiceBean<T> implements InitializingBean, DisposableBean,
    ApplicationContextAware, ApplicationListener<ContextRefreshedEvent>, BeanNameAware {

  private Logger logger = LoggerFactory.getLogger(ServiceBean.class);

  private static final Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class)
      .getDefaultExtension();

  private static final ProxyFactory proxyFactory = ExtensionLoader
      .getExtensionLoader(ProxyFactory.class).getDefaultExtension();

  private final List<Exporter<?>> exporters = new ArrayList<>();

  private static final ScheduledExecutorService delayExportExecutor = Executors
      .newSingleThreadScheduledExecutor(
          new NamedThreadFactory("ServiceDelayExporter", true));


  protected String interfaceName;
  protected Class<?> interfaceClass;

  private String path;
  private transient volatile boolean exported;

  private transient volatile boolean unexported;

  private transient ApplicationContext applicationContext;

  private transient String beanName;
  protected Boolean export = true;

  protected Integer delay = 3 * 100;

  protected List<RegistryConfig> registries;

  private final List<NetURL> urls = new ArrayList<NetURL>();
  private List<ProtocolConfig> protocols;

  private T ref;

  @Override
  public void setBeanName(String name) {
    this.beanName = name;
  }

  @Override
  public void destroy() throws Exception {

  }

  public T getRef() {
    return ref;
  }

  public void setRef(T ref) {
    this.ref = ref;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    Map<String, ProtocolConfig> protocolConfigMap = applicationContext
        .getBeansOfType(ProtocolConfig.class);
    if (protocolConfigMap.isEmpty()) {
      // fixme throw exception
    }
    setProtocols(protocolConfigMap.values());

    export();
  }

  private void export() {
    if (!shouldExport()) {
      return;
    }

    if (shouldDelay()) {
      delayExportExecutor.schedule(this::doExport, delay, TimeUnit.MILLISECONDS);
    } else {
      doExport();
    }
  }

  private void doExport() {
    if (unexported) {
      throw new IllegalStateException(
          "The service " + interfaceClass.getName() + " has already unexported!");
    }
    if (exported) {
      return;
    }
    exported = true;

    if (StringUtils.isEmpty(path)) {
      path = interfaceName;
    }
    doExportUrls();
  }

  private void doExportUrls() {
    for (ProtocolConfig protocolConfig : protocols) {
      String pathKey = NetURL
          .buildKey(getContextPath(protocolConfig).map(p -> p + "/" + path).orElse(path));
      ProviderModel providerModel = new ProviderModel(pathKey, ref, interfaceClass);
      ApplicationModel.initProviderModel(pathKey, providerModel);
      doExportUrlsFor1Protocol(protocolConfig);
    }
  }


  @SuppressWarnings({"unchecked"})
  public void setProtocols(Collection<? extends ProtocolConfig> protocols) {
    ConfigManager.getInstance().addProtocols((List<ProtocolConfig>) protocols);
    this.protocols = (List<ProtocolConfig>) protocols;
  }

  private void doExportUrlsFor1Protocol(ProtocolConfig protocolConfig) {
    String name = protocolConfig.getName();
    if (StringUtils.isEmpty(name)) {
      name = Constants.DUBBO;
    }

    Map<String, String> map = new HashMap<String, String>();
    map.put(Constants.SIDE_KEY, Constants.PROVIDER_SIDE);

    // export service
    String host = protocolConfig
        .getHost();//this.findConfigedHosts(protocolConfig, registryURLs, map);
    Integer port = protocolConfig.getPort();//this.findConfigedPorts(protocolConfig, name, map);
    NetURL url = new NetURL(name, host, port,
        getContextPath(protocolConfig).map(p -> p + "/" + path).orElse(path), map);

    String scope = url.getParameter(Constants.SCOPE_KEY);
    // don't export when none is configured
    if (!Constants.SCOPE_NONE.equalsIgnoreCase(scope)) {
      // export to remote if the config is not local (export to local only when config is local)
      if (!Constants.SCOPE_LOCAL.equalsIgnoreCase(scope)) {
        if (logger.isInfoEnabled()) {
          logger.info("Export dubbo service " + interfaceClass.getName() + " to url " + url);
        }
        Invoker<?> invoker = proxyFactory.getInvoker(ref, (Class) interfaceClass, url);
        DelegateProviderMetaDataInvoker<T> wrapperInvoker = new DelegateProviderMetaDataInvoker(
            invoker, this);

        Exporter<?> exporter = protocol.export(wrapperInvoker);
        exporters.add(exporter);
      }
    }
    this.urls.add(url);
  }

  private Optional<String> getContextPath(ProtocolConfig protocolConfig) {
    String contextPath = protocolConfig.getContextpath();
    return Optional.ofNullable(contextPath);
  }

  private boolean shouldDelay() {
    Integer delay = getDelay();
    return delay != null && delay > 0;
  }

  private Integer getDelay() {
    return this.delay;
  }

  private boolean shouldExport() {
    Boolean shouldExport = getExport();

    // default value is true
    if (shouldExport == null) {
      return true;
    }

    return shouldExport;
  }


  public Boolean getExport() {
    return export;
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  @Override
  public void onApplicationEvent(ContextRefreshedEvent event) {
    if (!isExported() && !isUnexported()) {
      if (logger.isInfoEnabled()) {
        logger.info("The service ready on spring started. service: " + getInterface());
      }
      export();
    }
  }

  public boolean isExported() {
    return exported;
  }

  public boolean isUnexported() {
    return unexported;
  }

  public String getInterface() {
    return interfaceName;
  }
}
