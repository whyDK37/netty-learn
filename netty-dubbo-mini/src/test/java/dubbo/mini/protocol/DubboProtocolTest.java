package dubbo.mini.protocol;

import dubbo.mini.common.Constants;
import dubbo.mini.common.NetURL;
import dubbo.mini.rpc.*;
import dubbo.mini.support.DemoService;
import dubbo.mini.support.ExtensionLoader;
import dubbo.mini.support.ServDemoServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DubboProtocolTest {


    @Test
    void createServer() {
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getDefaultExtension();

        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getDefaultExtension();

        NetURL url = new NetURL("dubbo", "127.0.0.1", 2022, "", null);
//        url.addParameter(Constants.BIND_IP_KEY, "127.0.0.1");
//        url.addParameter(Constants.BIND_PORT_KEY, 12345);
        Invoker<?> invoker = proxyFactory.getInvoker(new ServDemoServiceImpl(), DemoService.class, url);
        System.out.println("invoker = " + invoker);
        System.out.println("invoker.getClass() = " + invoker.getClass());
//        DelegateProviderMetaDataInvoker wrapperInvoker = new DelegateProviderMetaDataInvoker(invoker, this);
//
        Exporter export = protocol.export(invoker);
        doInvok(export);
    }

    private void doInvok(Exporter export) {

        Invoker invoker = export.getInvoker();
        System.out.println("invoker.getInterface() = " + invoker.getInterface());

        RpcInvocation rpcInvocation = new RpcInvocation();
        rpcInvocation.setMethodName("sayHello");
        rpcInvocation.setParameterTypes(new Class[]{String.class});
        rpcInvocation.setArguments(new Object[]{"test"});

        Result result = invoker.invoke(rpcInvocation);
        System.out.println("result.getValue() = " + result.getValue());
        assertNotNull(result.getValue());

    }

}