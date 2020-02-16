package dubbo.mini.protocol;

import dubbo.mini.rpc.Invoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractExporter<T> implements Exporter<T> {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private final Invoker<T> invoker;

  private volatile boolean unexported = false;

  public AbstractExporter(Invoker<T> invoker) {
    if (invoker == null) {
      throw new IllegalStateException("service invoker == null");
    }
    if (invoker.getInterface() == null) {
      throw new IllegalStateException("service type == null");
    }
    if (invoker.getUrl() == null) {
      throw new IllegalStateException("service url == null");
    }
    this.invoker = invoker;
  }

  @Override
  public Invoker<T> getInvoker() {
    return invoker;
  }

  @Override
  public void unexport() {
    if (unexported) {
      return;
    }
    unexported = true;
    getInvoker().destroy();
  }

  @Override
  public String toString() {
    return getInvoker().toString();
  }

}