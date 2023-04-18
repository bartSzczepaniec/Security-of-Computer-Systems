package pg.projekt.sockets.messages;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
public class MessageToBeConfirmed {
    private Message msg;
    private Integer confirmationSignPos; // Position of confirmation mark in the textpane

    public MessageToBeConfirmed(Message msg, Integer confirmationSignPos) {
        this.msg = msg;
        this.confirmationSignPos = confirmationSignPos;
    }
}
