package pg.projekt.sockets.send;


import lombok.Getter;
import lombok.Setter;
import pg.projekt.sockets.messages.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SendThread implements Runnable{

    private Thread worker;
    private int port;
    private String address;
    private List<Message> sentMsgList;
    private List<Message> messagesToSend;

    private Socket clientSocket;
    /**
     * creates new thread
     * @param address - the socket addres
     * @param port - the socket port
     * @param msgList - a list to store sent messages in (shared between reciever, sender and printer threads)
     */
    public SendThread(String address, int port, List<Message> msgList, List<Message> messagesToSend){
        this.worker = new Thread(this);
        this.address = address;
        this.port = port;
        this.sentMsgList = msgList;
        this.messagesToSend = messagesToSend;
        this.clientSocket = null;
        // TODO: add isRunning 
    }

    public void start(){
        worker.start();
        System.out.println("Sender thread started (addres: " + address + ", port: " + port +")");
    }

    /**
     * Put sent message on list
     * @param msgConent - content of the message
     */
    public synchronized void putMsgOnList(String msgConent){
        Message msg = new Message(msgConent, address);
        this.sentMsgList.add(msg);
    }

    @Override
    public void run(){
        // read messages from socket until the ned

        while(true) {
            ObjectOutputStream out =null;
            ObjectInputStream in = null;
            try{
                clientSocket = new Socket(address, port);
                out = new ObjectOutputStream(clientSocket.getOutputStream());
                in = new ObjectInputStream(clientSocket.getInputStream());
                int counter =0;
                while (true) {
                    if(messagesToSend.size() > 0){
                        Message msg = messagesToSend.remove(0);
                        out.writeObject(msg);
                        out.flush();
                        putMsgOnList(msg.getContent());
                    }
                    if(clientSocket.isClosed()){
                        throw new SocketException("Socket closed");
                    }
                }

            } catch (SocketException | NullPointerException ex)
            {
                System.out.println("Socket closed by other side or no open socket present - communication terminated");
            } finally {
                try {
                    out.close();
                    in.close();
                    clientSocket.close();
                    System.out.println("Closing gently");
                    break;
                } catch (IOException | NullPointerException e) {
                    System.out.println(e);
                    break;
                }
            }

        }
        System.out.println("Sender thread finished (addres: " + address + ", port: " + port +")");
    }
}
