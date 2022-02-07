package dubbo.mini.serialize;

import com.esotericsoftware.kryo.Kryo;

public class ThreadLocalKryoFactory extends AbstractKryoFactory {

  private final ThreadLocal<Kryo> holder = new ThreadLocal<Kryo>() {
    @Override
    protected Kryo initialValue() {
      return create();
    }
  };

  @Override
  public void returnKryo(Kryo kryo) {
    // do nothing
  }

  @Override
  public Kryo getKryo() {
    return holder.get();
  }
}
