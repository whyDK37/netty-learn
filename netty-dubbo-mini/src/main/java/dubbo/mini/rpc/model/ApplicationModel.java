package dubbo.mini.rpc.model;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationModel {

  protected static final Logger LOGGER = LoggerFactory.getLogger(ApplicationModel.class);

  /**
   * full qualified class name -> provided service
   */
  private static final ConcurrentMap<String, ProviderModel> providedServices = new ConcurrentHashMap<>();
  /**
   * full qualified class name -> subscribe service
   */
  private static final ConcurrentMap<String, ConsumerModel> consumedServices = new ConcurrentHashMap<>();

  private static String application;

  public static Collection<ConsumerModel> allConsumerModels() {
    return consumedServices.values();
  }

  public static Collection<ProviderModel> allProviderModels() {
    return providedServices.values();
  }

  public static ProviderModel getProviderModel(String serviceName) {
    return providedServices.get(serviceName);
  }

  public static ConsumerModel getConsumerModel(String serviceName) {
    return consumedServices.get(serviceName);
  }

  public static void initConsumerModel(String serviceName, ConsumerModel consumerModel) {
    if (consumedServices.putIfAbsent(serviceName, consumerModel) != null) {
      LOGGER.warn("Already register the same consumer:" + serviceName);
    }
  }

  public static void initProviderModel(String serviceName, ProviderModel providerModel) {
    if (providedServices.putIfAbsent(serviceName, providerModel) != null) {
      LOGGER.warn("Already register the same:" + serviceName);
    }
  }

  public static String getApplication() {
    return application;
  }

  public static void setApplication(String application) {
    ApplicationModel.application = application;
  }

  /**
   * For unit test
   */
  public static void reset() {
    providedServices.clear();
    consumedServices.clear();
  }
}
