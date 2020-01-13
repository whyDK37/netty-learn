package project.fep;

import dubbo.mini.client.Client;

import java.util.concurrent.TimeUnit;

import static project.fep.ServerConst.*;

public class ClientCnxTest2 {
    public static void main(String[] args) throws Throwable {
        Client clientCnx = new Client("32143", ip, port, timeout, idleTimeout, connectTimeout);

        TimeUnit.DAYS.sleep(1L);
    }

}