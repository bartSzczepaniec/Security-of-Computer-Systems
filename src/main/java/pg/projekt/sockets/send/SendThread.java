package pg.projekt.sockets.send;


import pg.projekt.sockets.messages.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class SendThread implements Runnable{

    private Thread worker;
    private int port;
    private String address;
    private List<Message> sentMsgList;
    private List<Message> messagesToSend;
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
        Message msg = new Message(msgConent, "sender");
        this.sentMsgList.add(msg);
    }

    @Override
    public void run(){
        // read messages from socket until the ned
        while(true) {
            try (Socket clientSocket = new Socket(address, port);
                 ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream()))
            {
                int counter =0;
                while (true) {
                    if(messagesToSend.size() > 0){
                        Message msg = messagesToSend.remove(0);
                        out.writeObject(msg);
                        out.flush();
                        putMsgOnList(msg.getContent());
                    }



                }

            } catch (SocketException ex)
            {
                System.out.println("Socket closed by other side - communication terminated");
                break;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
        System.out.println("Sender thread finished (addres: " + address + ", port: " + port +")");
    }
}
