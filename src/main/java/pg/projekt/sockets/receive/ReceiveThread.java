package pg.projekt.sockets.receive;

import lombok.*;
import pg.projekt.AppGUI;
import pg.projekt.CipherMode;
import pg.projekt.EncryptionManager;
import pg.projekt.sockets.messages.Message;
import pg.projekt.sockets.messages.MessageType;
import pg.projekt.sockets.send.SendThread;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class ReceiveThread implements Runnable{
    private ServerSocket serverSocket;
    private Thread worker;

    private AppGUI app;

    private EncryptionManager encryptionManager;
    private List<Message> receivedMsgList;
    private int port;
    private AtomicBoolean running;
    private Socket clientSocket;
    private boolean isInitailzer;

    /**
     * creates new thread
     * @param msgList - a list to store recieved messages in (shared between reciever, sender and printer threads)
     * @param port - port number to create ServerSocket on
     * @param app - app
     */
    public ReceiveThread(List<Message> msgList, int port, AppGUI app){
        this.serverSocket = null;
        this.port = port;
        this.worker = null;
        this.receivedMsgList = msgList;
        this.running = new AtomicBoolean(false);
        this.clientSocket = null;
        this.app = app;
        this.encryptionManager = app.getEncryptionManager();
        this.isInitailzer = false;
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
     * @param msg - whole message
     */
    public synchronized void putMsgOnList(Message msg){
        this.receivedMsgList.add(msg);
    }


    @Override
    public void run(){

        // init streams
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        try
        {
            clientSocket = serverSocket.accept();
            // TODO: accept or reject connection
            // TODO: tworzy sendthread'a
            if(!isInitailzer){
                String add = ((InetSocketAddress)clientSocket.getRemoteSocketAddress()).getAddress().getHostAddress();
                System.out.println("CONNECTED FROM: " + add);
                this.app.setSendThread(new SendThread(add, 10000, app.getMsgList(), app.getToBeSent(), encryptionManager, false));
                this.app.getSendThread().start();
                this.app.setConnected(true);
                this.app.setConnectionButtons();
            }



            // init in/out streams
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            in = new ObjectInputStream(clientSocket.getInputStream());

            Object input;
            while ((input = in.readObject()) != null) {
                // Read object from stream
                Message msg = (Message)input;

                switch (msg.getType()){
                    // MSG initializing the conversatoion
                    case INIT_PK:
                        System.out.println("RECEIVER: SENDING PK");
                        // send Public key in return
                        out.writeObject(new Message(encryptionManager.getPublicKey(),"Friend", MessageType.PK));
                        out.flush();
                        break;
                    case SK:
                        // read sessiong key
                        byte[] encryptedSessionKey = msg.getPayload();
                        byte[] sessionKey = EncryptionManager.decryptRSA(encryptedSessionKey, encryptionManager.getPrivateKey(), false);
                        // set session key in encryption manager
                        encryptionManager.setSessionKey(sessionKey);
                        String sess_key = new String(sessionKey, StandardCharsets.UTF_8);
                        System.out.print("RECEIVER: RECEIVED SESSION KEY - ");
                        System.out.println(sess_key);
                        break;
                    default:
                        // decrypt message and put it on list to be read
                        System.out.println("RECEIVER: before decryption: ");
                        System.out.println(new String(msg.getPayload(), StandardCharsets.UTF_8));
                        msg.decryptPayload(app.getEncryptionManager().getSessionKey(), encryptionManager.getCipherMode());
                        putMsgOnList(msg);
                }


                // Send confirmation of receiving
                //System.out.println("RECEIVED:" + msg.getUuid().toString());
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
