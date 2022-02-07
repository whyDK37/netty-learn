package dubbo.mini.rpc;

import dubbo.mini.common.Constants;
import dubbo.mini.common.NetURL;
import dubbo.mini.common.utils.ArrayUtils;
import dubbo.mini.common.utils.ClassHelper;
import dubbo.mini.common.utils.ReflectUtils;
import dubbo.mini.common.utils.StringUtils;
import dubbo.mini.exception.NoSuchPropertyException;
import dubbo.mini.rpc.proxy.InvokerInvocationHandler;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.LoaderClassPath;
import javassist.NotFoundException;

public class JavassistProxyFactory extends AbstractProxyFactory {

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getProxy(Invoker<T> invoker, Class<?>[] interfaces) {
    return (T) Proxy.getProxy(interfaces).newInstance(new InvokerInvocationHandler(invoker));
  }

  @Override
  public <T> Invoker<T> getInvoker(T proxy, Class<T> type, NetURL url) {
    // TODO Wrapper cannot handle this scenario correctly: the classname contains '$'
    final Wrapper wrapper = Wrapper
        .getWrapper(proxy.getClass().getName().indexOf('$') < 0 ? proxy.getClass() : type);
    return new AbstractProxyInvoker<T>(proxy, type, url) {
      @Override
      protected Object doInvoke(T proxy, String methodName,
          Class<?>[] parameterTypes,
          Object[] arguments) throws Throwable {
        return wrapper.invokeMethod(proxy, methodName, parameterTypes, arguments);
      }
    };
  }

}


abstract class Wrapper {

  private static final Map<Class<?>, Wrapper> WRAPPER_MAP = new ConcurrentHashMap<Class<?>, Wrapper>(); //class wrapper map
  private static final String[] EMPTY_STRING_ARRAY = new String[0];
  private static final String[] OBJECT_METHODS = new String[]{"getClass", "hashCode", "toString",
      "equals"};
  private static final Wrapper OBJECT_WRAPPER = new Wrapper() {
    @Override
    public String[] getMethodNames() {
      return OBJECT_METHODS;
    }

    @Override
    public String[] getDeclaredMethodNames() {
      return OBJECT_METHODS;
    }

    @Override
    public String[] getPropertyNames() {
      return EMPTY_STRING_ARRAY;
    }

    @Override
    public Class<?> getPropertyType(String pn) {
      return null;
    }

    @Override
    public Object getPropertyValue(Object instance, String pn) throws NoSuchPropertyException {
      throw new NoSuchPropertyException("Property [" + pn + "] not found.");
    }

    @Override
    public void setPropertyValue(Object instance, String pn, Object pv)
        throws NoSuchPropertyException {
      throw new NoSuchPropertyException("Property [" + pn + "] not found.");
    }

    @Override
    public boolean hasProperty(String name) {
      return false;
    }

    @Override
    public Object invokeMethod(Object instance, String mn, Class<?>[] types, Object[] args)
        throws NoSuchMethodException {
      if ("getClass".equals(mn)) {
        return instance.getClass();
      }
      if ("hashCode".equals(mn)) {
        return instance.hashCode();
      }
      if ("toString".equals(mn)) {
        return instance.toString();
      }
      if ("equals".equals(mn)) {
        if (args.length == 1) {
          return instance.equals(args[0]);
        }
        throw new IllegalArgumentException("Invoke method [" + mn + "] argument number error.");
      }
      throw new NoSuchMethodException("Method [" + mn + "] not found.");
    }
  };
  private static AtomicLong WRAPPER_CLASS_COUNTER = new AtomicLong(0);

  /**
   * get wrapper.
   *
   * @param c Class instance.
   * @return Wrapper instance(not null).
   */
  public static Wrapper getWrapper(Class<?> c) {
    while (ClassGenerator.isDynamicClass(c)) // can not wrapper on dynamic class.
    {
      c = c.getSuperclass();
    }

    if (c == Object.class) {
      return OBJECT_WRAPPER;
    }

    Wrapper ret = WRAPPER_MAP.get(c);
    if (ret == null) {
      ret = makeWrapper(c);
      WRAPPER_MAP.put(c, ret);
    }
    return ret;
  }

