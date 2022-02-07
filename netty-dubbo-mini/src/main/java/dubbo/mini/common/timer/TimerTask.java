package dubbo.mini.common.timer;


public interface TimerTask {

  void run(Timeout timeout) throws Exception;
}