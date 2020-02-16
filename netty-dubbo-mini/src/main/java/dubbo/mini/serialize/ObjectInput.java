package dubbo.mini.serialize;

import java.io.IOException;
import java.lang.reflect.Type;

public interface ObjectInput extends DataInput {

  Object readObject() throws IOException, ClassNotFoundException;


  <T> T readObject(Class<T> cls) throws IOException, ClassNotFoundException;


  <T> T readObject(Class<T> cls, Type type) throws IOException, ClassNotFoundException;

}