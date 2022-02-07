package dubbo.mini.config.spring;

import dubbo.mini.common.Constants;
import dubbo.mini.common.NetURL;
import dubbo.mini.support.DemoService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ReferenceBeanTest {

  @Test
  void test() {
    ReferenceBean<DemoService> referenceBean = new ReferenceBean<>();
    NetURL url = new NetURL("dubbo", "127.0.0.1", 2022, "", null);
    url = url.addParameter(Constants.INTERFACES, DemoService.class.getName());
    referenceBean.setInterface(DemoService.class);
    referenceBean.setInterface(DemoService.class.getName());
    referenceBean.setUrl(url.toString());
    DemoService demoService = referenceBean.get();
    Assertions.assertNotNull(demoService);
  }
}
