package dubbo.mini.serialize;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface Serialization {

    /**
     * Get content type unique id, recommended that custom implementations use values greater than 20.
     *
     * @return content type id
     */
    byte getContentTypeId();

    /**
     * Get content type
     *
     * @return content type
     */
    String getContentType();

    /**
     * Get a serialization implementation instance
     *
     * @param url    URL address for the remote service
     * @param output the underlying output stream
     * @return serializer
     * @throws IOException
     */
    ObjectOutput serialize(OutputStream output) throws IOException;

    /**
     * Get a deserialization implementation instance
     *
     * @param url   URL address for the remote service
     * @param input the underlying input stream
     * @return deserializer
     * @throws IOException
     */
    ObjectInput deserialize(InputStream input) throws IOException;

}