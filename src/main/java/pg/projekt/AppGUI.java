package pg.projekt;

import com.google.common.hash.HashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import pg.projekt.sockets.messages.Message;
import pg.projekt.sockets.messages.MessageType;
import pg.projekt.sockets.messages.MsgReader;
import pg.projekt.sockets.receive.ReceiveThread;
import pg.projekt.sockets.receive.CheckThread;
import pg.projekt.sockets.send.SendThread;

import javax.swing.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Getter
@Setter
public class AppGUI {
    private JFrame frame;
    private JPanel mainPanel;
    private JTextField ipTextField;
    private JTextField portTextField;
    private JButton connectButton;
    private JLabel ipLabel;
    private JLabel portLabel;
    private JTextPane messagesPane;
    private JLabel chatLabel;
    private JTextField sendMessageField;
    private JButton sendMessageButton;
    private JButton fileChooseButton;
    private JButton sendFileButton;
    private JLabel fileLabel;
    private JButton disconnectButton;
    private JLabel modeLabel;
    private JRadioButton cbcRadioButton;
    private JRadioButton ecbRadioButton;

    private JFileChooser jFileChooser;

    private EncryptionManager encryptionManager;

    private List<Message> msgList;
    private List<Message> toBeSent;
    private List<Message> fileMessagesToBeSent;

    private ReceiveThread receiveThread;
    private SendThread sendThread;
    private CheckThread checkThread;
    private MsgReader msgReader;

    private int myPort;

    private boolean isConnected;

    private File chosenFile;

    public AppGUI() {
        frame = new JFrame("Security of Computer Systems");
        jFileChooser = new JFileChooser();
        encryptionManager = new EncryptionManager();
        encryptionManager.setCipherMode(CipherMode.ECB);
        isConnected = false;
        chosenFile = null;
    }