  private static Wrapper makeWrapper(Class<?> c) {
    if (c.isPrimitive()) {
      throw new IllegalArgumentException("Can not create wrapper for primitive type: " + c);
    }

    String name = c.getName();
    ClassLoader cl = ClassHelper.getClassLoader(c);

    StringBuilder c1 = new StringBuilder(
        "public void setPropertyValue(Object o, String n, Object v){ ");
    StringBuilder c2 = new StringBuilder("public Object getPropertyValue(Object o, String n){ ");
    StringBuilder c3 = new StringBuilder(
        "public Object invokeMethod(Object o, String n, Class[] p, Object[] v) throws "
            + InvocationTargetException.class.getName() + "{ ");

    c1.append(name).append(" w; try{ w = ((").append(name)
        .append(")$1); }catch(Throwable e){ throw new IllegalArgumentException(e); }");
    c2.append(name).append(" w; try{ w = ((").append(name)
        .append(")$1); }catch(Throwable e){ throw new IllegalArgumentException(e); }");
    c3.append(name).append(" w; try{ w = ((").append(name)
        .append(")$1); }catch(Throwable e){ throw new IllegalArgumentException(e); }");

    Map<String, Class<?>> pts = new HashMap<>(); // <property name, property types>
    Map<String, Method> ms = new LinkedHashMap<>(); // <method desc, Method instance>
    List<String> mns = new ArrayList<>(); // method names.
    List<String> dmns = new ArrayList<>(); // declaring method names.

    // get all public field.
    for (Field f : c.getFields()) {
      String fn = f.getName();
      Class<?> ft = f.getType();
      if (Modifier.isStatic(f.getModifiers()) || Modifier.isTransient(f.getModifiers())) {
        continue;
      }

      c1.append(" if( $2.equals(\"").append(fn).append("\") ){ w.").append(fn).append("=")
          .append(arg(ft, "$3")).append("; return; }");
      c2.append(" if( $2.equals(\"").append(fn).append("\") ){ return ($w)w.").append(fn)
          .append("; }");
      pts.put(fn, ft);
    }

    Method[] methods = c.getMethods();
    // get all public method.
    boolean hasMethod = hasMethods(methods);
    if (hasMethod) {
      c3.append(" try{");
      for (Method m : methods) {
        //ignore Object's method.
        if (m.getDeclaringClass() == Object.class) {
          continue;
        }

        String mn = m.getName();
        c3.append(" if( \"").append(mn).append("\".equals( $2 ) ");
        int len = m.getParameterTypes().length;
        c3.append(" && ").append(" $3.length == ").append(len);

        boolean override = false;
        for (Method m2 : methods) {
          if (m != m2 && m.getName().equals(m2.getName())) {
            override = true;
            break;
          }
        }
        if (override) {
          if (len > 0) {
            for (int l = 0; l < len; l++) {
              c3.append(" && ").append(" $3[").append(l).append("].getName().equals(\"")
                  .append(m.getParameterTypes()[l].getName()).append("\")");
            }
          }
        }

        c3.append(" ) { ");

        if (m.getReturnType() == Void.TYPE) {
          c3.append(" w.").append(mn).append('(').append(args(m.getParameterTypes(), "$4"))
              .append(");").append(" return null;");
        } else {
          c3.append(" return ($w)w.").append(mn).append('(')
              .append(args(m.getParameterTypes(), "$4")).append(");");
        }

        c3.append(" }");

        mns.add(mn);
        if (m.getDeclaringClass() == c) {
          dmns.add(mn);
        }
        ms.put(ReflectUtils.getDesc(m), m);
      }
      c3.append(" } catch(Throwable e) { ");
      c3.append("     throw new java.lang.reflect.InvocationTargetException(e); ");
      c3.append(" }");
    }

    c3.append(" throw new " + NoSuchMethodException.class.getName()
        + "(\"Not found method \\\"\"+$2+\"\\\" in class " + c.getName() + ".\"); }");

    // deal with get/set method.
    Matcher matcher;
    for (Map.Entry<String, Method> entry : ms.entrySet()) {
      String md = entry.getKey();
      Method method = entry.getValue();
      if ((matcher = ReflectUtils.GETTER_METHOD_DESC_PATTERN.matcher(md)).matches()) {
        String pn = propertyName(matcher.group(1));
        c2.append(" if( $2.equals(\"").append(pn).append("\") ){ return ($w)w.")
            .append(method.getName()).append("(); }");
        pts.put(pn, method.getReturnType());
      } else if ((matcher = ReflectUtils.IS_HAS_CAN_METHOD_DESC_PATTERN.matcher(md)).matches()) {
        String pn = propertyName(matcher.group(1));
        c2.append(" if( $2.equals(\"").append(pn).append("\") ){ return ($w)w.")
            .append(method.getName()).append("(); }");
        pts.put(pn, method.getReturnType());
      } else if ((matcher = ReflectUtils.SETTER_METHOD_DESC_PATTERN.matcher(md)).matches()) {
        Class<?> pt = method.getParameterTypes()[0];
        String pn = propertyName(matcher.group(1));
        c1.append(" if( $2.equals(\"").append(pn).append("\") ){ w.").append(method.getName())
            .append("(").append(arg(pt, "$3")).append("); return; }");
        pts.put(pn, pt);
      }
    }
    c1.append(" throw new " + NoSuchPropertyException.class.getName()
        + "(\"Not found property \\\"\"+$2+\"\\\" field or setter method in class " + c.getName()
        + ".\"); }");
    c2.append(" throw new " + NoSuchPropertyException.class.getName()
        + "(\"Not found property \\\"\"+$2+\"\\\" field or setter method in class " + c.getName()
        + ".\"); }");

    // make class
    long id = WRAPPER_CLASS_COUNTER.getAndIncrement();
    ClassGenerator cc = ClassGenerator.newInstance(cl);
    cc.setClassName(
        (Modifier.isPublic(c.getModifiers()) ? Wrapper.class.getName() : c.getName() + "$sw") + id);
    cc.setSuperClass(Wrapper.class);

    cc.addDefaultConstructor();
    cc.addField("public static String[] pns;"); // property name array.
    cc.addField("public static " + Map.class.getName() + " pts;"); // property type map.
    cc.addField("public static String[] mns;"); // all method name array.
    cc.addField("public static String[] dmns;"); // declared method name array.
    for (int i = 0, len = ms.size(); i < len; i++) {
      cc.addField("public static Class[] mts" + i + ";");
    }

    cc.addMethod("public String[] getPropertyNames(){ return pns; }");
    cc.addMethod("public boolean hasProperty(String n){ return pts.containsKey($1); }");
    cc.addMethod("public Class getPropertyType(String n){ return (Class)pts.get($1); }");
    cc.addMethod("public String[] getMethodNames(){ return mns; }");
    cc.addMethod("public String[] getDeclaredMethodNames(){ return dmns; }");
    cc.addMethod(c1.toString());
    cc.addMethod(c2.toString());
    cc.addMethod(c3.toString());

    try {
      Class<?> wc = cc.toClass();
      // setup static field.
      wc.getField("pts").set(null, pts);
      wc.getField("pns").set(null, pts.keySet().toArray(new String[0]));
      wc.getField("mns").set(null, mns.toArray(new String[0]));
      wc.getField("dmns").set(null, dmns.toArray(new String[0]));
      int ix = 0;
      for (Method m : ms.values()) {
        wc.getField("mts" + ix++).set(null, m.getParameterTypes());
      }
      return (Wrapper) wc.newInstance();
    } catch (RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      throw new RuntimeException(e.getMessage(), e);
    } finally {
      cc.release();
      ms.clear();
      mns.clear();
      dmns.clear();
    }
  }

