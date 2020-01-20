package dubbo.mini.config.spring;

import dubbo.mini.common.Constants;
import dubbo.mini.common.NetURL;
import dubbo.mini.common.utils.StringUtils;
import dubbo.mini.rpc.Invoker;
import dubbo.mini.rpc.ProxyFactory;
import dubbo.mini.rpc.model.ApplicationModel;
import dubbo.mini.rpc.model.ConsumerModel;
import dubbo.mini.support.ExtensionLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author why
 */
public class ReferenceBean<T> implements FactoryBean, ApplicationContextAware, InitializingBean, DisposableBean {

    private static Logger logger = LoggerFactory.getLogger(ReferenceBean.class);

    private T ref;


    private String interfaceName;
    private Class<?> interfaceClass;

    private static final ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getDefaultExtension();
    /**
     * The invoker of the reference service
     */
    private NetURL url;
    private transient volatile Invoker<?> invoker;

    private transient volatile boolean destroyed;
    private transient volatile boolean initialized;

    public synchronized T get() {
        checkAndUpdateSubConfigs();

        if (destroyed) {
            throw new IllegalStateException("The invoker of ReferenceConfig(" + url + ") has already destroyed!");
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
            throw new IllegalStateException("The interface class " + interfaceClass + " is not a interface!");
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

        if (logger.isInfoEnabled()) {
            logger.info("Refer dubbo service " + interfaceName + " from url " + invoker.getUrl());
        }

        return (T) proxyFactory.getProxy(invoker);
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
            logger.warn("Unexpected error occured when destroy invoker of ReferenceConfig(" + url + ").", t);
        }
        invoker = null;
        ref = null;
    }

    @Override
    public Object getObject() throws Exception {
        return null;
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
