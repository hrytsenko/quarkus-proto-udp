package hrytsenko.client;

import data.CreateMovieRequest;
import data.Envelope;
import data.FindMovieRequest;
import hrytsenko.client.MovieResource.Movie;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
class UdpAdapter {

  @Inject
  UdpClient client;

  AtomicLong correlation = new AtomicLong(1);

  @SneakyThrows
  void createMovie(Movie movie) {
    var requestPayload = CreateMovieRequest.newBuilder()
        .setImdb(movie.imdb())
        .setTitle(movie.title())
        .build();
    var requestEnvelope = Envelope.newBuilder()
        .setId(correlation.getAndIncrement())
        .setCreateMovieRequest(requestPayload)
        .build();
    var responseEnvelope = client.sendRequest(requestEnvelope).get();
    failOnError(responseEnvelope, Envelope.KindCase.CREATE_MOVIE_RESPONSE);
  }

  @SneakyThrows
  Optional<Movie> findMovie(String imdb) {
    var requestPayload = FindMovieRequest.newBuilder()
        .setImdb(imdb)
        .build();
    var requestEnvelope = Envelope.newBuilder()
        .setId(correlation.getAndIncrement())
        .setFindMovieRequest(requestPayload)
        .build();
    var responseEnvelope = client.sendRequest(requestEnvelope).get();
    failOnError(responseEnvelope, Envelope.KindCase.FIND_MOVIE_RESPONSE);

    var responsePayload = responseEnvelope.getFindMovieResponse();
    if (!responsePayload.getExists()) {
      return Optional.empty();
    }
    var movie = Movie.builder()
        .imdb(responsePayload.getImdb())
        .title(responsePayload.getTitle())
        .build();
    return Optional.of(movie);
  }

  private void failOnError(Envelope responseEnvelope, Envelope.KindCase expectedResponse) {
    if (responseEnvelope.hasErrorResponse()) {
      log.error("Error response {}", responseEnvelope);
      throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
    }
    if (responseEnvelope.getKindCase() != expectedResponse) {
      log.info("Unexpected response {}", responseEnvelope);
      throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
    }
  }

}
