package dubbo.mini.config;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Inherited
public @interface Service {

  Class<?> interfaceClass() default void.class;

  String interfaceName() default "";

  String path() default "";

  boolean export() default true;

  String proxy() default "";

  String[] protocol() default {};


}
