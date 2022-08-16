package io.netty.buffer;

import io.netty.util.internal.PlatformDependent;
import org.junit.jupiter.api.Test;


/**
 * 完整参数：-ea -XX:MaxDirectMemorySize=1m -Dio.netty.maxDirectMemory=1024k -Dio.netty.leakDetectionLevel=PARANOID -Dio.netty.tryReflectionSetAccessible=true --add-opens=java.base/java.nio=ALL-UNNAMED
 *
 * 诊断参数：
 * -Dio.netty.leakDetectionLevel=ADVANCED
 * <p>
 * 内存参数：
 * -Dio.netty.maxDirectMemory=1024
 * 或者通过jvm参数设置
 * -XX:MaxDirectMemorySize=1m
 * <p>
 * jdk 17 需要显示设置下面的参数
 * -Dio.netty.tryReflectionSetAccessible=true
 * 设置
 * --add-opens=java.base/java.nio=ALL-UNNAMED
 * --add-opens=java.base/sun.nio.ch=ALL-UNNAMED
 */
public class PooledByteBufAllocatorTest {

    public static void main(String[] args) throws NoSuchFieldException {
        PooledByteBufAllocatorTest pooledByteBufAllocatorTest = new PooledByteBufAllocatorTest();
        pooledByteBufAllocatorTest.leapTest();
    }

    @Test
    public void leapTest() throws NoSuchFieldException {
        PooledByteBufAllocator allocator = new PooledByteBufAllocator();
        NettyDirectMemReporter directMemReporter = new NettyDirectMemReporter();
        for (int loop = 0; loop < 200; loop++) {
            System.out.println("loop = " + loop + ", usedDirectMemory = " + PlatformDependent.usedDirectMemory());
            directMemReporter.printLog();

            ByteBuf byteBuf = allocator.newDirectBuffer(1024 * (loop + 1), 1024 * 1024);
        }

    }
}
