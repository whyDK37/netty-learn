package dubbo.mini.protocol;

import dubbo.mini.common.Constants;
import dubbo.mini.common.NetURL;
import dubbo.mini.common.utils.ProtocolUtils;
import dubbo.mini.rpc.Invoker;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author why
 */
public abstract class AbstractProtocol implements Protocol {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  protected final Map<String, Exporter<?>> exporterMap = new ConcurrentHashMap<>();
  protected final Set<Invoker<?>> invokers = new CopyOnWriteArraySet<Invoker<?>>();


  protected static String serviceKey(NetURL url) {
    int port = url.getParameter(Constants.BIND_PORT_KEY, url.getPort());
    return serviceKey(port, url.getPath(), url.getParameter(Constants.VERSION_KEY),
        url.getParameter(Constants.GROUP_KEY));
  }

  protected static String serviceKey(int port, String serviceName, String serviceVersion,
      String serviceGroup) {
    return ProtocolUtils.serviceKey(port, serviceName, serviceVersion, serviceGroup);
  }

  @Override
  public void destroy() {
    for (Invoker<?> invoker : invokers) {
      if (invoker != null) {
        invokers.remove(invoker);
        try {
          if (logger.isInfoEnabled()) {
            logger.info("Destroy reference: " + invoker.getUrl());
          }
          invoker.destroy();
        } catch (Throwable t) {
          logger.warn(t.getMessage(), t);
        }
      }
    }
    for (String key : new ArrayList<>(exporterMap.keySet())) {
      Exporter<?> exporter = exporterMap.remove(key);
      if (exporter != null) {
        try {
          if (logger.isInfoEnabled()) {
            logger.info("Unexport service: " + exporter.getInvoker().getUrl());
          }
          exporter.unexport();
        } catch (Throwable t) {
          logger.warn(t.getMessage(), t);
        }
      }
    }
  }
}
