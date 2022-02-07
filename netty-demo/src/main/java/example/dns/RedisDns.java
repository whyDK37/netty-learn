package example.dns;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.resolver.AddressResolver;
import io.netty.resolver.dns.DnsAddressResolverGroup;
import io.netty.resolver.dns.DnsServerAddressStreamProviders;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class RedisDns {

  static DnsAddressResolverGroup resolverGroup;
  // 委托给 InflightNameResolver -> DnsNameResolver 获取域名
  static AddressResolver<InetSocketAddress> resolver = null;
  static NioEventLoopGroup eventExecutors;

  static String host = "r-8vb9a45c41cbd974"
      + ".redis"
      + ".zhangbei"
      + ".rds"
      + ".aliyuncs"
      + ".com";
  static int port = 6379;

  public static void main(String[] args) {
    resolverGroup = new DnsAddressResolverGroup(
        NioDatagramChannel.class, DnsServerAddressStreamProviders.platformDefault());

    eventExecutors = new NioEventLoopGroup(4,
        new DefaultThreadFactory("redis-dns"));

    resolver = resolverGroup.getResolver(eventExecutors.next());

    monitorChange();
  }

  private static void monitorChange() {
    eventExecutors.schedule(new Runnable() {
      @Override
      public void run() {

        monitorMasters();
      }
    }, 5000, TimeUnit.MILLISECONDS);
  }

  private static void monitorMasters() {
    System.out.println("resolver = " + resolver);
    Future<InetSocketAddress> resolveFuture = resolver.resolve(
        InetSocketAddress.createUnresolved(host,
            port));
    resolveFuture.addListener(new FutureListener<InetSocketAddress>() {
      @Override
      public void operationComplete(Future<InetSocketAddress> future) {
        monitorChange();
        if (!future.isSuccess()) {
          System.out.println("Unable to resolve " + host);
          future.cause().printStackTrace();
          // fixme 如果异常时没有重新获取 resolver 会一直获取不到dns。
          resolver.close();
          resolver = resolverGroup.getResolver(eventExecutors.next());
          return;
        }

        InetSocketAddress newMasterAddr = future.getNow();
        System.out.println("Detected DNS : " +
            newMasterAddr.getAddress().getHostAddress());
      }
    });
  }
}
