package hrytsenko.client;

import data.Envelope;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.datagram.DatagramPacket;
import io.vertx.core.datagram.DatagramSocket;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Slf4j
@ApplicationScoped
class UdpClient {

  @ConfigProperty(name = "client.host")
  String clientHost;
  @ConfigProperty(name = "client.port")
  int clientPort;
  @ConfigProperty(name = "client.timeout")
  int clientTimeout;

  @ConfigProperty(name = "server.host")
  String serverHost;
  @ConfigProperty(name = "server.port")
  int serverPort;

  @Inject
  Vertx vertx;
  DatagramSocket socket;

  Map<Long, Promise<Envelope>> responsePromises = new ConcurrentHashMap<>();

  @PostConstruct
  void init() {
    socket = vertx.createDatagramSocket();
    socket.listen(clientPort, clientHost,
        result -> {
          if (result.failed()) {
            throw new RuntimeException("Cannot start client", result.cause());
          }
          log.info("Client started");
          socket.handler(this::handleResponse);
        });
    log.info("Client created");
  }

  private void handleResponse(DatagramPacket packet) {
    var serverHost = packet.sender().host();
    var serverPort = packet.sender().port();
    log.info("Received packet from {}:{}", serverHost, serverPort);

    try {
      var responseContent = packet.data().getBytes();
      var responseEnvelope = Envelope.parseFrom(responseContent);
      log.info("Received response {}", responseEnvelope);

      var responsePromise = responsePromises.get(responseEnvelope.getId());
      if (responsePromise != null) {
        responsePromise.tryComplete(responseEnvelope);
      }
    } catch (Exception exception) {
      log.error("Cannot handle response", exception);
    }
  }

  CompletableFuture<Envelope> sendRequest(Envelope requestEnvelope) {
    log.info("Send request {}", requestEnvelope.getId());
    var requestFuture = new CompletableFuture<Envelope>();
    var requestContent = requestEnvelope.toByteArray();
    socket.send(Buffer.buffer(requestContent), serverPort, serverHost,
        result -> {
          if (result.succeeded()) {
            log.info("Request sent {}", requestEnvelope);
          } else {
            log.error("Cannot send request", result.cause());
            requestFuture.completeExceptionally(result.cause());
          }
        });

    var requestTimer = vertx.setTimer(clientTimeout, _ -> {
      log.info("Request dropped {}", requestEnvelope.getId());
      responsePromises.remove(requestEnvelope.getId());
      requestFuture.completeExceptionally(new RuntimeException("Request dropped"));
    });

    var responsePromise = Promise.<Envelope>promise();
    responsePromises.put(requestEnvelope.getId(), responsePromise);
    responsePromise.future().onComplete(result -> {
      log.info("Request completed {}", requestEnvelope.getId());
      vertx.cancelTimer(requestTimer);
      if (result.succeeded()) {
        requestFuture.complete(result.result());
      } else {
        requestFuture.completeExceptionally(result.cause());
      }
    });
    return requestFuture;
  }

}
