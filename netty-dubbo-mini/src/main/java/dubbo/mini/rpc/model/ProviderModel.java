package dubbo.mini.rpc.model;

public class ProviderModel {

  private final String serviceName;
  private final Object serviceInstance;
  private final Class<?> serviceInterfaceClass;

  public ProviderModel(String serviceName, Object serviceInstance, Class<?> serviceInterfaceClass) {
    if (null == serviceInstance) {
      throw new IllegalArgumentException("Service[" + serviceName + "]Target is NULL.");
    }

    this.serviceName = serviceName;
    this.serviceInstance = serviceInstance;
    this.serviceInterfaceClass = serviceInterfaceClass;

  }


  public String getServiceName() {
    return serviceName;
  }

  public Class<?> getServiceInterfaceClass() {
    return serviceInterfaceClass;
  }

  public Object getServiceInstance() {
    return serviceInstance;
  }

}
