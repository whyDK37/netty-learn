package io.netty.buffer;

import org.junit.jupiter.api.Test;

public class UnpooledByteBufAllocatorTest {

    public static void main(String[] args) {
        UnpooledByteBufAllocatorTest pooledByteBufAllocatorTest = new UnpooledByteBufAllocatorTest();
        pooledByteBufAllocatorTest.leapTest();
    }

    /**
     * -Dio.netty.leakDetectionLevel=ADVANCED
     */
    @Test
    public void leapTest() {
        UnpooledByteBufAllocator allocator = new UnpooledByteBufAllocator(false, false, true);

        for (int loop = 0; loop < Integer.MAX_VALUE; loop++) {
            System.out.println("loop = " + loop);
            ByteBuf byteBuf = allocator.newDirectBuffer(1024 * 1024 * 1024, 1024 * 1024 * 1024);
        }

    }
}
