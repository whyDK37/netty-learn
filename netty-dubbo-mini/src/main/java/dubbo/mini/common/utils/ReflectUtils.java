package dubbo.mini.common.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author why
 */
public class ReflectUtils {


  /**
   * void(V).
   */
  public static final char JVM_VOID = 'V';

  /**
   * boolean(Z).
   */
  public static final char JVM_BOOLEAN = 'Z';

  /**
   * byte(B).
   */
  public static final char JVM_BYTE = 'B';

  /**
   * char(C).
   */
  public static final char JVM_CHAR = 'C';

  /**
   * double(D).
   */
  public static final char JVM_DOUBLE = 'D';

  /**
   * float(F).
   */
  public static final char JVM_FLOAT = 'F';

  /**
   * int(I).
   */
  public static final char JVM_INT = 'I';

  /**
   * long(J).
   */
  public static final char JVM_LONG = 'J';

  /**
   * short(S).
   */
  public static final char JVM_SHORT = 'S';


  public static final String JAVA_IDENT_REGEX = "(?:[_$a-zA-Z][_$a-zA-Z0-9]*)";

  public static final String CLASS_DESC =
      "(?:L" + JAVA_IDENT_REGEX + "(?:\\/" + JAVA_IDENT_REGEX + ")*;)";
  public static final String ARRAY_DESC = "(?:\\[+(?:(?:[VZBCDFIJS])|" + CLASS_DESC + "))";
  public static final String DESC_REGEX =
      "(?:(?:[VZBCDFIJS])|" + CLASS_DESC + "|" + ARRAY_DESC + ")";


  public static final Pattern GETTER_METHOD_DESC_PATTERN = Pattern
      .compile("get([A-Z][_a-zA-Z0-9]*)\\(\\)(" + DESC_REGEX + ")");

  public static final Pattern SETTER_METHOD_DESC_PATTERN = Pattern
      .compile("set([A-Z][_a-zA-Z0-9]*)\\((" + DESC_REGEX + ")\\)V");

  public static final Pattern IS_HAS_CAN_METHOD_DESC_PATTERN = Pattern
      .compile("(?:is|has|can)([A-Z][_a-zA-Z0-9]*)\\(\\)Z");

  private static final ConcurrentMap<String, Class<?>> NAME_CLASS_CACHE = new ConcurrentHashMap<String, Class<?>>();


