package pg.projekt.sockets.messages;


import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode
public class Message implements Serializable {
    private String content;
    private String sender;

    public Message(String content, String sender){
        this.content = content;
        this.sender = sender;
    }

    @Override
    public String toString(){
        return content;
    }

}
