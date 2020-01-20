package dubbo.mini.config;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface Reference {
    Class<?> interfaceClass() default void.class;

    String version() default "";

    String url() default "";

    String client() default "";


    String proxy() default "";

}
