package us.byteb.app.wschat.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import us.byteb.app.wschat.entity.ChatMessage;
import us.byteb.app.wschat.entity.Login;
import us.byteb.app.wschat.entity.MessagePayload;

public class GsonUtils {
  private static final GsonBuilder gsonBuilder =
      new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

  static {
    final RuntimeTypeAdapterFactory typeFactory =
        RuntimeTypeAdapterFactory.of(
                MessagePayload.class,
                "type") // Here you specify which is the parent class and what field particularizes
                        // the child class.
            .registerSubtype(
                ChatMessage.class,
                "ChatMessage") // if the flag equals the class name, you can skip the second
                               // parameter. This is only necessary, when the "type" field does not
                               // equal the class name.
            .registerSubtype(Login.class, "Login");
    gsonBuilder.registerTypeAdapterFactory(typeFactory);
  }

  public static Gson getGson() {
    return gsonBuilder.create();
  }
}
