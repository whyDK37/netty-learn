package dubbo.mini.support;

import dubbo.mini.common.utils.StringUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SPI 类加载器
 */
public class ExtensionLoader<T> {

  private static final Logger logger = LoggerFactory
      .getLogger(ExtensionLoader.class);

  private static final String SELF_DIRECTORY = "META-INF/self/";
  private static final String INTERNAL_DIRECTORY = "META-INF/internal/";

  private static final Pattern NAME_SEPARATOR = Pattern
      .compile("\\s*[,]+\\s*");

  private static final ConcurrentMap<Class<?>, ExtensionLoader<?>> EXTENSION_LOADERS = new ConcurrentHashMap<>();

  private static final ConcurrentMap<Class<?>, Object> EXTENSION_INSTANCES = new ConcurrentHashMap<>();

  private static final ConcurrentMap<String, Object> EXTENSION_KEY_INSTANCE = new ConcurrentHashMap<>();

  private final Class<?> type;

  private final ConcurrentMap<Class<?>, String> cachedNames = new ConcurrentHashMap<>();

  private final Holder<Map<String, Class<?>>> cachedClasses = new Holder<>();

  private final ConcurrentMap<String, Holder<Object>> cachedInstances = new ConcurrentHashMap<>();

  private String cachedDefaultName;

  private ConcurrentHashMap<String, IllegalStateException> exceptions = new ConcurrentHashMap<>();

  private static <T> boolean withExtensionAnnotation(Class<T> type) {
    return type.isAnnotationPresent(SPI.class);
  }