    public void startApp() {
        // Before entering the main app
        boolean passwordEntered = false;
        try {
            passwordEntered = enterPassword();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(!passwordEntered) {
            return;
        }

        enterPort();

        sendMessageField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {

            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                   sendMessage(); // Sending a Text message
                }

            }
        });

        setupButtons();

        frame.add(mainPanel);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    receiveThread.getServerSocket().close();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        frame.pack();
        frame.setVisible(true);

        this.msgList = Collections.synchronizedList(new ArrayList<Message>());
        this.toBeSent = Collections.synchronizedList(new ArrayList<Message>());
        this.fileMessagesToBeSent = Collections.synchronizedList(new ArrayList<Message>());

        this.receiveThread = new ReceiveThread(msgList, myPort, this);
        this.receiveThread.start();

        this.msgReader = new MsgReader(msgList, messagesPane);
        this.msgReader.start();

        this.checkThread = new CheckThread(receiveThread);
        this.checkThread.start();
    }

    private boolean enterPassword() throws IOException {
        JPasswordField jPasswordField = new JPasswordField();

        // Checking if file with password is already set
        boolean passwordFileExists;
        File passwordFile = new File("src/main/resources/encryptionKey/password.txt");
        if (passwordFile.createNewFile()) {
            passwordFileExists = false;
        }
        else {
            passwordFileExists = true;
        }

        boolean passwordIsOk = false;

        // Entering correct password to continue
        while (!passwordIsOk) {
            jPasswordField.setText("");
            int result = JOptionPane.showConfirmDialog(null, jPasswordField, "Enter the password:", JOptionPane.DEFAULT_OPTION);
            System.out.println(result);
            if (result == JOptionPane.DEFAULT_OPTION) {
                return false;
            }
            if (result == JOptionPane.OK_OPTION) {

                String password = new String(jPasswordField.getPassword());
                System.out.println("ENTERED PASSWORD: " + password);

                HashCode passwordHashBytes = encryptionManager.shaHashingToString(password);
                String passwordHash = passwordHashBytes.toString();

                // Password was already set
                if(passwordFileExists) {
                    if(encryptionManager.isPasswordCorrect(passwordHash, passwordFile)) {
                        passwordIsOk = true;
                    }
                } // Setting a new password
                else {
                    if(password.length() >= 4) {
                        System.out.println("PASSWORD SET: " + password);
                        FileWriter fileWriter = new FileWriter("src/main/resources/EncryptionKey/password.txt");
                        fileWriter.write(passwordHash);
                        fileWriter.close();

                        passwordIsOk = true;
                    }
                }
                if (passwordIsOk) {
                    encryptionManager.setLocalKey(passwordHashBytes.asBytes());
                    encryptionManager.generateRSAkeys();
                }
            }
            if(!passwordIsOk) {
                JOptionPane.showMessageDialog (null, "You must enter correct password", "Wrong password", JOptionPane.ERROR_MESSAGE);
            }
        }
        return true;
    }

    public void enableFileButton() {
        sendFileButton.setEnabled(true);
    }

    public void disableFileButton() {
        sendFileButton.setEnabled(false);
    }

    private void enterPort() {
        boolean isPortSet = false;
        try {
            myPort = Integer.parseInt(JOptionPane.showInputDialog("Enter port:", 10000));
        }
        catch (NumberFormatException e) {
            JOptionPane.showMessageDialog (null, "You must enter correct password", "Wrong password", JOptionPane.ERROR_MESSAGE);
            System.out.println("ENTERED TEXT IS NOT A PORT NUMBER");
            System.out.println("PORT WAS SET TO 10000");
            myPort=10000;
        }finally {
            frame.setTitle(frame.getTitle() + " - " + myPort);
        }
    }

    public void sendMessage() {

        if(!sendMessageField.getText().isEmpty()) {
            String msgToSend = sendMessageField.getText();
            System.out.println("Message sent: " + msgToSend);
            toBeSent.add(new Message(msgToSend, "Friend")); // message sending
            sendMessageField.setText("");
        }
    }

    public void setConnectionButtons(){
        if(isConnected){
            messagesPane.setText("");
            if(chosenFile != null) {
                enableFileButton();
            }
            sendMessageButton.setEnabled(true);
            connectButton.setEnabled(false);
            disconnectButton.setEnabled(true);
            msgList.removeAll(msgList);
            toBeSent.removeAll(toBeSent);
            msgList.add(new Message("Connected"));
        }else{
            messagesPane.setText("");
            msgList.removeAll(msgList);
            toBeSent.removeAll(toBeSent);
            connectButton.setEnabled(true);
            disconnectButton.setEnabled(false);
            msgList.add(new Message("Disconnected"));
            disableFileButton();
            sendMessageButton.setEnabled(false);

        }

    }
    private void setupButtons() {
        // Choosing a file to send
        fileChooseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int result = jFileChooser.showOpenDialog(null);

                if(result == JFileChooser.APPROVE_OPTION) {
                    chosenFile = jFileChooser.getSelectedFile();
                    // TODO - handle file
                    fileLabel.setText("File: " + chosenFile.getName());
                    System.out.println(chosenFile.length());
                    sendFileButton.setEnabled(true);
                }
            }
        });

        sendFileButton.addActionListener(new ActionListener() {
             @SneakyThrows
             @Override
             public void actionPerformed(ActionEvent e) {
                 String fileInfo = chosenFile.getName() + ":" + Long.toString(chosenFile.length());
                 System.out.println("FILE INFO  sent: " + fileInfo);
                 disableFileButton();
                 fileMessagesToBeSent.add(new Message(fileInfo, "Friend", MessageType.INIT_FILE));


                 // TODO - do it in a different thread
                 FileInputStream fileInputStream = new FileInputStream(chosenFile);
                 long bytesLeft = chosenFile.length();
                 int partSize = 1024;

                 byte[] payload = new byte[partSize];
                 while(fileInputStream.read(payload) != -1)
                 {
                     byte[] payloadToSend = Arrays.copyOf(payload, partSize);
                     Message filePart = new Message(payloadToSend, "Friend", MessageType.FILE);
                     //System.out.println("F - part sent: "+new String(filePart.getPayload(), StandardCharsets.UTF_8));
                     fileMessagesToBeSent.add(filePart);
                 }
                 fileInputStream.close();


                 fileLabel.setText("File:");
                 sendFileButton.setEnabled(false);
                 chosenFile = null;
             }
         });

        // Sending a Text message
        sendMessageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        // Connecting to client with chosen IP address and Port
        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                receiveThread.setInitailzer(true);
                messagesPane.setText("");
                msgList.removeAll(msgList);
                toBeSent.removeAll(toBeSent);
                fileMessagesToBeSent.removeAll(fileMessagesToBeSent);

                String chosenIP = ipTextField.getText();
                int chosenPort = Integer.valueOf(portTextField.getText());
                System.out.println("Connecting with: IP: " + chosenIP + " Port: " + chosenPort);
                sendThread = new SendThread(chosenIP, chosenPort, msgList, toBeSent, fileMessagesToBeSent, encryptionManager, true, receiveThread.getApp());
                // TODO: implment is runniong in SendThread (also Receive)
                sendThread.start();


                // TODO: check if connection succesful
                // TODO: something displayed under buttons
                // TODO: block horizontal scorlling
                /*try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }*/
                /*if(sendThread.getRunning().get()){
                    isConnected = true;
                    setConnectionButtons();
                }else{
                    isConnected = false;
                    setConnectionButtons();
                }*/





            }
        });
        /**
         * Actions for disconnect button
         */
        disconnectButton.addActionListener(new ActionListener() {
            /**
             * Actions to perform when disconnect button is pressed
             * @param e the event to be processed
             */
            @Override
            public void actionPerformed(ActionEvent e) {

                try {
                    // close sockets
                    if(sendThread.getClientSocket() != null){
                        sendThread.getClientSocket().close();
                    }

                    if ( receiveThread.getClientSocket() != null){
                        // close client socket if it exists
                        receiveThread.getClientSocket().close();
                    }
                    // close recieveThread socket
                    receiveThread.getServerSocket().close();
                    // wait for reciever thread to finish
                    receiveThread.getWorker().join();

                    // restart reciever thread - ready for new connections
                    receiveThread.start();

                    // toggle connection buttons
                    // TODO: state bool - connected/not connected - based on it button state
                    isConnected = false;
                    //setConnectionButtons();

                } catch (IOException | NullPointerException | InterruptedException ex) {
                    System.err.println("Closed not existing connection");
                    System.err.println(ex);
                    // toggle buttons - also maybe bool state
                    isConnected = false;
                    setConnectionButtons();
                }
            }
        });


        /**
         * Radio buttons for choosing encryption mode
         */
        cbcRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                encryptionManager.setCipherMode(CipherMode.CBC);

                Message cipherParams = new Message(EncryptionManager.encryptRSA(encryptionManager.getCipherMode().name().getBytes(), encryptionManager.getFriendPublicKey(), true),"Friend", MessageType.PARAM);
                toBeSent.add(cipherParams);
            }
        });

        ecbRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                encryptionManager.setCipherMode(CipherMode.ECB);
                Message cipherParams = new Message(EncryptionManager.encryptRSA(encryptionManager.getCipherMode().name().getBytes(), encryptionManager.getFriendPublicKey(), true),"Friend", MessageType.PARAM);
                toBeSent.add(cipherParams);
            }
        });
    }

}
