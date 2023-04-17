package pg.projekt;

import pg.projekt.sockets.messages.Message;
import pg.projekt.sockets.messages.MsgReader;
import pg.projekt.sockets.receive.ReceiveThread;
import pg.projekt.sockets.send.SendThread;

import javax.swing.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    private JFileChooser jFileChooser;

    private EncryptionManager encryptionManager;

    private List<Message> msgList;
    private List<Message> toBeSent;

    private ReceiveThread receiveThread;
    private SendThread sendThread;
    private MsgReader msgReader;

    private int myPort;


    public AppGUI() {
        frame = new JFrame("Security of Computer Systems");
        jFileChooser = new JFileChooser();
        encryptionManager = new EncryptionManager();
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

        this.receiveThread = new ReceiveThread(msgList, myPort);
        this.receiveThread.start();

        this.msgReader = new MsgReader(msgList, messagesPane);
        this.msgReader.start();
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
                String passwordHash = encryptionManager.shaHashingToString(password);

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
                    encryptionManager.setLocalKey(passwordHash);
                }
            }
            if(!passwordIsOk) {
                JOptionPane.showMessageDialog (null, "You must enter correct password", "Wrong password", JOptionPane.ERROR_MESSAGE);
            }
        }
        return true;
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
        }
    }

    public void sendMessage() {
        if(!sendMessageField.getText().isEmpty()) {
            String msgToSend = sendMessageField.getText();
            System.out.println("Message sent: " + msgToSend);
            toBeSent.add(new Message(msgToSend, "youraddress")); // message sending
            sendMessageField.setText("");
        }
    }

    public void setConnectionButtons(boolean waitingToConnect){
        if(waitingToConnect){
            connectButton.setEnabled(true);
            disconnectButton.setEnabled(false);
        }else{
            connectButton.setEnabled(false);
            disconnectButton.setEnabled(true);
        }

    }
    private void setupButtons() {
        // Choosing a file to send
        fileChooseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int result = jFileChooser.showOpenDialog(null);

                if(result == JFileChooser.APPROVE_OPTION) {
                    File chosenFile = jFileChooser.getSelectedFile();
                    // TODO - handle file
                    fileLabel.setText(chosenFile.getName());
                }
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
                messagesPane.setText("");
                msgList.removeAll(msgList);
                toBeSent.removeAll(toBeSent);
                msgList.add(new Message("Connecting..."));

                String chosenIP = ipTextField.getText();
                int chosenPort = Integer.valueOf(portTextField.getText());
                System.out.println("Connecting with: IP: " + chosenIP + " Port: " + chosenPort);
                sendThread = new SendThread(chosenIP, chosenPort, msgList, toBeSent);
                // TODO: implment is runniong in SendThread (also Receive)
                sendThread.start();


                // TODO: check if connection succesful
                // TODO: something displayed under buttons
                // TODO: block horizontal scorlling
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                if(sendThread.getRunning().get()){
                    setConnectionButtons(false);
                    msgList.add(new Message("Connected"));
                }else{
                    setConnectionButtons(true);
                }





            }
        });

        disconnectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                try {
                    // close sockets
                    sendThread.getClientSocket().close();
                    receiveThread.getClientSocket().close();
                    receiveThread.getServerSocket().close();
                    // wait for reciever thread to finish
                    receiveThread.getWorker().join();

                    // restart reciever thread - ready for new connections
                    receiveThread.start();

                    setConnectionButtons(true);
                    msgList.add(new Message("Disconnected"));

                } catch (IOException | NullPointerException | InterruptedException ex) {
                    System.err.println("Closed not exisiting connection");
                    setConnectionButtons(true);
                }
            }
        });
    }

}
