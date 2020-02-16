package dubbo.mini.rpc.model;

import dubbo.mini.util.Assert;

public class ConsumerModel {

  private final Object proxyObject;
  private final String serviceName;
  private final Class<?> serviceInterfaceClass;

  public ConsumerModel(String serviceName
      , Class<?> serviceInterfaceClass
      , Object proxyObject) {

    Assert.notEmptyString(serviceName, "Service name can't be null or blank");
    Assert.notNull(serviceInterfaceClass, "Service interface class can't null");
    Assert.notNull(proxyObject, "Proxy object can't be null");

    this.serviceName = serviceName;
    this.serviceInterfaceClass = serviceInterfaceClass;
    this.proxyObject = proxyObject;
  }

  public Object getProxyObject() {
    return proxyObject;
  }


  public Class<?> getServiceInterfaceClass() {
    return serviceInterfaceClass;
  }

  public String getServiceName() {
    return serviceName;
  }
}
