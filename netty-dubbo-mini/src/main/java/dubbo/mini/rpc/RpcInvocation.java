package dubbo.mini.rpc;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class RpcInvocation implements Invocation, Serializable {

  private static final long serialVersionUID = -4355285085441097045L;

  private String methodName;

  private Class<?>[] parameterTypes;

  private Object[] arguments;

  private Map<String, String> attachments;
  private Invoker<?> invoker;


  public RpcInvocation() {
  }

  public RpcInvocation(Method method, Object[] arguments) {
    this(method.getName(), method.getParameterTypes(), arguments, null);
  }

  public RpcInvocation(Method method, Object[] arguments, Map<String, String> attachment) {
    this(method.getName(), method.getParameterTypes(), arguments, attachment);
  }

  public RpcInvocation(String methodName, Class<?>[] parameterTypes, Object[] arguments) {
    this(methodName, parameterTypes, arguments, null);
  }

  public RpcInvocation(String methodName, Class<?>[] parameterTypes, Object[] arguments,
      Map<String, String> attachments) {
    this.methodName = methodName;
    this.parameterTypes = parameterTypes == null ? new Class<?>[0] : parameterTypes;
    this.arguments = arguments == null ? new Object[0] : arguments;
    this.attachments = attachments == null ? new HashMap<String, String>() : attachments;
  }

  @Override
  public String getMethodName() {
    return methodName;
  }

  public void setMethodName(String methodName) {
    this.methodName = methodName;
  }

  @Override
  public Class<?>[] getParameterTypes() {
    return parameterTypes;
  }

  public void setParameterTypes(Class<?>[] parameterTypes) {
    this.parameterTypes = parameterTypes == null ? new Class<?>[0] : parameterTypes;
  }

  @Override
  public Object[] getArguments() {
    return arguments;
  }

  public void setArguments(Object[] arguments) {
    this.arguments = arguments == null ? new Object[0] : arguments;
  }

  @Override
  public Map<String, String> getAttachments() {
    return attachments;
  }

  public void setAttachments(Map<String, String> attachments) {
    this.attachments = attachments == null ? new HashMap<String, String>() : attachments;
  }

  public void setAttachment(String key, String value) {
    if (attachments == null) {
      attachments = new HashMap<>();
    }
    attachments.put(key, value);
  }

  public void setAttachmentIfAbsent(String key, String value) {
    if (attachments == null) {
      attachments = new HashMap<>();
    }
    if (!attachments.containsKey(key)) {
      attachments.put(key, value);
    }
  }

  public void addAttachments(Map<String, String> attachments) {
    if (attachments == null) {
      return;
    }
    if (this.attachments == null) {
      this.attachments = new HashMap<>();
    }
    this.attachments.putAll(attachments);
  }

  public void addAttachmentsIfAbsent(Map<String, String> attachments) {
    if (attachments == null) {
      return;
    }
    for (Map.Entry<String, String> entry : attachments.entrySet()) {
      setAttachmentIfAbsent(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public String getAttachment(String key) {
    if (attachments == null) {
      return null;
    }
    return attachments.get(key);
  }

  @Override
  public String getAttachment(String key, String defaultValue) {
    if (attachments == null) {
      return defaultValue;
    }
    String value = attachments.get(key);
    if (value == null || "".equals(value)) {
      return defaultValue;
    }
    return value;
  }

  @Override
  public Invoker<?> getInvoker() {
    return this.invoker;
  }

  @Override
  public String toString() {
    return "RpcInvocation [methodName=" + methodName + ", parameterTypes="
        + Arrays.toString(parameterTypes) + ", arguments=" + Arrays.toString(arguments)
        + ", attachments=" + attachments + "]";
  }

  public void setInvoker(Invoker<?> invoker) {
    this.invoker = invoker;
  }
}