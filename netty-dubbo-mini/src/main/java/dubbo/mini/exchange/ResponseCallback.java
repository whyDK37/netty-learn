package dubbo.mini.exchange;

public interface ResponseCallback {

  /**
   * done. 处理执行完成
   *
   * @param response
   */
  void done(Object response);

  /**
   * caught exception. 处理发生异常
   *
   * @param exception
   */
  void caught(Throwable exception);

}