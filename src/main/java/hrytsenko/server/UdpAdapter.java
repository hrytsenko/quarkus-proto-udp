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

  Envelope handle(Envelope request) {
    return switch (request.getKindCase()) {
      case CREATE_MOVIE_REQUEST -> createMovie(request);
      case FIND_MOVIE_REQUEST -> findMovie(request);
      default -> unknownRequest(request);
    };
  }

  private Envelope createMovie(Envelope request) {
    log.info("Create movie {}", request);
    var requestPayload = request.getCreateMovieRequest();
    var movie = Movie.builder()
        .imdb(requestPayload.getImdb())
        .title(requestPayload.getTitle())
        .build();
    movieRepository.createMovie(movie);

    var responsePayload = CreateMovieResponse.newBuilder()
        .build();
    return Envelope.newBuilder()
        .setId(request.getId())
        .setCreateMovieResponse(responsePayload)
        .build();
  }

  private Envelope findMovie(Envelope request) {
    log.info("Find movie {}", request);
    var requestPayload = request.getFindMovieRequest();
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
        .setId(request.getId())
        .setFindMovieResponse(responsePayload)
        .build();
  }

  private static Envelope unknownRequest(Envelope request) {
    var errorPayload = ErrorResponse.newBuilder()
        .setCode(Code.UNKNOWN_REQUEST)
        .build();
    return Envelope.newBuilder()
        .setId(request.getId())
        .setErrorResponse(errorPayload)
        .build();
  }

}
