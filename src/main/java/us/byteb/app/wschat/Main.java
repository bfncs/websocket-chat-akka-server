package us.byteb.app.wschat;

import static java.text.MessageFormat.format;
import static us.byteb.app.wschat.gson.GsonUtils.getGson;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.ws.Message;
import akka.http.javadsl.model.ws.TextMessage;
import akka.http.javadsl.model.ws.WebSocket;
import akka.japi.Function;
import akka.japi.Pair;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.*;
import io.vavr.control.Try;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import scala.concurrent.duration.FiniteDuration;
import us.byteb.app.wschat.entity.ChatMessage;
import us.byteb.app.wschat.entity.HubMessage;
import us.byteb.app.wschat.entity.Login;
import us.byteb.app.wschat.entity.MessagePayload;

public class Main {

  public static final FiniteDuration KEEPALIVE_MAX_IDLE = new FiniteDuration(10, TimeUnit.SECONDS);
  public static final String KEEPALIVE = "keepalive";
  public static final TextMessage KEEPALIVE_MESSAGE = TextMessage.create(KEEPALIVE);

  public static void main(String[] args) throws Exception {
    ActorSystem system = ActorSystem.create();
    final Materializer materializer = ActorMaterializer.create(system);

    final Pair<Sink<HubMessage, NotUsed>, Source<HubMessage, NotUsed>> messageHub =
        buildMessageHub().run(materializer);

    buildTickSource()
        .map(
            s ->
                new HubMessage(
                    "System", new ChatMessage("System", format("Hey, the epoch is {0}.", s))))
        .runWith(messageHub.first(), materializer);

    final Function<String, Flow<Message, Message, NotUsed>> flowCreator =
        user -> buildMessageFlow(user, messageHub.first(), messageHub.second());

    try {
      CompletionStage<ServerBinding> serverBindingFuture =
          Http.get(system)
              .bindAndHandleSync(
                  buildRequestHandler(flowCreator),
                  ConnectHttp.toHost("localhost", 8080),
                  materializer);

      Http.get(system);
      // will throw if binding fails
      serverBindingFuture.toCompletableFuture().get(1, TimeUnit.SECONDS);

      System.out.println("Press ENTER to stop.");
      new BufferedReader(new InputStreamReader(System.in)).readLine();
    } finally {
      system.terminate();
    }
  }

  private static RunnableGraph<Pair<Sink<HubMessage, NotUsed>, Source<HubMessage, NotUsed>>>
      buildMessageHub() {
    return MergeHub.of(HubMessage.class).toMat(BroadcastHub.of(HubMessage.class), Keep.both());
  }

  private static Source<String, Cancellable> buildTickSource() {
    final FiniteDuration twoMinutes = new FiniteDuration(120, TimeUnit.SECONDS);
    return Source.tick(twoMinutes, twoMinutes, 0)
        .map(nothing -> String.valueOf(Instant.now().getEpochSecond()));
  }

  private static Flow<Message, Message, NotUsed> buildMessageFlow(
      String user, Sink<HubMessage, NotUsed> sink, Source<HubMessage, NotUsed> source) {
    final Sink<Message, NotUsed> hubSink =
        Flow.of(Message.class)
            .flatMapConcat(message -> message.asTextMessage().getStreamedText())
            .filter(message -> !message.equals(KEEPALIVE))
            .map(message -> getGson().fromJson(message, MessagePayload.class))
            .map(message -> new HubMessage(user, message))
            .to(sink);

    final Source<Message, NotUsed> hubSource =
        source
            .filter(hubMessage -> !hubMessage.getUser().equals(user))
            .flatMapConcat(
                hubMessage -> {
                  final MessagePayload payload = hubMessage.getPayload();
                  if (payload instanceof ChatMessage || payload instanceof Login) {
                    return Source.single(TextMessage.create(getGson().toJson(payload)));
                  }
                  return Source.empty();
                })
            .keepAlive(KEEPALIVE_MAX_IDLE, () -> KEEPALIVE_MESSAGE);

    return Flow.fromSinkAndSource(hubSink, hubSource);
  }

  private static Function<HttpRequest, HttpResponse> buildRequestHandler(
      Function<String, Flow<Message, Message, NotUsed>> flowCreator) {
    return request -> {
      System.out.println("Handling request to " + request.getUri());
      if (request.getUri().path().equals("/events")) {
        return request
            .getUri()
            .query()
            .get("user")
            .flatMap(user -> Try.of(() -> flowCreator.apply(user)).toJavaOptional())
            .map(flow -> WebSocket.handleWebSocketRequestWith(request, flow))
            .orElse(HttpResponse.create().withStatus(400).withEntity("User missing."));

      } else {
        return HttpResponse.create().withStatus(404);
      }
    };
  }
}
