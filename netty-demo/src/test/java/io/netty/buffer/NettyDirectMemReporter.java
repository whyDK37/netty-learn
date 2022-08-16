package io.netty.buffer;

import io.netty.util.internal.PlatformDependent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class NettyDirectMemReporter {
    private static final Logger log = LoggerFactory.getLogger(NettyDirectMemReporter.class);
    //    private AtomicLong directMem = new AtomicLong();
    private ScheduledExecutorService executor;

    public NettyDirectMemReporter() throws NoSuchFieldException {
//        Class<PlatformDependent> platformDependentClass = PlatformDependent.class;
//        Field field = platformDependentClass.getDeclaredField("DIRECT_MEMORY_COUNTER");
//        field.setAccessible(true);
//        try {
//            directMem = (AtomicLong) field.get(PlatformDependent.class);
//        } catch (IllegalAccessException e) {
//        }
    }

    public void startReport() {
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            printLog();
        }, 0, 1, TimeUnit.SECONDS);
    }

    public void printLog() {
        log.info("netty direct memory size:{}b, max:{}", PlatformDependent.usedDirectMemory(), PlatformDependent.maxDirectMemory());
    }
}