  private static String arg(Class<?> cl, String name) {
    if (cl.isPrimitive()) {
      if (cl == Boolean.TYPE) {
        return "((Boolean)" + name + ").booleanValue()";
      }
      if (cl == Byte.TYPE) {
        return "((Byte)" + name + ").byteValue()";
      }
      if (cl == Character.TYPE) {
        return "((Character)" + name + ").charValue()";
      }
      if (cl == Double.TYPE) {
        return "((Number)" + name + ").doubleValue()";
      }
      if (cl == Float.TYPE) {
        return "((Number)" + name + ").floatValue()";
      }
      if (cl == Integer.TYPE) {
        return "((Number)" + name + ").intValue()";
      }
      if (cl == Long.TYPE) {
        return "((Number)" + name + ").longValue()";
      }
      if (cl == Short.TYPE) {
        return "((Number)" + name + ").shortValue()";
      }
      throw new RuntimeException("Unknown primitive type: " + cl.getName());
    }
    return "(" + ReflectUtils.getName(cl) + ")" + name;
  }

  private static String args(Class<?>[] cs, String name) {
    int len = cs.length;
    if (len == 0) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < len; i++) {
      if (i > 0) {
        sb.append(',');
      }
      sb.append(arg(cs[i], name + "[" + i + "]"));
    }
    return sb.toString();
  }

  private static String propertyName(String pn) {
    return pn.length() == 1 || Character.isLowerCase(pn.charAt(1)) ?
        Character.toLowerCase(pn.charAt(0)) + pn.substring(1) : pn;
  }

  private static boolean hasMethods(Method[] methods) {
    if (methods == null || methods.length == 0) {
      return false;
    }
    for (Method m : methods) {
      if (m.getDeclaringClass() != Object.class) {
        return true;
      }
    }
    return false;
  }

  /**
   * get property name array.
   *
   * @return property name array.
   */
  abstract public String[] getPropertyNames();

  /**
   * get property type.
   *
   * @param pn property name.
   * @return Property type or nul.
   */
  abstract public Class<?> getPropertyType(String pn);

  /**
   * has property.
   *
   * @param name property name.
   * @return has or has not.
   */
  abstract public boolean hasProperty(String name);

  /**
   * get property value.
   *
   * @param instance instance.
   * @param pn       property name.
   * @return value.
   */
  abstract public Object getPropertyValue(Object instance, String pn)
      throws NoSuchPropertyException, IllegalArgumentException;

  /**
   * set property value.
   *
   * @param instance instance.
   * @param pn       property name.
   * @param pv       property value.
   */
  abstract public void setPropertyValue(Object instance, String pn, Object pv)
      throws NoSuchPropertyException, IllegalArgumentException;

  /**
   * get property value.
   *
   * @param instance instance.
   * @param pns      property name array.
   * @return value array.
   */
  public Object[] getPropertyValues(Object instance, String[] pns)
      throws NoSuchPropertyException, IllegalArgumentException {
    Object[] ret = new Object[pns.length];
    for (int i = 0; i < ret.length; i++) {
      ret[i] = getPropertyValue(instance, pns[i]);
    }
    return ret;
  }

  /**
   * set property value.
   *
   * @param instance instance.
   * @param pns      property name array.
   * @param pvs      property value array.
   */
  public void setPropertyValues(Object instance, String[] pns, Object[] pvs)
      throws NoSuchPropertyException, IllegalArgumentException {
    if (pns.length != pvs.length) {
      throw new IllegalArgumentException("pns.length != pvs.length");
    }

    for (int i = 0; i < pns.length; i++) {
      setPropertyValue(instance, pns[i], pvs[i]);
    }
  }

  /**
   * get method name array.
   *
   * @return method name array.
   */
  abstract public String[] getMethodNames();

  /**
   * get method name array.
   *
   * @return method name array.
   */
  abstract public String[] getDeclaredMethodNames();

  /**
   * has method.
   *
   * @param name method name.
   * @return has or has not.
   */
  public boolean hasMethod(String name) {
    for (String mn : getMethodNames()) {
      if (mn.equals(name)) {
        return true;
      }
    }
    return false;
  }

  /**
   * invoke method.
   *
   * @param instance instance.
   * @param mn       method name.
   * @param types
   * @param args     argument array.
   * @return return value.
   */
  abstract public Object invokeMethod(Object instance, String mn, Class<?>[] types, Object[] args)
      throws NoSuchMethodException, InvocationTargetException;
}


