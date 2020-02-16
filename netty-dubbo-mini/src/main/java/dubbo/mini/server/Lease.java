package dubbo.mini.server;

import java.util.Date;

/**
 * @author why
 */
public class Lease {

  private Date createTime;
  private Date latest;

  public Lease() {
    this.createTime = new Date();
    this.latest = createTime;
  }

  public void touch() {
    latest = new Date();
  }

  public Date getCreateTime() {
    return createTime;
  }

  public Date getLatest() {
    return latest;
  }

  @Override
  public String toString() {
    return "Lease{" +
        "createTime=" + createTime +
        ", latest=" + latest +
        '}';
  }
}
