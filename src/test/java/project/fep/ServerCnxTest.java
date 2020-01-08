package project.fep;

import project.fep.server.Machine;
import project.fep.server.ServerCnx;
import project.fep.server.SessionManager;
import project.fep.support.DefaultFuture;

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
            } else if ("1".equals(input)) {
                Machine next = SessionManager.getInstance().getMachines().iterator().next();
                MessageInfo.Message message = MessageInfo.Message.newBuilder()
                        .setDataType(MessageInfo.Message.DataType.QUERY)
                        .setId(DefaultFuture.REQUEST_ID.incrementAndGet())
                        .build();
                DefaultFuture defaultFuture = DefaultFuture.newFuture(next.getChannel(), message);
                MessageInfo.Query query = defaultFuture.get();
                System.out.println("query.getSuccess() = " + query.getSuccess());
            }
        }
    }

}