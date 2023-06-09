package pg.projekt.sockets.receive;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import pg.projekt.sockets.messages.Message;

import java.io.IOException;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class CheckThread implements Runnable{
    private ReceiveThread receiveThread;
    private Thread worker;
    private List<Message> msgList;
    private int port;

    public CheckThread(ReceiveThread receiveThread){
        this.msgList = receiveThread.getReceivedMsgList();
        this.port = receiveThread.getPort();
        this.receiveThread = receiveThread;
    }

    public void start(){
        this.worker = new Thread(this);
        worker.start();
    }

    @Override
    public void run(){

        while(true){

            try {
                if(!receiveThread.getRunning().get()){
                    Thread.sleep(3000);
                    receiveThread.getServerSocket().close();
                    receiveThread.getWorker().join();
                    System.out.println("Restarting receiver...");
                    receiveThread.start();
                }
                Thread.sleep(1500);
            } catch (InterruptedException | IOException e) {
                System.err.println("Blad");
            }
        }

    }
}
