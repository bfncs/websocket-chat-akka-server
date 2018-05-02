package us.byteb.app.wschat.entity;

public class ChatMessage extends MessagePayload {

    // TODO: find a better way to provide type in serialization
    private String type = "ChatMessage";
    private String author;
    private String message;

    public ChatMessage() {
    }

    public ChatMessage(String author, String message) {
        this.author = author;
        this.message = message;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
