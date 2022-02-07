package dubbo.mini.rpc;

import java.util.Map;

public interface Invocation {

  String getMethodName();

  Class<?>[] getParameterTypes();

  Object[] getArguments();

  Map<String, String> getAttachments();

  String getAttachment(String key);

  String getAttachment(String key, String defaultValue);

  Invoker<?> getInvoker();

  void setInvoker(Invoker<?> invoker);

  void addAttachmentsIfAbsent(Map<String, String> attachment);

  void addAttachments(Map<String, String> contextAttachments);

  void setAttachment(String asyncKey, String toString);
}