abstract class Proxy {

  public static final InvocationHandler RETURN_NULL_INVOKER = (proxy, method, args) -> null;
  public static final InvocationHandler THROW_UNSUPPORTED_INVOKER = new InvocationHandler() {
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
      throw new UnsupportedOperationException(
          "Method [" + ReflectUtils.getName(method) + "] unimplemented.");
    }
  };
  private static final AtomicLong PROXY_CLASS_COUNTER = new AtomicLong(0);
  private static final String PACKAGE_NAME = Proxy.class.getPackage().getName();
  private static final Map<ClassLoader, Map<String, Object>> ProxyCacheMap = new WeakHashMap<ClassLoader, Map<String, Object>>();

  private static final Object PendingGenerationMarker = new Object();

  protected Proxy() {
  }

  /**
   * Get proxy.
   *
   * @param ics interface class array.
   * @return Proxy instance.
   */
  public static Proxy getProxy(Class<?>... ics) {
    return getProxy(ClassHelper.getClassLoader(Proxy.class), ics);
  }

  /**
   * Get proxy.
   *
   * @param cl  class loader.
   * @param ics interface class array.
   * @return Proxy instance.
   */
  public static Proxy getProxy(ClassLoader cl, Class<?>... ics) {
    if (ics.length > Constants.MAX_PROXY_COUNT) {
      throw new IllegalArgumentException("interface limit exceeded");
    }

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < ics.length; i++) {
      String itf = ics[i].getName();
      if (!ics[i].isInterface()) {
        throw new RuntimeException(itf + " is not a interface.");
      }

      Class<?> tmp = null;
      try {
        tmp = Class.forName(itf, false, cl);
      } catch (ClassNotFoundException e) {
      }

      if (tmp != ics[i]) {
        throw new IllegalArgumentException(ics[i] + " is not visible from class loader");
      }

      sb.append(itf).append(';');
    }

    // use interface class name list as key.
    String key = sb.toString();

    // get cache by class loader.
    Map<String, Object> cache;
    synchronized (ProxyCacheMap) {
      cache = ProxyCacheMap.computeIfAbsent(cl, k -> new HashMap<>());
    }

    Proxy proxy = null;
    synchronized (cache) {
      do {
        Object value = cache.get(key);
        if (value instanceof Reference<?>) {
          proxy = (Proxy) ((Reference<?>) value).get();
          if (proxy != null) {
            return proxy;
          }
        }

        if (value == PendingGenerationMarker) {
          try {
            cache.wait();
          } catch (InterruptedException e) {
          }
        } else {
          cache.put(key, PendingGenerationMarker);
          break;
        }
      }
      while (true);
    }

    long id = PROXY_CLASS_COUNTER.getAndIncrement();
    String pkg = null;
    ClassGenerator ccp = null, ccm = null;
    try {
      ccp = ClassGenerator.newInstance(cl);

      Set<String> worked = new HashSet<>();
      List<Method> methods = new ArrayList<>();

      for (int i = 0; i < ics.length; i++) {
        if (!Modifier.isPublic(ics[i].getModifiers())) {
          String npkg = ics[i].getPackage().getName();
          if (pkg == null) {
            pkg = npkg;
          } else {
            if (!pkg.equals(npkg)) {
              throw new IllegalArgumentException("non-public interfaces from different packages");
            }
          }
        }
        ccp.addInterface(ics[i]);

        for (Method method : ics[i].getMethods()) {
          String desc = ReflectUtils.getDesc(method);
          if (worked.contains(desc)) {
            continue;
          }
          worked.add(desc);

          int ix = methods.size();
          Class<?> rt = method.getReturnType();
          Class<?>[] pts = method.getParameterTypes();

          StringBuilder code = new StringBuilder("Object[] args = new Object[").append(pts.length)
              .append("];");
          for (int j = 0; j < pts.length; j++) {
            code.append(" args[").append(j).append("] = ($w)$").append(j + 1).append(";");
          }
          code.append(" Object ret = handler.invoke(this, methods[").append(ix).append("], args);");
          if (!Void.TYPE.equals(rt)) {
            code.append(" return ").append(asArgument(rt, "ret")).append(";");
          }

          methods.add(method);
          ccp.addMethod(method.getName(), method.getModifiers(), rt, pts,
              method.getExceptionTypes(), code.toString());
        }
      }

      if (pkg == null) {
        pkg = PACKAGE_NAME;
      }

      // create ProxyInstance class.
      String pcn = pkg + ".proxy" + id;
      ccp.setClassName(pcn);
      ccp.addField("public static java.lang.reflect.Method[] methods;");
      ccp.addField("private " + InvocationHandler.class.getName() + " handler;");
      ccp.addConstructor(Modifier.PUBLIC, new Class<?>[]{InvocationHandler.class}, new Class<?>[0],
          "handler=$1;");
      ccp.addDefaultConstructor();
      Class<?> clazz = ccp.toClass();
      clazz.getField("methods").set(null, methods.toArray(new Method[0]));

      // create Proxy class.
      String fcn = Proxy.class.getName() + id;
      ccm = ClassGenerator.newInstance(cl);
      ccm.setClassName(fcn);
      ccm.addDefaultConstructor();
      ccm.setSuperClass(Proxy.class);
      ccm.addMethod(
          "public Object newInstance(" + InvocationHandler.class.getName() + " h){ return new "
              + pcn + "($1); }");
      Class<?> pc = ccm.toClass();
      proxy = (Proxy) pc.newInstance();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    } finally {
      // release ClassGenerator
      if (ccp != null) {
        ccp.release();
      }
      if (ccm != null) {
        ccm.release();
      }
      synchronized (cache) {
        if (proxy == null) {
          cache.remove(key);
        } else {
          cache.put(key, new WeakReference<Proxy>(proxy));
        }
        cache.notifyAll();
      }
    }
    return proxy;
  }

  private static String asArgument(Class<?> cl, String name) {
    if (cl.isPrimitive()) {
      if (Boolean.TYPE == cl) {
        return name + "==null?false:((Boolean)" + name + ").booleanValue()";
      }
      if (Byte.TYPE == cl) {
        return name + "==null?(byte)0:((Byte)" + name + ").byteValue()";
      }
      if (Character.TYPE == cl) {
        return name + "==null?(char)0:((Character)" + name + ").charValue()";
      }
      if (Double.TYPE == cl) {
        return name + "==null?(double)0:((Double)" + name + ").doubleValue()";
      }
      if (Float.TYPE == cl) {
        return name + "==null?(float)0:((Float)" + name + ").floatValue()";
      }
      if (Integer.TYPE == cl) {
        return name + "==null?(int)0:((Integer)" + name + ").intValue()";
      }
      if (Long.TYPE == cl) {
        return name + "==null?(long)0:((Long)" + name + ").longValue()";
      }
      if (Short.TYPE == cl) {
        return name + "==null?(short)0:((Short)" + name + ").shortValue()";
      }
      throw new RuntimeException(name + " is unknown primitive type.");
    }
    return "(" + ReflectUtils.getName(cl) + ")" + name;
  }

  /**
   * get instance with default handler.
   *
   * @return instance.
   */
  public Object newInstance() {
    return newInstance(THROW_UNSUPPORTED_INVOKER);
  }

  /**
   * get instance with special handler.
   *
   * @return instance.
   */
  abstract public Object newInstance(InvocationHandler handler);
}


