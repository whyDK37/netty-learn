package dubbo.mini.serialize;


import dubbo.mini.common.NetURL;
import dubbo.mini.support.SPI;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@SPI("hessian2")
public interface Serialization {

  byte getContentTypeId();

  String getContentType();

  ObjectOutput serialize(NetURL url, OutputStream output) throws IOException;

  ObjectInput deserialize(NetURL url, InputStream input) throws IOException;

}