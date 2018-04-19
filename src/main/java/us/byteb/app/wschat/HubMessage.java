package us.byteb.app.wschat;

import akka.http.javadsl.model.ws.Message;

class HubMessage {
    private String user;
    private Message message;

    public HubMessage(String user, Message message) {
        this.user = user;
        this.message = message;
    }

    public String getUser() {
        return user;
    }

    public Message getMessage() {
        return message;
    }
}
