package dubbo.mini.support;

import dubbo.mini.codec.Codec;
import dubbo.mini.codec.TelnetCodec;
import dubbo.mini.common.Constants;
import dubbo.mini.serialize.Serialization;
import dubbo.mini.serialize.hessian2.Hessian2Serialization;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author why
 */
class ExtensionLoaderTest {

  @Test
  void getExtensionLoader() {
    ExtensionLoader<Serialization> extensionLoader = ExtensionLoader
        .getExtensionLoader(Serialization.class);

    Serialization extension = extensionLoader
        .getExtension(Constants.DEFAULT_REMOTING_SERIALIZATION);
    Assertions.assertTrue(extension instanceof Hessian2Serialization);

    Codec telnet = ExtensionLoader.getExtensionLoader(Codec.class).getExtension("telnet");
    Assertions.assertTrue(telnet instanceof TelnetCodec);

  }
}