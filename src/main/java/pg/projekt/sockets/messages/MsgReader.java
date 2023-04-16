package pg.projekt.sockets.messages;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.util.List;

public class MsgReader implements Runnable{
    private Thread worker;

    private List<Message> msgList;
    private JTextPane messagesPane;

    public MsgReader(List<Message> msgList, JTextPane messagesPane){
        this.msgList = msgList;
        this.worker = null;
        this.messagesPane = messagesPane;
    }

    public void start(){
        this.worker = new Thread(this);
        worker.start();
    }

    @Override
    public void run(){
        SimpleAttributeSet boldText = new SimpleAttributeSet();
        SimpleAttributeSet basicText = new SimpleAttributeSet();
        StyleConstants.setBold(boldText, true);

        while(true){

            try {
                Thread.sleep(200); // magical sleep, without doesnt work
                Document doc = messagesPane.getDocument();

                for(Message s : msgList){
                    if(!s.isPrinted()){
                        System.out.println(s.getSender() + ": " + s.getContent());
                        // append text to currently displayed
                        String sender = s.getSender();
                        String content = s.getContent();

                        doc.insertString(doc.getLength(),sender + ": " , boldText );
                        doc.insertString(doc.getLength(), content+ "\n", basicText );

                        s.setPrinted(true);
                    }

                }
            } catch (InterruptedException | BadLocationException e) {
                throw new RuntimeException(e);
            }

        }


    }


}
