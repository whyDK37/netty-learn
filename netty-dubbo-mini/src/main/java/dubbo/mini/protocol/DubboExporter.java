package dubbo.mini.protocol;

import dubbo.mini.rpc.Invoker;
import java.util.Map;

public class DubboExporter<T> extends AbstractExporter<T> {

  private final String key;

  private final Map<String, Exporter<?>> exporterMap;

  public DubboExporter(Invoker<T> invoker, String key, Map<String, Exporter<?>> exporterMap) {
    super(invoker);
    this.key = key;
    this.exporterMap = exporterMap;
  }

  @Override
  public void unexport() {
    super.unexport();
    exporterMap.remove(key);
  }

}