package pg.projekt.sockets.messages;


import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode
public class Message implements Serializable {
    private String content;
    private String sender;
    private boolean printed;

    public Message(String content, String sender){
        this.content = content;
        this.sender = sender;
        this.printed = false;
    }

    @Override
    public String toString(){
        return content;
    }

}
