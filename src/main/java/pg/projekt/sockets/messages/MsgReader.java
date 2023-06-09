package pg.projekt.sockets.messages;


import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MsgReader implements Runnable{
    private Thread worker;

    private List<Message> msgList;
    private List<MessageToBeConfirmed> msgToConfirmList;
    private JTextPane messagesPane;

    public MsgReader(List<Message> msgList, JTextPane messagesPane){
        this.msgList = msgList;
        this.worker = null;
        this.messagesPane = messagesPane;
        this.msgToConfirmList = Collections.synchronizedList(new ArrayList<MessageToBeConfirmed>());
    }

    public void start(){
        this.worker = new Thread(this);
        worker.start();
    }

    @Override
    public void run(){
        SimpleAttributeSet boldText = new SimpleAttributeSet();
        SimpleAttributeSet basicText = new SimpleAttributeSet();
        SimpleAttributeSet infoText = new SimpleAttributeSet();

        StyleConstants.setBold(boldText, true);
        StyleConstants.setForeground(infoText, Color.RED);

        while(true){

            try {
                Thread.sleep(200); // magical sleep, without doesnt work
                Document doc = messagesPane.getDocument();

                for(int i =0; i< msgList.size(); i++){
                    Message s = msgList.get(i);
                    //System.out.println(s.getSender() + ": " + s.getContent() + "|" + s.getUuid().toString());
                    // append text to currently displayed
                    MessageType type = s.getType();

                    switch (type){
                        case TEXT:
                            String sender = s.getSender();
                            String content = s.getContent();

                            doc.insertString(doc.getLength(),sender + ": " , boldText );
                            doc.insertString(doc.getLength(), content+ "\n", basicText );
                            // TODO: miejsce na confirmed
                            if (sender.equals("You"))
                                msgToConfirmList.add(new MessageToBeConfirmed(s, doc.getLength()-1));
                            break;
                        case INFO:
                            String info = s.getContent();
                            doc.insertString(doc.getLength(), info +"\n", infoText);
                            break;
                        case CONFIRM:
                            confirmMessage(s, doc, basicText);
                            break;
                    }


                    msgList.remove(s);
                }
            }
            catch (InterruptedException|BadLocationException e) {
                throw new RuntimeException(e);
            }

        }


    }

    public void confirmMessage(Message confirmation, Document doc, SimpleAttributeSet basicText) throws BadLocationException {
        for (MessageToBeConfirmed msgToConfirm : msgToConfirmList) {
            //System.out.println("CHECKING: " + msgToConfirm.getMsg().getUuid().toString());
            if(msgToConfirm.getMsg().getUuid().toString().equals(confirmation.getContent())) {
                doc.insertString(msgToConfirm.getConfirmationSignPos(), "✓", basicText );
            }
        }
    }


}
