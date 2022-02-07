package dubbo.mini.config.spring;

import dubbo.mini.common.Constants;
import dubbo.mini.common.NetURL;
import dubbo.mini.common.utils.NetUtils;
import dubbo.mini.protocol.ProtocolAdaptive;
import dubbo.mini.rpc.Invoker;
import dubbo.mini.rpc.ProxyFactory;
import dubbo.mini.rpc.model.ApplicationModel;
import dubbo.mini.rpc.model.ConsumerModel;
import dubbo.mini.support.ExtensionLoader;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @author why
 */
public class ReferenceBean<T> implements FactoryBean, ApplicationContextAware, InitializingBean,
    DisposableBean {

  private static Logger logger = LoggerFactory.getLogger(ReferenceBean.class);

  private T ref;

  private String interfaceName;
  private Class<?> interfaceClass;

  private static final ProxyFactory proxyFactory = ExtensionLoader
      .getExtensionLoader(ProxyFactory.class).getDefaultExtension();
  /**
   * The invoker of the reference service
   */
  private String urlStr;
  private transient volatile Invoker<?> invoker;

  private transient volatile boolean destroyed;
  private transient volatile boolean initialized;

  public void setUrl(String url) {
    this.urlStr = url;
  }

  public synchronized T get() {
    checkAndUpdateSubConfigs();

    if (destroyed) {
      throw new IllegalStateException(
          "The invoker of ReferenceConfig(" + urlStr + ") has already destroyed!");
    }
    if (ref == null) {
      init();
    }
    return ref;
  }

  public void checkAndUpdateSubConfigs() {
    try {
      interfaceClass = Class.forName(interfaceName, true, Thread.currentThread()
          .getContextClassLoader());
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }


  public void setInterface(Class<?> interfaceClass) {
    if (interfaceClass != null && !interfaceClass.isInterface()) {
      throw new IllegalStateException(
          "The interface class " + interfaceClass + " is not a interface!");
    }
    this.interfaceClass = interfaceClass;
    setInterface(interfaceClass == null ? null : interfaceClass.getName());
  }

  public void setInterface(String interfaceName) {
    this.interfaceName = interfaceName;
  }

  private void init() {
    if (initialized) {
      return;
    }
    initialized = true;

    Map<String, String> map = new HashMap<>();
    map.put(Constants.SIDE_KEY, Constants.CONSUMER_SIDE);
    map.put(Constants.INTERFACE_KEY, interfaceName);
    ref = createProxy(map);

    String serviceKey = NetURL.buildKey(interfaceName);
    ApplicationModel.initConsumerModel(serviceKey, buildConsumerModel(serviceKey));
  }

  private T createProxy(Map<String, String> map) {
    if (shouldJvmRefer(map)) {
      NetURL url = new NetURL(Constants.LOCAL_PROTOCOL, Constants.LOCALHOST_VALUE, 0,
          interfaceClass.getName(), map);
//            invoker = refprotocol.refer(interfaceClass, url);
//            if (logger.isInfoEnabled()) {
//                logger.info("Using injvm service " + interfaceClass.getName());
//            }
    } else {
      NetURL url;
      if (urlStr != null && urlStr.length()
          > 0) { // user specified URL, could be peer-to-peer address, or register center's address.
        url = NetURL.valueOf(urlStr);
      } else { // assemble URL from register center's configuration
        throw new IllegalStateException(
            "No url to reference " + interfaceName + " on the consumer " + NetUtils.getLocalHost());
      }
      invoker = ProtocolAdaptive.getInstance().refer(interfaceClass, url);
    }
    if (logger.isInfoEnabled()) {
      logger.info("Refer dubbo service " + interfaceName + " from url " + invoker.getUrl());
    }

    return (T) proxyFactory.getProxy(invoker);
  }

  private boolean shouldJvmRefer(Map<String, String> map) {
    return false;
  }


  private ConsumerModel buildConsumerModel(String serviceKey) {
    return new ConsumerModel(serviceKey, interfaceClass, ref);
  }

  @Override
  public void destroy() throws Exception {
    if (ref == null) {
      return;
    }
    if (destroyed) {
      return;
    }
    destroyed = true;
    try {
      invoker.destroy();
    } catch (Throwable t) {
      logger
          .warn("Unexpected error occured when destroy invoker of ReferenceConfig(" + urlStr + ").",
              t);
    }
    invoker = null;
    ref = null;
  }

  @Override
  public Object getObject() throws Exception {
    return get();
  }

  @Override
  public Class<?> getObjectType() {
    return null;
  }

  @Override
  public boolean isSingleton() {
    return false;
  }

  @Override
  public void afterPropertiesSet() throws Exception {

  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

  }
}
