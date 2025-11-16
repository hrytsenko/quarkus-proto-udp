package hrytsenko.server;

import data.CreateMovieResponse;
import data.Envelope;
import data.ErrorResponse;
import data.ErrorResponse.Code;
import data.FindMovieResponse;
import hrytsenko.server.MovieRepository.Movie;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
class UdpAdapter {

  @Inject
  MovieRepository movieRepository;

  Envelope handleRequest(Envelope request) {
    return switch (request.getPayloadCase()) {
      case CREATE_MOVIE_REQUEST -> createMovie(request);
      case FIND_MOVIE_REQUEST -> findMovie(request);
      default -> unknownRequest(request);
    };
  }

  private Envelope createMovie(Envelope requestEnvelope) {
    log.info("Create movie {}", requestEnvelope);
    var requestPayload = requestEnvelope.getCreateMovieRequest();
    var movie = Movie.builder()
        .imdb(requestPayload.getImdb())
        .title(requestPayload.getTitle())
        .build();
    movieRepository.createMovie(movie);

    var responsePayload = CreateMovieResponse.newBuilder()
        .build();
    return Envelope.newBuilder()
        .setId(requestEnvelope.getId())
        .setCreateMovieResponse(responsePayload)
        .build();
  }

  private Envelope findMovie(Envelope requestEnvelope) {
    log.info("Find movie {}", requestEnvelope);
    var requestPayload = requestEnvelope.getFindMovieRequest();
    var movie = movieRepository.findMovie(requestPayload.getImdb());

    var responsePayload = movie
        .map(it -> FindMovieResponse.newBuilder()
            .setExists(true)
            .setImdb(it.imdb())
            .setTitle(it.title())
            .build())
        .orElseGet(() -> FindMovieResponse.newBuilder()
            .setExists(false)
            .build());
    return Envelope.newBuilder()
        .setId(requestEnvelope.getId())
        .setFindMovieResponse(responsePayload)
        .build();
  }

  private static Envelope unknownRequest(Envelope requestEnvelope) {
    log.info("Unknown request {}", requestEnvelope);
    var errorPayload = ErrorResponse.newBuilder()
        .setCode(Code.UNKNOWN_REQUEST)
        .build();
    return Envelope.newBuilder()
        .setId(requestEnvelope.getId())
        .setErrorResponse(errorPayload)
        .build();
  }

}
