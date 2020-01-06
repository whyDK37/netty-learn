package fep;

import fep.server.ServerCnx;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static fep.ServerConst.*;

public class ServerCnxTest {

    @Test
    public void server() throws Throwable {
        ServerCnx serverCnx = new ServerCnx(ip, port, timeout, idleTimeout, connectTimeout);
        serverCnx.open();
        TimeUnit.DAYS.sleep(1L);
    }

}