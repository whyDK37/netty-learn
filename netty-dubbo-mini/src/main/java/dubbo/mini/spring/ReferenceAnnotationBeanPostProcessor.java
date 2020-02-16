package dubbo.mini.spring;

import dubbo.mini.config.spring.ReferenceBean;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

public class ReferenceAnnotationBeanPostProcessor extends InstantiationAwareBeanPostProcessorAdapter
    implements ApplicationContextAware, ApplicationListener<ApplicationEvent> {

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

  }

  @Override
  public void onApplicationEvent(ApplicationEvent applicationEvent) {

  }


  private static class ReferenceBeanInvocationHandler implements InvocationHandler {

    private final ReferenceBean referenceBean;

    private Object bean;

    private ReferenceBeanInvocationHandler(ReferenceBean referenceBean) {
      this.referenceBean = referenceBean;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      Object result;
      try {
        if (bean == null) { // If the bean is not initialized, invoke init()
          // issue: https://github.com/apache/incubator-dubbo/issues/3429
          init();
        }
        result = method.invoke(bean, args);
      } catch (InvocationTargetException e) {
        // re-throws the actual Exception.
        throw e.getTargetException();
      }
      return result;
    }

    private void init() {
      this.bean = referenceBean.get();
    }
  }
}