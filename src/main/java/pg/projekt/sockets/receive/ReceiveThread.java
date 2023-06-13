package pg.projekt.sockets.receive;

import lombok.*;
import pg.projekt.AppGUI;
import pg.projekt.CipherMode;
import pg.projekt.EncryptionManager;
import pg.projekt.guiparts.ProgressBarUI;
import pg.projekt.sockets.messages.Message;
import pg.projekt.sockets.messages.MessageType;
import pg.projekt.sockets.send.SendThread;

import javax.swing.*;
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


    @SneakyThrows
    @Override
    public void run(){

        // init streams
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        FileOutputStream fileOutputStream = null;
        long fileSizeLeft = 0;
        try
        {
            clientSocket = serverSocket.accept();
            // TODO: accept or reject connection
            // TODO: tworzy sendthread'a
            if(!isInitailzer){

                String add = ((InetSocketAddress)clientSocket.getRemoteSocketAddress()).getAddress().getHostAddress();
                int result = JOptionPane.showConfirmDialog(null, add,
                        "New connection incoming", JOptionPane.OK_CANCEL_OPTION);
                if(result != 0){

                    throw new SocketException("Connection refused");
                }
                System.out.println("CONNECTED FROM: " + add);
                this.app.setSendThread(new SendThread(add, 10000, app.getMsgList(), app.getToBeSent(), app.getFileMessagesToBeSent(), encryptionManager, false, app));
                this.app.getSendThread().start();
                this.app.setConnected(true);
                this.app.setConnectionButtons();
            }



            // init in/out streams
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            in = new ObjectInputStream(clientSocket.getInputStream());


            // init progressbar for file receiving
            ProgressBarUI progressBarUI = null;

            Object input;
            while ((input = in.readObject()) != null) {
                // Read object from stream
                Message msg = (Message)input;

                switch (msg.getType()){
                    // MSG initializing the conversatoion
                    case INIT_PK:
                        System.out.println("RECEIVER: SENDING PK");
                        // set own public key
                        encryptionManager.setFriendPublicKey(msg.getPayload());
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
                    case INIT_FILE:
                        msg.decryptPayload(encryptionManager.getSessionKey(), encryptionManager.getCipherMode());
                        String fileInfo = new String(msg.getPayload(), StandardCharsets.UTF_8);
                        String[] fileInfoArr = fileInfo.split(":");
                        String fileName = fileInfoArr[0];

                        System.out.println("NEW FILE COMING NAME - " + fileInfoArr[0]);
                        System.out.println("NEW FILE COMING SIZE - " + fileInfoArr[1]);
                        File uploadedFile = new File("src/main/resources/downloads/"+fileInfoArr[0]);
                        fileOutputStream = new FileOutputStream(uploadedFile);
                        fileSizeLeft = Long.parseLong(fileInfoArr[1]);

                        progressBarUI = new ProgressBarUI(fileSizeLeft, fileName, true);
                        progressBarUI.startProgressBar();
                        // send confirmation of file sending initialization
                        Message fileInitConfirmation = new Message(fileInfo, "Friend", MessageType.CONFIRM_INIT_FILE);
                        out.writeObject(fileInitConfirmation);
                        out.flush();

                        break;
                    case FILE:
                        msg.decryptPayload(encryptionManager.getSessionKey(), encryptionManager.getCipherMode());
                        //System.out.println("part got: "+new String(msg.getPayload(), StandardCharsets.UTF_8));
                        int sizeToGet = msg.getPayload().length;
                        if (fileSizeLeft < msg.getPayload().length) {
                            sizeToGet = (int) fileSizeLeft;
                        }
                        fileOutputStream.write(msg.getPayload(), 0, sizeToGet);
                        fileSizeLeft -= sizeToGet;

                        progressBarUI.updateProgress(fileSizeLeft);

                        System.out.println("r - " + fileSizeLeft);
                        if(!(fileSizeLeft > 0)) {
                            System.out.println("GOT WHOLE FILE");
                            fileOutputStream.close();
                        }
                        // send confirmation of receiving part of the file
                        Message fileConfirmation = new Message("" + fileSizeLeft, "Friend", MessageType.CONFIRM_FILE);
                        out.writeObject(fileConfirmation);
                        out.flush();

                        break;
                    case PARAM:
                        byte[] cipherMode = EncryptionManager.decryptRSA(msg.getPayload(), encryptionManager.getPrivateKey(), false);
                        String cipherModeString = new String(cipherMode, StandardCharsets.UTF_8);
                        if(cipherModeString.equals(CipherMode.CBC.name())){
                            encryptionManager.setCipherMode(CipherMode.CBC);
                            app.getCbcRadioButton().setSelected(true);
                        }else if(cipherModeString.equals(CipherMode.ECB.name())){
                            encryptionManager.setCipherMode(CipherMode.ECB);
                            app.getEcbRadioButton().setSelected(true);
                        }
                        System.out.println("RECEIVER: FRIEND SET CIPHER MODE TO " + cipherModeString);
                        break;
                    default:
                        // decrypt message and put it on list to be read
                        System.out.println("RECEIVER: before decryption: ");
                        System.out.println(new String(msg.getPayload(), StandardCharsets.UTF_8));
                        msg.decryptPayload(encryptionManager.getSessionKey(), encryptionManager.getCipherMode());
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
                if(out != null){
                    out.close();
                }
                if(in != null){
                    in.close();
                }
                if(clientSocket != null && !clientSocket.isClosed()){
                    clientSocket.close();
                }

                System.out.println("RECEIVER: closing gently");
            } catch (IOException | NullPointerException e) {
                System.err.println(e);
            }
        }

        this.running.set(false);
        System.out.println("RECEIVER: thread stopped (port: " + port +")");
        // close send thread asosciated with app
        if(app.getSendThread() != null && app.getSendThread().getClientSocket() != null){
            app.getSendThread().getClientSocket().close();
        }

        app.setConnected(false);
        app.setConnectionButtons();

    }


}
