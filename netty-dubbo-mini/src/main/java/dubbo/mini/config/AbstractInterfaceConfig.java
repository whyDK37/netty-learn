package dubbo.mini.config;

/**
 * @author why
 */
public abstract class AbstractInterfaceConfig {

    protected String interfaceName;
    protected Class<?> interfaceClass;

    public Class<?> getInterfaceClass() {
        return interfaceClass;
    }

    public AbstractInterfaceConfig setInterfaceClass(Class<?> interfaceClass) {
        this.interfaceClass = interfaceClass;
        return this;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public AbstractInterfaceConfig setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
        return this;
    }
}
