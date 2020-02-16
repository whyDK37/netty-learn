package netty.heartbeat;

public class ConstantValue {

  public static final int HEAD_DATA = 0X76;

  public static final int DEFAULT_IO_THREADS = Math
      .min(Runtime.getRuntime().availableProcessors() + 1, 32);
  public static final int DEFAULT_IDLE_TIMEOUT = 600 * 1000;
}
