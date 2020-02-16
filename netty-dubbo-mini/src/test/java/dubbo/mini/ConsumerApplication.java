/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package dubbo.mini;


import dubbo.mini.config.spring.ReferenceBean;
import dubbo.mini.rpc.ProxyFactory;
import dubbo.mini.support.DemoService;
import dubbo.mini.support.ExtensionLoader;
import java.io.IOException;

public class ConsumerApplication {

  private static ProxyFactory proxy = ExtensionLoader.getExtensionLoader(ProxyFactory.class)
      .getDefaultExtension();

  /**
   * In order to make sure multicast registry works, need to specify '-Djava.net.preferIPv4Stack=true'
   * before launch the application
   */
  public static void main(String[] args) throws IOException {

    ReferenceBean<DemoService> reference = new ReferenceBean<>();
    reference.setInterface(DemoService.class);

    DemoService service = reference.get();
    String message = service.sayHello("dubbo");
    System.out.println(message);

    // 客户端回调服务
//        DubboProtocol protocol = DubboProtocol.getDubboProtocol();
//        Collection<Invoker<?>> invokers = protocol.getInvokers();
//        Collection<ExchangeServer> servers = protocol.getServers();
//        Collection<Exporter<?>> exporters = protocol.getExporters();
//        int defaultPort = protocol.getDefaultPort();
//        DemoService cliSer = new CliDemoServiceImpl();
//        NetURL url = NetURL.valueOf("injvm://127.0.0.1/org.apache.dubbo.demo.DemoService")
//                .addParameter(Constants.INTERFACE_KEY, DemoService.class.getName())
//                .addParameter(Constants.IS_CALLBACK_SERVICE, Boolean.TRUE)
//                .addParameter(Constants.METHODS_KEY, "sayHello")
//                .addParameter(Constants.IS_SERVER_KEY, Boolean.FALSE);
//        Exporter<?> exporter = protocol.export(proxy.getInvoker(cliSer, DemoService.class, url));

    System.in.read();
  }
}
