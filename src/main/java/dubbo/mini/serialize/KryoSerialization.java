package dubbo.mini.serialize;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class KryoSerialization implements Serialization {

    @Override
    public byte getContentTypeId() {
        return 8;
    }

    @Override
    public String getContentType() {
        return "x-application/kryo";
    }

    @Override
    public ObjectOutput serialize( OutputStream out) throws IOException {
        return new KryoObjectOutput(out);
    }

    @Override
    public ObjectInput deserialize(InputStream is) throws IOException {
        return new KryoObjectInput(is);
    }
}