final class ClassGenerator {

  private static final AtomicLong CLASS_NAME_COUNTER = new AtomicLong(0);
  private static final String SIMPLE_NAME_TAG = "<init>";
  private static final Map<ClassLoader, ClassPool> POOL_MAP = new ConcurrentHashMap<ClassLoader, ClassPool>(); //ClassLoader - ClassPool
  private ClassPool mPool;
  private CtClass mCtc;
  private String mClassName;
  private String mSuperClass;
  private Set<String> mInterfaces;
  private List<String> mFields;
  private List<String> mConstructors;
  private List<String> mMethods;
  private Map<String, Method> mCopyMethods; // <method desc,method instance>
  private Map<String, Constructor<?>> mCopyConstructors; // <constructor desc,constructor instance>
  private boolean mDefaultConstructor = false;

  private ClassGenerator() {
  }

  private ClassGenerator(ClassPool pool) {
    mPool = pool;
  }

  public static ClassGenerator newInstance() {
    return new ClassGenerator(getClassPool(Thread.currentThread().getContextClassLoader()));
  }

  public static ClassGenerator newInstance(ClassLoader loader) {
    return new ClassGenerator(getClassPool(loader));
  }

  public static boolean isDynamicClass(Class<?> cl) {
    return ClassGenerator.DC.class.isAssignableFrom(cl);
  }

