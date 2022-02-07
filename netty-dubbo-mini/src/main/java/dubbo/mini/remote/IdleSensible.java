package dubbo.mini.remote;

public interface IdleSensible {

  default boolean canHandleIdle() {
    return false;
  }
}