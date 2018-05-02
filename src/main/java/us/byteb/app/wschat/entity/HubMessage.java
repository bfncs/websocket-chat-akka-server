package us.byteb.app.wschat.entity;

public class HubMessage {
  private String user;
  private MessagePayload payload;

  public HubMessage(final String user, final MessagePayload payload) {
    this.user = user;
    this.payload = payload;
  }

  public String getUser() {
    return user;
  }

  public MessagePayload getPayload() {
    return payload;
  }
}
