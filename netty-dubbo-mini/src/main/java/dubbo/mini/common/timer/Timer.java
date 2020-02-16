package dubbo.mini.common.timer;

import java.util.Set;
import java.util.concurrent.TimeUnit;

public interface Timer {

  Timeout newTimeout(TimerTask task, long delay, TimeUnit unit);

  /**
   * Releases all resources acquired by this {@link Timer} and cancels all tasks which were
   * scheduled but not executed yet.
   *
   * @return the handles associated with the tasks which were canceled by this method
   */
  Set<Timeout> stop();

  /**
   * the timer is stop
   *
   * @return true for stop
   */
  boolean isStop();
}