  public static ClassPool getClassPool(ClassLoader loader) {
    if (loader == null) {
      return ClassPool.getDefault();
    }

    ClassPool pool = POOL_MAP.get(loader);
    if (pool == null) {
      pool = new ClassPool(true);
      pool.appendClassPath(new LoaderClassPath(loader));
      POOL_MAP.put(loader, pool);
    }
    return pool;
  }

  private static String modifier(int mod) {
    StringBuilder modifier = new StringBuilder();
    if (Modifier.isPublic(mod)) {
      modifier.append("public");
    }
    if (Modifier.isProtected(mod)) {
      modifier.append("protected");
    }
    if (Modifier.isPrivate(mod)) {
      modifier.append("private");
    }

    if (Modifier.isStatic(mod)) {
      modifier.append(" static");
    }
    if (Modifier.isVolatile(mod)) {
      modifier.append(" volatile");
    }

    return modifier.toString();
  }

  public String getClassName() {
    return mClassName;
  }

  public ClassGenerator setClassName(String name) {
    mClassName = name;
    return this;
  }

  public ClassGenerator addInterface(String cn) {
    if (mInterfaces == null) {
      mInterfaces = new HashSet<String>();
    }
    mInterfaces.add(cn);
    return this;
  }

  public ClassGenerator addInterface(Class<?> cl) {
    return addInterface(cl.getName());
  }

