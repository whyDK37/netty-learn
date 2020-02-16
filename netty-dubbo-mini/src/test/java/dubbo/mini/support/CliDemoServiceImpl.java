package dubbo.mini.support;

/**
 * @author why
 */
public class CliDemoServiceImpl implements DemoService {

  @Override
  public String sayHello(String name) {
    return "client ," + name;
  }
}
