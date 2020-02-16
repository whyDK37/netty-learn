package dubbo.mini.serialize.hessian2;

import dubbo.mini.common.NetURL;
import dubbo.mini.serialize.ObjectInput;
import dubbo.mini.serialize.ObjectOutput;
import dubbo.mini.serialize.Serialization;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Hessian2Serialization implements Serialization {

  public static final byte ID = 2;

  @Override
  public byte getContentTypeId() {
    return ID;
  }

  @Override
  public String getContentType() {
    return "x-application/hessian2";
  }

  @Override
  public ObjectOutput serialize(NetURL url, OutputStream output) throws IOException {
    return new Hessian2ObjectOutput(output);
  }

  @Override
  public ObjectInput deserialize(NetURL url, InputStream input) throws IOException {
    return new Hessian2ObjectInput(input);
  }

}