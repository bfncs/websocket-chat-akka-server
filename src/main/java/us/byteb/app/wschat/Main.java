package us.byteb.app.wschat;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpHeader;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.ws.Message;
import akka.http.javadsl.model.ws.TextMessage;
import akka.http.javadsl.model.ws.WebSocket;
import akka.japi.Function;
import akka.japi.JavaPartialFunction;
import akka.japi.Pair;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.*;
import scala.concurrent.duration.FiniteDuration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"Convert2MethodRef", "ConstantConditions"})
public class Main {

    public static void main(String[] args) throws Exception {
        ActorSystem system = ActorSystem.create();
        final Materializer materializer = ActorMaterializer.create(system);

        final FiniteDuration oneSecond = new FiniteDuration(1, TimeUnit.SECONDS);
        Source.tick(oneSecond, oneSecond, 0)
                .map(nothing -> UUID.randomUUID())
                .runWith(Sink.foreach(id -> System.out.println(id)), materializer);

        final Pair<Sink<HubMessage, NotUsed>, Source<HubMessage, NotUsed>> hub =
                MergeHub.of(HubMessage.class)
                        .toMat(BroadcastHub.of(HubMessage.class), Keep.both())
                        .run(materializer);

        final Function<String, Flow<Message, Message, NotUsed>> flowCreator = user -> {
            final Sink<Message, NotUsed> hubSink = Flow.fromFunction((Message message) -> new HubMessage(user, message)).to(hub.first());
            final Source<Message, NotUsed> hubSource = hub.second().map(hubMessage -> hubMessage.getMessage());
            final Flow<Message, Message, NotUsed> flow = Flow.fromSinkAndSource(hubSink, hubSource);
            return flow;
        };

        try {

            CompletionStage<ServerBinding> serverBindingFuture =
                    Http.get(system).bindAndHandleSync(
                            buildRequestHandler(flowCreator),
                            ConnectHttp.toHost("localhost", 8080),
                            materializer
                    );

            Http.get(system);
            // will throw if binding fails
            serverBindingFuture.toCompletableFuture().get(1, TimeUnit.SECONDS);

            System.out.println("Press ENTER to stop.");
            new BufferedReader(new InputStreamReader(System.in)).readLine();
        } finally {
            system.terminate();
        }
    }

    private static Function<HttpRequest, HttpResponse> buildRequestHandler(Function<String, Flow<Message, Message, NotUsed>> flowCreator) {
        return request -> {
            System.out.println("Handling request to " + request.getUri());
            final Optional<HttpHeader> user = request.getHeader("user");
            if (request.getUri().path().equals("/events") && user.isPresent()) {
                return WebSocket.handleWebSocketRequestWith(request, flowCreator.apply(user.get().value()));
            } else {
                return HttpResponse.create().withStatus(404);
            }
        };
    }


    /**
     * A handler that treats incoming messages as a name,
     * and responds with a greeting to that name
     */
    private static Flow<Message, Message, NotUsed> greeter() {
        return
                Flow.<Message>create()
                        .collect(new JavaPartialFunction<Message, Message>() {
                            @Override
                            public Message apply(Message msg, boolean isCheck) throws Exception {
                                if (isCheck) {
                                    if (msg.isText()) {
                                        return null;
                                    } else {
                                        throw noMatch();
                                    }
                                } else {
                                    return handleTextMessage(msg.asTextMessage());
                                }
                            }
                        });
    }

    private static TextMessage handleTextMessage(TextMessage msg) {
        if (msg.isStrict()) // optimization that directly creates a simple response...
        {
            return TextMessage.create("Hello " + msg.getStrictText());
        } else // ... this would suffice to handle all text messages in a streaming fashion
        {
            return TextMessage.create(Source.single("Hello ").concat(msg.getStreamedText()));
        }
    }
}