  public static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];
  public static final Pattern DESC_PATTERN = Pattern.compile(DESC_REGEX);
  private static final ConcurrentMap<String, Class<?>> DESC_CLASS_CACHE = new ConcurrentHashMap<>();


  public static Class<?> forName(ClassLoader cl, String name) {
    try {
      return name2class(cl, name);
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException("Not found class " + name + ", cause: " + e.getMessage(), e);
    }
  }


  public static Class<?> forName(String name) {
    try {
      return name2class(name);
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException("Not found class " + name + ", cause: " + e.getMessage(), e);
    }
  }

  public static Class<?> name2class(String name) throws ClassNotFoundException {
    return name2class(ClassHelper.getClassLoader(), name);
  }


  private static Class<?> name2class(ClassLoader cl, String name) throws ClassNotFoundException {
    int c = 0, index = name.indexOf('[');
    if (index > 0) {
      c = (name.length() - index) / 2;
      name = name.substring(0, index);
    }
    if (c > 0) {
      StringBuilder sb = new StringBuilder();
      while (c-- > 0) {
        sb.append("[");
      }

      if ("void".equals(name)) {
        sb.append(JVM_VOID);
      } else if ("boolean".equals(name)) {
        sb.append(JVM_BOOLEAN);
      } else if ("byte".equals(name)) {
        sb.append(JVM_BYTE);
      } else if ("char".equals(name)) {
        sb.append(JVM_CHAR);
      } else if ("double".equals(name)) {
        sb.append(JVM_DOUBLE);
      } else if ("float".equals(name)) {
        sb.append(JVM_FLOAT);
      } else if ("int".equals(name)) {
        sb.append(JVM_INT);
      } else if ("long".equals(name)) {
        sb.append(JVM_LONG);
      } else if ("short".equals(name)) {
        sb.append(JVM_SHORT);
      } else {
        sb.append('L').append(name).append(';'); // "java.lang.Object" ==> "Ljava.lang.Object;"
      }
      name = sb.toString();
    } else {
      if ("void".equals(name)) {
        return void.class;
      } else if ("boolean".equals(name)) {
        return boolean.class;
      } else if ("byte".equals(name)) {
        return byte.class;
      } else if ("char".equals(name)) {
        return char.class;
      } else if ("double".equals(name)) {
        return double.class;
      } else if ("float".equals(name)) {
        return float.class;
      } else if ("int".equals(name)) {
        return int.class;
      } else if ("long".equals(name)) {
        return long.class;
      } else if ("short".equals(name)) {
        return short.class;
      }
    }

    if (cl == null) {
      cl = ClassHelper.getClassLoader();
    }
    Class<?> clazz = NAME_CLASS_CACHE.get(name);
    if (clazz == null) {
      clazz = Class.forName(name, true, cl);
      NAME_CLASS_CACHE.put(name, clazz);
    }
    return clazz;
  }


  public static String getDesc(final Method m) {
    StringBuilder ret = new StringBuilder(m.getName()).append('(');
    Class<?>[] parameterTypes = m.getParameterTypes();
    for (int i = 0; i < parameterTypes.length; i++) {
      ret.append(getDesc(parameterTypes[i]));
    }
    ret.append(')').append(getDesc(m.getReturnType()));
    return ret.toString();
  }

  public static String getDesc(final Class<?>[] cs) {
    if (cs.length == 0) {
      return "";
    }

    StringBuilder sb = new StringBuilder(64);
    for (Class<?> c : cs) {
      sb.append(getDesc(c));
    }
    return sb.toString();
  }

  public static String getDesc(Class<?> c) {
    StringBuilder ret = new StringBuilder();

    while (c.isArray()) {
      ret.append('[');
      c = c.getComponentType();
    }

    if (c.isPrimitive()) {
      String t = c.getName();
      if ("void".equals(t)) {
        ret.append(JVM_VOID);
      } else if ("boolean".equals(t)) {
        ret.append(JVM_BOOLEAN);
      } else if ("byte".equals(t)) {
        ret.append(JVM_BYTE);
      } else if ("char".equals(t)) {
        ret.append(JVM_CHAR);
      } else if ("double".equals(t)) {
        ret.append(JVM_DOUBLE);
      } else if ("float".equals(t)) {
        ret.append(JVM_FLOAT);
      } else if ("int".equals(t)) {
        ret.append(JVM_INT);
      } else if ("long".equals(t)) {
        ret.append(JVM_LONG);
      } else if ("short".equals(t)) {
        ret.append(JVM_SHORT);
      }
    } else {
      ret.append('L');
      ret.append(c.getName().replace('.', '/'));
      ret.append(';');
    }
    return ret.toString();
  }


  public static String getName(Class<?> c) {
    if (c.isArray()) {
      StringBuilder sb = new StringBuilder();
      do {
        sb.append("[]");
        c = c.getComponentType();
      }
      while (c.isArray());

      return c.getName() + sb.toString();
    }
    return c.getName();
  }

  public static String getName(final Method m) {
    StringBuilder ret = new StringBuilder();
    ret.append(getName(m.getReturnType())).append(' ');
    ret.append(m.getName()).append('(');
    Class<?>[] parameterTypes = m.getParameterTypes();
    for (int i = 0; i < parameterTypes.length; i++) {
      if (i > 0) {
        ret.append(',');
      }
      ret.append(getName(parameterTypes[i]));
    }
    ret.append(')');
    return ret.toString();
  }


  public static String getDesc(final Constructor<?> c) {
    StringBuilder ret = new StringBuilder("(");
    Class<?>[] parameterTypes = c.getParameterTypes();
    for (int i = 0; i < parameterTypes.length; i++) {
      ret.append(getDesc(parameterTypes[i]));
    }
    ret.append(')').append('V');
    return ret.toString();
  }


  public static String getDescWithoutMethodName(Method m) {
    StringBuilder ret = new StringBuilder();
    ret.append('(');
    Class<?>[] parameterTypes = m.getParameterTypes();
    for (int i = 0; i < parameterTypes.length; i++) {
      ret.append(getDesc(parameterTypes[i]));
    }
    ret.append(')').append(getDesc(m.getReturnType()));
    return ret.toString();
  }

  public static Class<?>[] desc2classArray(String desc) throws ClassNotFoundException {
    Class<?>[] ret = desc2classArray(ClassHelper.getClassLoader(), desc);
    return ret;
  }

  private static Class<?>[] desc2classArray(ClassLoader cl, String desc)
      throws ClassNotFoundException {
    if (desc.length() == 0) {
      return EMPTY_CLASS_ARRAY;
    }

    List<Class<?>> cs = new ArrayList<Class<?>>();
    Matcher m = DESC_PATTERN.matcher(desc);
    while (m.find()) {
      cs.add(desc2class(cl, m.group()));
    }
    return cs.toArray(EMPTY_CLASS_ARRAY);
  }


  private static Class<?> desc2class(ClassLoader cl, String desc) throws ClassNotFoundException {
    switch (desc.charAt(0)) {
      case JVM_VOID:
        return void.class;
      case JVM_BOOLEAN:
        return boolean.class;
      case JVM_BYTE:
        return byte.class;
      case JVM_CHAR:
        return char.class;
      case JVM_DOUBLE:
        return double.class;
      case JVM_FLOAT:
        return float.class;
      case JVM_INT:
        return int.class;
      case JVM_LONG:
        return long.class;
      case JVM_SHORT:
        return short.class;
      case 'L':
        desc = desc.substring(1, desc.length() - 1)
            .replace('/', '.'); // "Ljava/lang/Object;" ==> "java.lang.Object"
        break;
      case '[':
        desc = desc.replace('/', '.');  // "[[Ljava/lang/Object;" ==> "[[Ljava.lang.Object;"
        break;
      default:
        throw new ClassNotFoundException("Class not found: " + desc);
    }

    if (cl == null) {
      cl = ClassHelper.getClassLoader();
    }
    Class<?> clazz = DESC_CLASS_CACHE.get(desc);
    if (clazz == null) {
      clazz = Class.forName(desc, true, cl);
      DESC_CLASS_CACHE.put(desc, clazz);
    }
    return clazz;
  }
}