  @SuppressWarnings("unchecked")
  public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) {
    if (type == null) {
      throw new IllegalArgumentException("Extension type == null");
    }
    if (!type.isInterface()) {
      throw new IllegalArgumentException("Extension type(" + type + ") is not interface!");
    }
    if (!withExtensionAnnotation(type)) {
      throw new IllegalArgumentException(
          "Extension type(" + type + ") is not extension, because WITHOUT @"
              + SPI.class.getSimpleName() + " Annotation!");
    }

    ExtensionLoader<T> loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
    if (loader == null) {
      EXTENSION_LOADERS.putIfAbsent(type, new ExtensionLoader<T>(type));
      loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
    }
    return loader;
  }

  private ExtensionLoader(Class<?> type) {
    this.type = type;
  }

  /**
   * 返回指定名字的扩展
   *
   * @param name
   * @return
   */
  @SuppressWarnings("unchecked")
  public T getExtension(String name) {
    if (name == null || name.length() == 0) {
      throw new IllegalArgumentException("Extension name == null");
    }
    if ("true".equals(name)) {
      return getDefaultExtension();
    }
    Holder<Object> holder = cachedInstances.get(name);
    if (holder == null) {
      cachedInstances.putIfAbsent(name, new Holder<>());
      holder = cachedInstances.get(name);
    }
    Object instance = holder.get();
    if (instance == null) {
      synchronized (holder) {
        instance = holder.get();
        if (instance == null) {
          instance = createExtension(name);
          holder.set(instance);
        }
      }
    }
    return (T) instance;
  }

  @SuppressWarnings("unchecked")
  public T getExtension(String name, String key) {
    if (name == null || name.length() == 0) {
      throw new IllegalArgumentException("Extension name == null");
    }
    if ("true".equals(name)) {
      return getDefaultExtension();
    }
    String extKey = name + "-" + StringUtils.trimToEmpty(key);
    Holder<Object> holder = cachedInstances.get(extKey);
    if (holder == null) {
      cachedInstances.putIfAbsent(extKey, new Holder<>());
      holder = cachedInstances.get(extKey);
    }
    Object instance = holder.get();
    if (instance == null) {
      synchronized (holder) {
        instance = holder.get();
        if (instance == null) {
          instance = createExtension(name, key);
          holder.set(instance);
        }
      }
    }
    return (T) instance;
  }

  /**
   * 返回缺省的扩展，如果没有设置则返回<code>null</code>
   */
  public T getDefaultExtension() {
    getExtensionClasses();
    if (null == cachedDefaultName || cachedDefaultName.length() == 0 || "true"
        .equals(cachedDefaultName)) {
      return null;
    }
    return getExtension(cachedDefaultName);
  }

  @SuppressWarnings("unchecked")
  private T createExtension(String name) {
    Class<?> clazz = getExtensionClasses().get(name);
    if (clazz == null) {
      throw new IllegalStateException("Extension instance(name: " + name + ", class: " + type
          + ")  could not be instantiated: class could not be found");
    }
    try {
      T instance = (T) EXTENSION_INSTANCES.get(clazz);
      if (instance == null) {
        EXTENSION_INSTANCES.putIfAbsent(clazz, (T) clazz.newInstance());
        instance = (T) EXTENSION_INSTANCES.get(clazz);
      }
      return instance;
    } catch (Throwable t) {
      throw new IllegalStateException("Extension instance(name: " + name + ", class: " + type
          + ")  could not be instantiated: " + t.getMessage(),
          t);
    }
  }

  @SuppressWarnings("unchecked")
  private T createExtension(String name, String key) {
    Class<?> clazz = getExtensionClasses().get(name);
    if (clazz == null) {
      throw new IllegalStateException("Extension instance(name: " + name + ", class: " + type
          + ")  could not be instantiated: class could not be found");
    }
    try {
      T instance = (T) EXTENSION_KEY_INSTANCE.get(name + "-" + key);
      if (instance == null) {
        EXTENSION_KEY_INSTANCE.putIfAbsent(name + "-" + key, clazz.newInstance());
        instance = (T) EXTENSION_KEY_INSTANCE.get(name + "-" + key);
      }
      return instance;
    } catch (Throwable t) {
      throw new IllegalStateException("Extension instance(name: " + name + ", class: " + type
          + ")  could not be instantiated: " + t.getMessage(),
          t);
    }
  }

  private Map<String, Class<?>> getExtensionClasses() {
    Map<String, Class<?>> classes = cachedClasses.get();
    if (classes == null) {
      synchronized (cachedClasses) {
        classes = cachedClasses.get();
        if (classes == null) {
          classes = loadExtensionClasses();
          cachedClasses.set(classes);
        }
      }
    }

    return classes;
  }

  private String getJarDirectoryPath() {
    URL url = Thread.currentThread().getContextClassLoader().getResource("");
    String dirtyPath;
    if (url != null) {
      dirtyPath = url.toString();
    } else {
      File file = new File("");
      dirtyPath = file.getAbsolutePath();
    }
    String jarPath = dirtyPath.replaceAll("^.*file:/", ""); // removes
    // file:/ and
    // everything
    // before it
    jarPath = jarPath.replaceAll("jar!.*", "jar"); // removes everything
    // after .jar, if .jar
    // exists in dirtyPath
    jarPath = jarPath.replaceAll("%20", " "); // necessary if path has
    // spaces within
    if (!jarPath.endsWith(".jar")) { // this is needed if you plan to run
      // the app using Spring Tools Suit play
      // button.
      jarPath = jarPath.replaceAll("/classes/.*", "/classes/");
    }
    Path path = Paths.get(jarPath).getParent(); // Paths - from java 8
    if (path != null) {
      return path.toString();
    }
    return null;
  }

  private Map<String, Class<?>> loadExtensionClasses() {
    final SPI defaultAnnotation = type.getAnnotation(SPI.class);
    if (defaultAnnotation != null) {
      String value = defaultAnnotation.value();
      if ((value = value.trim()).length() > 0) {
        String[] names = NAME_SEPARATOR.split(value);
        if (names.length > 1) {
          throw new IllegalStateException(
              "more than 1 default extension name on extension " + type.getName()
                  + ": " + Arrays.toString(names));
        }
        if (names.length == 1) {
          cachedDefaultName = names[0];
        }
      }
    }

    Map<String, Class<?>> extensionClasses = new HashMap<>();
    // 2. load inner extension class with default classLoader
    // ClassLoader classLoader = findClassLoader();
    loadFile(extensionClasses, INTERNAL_DIRECTORY, this.getClass().getClassLoader());
    loadFile(extensionClasses, SELF_DIRECTORY, this.getClass().getClassLoader());
//         loadFile(extensionClasses, SERVICES_DIRECTORY, localClassLoader);

    return extensionClasses;
  }

  private void loadFile(Map<String, Class<?>> extensionClasses, String dir,
      ClassLoader classLoader) {
    String fileName = dir + type.getName();
    try {
      Enumeration<URL> urls;
      if (classLoader != null) {
        urls = classLoader.getResources(fileName);
      } else {
        urls = ClassLoader.getSystemResources(fileName);
      }
      if (urls != null) {
        while (urls.hasMoreElements()) {
          URL url = urls.nextElement();
          try {
            BufferedReader reader = null;
            try {
              reader = new BufferedReader(new InputStreamReader(url.openStream(), "utf-8"));
              String line = null;
              while ((line = reader.readLine()) != null) {
                final int ci = line.indexOf('#');
                if (ci >= 0) {
                  line = line.substring(0, ci);
                }
                line = line.trim();
                if (line.length() > 0) {
                  try {
                    String name = null;
                    int i = line.indexOf('=');
                    if (i > 0) {
                      name = line.substring(0, i).trim();
                      line = line.substring(i + 1).trim();
                    }
                    if (line.length() > 0) {
                      Class<?> clazz = classLoader.loadClass(line);
                      // Class<?> clazz =
                      // Class.forName(line, true,
                      // classLoader);
                      if (!type.isAssignableFrom(clazz)) {
                        throw new IllegalStateException(
                            "Error when load extension class(interface: " + type
                                + ", class line: " + clazz.getName()
                                + "), class " + clazz.getName()
                                + "is not subtype of interface.");
                      } else {
                        try {
                          clazz.getConstructor(type);
                        } catch (NoSuchMethodException e) {
                          clazz.getConstructor();
                          String[] names = NAME_SEPARATOR.split(name);
                          if (names != null && names.length > 0) {
                            for (String n : names) {
                              if (!cachedNames.containsKey(clazz)) {
                                cachedNames.put(clazz, n);
                              }
                              Class<?> c = extensionClasses.get(n);
                              if (c == null) {
                                extensionClasses.put(n, clazz);
                              } else if (c != clazz) {
                                cachedNames.remove(clazz);
                                throw new IllegalStateException(
                                    "Duplicate extension " + type.getName() + " name "
                                        + n + " on "
                                        + c.getName() + " and "
                                        + clazz.getName());
                              }
                            }
                          }
                        }
                      }
                    }
                  } catch (Throwable t) {
                    IllegalStateException e = new IllegalStateException(
                        "Failed to load extension class(interface: " + type + ", class line: "
                            + line + ") in " + url
                            + ", cause: "
                            + t.getMessage(),
                        t);
                    exceptions.put(line, e);
                  }
                }
              } // end of while read lines
            } finally {
              if (reader != null) {
                reader.close();
              }
            }
          } catch (Throwable t) {
            logger.error(
                "Exception when load extension class(interface: " + type + ", class file: " + url
                    + ") in " + url,
                t);
          }
        } // end of while urls
      }
    } catch (Throwable t) {
      logger.error(
          "Exception when load extension class(interface: " + type + ", description file: "
              + fileName + ").",
          t);
    }
  }

  public boolean hasExtension(String name) {
    if (StringUtils.isEmpty(name)) {
      throw new IllegalArgumentException("Extension name == null");
    }
    Class<?> c = this.getExtensionClass(name);
    return c != null;
  }


  private Class<?> getExtensionClass(String name) {
    if (type == null) {
      throw new IllegalArgumentException("Extension type == null");
    }
    if (name == null) {
      throw new IllegalArgumentException("Extension name == null");
    }
    return getExtensionClasses().get(name);
  }

  @SuppressWarnings("unused")
  private static ClassLoader findClassLoader() {
    return ExtensionLoader.class.getClassLoader();
  }

  @Override
  public String toString() {
    return this.getClass().getName() + "[" + type.getName() + "]";
  }

  public Set<String> getSupportedExtensions() {
    Map<String, Class<?>> clazzes = getExtensionClasses();
    return Collections.unmodifiableSet(new TreeSet<>(clazzes.keySet()));
  }

  private static class Holder<T> {

    private volatile T value;

    private void set(T value) {
      this.value = value;
    }

    private T get() {
      return value;
    }

  }
}
