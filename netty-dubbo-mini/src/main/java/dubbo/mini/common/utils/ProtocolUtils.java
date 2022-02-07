package dubbo.mini.common.utils;

import dubbo.mini.common.Constants;
import dubbo.mini.common.NetURL;

/**
 * @author why
 */
public class ProtocolUtils {

  private ProtocolUtils() {
  }

  public static String serviceKey(NetURL url) {
    return serviceKey(url.getPort(), url.getPath(), url.getParameter(Constants.VERSION_KEY),
        url.getParameter(Constants.GROUP_KEY));
  }

  public static String serviceKey(int port, String serviceName, String serviceVersion,
      String serviceGroup) {
    StringBuilder buf = new StringBuilder();
    if (!StringUtils.isEmpty(serviceGroup)) {
      buf.append(serviceGroup);
      buf.append("/");
    }
    buf.append(serviceName);
    if (serviceVersion != null && serviceVersion.length() > 0 && !"0.0.0".equals(serviceVersion)) {
      buf.append(":");
      buf.append(serviceVersion);
    }
    buf.append(":");
    buf.append(port);
    return buf.toString();
  }
}
