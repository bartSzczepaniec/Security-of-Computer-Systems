package pg.projekt.sockets.send;


import lombok.Getter;
import lombok.Setter;
import pg.projekt.AppGUI;
import pg.projekt.EncryptionManager;
import pg.projekt.guiparts.ProgressBarUI;
import pg.projekt.sockets.messages.Message;
import pg.projekt.sockets.messages.MessageType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
@Setter
public class ConfirmationThread implements Runnable{
    private ObjectInputStream in;
    private Thread worker;
    private AtomicBoolean running;
    private SendThread st;
    private List<Message> sentMsgList;
    private EncryptionManager encryptionManager;

    private AppGUI app;

    public ConfirmationThread(ObjectInputStream in, List<Message> sentMsgList, EncryptionManager encryptionManager, AppGUI app) {
        this.in = in;
        this.sentMsgList = sentMsgList;
        this.running = new AtomicBoolean(false);
        this.encryptionManager = encryptionManager;
        this.app = app;
    }

    public void start(){
        this.worker = new Thread(this);
        worker.start();
        this.running.set(true);

    }

    @Override
    public void run() {
        try {
            Object input;
            ProgressBarUI progressBarUI = null;

            while ((input = in.readObject()) != null) {
                // Read object from stream
                Message msg = (Message) input;
                switch(msg.getType()){
                    case PK: // message contains public key
                        byte[] friendPublicKey = msg.getPayload();
                        encryptionManager.setFriendPublicKey(friendPublicKey);
                        System.out.println("CONFIRMATION THREAD: RECEIVED PK");
                        break;
                    case CONFIRM_INIT_FILE:
                        String fileInfo = new String(msg.getPayload(), StandardCharsets.UTF_8);
                        String[] fileInfoArr = fileInfo.split(":");
                        String fileName = fileInfoArr[0];
                        long sizeOfFile = Long.parseLong(fileInfoArr[1]);
                        progressBarUI = new ProgressBarUI(sizeOfFile, fileName, false);
                        progressBarUI.startProgressBar();
                        break;
                    case CONFIRM_FILE:
                        String bytesLeft = new String(msg.getPayload(), StandardCharsets.UTF_8);
                        long fileSizeLeft = Long.parseLong(bytesLeft);
                        progressBarUI.updateProgress(fileSizeLeft);
                        if(!(fileSizeLeft > 0)) {
                            app.getFileChooseButton().setEnabled(true);
                        }
                        break;
                    default:
                        putConfirmationOnList(msg);
                        break;
                }

            }
        } catch (IOException | ClassNotFoundException e) {

        }
    }

    public synchronized void putConfirmationOnList(Message msg) {
        this.sentMsgList.add(msg);
    }
}
