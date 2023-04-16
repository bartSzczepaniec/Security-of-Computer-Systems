package pg.projekt.sockets.recieve;

import lombok.*;
import pg.projekt.sockets.messages.Message;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class RecieveThread implements Runnable{
    private ServerSocket serverSocket;
    private Thread worker;

    private List<Message> recievedMsgList;
    private int port;

    /**
     * creates new thread
     * @param msgList - a list to store recieved messages in (shared between reciever, sender and printer threads)
     * @param port - port number to create ServerSocket on
     */
    public RecieveThread(List<Message> msgList, int port){
        this.serverSocket = null;
        this.port = port;
        this.worker = new Thread(this);
        this.recievedMsgList = msgList;
    }

    /**
     *  Starts the reciever thread
     */
    public void start(){
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        worker.start();

    }

    /**
     * Put read message on the list
     * @param msgContent - content of message
     */
    public synchronized void putMsgOnList(String msgContent){
        Message msg = new Message(msgContent, "reciever");
        this.recievedMsgList.add(msg);
    }


    @Override
    public void run(){
        while(true) {
            try (Socket clientSocket = serverSocket.accept();
                 ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream()); // ev. for msg confirmation
                 ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream()))
            {
                Object input;
                while ((input = in.readObject()) != null) {
                    // Read object from stream
                    String inputString = input.toString();
                    putMsgOnList(inputString);
                }
                System.out.println("Communication was termined by the other side");
                break;

            } catch(EOFException ex){
                System.out.println("Communication was termined by the other side");
            }
            catch (SocketException ex) // thrown when socket closed
            {
                System.out.println("Socket closed by other side - communication terminated");
                System.out.println(ex);
                break;
            } catch (ClassNotFoundException | IOException e) {
                throw new RuntimeException(e);
            }

        }


    }


}
