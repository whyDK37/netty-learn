package fep;

import fep.client.ClientCnx;

import java.util.concurrent.TimeUnit;

import static fep.ServerConst.*;

public class ClientCnxTest {
    public static void main(String[] args) throws Throwable {
        ClientCnx clientCnx = new ClientCnx(ip, port, timeout, idleTimeout, connectTimeout);
        clientCnx.connect();

        String orgCode = "123412";
        MessageInfo.Authentication authentication = clientCnx.authentication(orgCode);
        System.out.println(authentication.getSuccess());

        TimeUnit.DAYS.sleep(1L);
    }

}