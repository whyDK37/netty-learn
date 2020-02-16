package dubbo.mini.protocol.jvm;

import dubbo.mini.protocol.AbstractExporter;
import dubbo.mini.protocol.Exporter;
import dubbo.mini.rpc.Invoker;
import java.util.Map;

class InjvmExporter<T> extends AbstractExporter<T> {

  private final String key;

  private final Map<String, Exporter<?>> exporterMap;

  InjvmExporter(Invoker<T> invoker, String key, Map<String, Exporter<?>> exporterMap) {
    super(invoker);
    this.key = key;
    this.exporterMap = exporterMap;
    exporterMap.put(key, this);
  }

  @Override
  public void unexport() {
    super.unexport();
    exporterMap.remove(key);
  }

}