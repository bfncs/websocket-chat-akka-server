package us.byteb.app.wschat.gson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import us.byteb.app.wschat.entity.ChatMessage;
import us.byteb.app.wschat.entity.Login;
import us.byteb.app.wschat.entity.MessagePayload;

class GsonUtilsTest {

  @org.junit.jupiter.api.Test
  void polymorphDeserializeChatMessage() {
    final String theAuthor = "theAuthor";
    final String theMessage = "theMessage";
    final ChatMessage expected = new ChatMessage(theAuthor, theMessage);

    final String jsonText =
        String.format(
            "{\"type\": \"ChatMessage\",\"author\": \"%s\",\"message\": \"%s\"}",
            theAuthor, theMessage);

    final MessagePayload parsed = GsonUtils.getGson().fromJson(jsonText, MessagePayload.class);
    assertTrue(parsed instanceof ChatMessage);

    final ChatMessage chatMessage = (ChatMessage) parsed;

    assertEquals(expected.getAuthor(), chatMessage.getAuthor());
    assertEquals(expected.getMessage(), chatMessage.getMessage());
  }

  @org.junit.jupiter.api.Test
  void polymorphDeserializeLogin() {
    final String theUser = "theUser";
    final Login expected = new Login(theUser);

    final String jsonText = String.format("{\"type\": \"Login\",\"user\": \"%s\"}", theUser);

    final MessagePayload parsed = GsonUtils.getGson().fromJson(jsonText, MessagePayload.class);

    assertTrue(parsed instanceof Login);

    final Login login = (Login) parsed;

    assertEquals(expected.getUser(), login.getUser());
  }
}
