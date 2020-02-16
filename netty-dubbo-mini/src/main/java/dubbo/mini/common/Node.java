package dubbo.mini.common;

public interface Node {

  NetURL getUrl();

  boolean isAvailable();

  void destroy();

}