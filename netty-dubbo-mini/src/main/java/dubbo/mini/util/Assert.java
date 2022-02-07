package dubbo.mini.util;

import dubbo.mini.common.utils.StringUtils;

public abstract class Assert {

  protected Assert() {
  }

  public static void notNull(Object obj, String message) {
    if (obj == null) {
      throw new IllegalArgumentException(message);
    }
  }


  public static void notEmptyString(String str, String message) {
    if (StringUtils.isEmpty(str)) {
      throw new IllegalArgumentException(message);
    }
  }

  public static void notNull(Object obj, RuntimeException exeception) {
    if (obj == null) {
      throw exeception;
    }
  }

}