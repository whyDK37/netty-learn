package dubbo.mini.codec;

import dubbo.mini.common.Constants;
import dubbo.mini.common.NetURL;
import dubbo.mini.serialize.Serialization;
import dubbo.mini.support.ExtensionLoader;

/**
 * @author why
 */
public class CodecSupport {

    public static Serialization getSerialization(NetURL url) {
        return ExtensionLoader.getExtensionLoader(Serialization.class).getExtension(
                url.getParameter(Constants.SERIALIZATION_KEY, Constants.DEFAULT_REMOTING_SERIALIZATION));
    }
}
