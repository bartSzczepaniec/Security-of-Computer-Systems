package pg.projekt.sockets.send;


import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.units.qual.A;
import pg.projekt.App;
import pg.projekt.AppGUI;
import pg.projekt.EncryptionManager;
import pg.projekt.sockets.messages.Message;
import pg.projekt.sockets.messages.MessageType;
import pg.projekt.sockets.messages.MsgReader;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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

    private EncryptionManager encryptionManager;
    /**
     * creates new thread
     * @param address - the socket addres
     * @param port - the socket port
     * @param msgList - a list to store sent messages in (shared between reciever, sender and printer threads)
     */
    public SendThread(String address, int port, List<Message> msgList, List<Message> messagesToSend, EncryptionManager encryptionManager){
        this.worker = null;
        this.address = address;
        this.port = port;
        this.sentMsgList = msgList;
        this.messagesToSend = messagesToSend;
        this.clientSocket = null;
        this.running = new AtomicBoolean(false);
        this.encryptionManager = encryptionManager;
    }

    public void start(){
        this.worker = new Thread(this);
        worker.start();
        System.out.println("SENDER: thread started (addres: " + address + ", port: " + port +")");
        this.running.set(true);
    }

    /**
     * Put sent message on list
     * @param msg - content of the message
     */
    public synchronized void putMsgOnList(Message msg){
        Message msgToPrint = new Message(msg.getContent(), "You", msg.getType(), msg.getUuid());
        this.sentMsgList.add(msgToPrint);
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
            // start confirmation thread
            ConfirmationThread ct = new ConfirmationThread(in, sentMsgList, encryptionManager);
            ct.start();

            // send key

            Message pk = new Message( new byte[0],"Friend", MessageType.INIT_PK);

            out.writeObject(pk);
            out.flush();

            while(encryptionManager.getFriendPublicKey() == null){
                Thread.sleep(500);
            }
            System.out.println("RECEIVED PUBLIC KEY: " + encryptionManager.getFriendPublicKey());
            Message sk = new Message(encryptionManager.generateSessionKey(), "Friend", MessageType.INIT_SK);
            while (true) {
                if(messagesToSend.size() > 0){
                    Message msg = messagesToSend.remove(0);

                    try{
                        putMsgOnList(msg);
                        out.writeObject(msg);
                        System.out.println("SENT");
                        out.flush();
                    }catch(ArrayIndexOutOfBoundsException ex){
                        System.err.println("SENDER: invalid message");
                        sentMsgList.add(new Message("Invalid message content"));
                    }

                }

                Thread.sleep(200);
                if(clientSocket.isClosed()){
                    break;
                }
            }

        } catch (NullPointerException | IOException | InterruptedException ex)
        {
            System.err.println("SENDER: connection closed by other side or no open socket present - communication terminated");
            System.err.println(ex);
        } finally {
            try {
                out.close();
                in.close();
                clientSocket.close();
                System.out.println("SENDER: closing gently");
            } catch (IOException | NullPointerException e) {
                System.err.println("SENDER: socket was not created");
                System.err.println(e);
            }
        }

        this.running.set(false);
        System.out.println("SENDER: thread stopped (addres: " + address + ", port: " + port +")");
    }
}
