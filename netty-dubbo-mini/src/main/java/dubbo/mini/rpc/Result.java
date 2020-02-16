package dubbo.mini.rpc;

import java.io.Serializable;
import java.util.Map;

public interface Result extends Serializable {

  Object getValue();

  Throwable getException();

  boolean hasException();

  Object recreate() throws Throwable;

  @Deprecated
  Object getResult();


  Map<String, String> getAttachments();

  void addAttachments(Map<String, String> map);

  void setAttachments(Map<String, String> map);

  String getAttachment(String key);

  String getAttachment(String key, String defaultValue);

  void setAttachment(String key, String value);

}