package project.fep;

import project.fep.server.ServerCnx;
import project.fep.server.SessionManager;

import java.util.Scanner;

import static project.fep.ServerConst.*;

public class ServerCnxTest {

    public static void main(String[] args) throws Throwable {
        ServerCnx serverCnx = new ServerCnx(ip, port, timeout, idleTimeout, connectTimeout);
        serverCnx.open();
        Scanner scanner = new Scanner(System.in);
        String input;
        while ((input = scanner.next()) != null) {
            if ("list".equals(input)) {
                System.out.println("client:" + SessionManager.getInstance().getMachines().size());
                SessionManager.getInstance().getMachines().forEach(machine -> {
                    System.out.println("machine = " + machine.toString());
                });
            }
        }
    }

}