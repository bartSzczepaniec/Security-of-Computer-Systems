package pg.projekt.sockets.messages;


import lombok.*;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

@Getter
@Setter
@EqualsAndHashCode
public class Message implements Serializable {
    private byte[] payload;

    public Message(String content, String sender){
        this.payload = (sender +"___joiner___" + content).getBytes(StandardCharsets.UTF_8);
    }

    // TODO: catch excpetion when joinerused
    public String getContent() throws ArrayIndexOutOfBoundsException{
        String payloadString = new String(this.payload, StandardCharsets.UTF_8);
        return payloadString.split("___joiner___")[1].trim();

    }

    public String getSender() throws ArrayIndexOutOfBoundsException{
        String payloadString = new String(this.payload, StandardCharsets.UTF_8);
        return payloadString.split("___joiner___")[0].trim();
    }


    @Override
    public String toString(){
        return getContent();
    }

}
