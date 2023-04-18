package pg.projekt.sockets.receive;

import lombok.*;
import pg.projekt.sockets.messages.Message;
import pg.projekt.sockets.messages.MessageType;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class ReceiveThread implements Runnable{
    private ServerSocket serverSocket;
    private Thread worker;

    private List<Message> receivedMsgList;
    private int port;
    private AtomicBoolean running;
    private Socket clientSocket;

    /**
     * creates new thread
     * @param msgList - a list to store recieved messages in (shared between reciever, sender and printer threads)
     * @param port - port number to create ServerSocket on
     */
    public ReceiveThread(List<Message> msgList, int port){
        this.serverSocket = null;
        this.port = port;
        this.worker = null;
        this.receivedMsgList = msgList;
        this.running = new AtomicBoolean(false);
        this.clientSocket = null;
    }

    /**
     *  Starts the receiver thread
     */
    public void start(){
        this.worker = new Thread(this);
        try {
            serverSocket = new ServerSocket(port);
            worker.start();
            this.running.set(true);
            System.out.println("RECEIVER: thread started (port: " + port +")");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    /**
     * Put read message on the list
     * @param msgContent - content of message
     */
    public synchronized void putMsgOnList(String msgContent, String sender){
        Message msg = new Message(msgContent, sender);
        this.receivedMsgList.add(msg);
    }


    @Override
    public void run(){
            // if user does not accept client socket closed
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        try
        {
            clientSocket = serverSocket.accept();
            out = new ObjectOutputStream(clientSocket.getOutputStream()); // ev. for msg confirmation
            in = new ObjectInputStream(clientSocket.getInputStream());

            Object input;
            while ((input = in.readObject()) != null) {
                // Read object from stream
                Message msg = (Message)input;
                putMsgOnList(msg.getContent(), msg.getSender());

                // Send confirmation of receiving
                Message confirmation = new Message(msg.getUuid().toString(), "Friend", MessageType.CONFIRM);
                out.writeObject(confirmation);
                out.flush();
            }

        } catch(EOFException | SocketException ex ){
            System.err.println("RECEIVER: communication terminated - EOF or other side closed");
            System.err.println(ex);
        } catch (ClassNotFoundException | IOException e) {
            throw new RuntimeException(e);
        }finally {
            try { // close gently
                out.close();
                in.close();
                clientSocket.close();
                System.out.println("RECEIVER: closing gently");
            } catch (IOException | NullPointerException e) {
                System.err.println(e);
            }
        }

        this.running.set(false);
        System.out.println("RECEIVER: thread stopped (port: " + port +")");

    }


}
