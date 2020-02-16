package dubbo.mini.serialize;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompatibleKryo extends Kryo {

  private static final Logger logger = LoggerFactory.getLogger(CompatibleKryo.class);

  @Override
  public Serializer getDefaultSerializer(Class type) {
    if (type == null) {
      throw new IllegalArgumentException("type cannot be null.");
    }

    /**
     * Kryo requires every class to provide a zero argument constructor. For any class does not match this condition, kryo have two ways:
     * 1. Use JavaSerializer,
     * 2. Set 'kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));', StdInstantiatorStrategy can generate an instance bypassing the constructor.
     *
     * In practice, it's not possible for Dubbo users to register kryo Serializer for every customized class. So in most cases, customized classes with/without zero argument constructor will
     * default to the default serializer.
     * It is the responsibility of kryo to handle with every standard jdk classes, so we will just escape these classes.
     */
    if (!ReflectionUtils.isJdk(type) && !type.isArray() && !type.isEnum() && !ReflectionUtils
        .checkZeroArgConstructor(type)) {
      if (logger.isWarnEnabled()) {
        logger.warn(type
            + " has no zero-arg constructor and this will affect the serialization performance");
      }
      return new JavaSerializer();
    }
    return super.getDefaultSerializer(type);
  }
}
