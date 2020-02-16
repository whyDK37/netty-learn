package dubbo.mini.codec;

import dubbo.mini.common.Constants;
import dubbo.mini.common.NetURL;
import dubbo.mini.serialize.ObjectInput;
import dubbo.mini.serialize.Serialization;
import dubbo.mini.support.ExtensionLoader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author why
 */
public class CodecSupport {

  private static Map<Byte, Serialization> ID_SERIALIZATION_MAP = new HashMap<Byte, Serialization>();
  private static Map<Byte, String> ID_SERIALIZATIONNAME_MAP = new HashMap<Byte, String>();

  public static Serialization getSerialization(NetURL url) {
    return ExtensionLoader.getExtensionLoader(Serialization.class).getExtension(
        url.getParameter(Constants.SERIALIZATION_KEY, Constants.DEFAULT_REMOTING_SERIALIZATION));
  }


  public static ObjectInput deserialize(NetURL url, InputStream is, byte proto) throws IOException {
    Serialization s = getSerialization(url, proto);
    return s.deserialize(url, is);
  }


  public static Serialization getSerialization(NetURL url, Byte id) throws IOException {
    Serialization serialization = getSerializationById(id);
    String serializationName = url
        .getParameter(Constants.SERIALIZATION_KEY, Constants.DEFAULT_REMOTING_SERIALIZATION);
    // Check if "serialization id" passed from network matches the id on this side(only take effect for JDK serialization), for security purpose.
    if (serialization == null
        || ((id == 3 || id == 7 || id == 4) && !(serializationName
        .equals(ID_SERIALIZATIONNAME_MAP.get(id))))) {
      throw new IOException("Unexpected serialization id:" + id
          + " received from network, please check if the peer send the right id.");
    }
    return serialization;
  }


  public static Serialization getSerializationById(Byte id) {
    return ID_SERIALIZATION_MAP.get(id);
  }
}
