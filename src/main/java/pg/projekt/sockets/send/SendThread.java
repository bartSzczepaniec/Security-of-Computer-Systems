package pg.projekt.sockets.send;


import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.units.qual.A;
import pg.projekt.App;
import pg.projekt.AppGUI;
import pg.projekt.CipherMode;
import pg.projekt.EncryptionManager;
import pg.projekt.sockets.messages.Message;
import pg.projekt.sockets.messages.MessageType;
import pg.projekt.sockets.messages.MsgReader;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
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
    private List<Message> fileMessagesToSend;
    private Socket clientSocket;
    private AtomicBoolean running;

    private EncryptionManager encryptionManager;

    private AppGUI app;

    private boolean isInitializer;
    /**
     * creates new thread
     * @param address - the socket addres
     * @param port - the socket port
     * @param msgList - a list to store sent messages in (shared between reciever, sender and printer threads)
     * @param messagesToSend - a list from which to get messages that need to be sent
     * @param encryptionManager - encryption manager to manage keys
     */
    public SendThread(String address, int port, List<Message> msgList, List<Message> messagesToSend, List<Message> fileMessagesToSend, EncryptionManager encryptionManager, boolean isInitializer, AppGUI app){
        this.worker = null;
        this.address = address;
        this.port = port;
        this.sentMsgList = msgList;
        this.messagesToSend = messagesToSend;
        this.fileMessagesToSend = fileMessagesToSend;
        this.clientSocket = null;
        this.running = new AtomicBoolean(false);
        this.encryptionManager = encryptionManager;
        this.isInitializer = isInitializer;
        this.app = app;
    }

    /**
     * A method for initalizing and starting the thread
     */
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
        // init streams
        ObjectOutputStream out =null;
        ObjectInputStream in = null;
        try{
            // create new socket
            clientSocket = new Socket();
            SocketAddress socketAddress = new InetSocketAddress(address, port);
            clientSocket.connect(socketAddress, 5000);


            // init in/out streams
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            in = new ObjectInputStream(clientSocket.getInputStream());
            // start confirmation thread
            ConfirmationThread ct = new ConfirmationThread(in, sentMsgList, encryptionManager, app);
            ct.start();

            // send public key (unencrypted)
            if(isInitializer){
                Message pk = new Message( encryptionManager.getPublicKey(),"Friend", MessageType.INIT_PK);
                out.writeObject(pk);
                out.flush();

                // wait for response from other side
                // TODO: timeout?

                int counter = 0;
                while(encryptionManager.getFriendPublicKey() == null){
                    Thread.sleep(500);
                    System.out.println("WAITING");
                    if(counter++ > 10){
                        throw new InterruptedException("Connection timed out");
                    }

                }
                app.setConnected(true);
                app.setConnectionButtons();
                System.out.println("SENDER: RECEIVED PUBLIC KEY");

                // sending encrypted session key (RSA)
                byte[] sessionKey = encryptionManager.generateAndSetSessionKey();
                encryptionManager.setSessionKey(sessionKey);
                System.out.println("SENDER: GENERATED SESSION KEY");// + new String(sessionKey, StandardCharsets.UTF_8));
                Message sk = new Message(EncryptionManager.encryptRSA(sessionKey, encryptionManager.getFriendPublicKey(), true), "Friend", MessageType.SK);
                out.writeObject(sk);
                out.flush();
                String sess_key = new String(sessionKey, StandardCharsets.UTF_8);
                System.out.print("SENDER: SENT SESSION KEY - ");
                System.out.println(sess_key);

                // send cipher mode

                Message cipherParams = new Message(EncryptionManager.encryptRSA(encryptionManager.getCipherMode().name().getBytes(), encryptionManager.getFriendPublicKey(), true),"Friend", MessageType.PARAM);
                out.writeObject(cipherParams);
                out.flush();

                System.out.println("SENDER: SENT CIPHER PARAMS");


            }


            // read messages to be sent
            while (true) {
                // get message if waiting
                if(messagesToSend.size() > 0){
                    // remove it from the list
                    Message msg = messagesToSend.remove(0);

                    try{
                        // put the message on sent list
                        putMsgOnList(msg);
                        if(msg.getType() == MessageType.PARAM){

                            out.writeObject(msg);
                            out.flush();

                            System.out.println("SENDER: SENT CIPHER PARAMS");
                        }else{
                            msg.encryptPayload(encryptionManager.getSessionKey(), encryptionManager.getCipherMode());
                            // send the message
                            out.writeObject(msg);
                            System.out.println("SENDER: MSG SENT");
                            out.flush();
                        }
                        // encrypt message

                    }catch(ArrayIndexOutOfBoundsException ex){
                        System.err.println("SENDER: invalid message");
                        sentMsgList.add(new Message("Invalid message content"));
                    }

                }
                else if(fileMessagesToSend.size() > 0) {
                    // remove it from the list
                    Message msg = fileMessagesToSend.remove(0);

                    try{
                        // encrypt message
                        //System.out.println("part sent: "+new String(msg.getPayload(), StandardCharsets.UTF_8));
                        msg.encryptPayload(encryptionManager.getSessionKey(), encryptionManager.getCipherMode());
                        // send the message
                        out.writeObject(msg);
                        out.flush();
                    }catch(ArrayIndexOutOfBoundsException ex){
                        System.err.println("SENDER: invalid message");
                        sentMsgList.add(new Message("Invalid message content"));
                    }
                }

                Thread.sleep(200);
                if(clientSocket.isClosed()){
                    // if socket closed break
                    break;
                }
            }

        } catch (NullPointerException | IOException | InterruptedException ex)
        {
            System.err.println("SENDER: connection closed by other side or no open socket present - communication terminated");
            app.setConnectionButtons();
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
        //app.setConnected(false);
        //app.setConnectionButtons();
    }
}
