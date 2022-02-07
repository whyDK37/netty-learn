package dubbo.mini.common.io;

import java.io.IOException;
import java.io.InputStream;

public class StreamUtils {

  public static void skipUnusedStream(InputStream is) throws IOException {
    if (is.available() > 0) {
      is.skip(is.available());
    }
  }
}
