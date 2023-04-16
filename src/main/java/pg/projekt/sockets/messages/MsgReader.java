package pg.projekt.sockets.messages;

import javax.swing.*;
import java.util.List;

public class MsgReader implements Runnable{
    private Thread worker;

    private List<Message> msgList;
    private JTextPane messagesPane;

    public MsgReader(List<Message> msgList, JTextPane messagesPane){
        this.msgList = msgList;
        this.worker = new Thread(this);
        this.messagesPane = messagesPane;
    }

    public void start(){ worker.start(); }

    @Override
    public void run(){
        while(true){

            try {
                Thread.sleep(200); // magical sleep, without doesnt work

                for(Message s : msgList){
                    if(!s.isPrinted()){
                        System.out.println(s.getSender() + ": " + s.getContent());
                        // append text to currently displayed
                        messagesPane.setText(messagesPane.getText() +
                                s.getSender() + ": " + s.getContent() + "\n");

                        s.setPrinted(true);
                    }

                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }


    }


}
