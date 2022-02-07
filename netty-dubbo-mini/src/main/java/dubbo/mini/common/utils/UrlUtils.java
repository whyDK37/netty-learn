package dubbo.mini.common.utils;

import dubbo.mini.common.Constants;
import dubbo.mini.common.NetURL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UrlUtils {

  private final static String URL_PARAM_STARTING_SYMBOL = "?";

  public static int getHeartbeat(NetURL url) {
    return url.getParameter(Constants.HEARTBEAT_KEY, Constants.DEFAULT_HEARTBEAT);
  }

  public static int getIdleTimeout(NetURL url) {
    int heartBeat = getHeartbeat(url);
    int idleTimeout = url.getParameter(Constants.HEARTBEAT_TIMEOUT_KEY, heartBeat * 3);
    if (idleTimeout < heartBeat * 2) {
      throw new IllegalStateException("idleTimeout < heartbeatInterval * 2");
    }
    return idleTimeout;
  }

  public static boolean isServiceKeyMatch(NetURL pattern, NetURL value) {
    return pattern.getParameter(Constants.INTERFACE_KEY).equals(
        value.getParameter(Constants.INTERFACE_KEY))
        && isItemMatch(pattern.getParameter(Constants.GROUP_KEY),
        value.getParameter(Constants.GROUP_KEY))
        && isItemMatch(pattern.getParameter(Constants.VERSION_KEY),
        value.getParameter(Constants.VERSION_KEY));
  }

  static boolean isItemMatch(String pattern, String value) {
    if (pattern == null) {
      return value == null;
    } else {
      return "*".equals(pattern) || pattern.equals(value);
    }
  }


  public static List<NetURL> parseURLs(String address, Map<String, String> defaults) {
    if (address == null || address.length() == 0) {
      return null;
    }
    String[] addresses = Constants.REGISTRY_SPLIT_PATTERN.split(address);
    if (addresses == null || addresses.length == 0) {
      return null; //here won't be empty
    }
    List<NetURL> registries = new ArrayList<NetURL>();
    for (String addr : addresses) {
      registries.add(parseURL(addr, defaults));
    }
    return registries;
  }

  public static NetURL parseURL(String address, Map<String, String> defaults) {
    if (address == null || address.length() == 0) {
      return null;
    }
    String url;
    if (address.contains("://") || address.contains(URL_PARAM_STARTING_SYMBOL)) {
      url = address;
    } else {
      String[] addresses = Constants.COMMA_SPLIT_PATTERN.split(address);
      url = addresses[0];
      if (addresses.length > 1) {
        StringBuilder backup = new StringBuilder();
        for (int i = 1; i < addresses.length; i++) {
          if (i > 1) {
            backup.append(",");
          }
          backup.append(addresses[i]);
        }
        url += URL_PARAM_STARTING_SYMBOL + Constants.BACKUP_KEY + "=" + backup.toString();
      }
    }
    String defaultProtocol = defaults == null ? null : defaults.get(Constants.PROTOCOL_KEY);
    if (defaultProtocol == null || defaultProtocol.length() == 0) {
      defaultProtocol = Constants.DUBBO_PROTOCOL;
    }
    int defaultPort = StringUtils
        .parseInteger(defaults == null ? null : defaults.get(Constants.PORT_KEY));
    String defaultPath = defaults == null ? null : defaults.get(Constants.PATH_KEY);
    Map<String, String> defaultParameters =
        defaults == null ? null : new HashMap<String, String>(defaults);
    if (defaultParameters != null) {
      defaultParameters.remove(Constants.PROTOCOL_KEY);
      defaultParameters.remove(Constants.HOST_KEY);
      defaultParameters.remove(Constants.PORT_KEY);
      defaultParameters.remove(Constants.PATH_KEY);
    }
    NetURL u = NetURL.valueOf(url);
    boolean changed = false;
    String protocol = u.getProtocol();
    String host = u.getHost();
    int port = u.getPort();
    String path = u.getPath();
    Map<String, String> parameters = new HashMap<String, String>(u.getParameters());
    if ((protocol == null || protocol.length() == 0) && defaultProtocol != null
        && defaultProtocol.length() > 0) {
      changed = true;
      protocol = defaultProtocol;
    }
    if (port <= 0) {
      if (defaultPort > 0) {
        changed = true;
        port = defaultPort;
      } else {
        changed = true;
        port = 9090;
      }
    }
    if (path == null || path.length() == 0) {
      if (defaultPath != null && defaultPath.length() > 0) {
        changed = true;
        path = defaultPath;
      }
    }
    if (defaultParameters != null && defaultParameters.size() > 0) {
      for (Map.Entry<String, String> entry : defaultParameters.entrySet()) {
        String key = entry.getKey();
        String defaultValue = entry.getValue();
        if (defaultValue != null && defaultValue.length() > 0) {
          String value = parameters.get(key);
          if (StringUtils.isEmpty(value)) {
            changed = true;
            parameters.put(key, defaultValue);
          }
        }
      }
    }
    if (changed) {
      u = new NetURL(protocol, host, port, path, parameters);
    }
    return u;
  }
}
