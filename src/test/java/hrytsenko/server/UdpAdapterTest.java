package hrytsenko.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import data.CreateMovieRequest;
import data.Envelope;
import data.ErrorResponse;
import data.ErrorResponse.Code;
import hrytsenko.server.MovieRepository.Movie;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class UdpAdapterTest {

  UdpAdapter udpAdapter;
  MovieRepository movieRepository;

  @BeforeEach
  void init() {
    movieRepository = Mockito.mock(MovieRepository.class);
    udpAdapter = new UdpAdapter();
    udpAdapter.movieRepository = movieRepository;
  }

  @Test
  void createMovie_MovieIsCreated() {
    var sourceRequest = Envelope.newBuilder()
        .setId(1)
        .setCreateMovieRequest(CreateMovieRequest.newBuilder()
            .setImdb("0084787")
            .setTitle("The Thing")
            .build())
        .build();

    var actualResponse = udpAdapter.handle(sourceRequest);

    var expectedResponse = Envelope.newBuilder()
        .setId(1)
        .setCreateMovieResponse(data.CreateMovieResponse.newBuilder()
            .build())
        .build();
    assertEquals(expectedResponse, actualResponse);

    var expectedMovie = Movie.builder()
        .imdb("0084787")
        .title("The Thing")
        .build();
    verify(movieRepository).createMovie(Mockito.eq(expectedMovie));
  }

  @Test
  void findMovie_MovieIsAbsent() {
    doReturn(Optional.empty()).when(movieRepository).findMovie(Mockito.any());

    var sourceRequest = Envelope.newBuilder()
        .setId(1)
        .setFindMovieRequest(data.FindMovieRequest.newBuilder()
            .setImdb("0084787")
            .build())
        .build();

    var actualResponse = udpAdapter.handle(sourceRequest);

    var expectedResponse = Envelope.newBuilder()
        .setId(1)
        .setFindMovieResponse(data.FindMovieResponse.newBuilder()
            .setExists(false)
            .build())
        .build();
    assertEquals(expectedResponse, actualResponse);

    verify(movieRepository).findMovie(Mockito.eq("0084787"));
  }

  @Test
  void findMovie_MovieIsPresent() {
    var movie = Movie.builder()
        .imdb("0084787")
        .title("The Thing")
        .build();
    doReturn(Optional.of(movie)).when(movieRepository).findMovie(Mockito.any());

    var sourceRequest = Envelope.newBuilder()
        .setId(1)
        .setFindMovieRequest(data.FindMovieRequest.newBuilder()
            .setImdb("0084787")
            .build())
        .build();

    var actualResponse = udpAdapter.handle(sourceRequest);

    var expectedResponse = Envelope.newBuilder()
        .setId(1)
        .setFindMovieResponse(data.FindMovieResponse.newBuilder()
            .setExists(true)
            .setImdb("0084787")
            .setTitle("The Thing")
            .build())
        .build();
    assertEquals(expectedResponse, actualResponse);

    verify(movieRepository).findMovie(Mockito.eq("0084787"));
  }

  @Test
  void handleRequest_RequestIsUnknown() {
    var sourceRequest = Envelope.newBuilder()
        .setId(1)
        .build();

    var actualResponse = udpAdapter.handle(sourceRequest);

    var expectedResponse = Envelope.newBuilder()
        .setId(1)
        .setErrorResponse(ErrorResponse.newBuilder()
            .setCode(Code.UNKNOWN_REQUEST)
            .build())
        .build();
    assertEquals(expectedResponse, actualResponse);
  }

}
