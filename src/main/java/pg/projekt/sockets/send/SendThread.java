package pg.projekt.sockets.send;


import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.units.qual.A;
import pg.projekt.sockets.messages.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
@Setter
public class SendThread implements Runnable{

    private Thread worker;
    private int port;
    private String address;
    private List<Message> sentMsgList;
    private List<Message> messagesToSend;
    private Socket clientSocket;
    private AtomicBoolean running;
    /**
     * creates new thread
     * @param address - the socket addres
     * @param port - the socket port
     * @param msgList - a list to store sent messages in (shared between reciever, sender and printer threads)
     */
    public SendThread(String address, int port, List<Message> msgList, List<Message> messagesToSend){
        this.worker = null;
        this.address = address;
        this.port = port;
        this.sentMsgList = msgList;
        this.messagesToSend = messagesToSend;
        this.clientSocket = null;
        this.running = new AtomicBoolean(false);
    }

    public void start(){
        this.worker = new Thread(this);
        worker.start();
        System.out.println("Sender thread started (addres: " + address + ", port: " + port +")");
        this.running.set(true);
    }

    /**
     * Put sent message on list
     * @param msgConent - content of the message
     */
    public synchronized void putMsgOnList(String msgConent){
        Message msg = new Message(msgConent, address+":"+port);
        this.sentMsgList.add(msg);
    }

    @Override
    public void run(){
        // read messages from socket until the ned

        ObjectOutputStream out =null;
        ObjectInputStream in = null;
        try{
            clientSocket = new Socket(address, port);
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            in = new ObjectInputStream(clientSocket.getInputStream());

            while (true) {
                if(messagesToSend.size() > 0){
                    Message msg = messagesToSend.remove(0);
                    out.writeObject(msg);
                    out.flush();
                    putMsgOnList(msg.getContent());
                }
                // check if there is any msg waiting and print every 0.2s

                Thread.sleep(200);
                if(clientSocket.isClosed()){
                    break;
                }
            }

        } catch (NullPointerException | IOException | InterruptedException ex)
        {
            System.err.println("Socket closed by other side or no open socket present - communication terminated");
            System.err.println(ex);
        } finally {
            try {
                out.close();
                in.close();
                clientSocket.close();
                System.out.println("SENDER: closing gently");
            } catch (IOException | NullPointerException e) {
                System.err.println(e);
            }
        }

        this.running.set(false);
        System.out.println("Sender thread stopped (addres: " + address + ", port: " + port +")");
    }
}