  public ClassGenerator setSuperClass(String cn) {
    mSuperClass = cn;
    return this;
  }

  public ClassGenerator setSuperClass(Class<?> cl) {
    mSuperClass = cl.getName();
    return this;
  }

  public ClassGenerator addField(String code) {
    if (mFields == null) {
      mFields = new ArrayList<String>();
    }
    mFields.add(code);
    return this;
  }

  public ClassGenerator addField(String name, int mod, Class<?> type) {
    return addField(name, mod, type, null);
  }

  public ClassGenerator addField(String name, int mod, Class<?> type, String def) {
    StringBuilder sb = new StringBuilder();
    sb.append(modifier(mod)).append(' ').append(ReflectUtils.getName(type)).append(' ');
    sb.append(name);
    if (!StringUtils.isEmpty(def)) {
      sb.append('=');
      sb.append(def);
    }
    sb.append(';');
    return addField(sb.toString());
  }

  public ClassGenerator addMethod(String code) {
    if (mMethods == null) {
      mMethods = new ArrayList<String>();
    }
    mMethods.add(code);
    return this;
  }

  public ClassGenerator addMethod(String name, int mod, Class<?> rt, Class<?>[] pts, String body) {
    return addMethod(name, mod, rt, pts, null, body);
  }

  public ClassGenerator addMethod(String name, int mod, Class<?> rt, Class<?>[] pts, Class<?>[] ets,
      String body) {
    StringBuilder sb = new StringBuilder();
    sb.append(modifier(mod)).append(' ').append(ReflectUtils.getName(rt)).append(' ').append(name);
    sb.append('(');
    for (int i = 0; i < pts.length; i++) {
      if (i > 0) {
        sb.append(',');
      }
      sb.append(ReflectUtils.getName(pts[i]));
      sb.append(" arg").append(i);
    }
    sb.append(')');
    if (ArrayUtils.isNotEmpty(ets)) {
      sb.append(" throws ");
      for (int i = 0; i < ets.length; i++) {
        if (i > 0) {
          sb.append(',');
        }
        sb.append(ReflectUtils.getName(ets[i]));
      }
    }
    sb.append('{').append(body).append('}');
    return addMethod(sb.toString());
  }

  public ClassGenerator addMethod(Method m) {
    addMethod(m.getName(), m);
    return this;
  }

  public ClassGenerator addMethod(String name, Method m) {
    String desc = name + ReflectUtils.getDescWithoutMethodName(m);
    addMethod(':' + desc);
    if (mCopyMethods == null) {
      mCopyMethods = new ConcurrentHashMap<String, Method>(8);
    }
    mCopyMethods.put(desc, m);
    return this;
  }

  public ClassGenerator addConstructor(String code) {
    if (mConstructors == null) {
      mConstructors = new LinkedList<String>();
    }
    mConstructors.add(code);
    return this;
  }

  public ClassGenerator addConstructor(int mod, Class<?>[] pts, String body) {
    return addConstructor(mod, pts, null, body);
  }

  public ClassGenerator addConstructor(int mod, Class<?>[] pts, Class<?>[] ets, String body) {
    StringBuilder sb = new StringBuilder();
    sb.append(modifier(mod)).append(' ').append(SIMPLE_NAME_TAG);
    sb.append('(');
    for (int i = 0; i < pts.length; i++) {
      if (i > 0) {
        sb.append(',');
      }
      sb.append(ReflectUtils.getName(pts[i]));
      sb.append(" arg").append(i);
    }
    sb.append(')');
    if (ArrayUtils.isNotEmpty(ets)) {
      sb.append(" throws ");
      for (int i = 0; i < ets.length; i++) {
        if (i > 0) {
          sb.append(',');
        }
        sb.append(ReflectUtils.getName(ets[i]));
      }
    }
    sb.append('{').append(body).append('}');
    return addConstructor(sb.toString());
  }

