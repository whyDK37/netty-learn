package dubbo.mini.exchange;

/**
 * Response
 */
public class Response {

  /**
   * 事件 - 心跳
   */
  public static final String HEARTBEAT_EVENT = null;

  /**
   * 事件 - 只读
   */
  public static final String READONLY_EVENT = "R";

  /**
   * ok.
   */
  public static final byte OK = 20;

  /**
   * client side timeout.
   */
  public static final byte CLIENT_TIMEOUT = 30;

  /**
   * server side timeout.
   */
  public static final byte SERVER_TIMEOUT = 31;

  /**
   * channel inactive, directly return the unfinished requests.
   */
  public static final byte CHANNEL_INACTIVE = 35;

  /**
   * request format error.
   */
  public static final byte BAD_REQUEST = 40;

  /**
   * response format error.
   */
  public static final byte BAD_RESPONSE = 50;

  /**
   * service not found.
   */
  public static final byte SERVICE_NOT_FOUND = 60;

  /**
   * service error.
   */
  public static final byte SERVICE_ERROR = 70;

  /**
   * internal server error.
   */
  public static final byte SERVER_ERROR = 80;

  /**
   * internal server error.
   */
  public static final byte CLIENT_ERROR = 90;

  /**
   * server side threadpool exhausted and quick return.
   */
  public static final byte SERVER_THREADPOOL_EXHAUSTED_ERROR = 100;

  /**
   * 响应编号
   * <p>
   * 一个 {@link Request#getId()} 和 {@link Response#mId} 一一对应。
   */
  private long mId = 0;

  /**
   * 状态
   */
  private byte mStatus = OK;

  /**
   * 是否事件
   */
  private boolean mEvent = false;

  /**
   * 错误消息
   */
  private String mErrorMsg;

  /**
   * 结果
   */
  private Object mResult;

  public Response() {
  }

  public Response(long id) {
    mId = id;
  }

  public long getId() {
    return mId;
  }

  public void setId(long id) {
    mId = id;
  }

  public byte getStatus() {
    return mStatus;
  }

  public void setStatus(byte status) {
    mStatus = status;
  }

  public boolean isEvent() {
    return mEvent;
  }

  public void setEvent(String event) {
    mEvent = true;
    mResult = event;
  }

  public void setEvent(boolean mEvent) {
    this.mEvent = mEvent;
  }

  public boolean isHeartbeat() {
    return mEvent && HEARTBEAT_EVENT == mResult;
  }

  @Deprecated
  public void setHeartbeat(boolean isHeartbeat) {
    if (isHeartbeat) {
      setEvent(HEARTBEAT_EVENT);
    }
  }

  public Object getResult() {
    return mResult;
  }

  public void setResult(Object msg) {
    mResult = msg;
  }

  public String getErrorMessage() {
    return mErrorMsg;
  }

  public void setErrorMessage(String msg) {
    mErrorMsg = msg;
  }

  @Override
  public String toString() {
    return "Response [id=" + mId + ", status=" + mStatus + ", event=" + mEvent
        + ", error=" + mErrorMsg + ", result=" + (mResult == this ? "this" : mResult) + "]";
  }
}