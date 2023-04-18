package pg.projekt.sockets.receive;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import pg.projekt.sockets.messages.Message;
import pg.projekt.sockets.send.SendThread;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class Receiver {


    public static void main(String[] args){
        try {
            int port = Integer.valueOf(args[0]);
            port = 10000;
            List<Message> msgList = Collections.synchronizedList(new ArrayList<Message>());
            List<Message> toBeSent = Collections.synchronizedList(new ArrayList<Message>());

            toBeSent.add(new Message("blabla", "sender"));
            toBeSent.add(new Message("blabla2", "sender"));
            toBeSent.add(new Message("blabla3", "sender"));
            toBeSent.add(new Message("blabla4", "sender"));

            ReceiveThread t = new ReceiveThread(msgList, port);
            SendThread t2 = new SendThread("localhost", port+1, msgList, toBeSent);
            //MsgReader t3 = new MsgReader(msgList);

            t.start();
            Thread.sleep(100);
            t2.start();
            //t3.start();
            System.out.println("Started");
            t.getWorker().join();
            Thread.sleep(5000);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }



}