  public ClassGenerator addConstructor(Constructor<?> c) {
    String desc = ReflectUtils.getDesc(c);
    addConstructor(":" + desc);
    if (mCopyConstructors == null) {
      mCopyConstructors = new ConcurrentHashMap<String, Constructor<?>>(4);
    }
    mCopyConstructors.put(desc, c);
    return this;
  }

  public ClassGenerator addDefaultConstructor() {
    mDefaultConstructor = true;
    return this;
  }

  public ClassPool getClassPool() {
    return mPool;
  }

  public Class<?> toClass() {
    return toClass(ClassHelper.getClassLoader(ClassGenerator.class),
        getClass().getProtectionDomain());
  }

  public Class<?> toClass(ClassLoader loader, ProtectionDomain pd) {
    if (mCtc != null) {
      mCtc.detach();
    }
    long id = CLASS_NAME_COUNTER.getAndIncrement();
    try {
      CtClass ctcs = mSuperClass == null ? null : mPool.get(mSuperClass);
      if (mClassName == null) {
        mClassName = (mSuperClass == null || javassist.Modifier.isPublic(ctcs.getModifiers())
            ? ClassGenerator.class.getName() : mSuperClass + "$sc") + id;
      }
      mCtc = mPool.makeClass(mClassName);
      if (mSuperClass != null) {
        mCtc.setSuperclass(ctcs);
      }
      mCtc.addInterface(mPool.get(DC.class.getName())); // add dynamic class tag.
      if (mInterfaces != null) {
        for (String cl : mInterfaces) {
          mCtc.addInterface(mPool.get(cl));
        }
      }
      if (mFields != null) {
        for (String code : mFields) {
          mCtc.addField(CtField.make(code, mCtc));
        }
      }
      if (mMethods != null) {
        for (String code : mMethods) {
          if (code.charAt(0) == ':') {
            mCtc.addMethod(CtNewMethod.copy(getCtMethod(mCopyMethods.get(code.substring(1))),
                code.substring(1, code.indexOf('(')), mCtc, null));
          } else {
            mCtc.addMethod(CtNewMethod.make(code, mCtc));
          }
        }
      }
      if (mDefaultConstructor) {
        mCtc.addConstructor(CtNewConstructor.defaultConstructor(mCtc));
      }
      if (mConstructors != null) {
        for (String code : mConstructors) {
          if (code.charAt(0) == ':') {
            mCtc.addConstructor(CtNewConstructor
                .copy(getCtConstructor(mCopyConstructors.get(code.substring(1))), mCtc, null));
          } else {
            String[] sn = mCtc.getSimpleName().split("\\$+"); // inner class name include $.
            mCtc.addConstructor(
                CtNewConstructor.make(code.replaceFirst(SIMPLE_NAME_TAG, sn[sn.length - 1]), mCtc));
          }
        }
      }
      return mCtc.toClass(loader, pd);
    } catch (RuntimeException e) {
      throw e;
    } catch (NotFoundException e) {
      throw new RuntimeException(e.getMessage(), e);
    } catch (CannotCompileException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  public void release() {
    if (mCtc != null) {
      mCtc.detach();
    }
    if (mInterfaces != null) {
      mInterfaces.clear();
    }
    if (mFields != null) {
      mFields.clear();
    }
    if (mMethods != null) {
      mMethods.clear();
    }
    if (mConstructors != null) {
      mConstructors.clear();
    }
    if (mCopyMethods != null) {
      mCopyMethods.clear();
    }
    if (mCopyConstructors != null) {
      mCopyConstructors.clear();
    }
  }

  private CtClass getCtClass(Class<?> c) throws NotFoundException {
    return mPool.get(c.getName());
  }

  private CtMethod getCtMethod(Method m) throws NotFoundException {
    return getCtClass(m.getDeclaringClass())
        .getMethod(m.getName(), ReflectUtils.getDescWithoutMethodName(m));
  }

  private CtConstructor getCtConstructor(Constructor<?> c) throws NotFoundException {
    return getCtClass(c.getDeclaringClass()).getConstructor(ReflectUtils.getDesc(c));
  }

  public static interface DC {

  } // dynamic class tag interface.
}