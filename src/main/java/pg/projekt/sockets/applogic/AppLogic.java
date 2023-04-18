package pg.projekt.sockets.applogic;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import pg.projekt.AppGUI;
import pg.projekt.EncryptionManager;
import pg.projekt.sockets.messages.Message;
import pg.projekt.sockets.messages.MsgReader;
import pg.projekt.sockets.receive.ReceiveThread;
import pg.projekt.sockets.send.SendThread;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class AppLogic {
    private AppGUI gui;
    private ReceiveThread receiveThread;
    private SendThread sendThread;
    private MsgReader msgReader;

    private List<Message> displayedMessages;
    private List<Message> messagesToBeSent;
    private int myPort;

    private EncryptionManager encryptionManager;

    public AppLogic(){
        this.encryptionManager = new EncryptionManager();

        this.myPort = 10000;
        this.gui = new AppGUI(this);

        this.displayedMessages = Collections.synchronizedList(new ArrayList<Message>());
        this.messagesToBeSent = Collections.synchronizedList(new ArrayList<Message>());

        this.receiveThread = null;
        this.msgReader = null;


    }

    public void StartApp(){
        this.receiveThread = new ReceiveThread(displayedMessages, myPort);
        this.msgReader = new MsgReader(displayedMessages, gui.getMessagesPane());
        receiveThread.start();
        msgReader.start();
        displayedMessages.add(new Message("SIEMA"));
    }

    public boolean sendMessage(){
        String msg = gui.getMessageFieldContent();
        messagesToBeSent.add(new Message(msg, "me" ));
        gui.resetMessageField();
        return true;
    }

    public void connect(){
        String ip = gui.getIpFieldValue();
        int portToConnect = gui.getPortFieldValue();
        resetMessages();
        displayedMessages.add(new Message("Connecting..."));

        sendThread = new SendThread(ip, portToConnect, displayedMessages, messagesToBeSent);
        sendThread.start();

    }

    public void resetMessages(){
        displayedMessages.removeAll(displayedMessages);
        messagesToBeSent.removeAll(messagesToBeSent);
        gui.resetMessagePane();
    }

    public void disconnect(){

        try {
            // close sockets
            sendThread.getClientSocket().close();
            receiveThread.getClientSocket().close();
            receiveThread.getServerSocket().close();
            // wait for reciever thread to finish
            receiveThread.getWorker().join();

            // restart reciever thread - ready for new connections
            receiveThread.start();
            displayedMessages.add(new Message("Disconnected"));

        } catch (IOException | NullPointerException | InterruptedException ex) {
            System.err.println("Closed not exisiting connection");
        }}


}
