package com.atguigu.netty.proto2;

import com.alibaba.fastjson.JSON;
import com.atguigu.netty.proto2.MyDataInfo.Worker;
import com.google.protobuf.InvalidProtocolBufferException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class MyDataInfoTest {
  @Test
  void test() throws InvalidProtocolBufferException {
    Worker worker = Worker.newBuilder().setName("worker测试")
        .setAge(12).build();
    System.out.println(worker);
    System.out.println(worker.toByteString());
    System.out.println(worker.toByteString().toString(StandardCharsets.UTF_8));
    System.out.println(worker.toByteString().toStringUtf8());
    System.out.println(Arrays.toString(worker.toByteArray()));
    System.out.println("bytes.length = " + worker.toByteArray().length);
    String base64 = Base64.getEncoder().encodeToString(worker.toByteArray());
    System.out.println(base64);
    Worker worker1 = Worker.parseFrom(Base64.getDecoder().decode(base64));
    System.out.println("worker1.getName() = " + worker1.getName());
    System.out.println("worker1.getAge() = " + worker1.getAge());

    System.out.println("{\"name\":\"worker测试\",\"age\":12}");
  }

}