package dubbo.mini.config;

/**
 * @author why
 */
public class RegistryConfig {

  private String address;

  public String getAddress() {
    return this.address;
  }

  public Boolean isDefault() {
    return false;
  }

  public String getId() {
    return null;
  }

  public static void refresh(RegistryConfig registryConfig) {

  }
}
