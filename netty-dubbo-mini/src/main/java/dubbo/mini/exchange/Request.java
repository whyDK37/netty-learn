package dubbo.mini.exchange;

import dubbo.mini.common.utils.StringUtils;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Request.
 */
public class Request {

  /**
   * 事件 - 心跳
   */
  public static final String HEARTBEAT_EVENT = null;

  /**
   * 事件 - 只读
   */
  public static final String READONLY_EVENT = "R";

  /**
   * 请求编号自增序列
   */
  private static final AtomicLong INVOKE_ID = new AtomicLong(0);

  /**
   * 请求编号
   */
  private final long mId;

  /**
   * 是否需要响应
   * <p>
   * true-需要 false-不需要
   */
  private boolean mTwoWay = true;

  /**
   * 是否是事件。例如，心跳事件。
   */
  private boolean mEvent = false;

  /**
   * 是否异常的请求。
   * <p>
   * 在消息解析的时候，会出现。
   */
  private boolean mBroken = false;
  /**
   * 数据
   */
  private Object mData;

  public Request() {
    mId = newId();
  }

  public Request(long id) {
    mId = id;
  }

  private static long newId() {
    // getAndIncrement() When it grows to MAX_VALUE, it will grow to MIN_VALUE, and the negative can be used as ID
    return INVOKE_ID.getAndIncrement();
  }

  private static String safeToString(Object data) {
    if (data == null) {
      return null;
    }
    String dataStr;
    try {
      dataStr = data.toString();
    } catch (Throwable e) {
      dataStr = "<Fail toString of " + data.getClass() + ", cause: " +
          StringUtils.toString(e) + ">";
    }
    return dataStr;
  }

  public long getId() {
    return mId;
  }

  public boolean isTwoWay() {
    return mTwoWay;
  }

  public void setTwoWay(boolean twoWay) {
    mTwoWay = twoWay;
  }

  public boolean isEvent() {
    return mEvent;
  }

  public void setEvent(String event) {
    this.mEvent = true;
    this.mData = event;
  }

  public void setEvent(boolean mEvent) {
    this.mEvent = mEvent;
  }

  public boolean isBroken() {
    return mBroken;
  }

  public void setBroken(boolean mBroken) {
    this.mBroken = mBroken;
  }

  public Object getData() {
    return mData;
  }

  public void setData(Object msg) {
    mData = msg;
  }

  public boolean isHeartbeat() {
    return mEvent && HEARTBEAT_EVENT == mData;
  }

  public void setHeartbeat(boolean isHeartbeat) {
    if (isHeartbeat) {
      setEvent(HEARTBEAT_EVENT);
    }
  }

  @Override
  public String toString() {
    return "Request [id=" + mId + ", twoway=" + mTwoWay + ", event=" + mEvent
        + ", broken=" + mBroken + ", data=" + (mData == this ? "this" : safeToString(mData)) + "]";
  }
}