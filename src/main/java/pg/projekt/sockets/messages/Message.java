package pg.projekt.sockets.messages;


import lombok.*;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

@Getter
@Setter
@EqualsAndHashCode
public class Message implements Serializable {
    private byte[] payload;

    public Message(String content){
        this(content, "", MessageType.INFO);
    }
    public Message(String content, String sender){
        this(content, sender, MessageType.TEXT);
    }
    public Message(String content, String sender, MessageType type){
        this.payload = (type + "___joiner___" + sender +"___joiner___" + content).getBytes(StandardCharsets.UTF_8);
    }

    public String getContent() throws ArrayIndexOutOfBoundsException{
        String payloadString = new String(this.payload, StandardCharsets.UTF_8);
        return payloadString.split("___joiner___")[2].trim();

    }

    public String getSender() throws ArrayIndexOutOfBoundsException{
        String payloadString = new String(this.payload, StandardCharsets.UTF_8);
        return payloadString.split("___joiner___")[1].trim();
    }

    public MessageType getType() throws ArrayIndexOutOfBoundsException{
        String payloadString = new String(this.payload, StandardCharsets.UTF_8);
        return MessageType.valueOf(payloadString.split("___joiner___")[0].trim());
    }


    @Override
    public String toString(){
        return getContent();
    }

}
