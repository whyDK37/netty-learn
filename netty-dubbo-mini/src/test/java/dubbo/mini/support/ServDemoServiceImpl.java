package dubbo.mini.support;

/**
 * @author why
 */
public class ServDemoServiceImpl implements DemoService {

  @Override
  public String sayHello(String name) {
    return "server," + name;
  }
}
