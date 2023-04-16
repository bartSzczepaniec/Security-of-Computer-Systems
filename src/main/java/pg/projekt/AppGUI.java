package pg.projekt;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

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

    private JFileChooser jFileChooser;

    public AppGUI() {
        frame = new JFrame("Security of Computer Systems");
        jFileChooser = new JFileChooser();
    }

    public void startApp() {
        try {
            enterPassword();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
                String chosenIP = ipTextField.getText();
                String chosenPort = portTextField.getText();
                System.out.println("Connecting with: IP: " + chosenIP + " Port: " + chosenPort);
                // TODO - handle connecting
            }
        });

        frame.add(mainPanel);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    private void enterPassword() throws IOException {
        JPasswordField jPasswordField = new JPasswordField();

        // Checking if file with password is already set
        boolean passwordFileExists;
        File passwordFile = new File("src/main/resources/EncryptionKey/password.txt");
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
            if (result == JOptionPane.OK_OPTION) {

                String password = new String(jPasswordField.getPassword());
                System.out.println("PASSWORD: " + password);
                String passwordHash = Hashing.sha256()
                        .hashString(password, StandardCharsets.UTF_8)
                        .toString();

                // Password was already set
                if(passwordFileExists) {
                    if(isPasswordCorrect(passwordHash, passwordFile)) {
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
            }
            if(!passwordIsOk) {
                JOptionPane.showMessageDialog (null, "You must enter correct password", "Wrong password", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Checking if hash of entered password equals saved hash in the file
    public boolean isPasswordCorrect(String hashedPassword, File passwordFile) throws FileNotFoundException {
        Scanner scanner = new Scanner(passwordFile);
        if (scanner.hasNextLine()) {
            String passwordFromFile = scanner.nextLine();
            return hashedPassword.equals(passwordFromFile);
        }
        scanner.close();
        return false;
    }

    public void sendMessage() {
        if(!sendMessageField.getText().isEmpty()) {
            System.out.println("Message sent: " + sendMessageField.getText());
            sendMessageField.setText("");
            // TODO - handle sending message
        }
    }

}
