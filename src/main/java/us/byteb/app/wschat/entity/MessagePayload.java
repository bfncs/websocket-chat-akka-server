package us.byteb.app.wschat.entity;

public abstract class MessagePayload {

    public String getType() {
        return this.getClass().getSimpleName();
    }
}
