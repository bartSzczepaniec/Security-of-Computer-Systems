package pg.projekt.sockets.messages;

import java.util.List;

public class MsgReader implements Runnable{
    private Thread worker;

    private List<Message> msgList;

    public MsgReader(List<Message> msgList){
        this.msgList = msgList;
        this.worker = new Thread(this);
    }

    public void start(){ worker.start(); }

    @Override
    public void run(){
        while(true){

            try {
                Thread.sleep(2000);

                for(Message s : msgList){
                    if(!s.isPrinted()){
                        System.out.println(s.getSender() + ": " + s.getContent());
                        s.setPrinted(true);
                    }

                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }


    }


}
