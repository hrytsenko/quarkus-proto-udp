package hrytsenko.server;

import data.Envelope;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.datagram.DatagramPacket;
import io.vertx.core.datagram.DatagramSocket;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Slf4j
@ApplicationScoped
class UdpServer {

  @ConfigProperty(name = "server.host")
  String serverHost;
  @ConfigProperty(name = "server.port")
  int serverPort;

  @Inject
  Vertx vertx;
  DatagramSocket socket;

  @Inject
  UdpAdapter udpAdapter;

  void onStart(@Observes StartupEvent event) {
    socket = vertx.createDatagramSocket();
    socket.listen(serverPort, serverHost,
        result -> {
          if (result.failed()) {
            throw new RuntimeException("Cannot start UDP server", result.cause());
          }
          log.info("UDP server started");
          socket.handler(this::handleRequest);
        });
    log.info("UDP server created");
  }

  private void handleRequest(DatagramPacket packet) {
    var clientHost = packet.sender().host();
    var clientPort = packet.sender().port();
    log.info("Received packet from {}:{}", clientHost, clientPort);

    try {
      var requestContent = packet.data().getBytes();
      var requestEnvelope = Envelope.parseFrom(requestContent);
      log.info("Received request {}", requestEnvelope.getId());

      var responseEnvelope = udpAdapter.handle(requestEnvelope);

      var responseContent = responseEnvelope.toByteArray();
      socket.send(Buffer.buffer(responseContent), clientPort, clientHost,
          result -> {
            if (result.failed()) {
              log.error("Response failed", result.cause());
            }
          });
    } catch (Exception exception) {
      log.error("Cannot handle request", exception);
    }
  }

}
