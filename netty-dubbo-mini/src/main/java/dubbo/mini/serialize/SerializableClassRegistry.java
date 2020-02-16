package dubbo.mini.serialize;

import com.esotericsoftware.kryo.Serializer;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class SerializableClassRegistry {


  private static final Map<Class, Object> registrations = new LinkedHashMap<>();

  /**
   * only supposed to be called at startup time
   *
   * @param clazz object type
   */
  public static void registerClass(Class clazz) {
    registerClass(clazz, null);
  }

  /**
   * only supposed to be called at startup time
   *
   * @param clazz      object type
   * @param serializer object serializer
   */
  public static void registerClass(Class clazz, Serializer serializer) {
    if (clazz == null) {
      throw new IllegalArgumentException("Class registered to kryo cannot be null!");
    }
    registrations.put(clazz, serializer);
  }

  /**
   * get registered classes
   *
   * @return class serializer
   */
  public static Map<Class, Object> getRegisteredClasses() {
    return registrations;
  }
}
