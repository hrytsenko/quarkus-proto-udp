package hrytsenko.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import data.CreateMovieRequest;
import data.CreateMovieResponse;
import data.Envelope;
import data.ErrorResponse;
import data.ErrorResponse.Code;
import data.FindMovieRequest;
import data.FindMovieResponse;
import hrytsenko.client.MovieResource.Movie;
import jakarta.ws.rs.WebApplicationException;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class UdpAdapterTest {

  UdpAdapter udpAdapter;
  UdpClient udpClient;

  @BeforeEach
  void init() {
    udpClient = Mockito.mock(UdpClient.class);
    udpAdapter = new UdpAdapter();
    udpAdapter.client = udpClient;
  }

  @Test
  void createMovie_MovieIsCreated() {
    mockResponse(Envelope.newBuilder()
        .setCreateMovieResponse(CreateMovieResponse.newBuilder()
            .build())
        .build());

    var sourceMovie = Movie.builder()
        .imdb("0084787")
        .title("The Thing")
        .build();

    udpAdapter.createMovie(sourceMovie);

    assertRequest(Envelope.newBuilder()
        .setId(1)
        .setCreateMovieRequest(CreateMovieRequest.newBuilder()
            .setImdb("0084787")
            .setTitle("The Thing")
            .build())
        .build());
  }

  @Test
  void createMovie_ServerError() {
    mockResponse(Envelope.newBuilder()
        .setErrorResponse(ErrorResponse.newBuilder()
            .setCode(Code.UNKNOWN_REQUEST)
            .build())
        .build());

    var sourceMovie = Movie.builder()
        .imdb("0084787")
        .title("The Thing")
        .build();

    assertThrows(WebApplicationException.class,
        () -> udpAdapter.createMovie(sourceMovie));
  }

  @Test
  void findMovie_MovieIsAbsent() {
    mockResponse(Envelope.newBuilder()
        .setFindMovieResponse(FindMovieResponse.newBuilder()
            .setExists(false)
            .build())
        .build());

    var actualMovie = udpAdapter.findMovie("0084787");

    assertFalse(actualMovie.isPresent());

    assertRequest(Envelope.newBuilder()
        .setId(1)
        .setFindMovieRequest(FindMovieRequest.newBuilder()
            .setImdb("0084787")
            .build())
        .build());
  }

  @Test
  void findMovie_MovieIsPresent() {
    mockResponse(Envelope.newBuilder()
        .setFindMovieResponse(FindMovieResponse.newBuilder()
            .setExists(true)
            .setImdb("0084787")
            .setTitle("The Thing")
            .build())
        .build());

    var actualMovie = udpAdapter.findMovie("0084787");

    assertTrue(actualMovie.isPresent());

    var expectedMovie = Movie.builder()
        .imdb("0084787")
        .title("The Thing")
        .build();
    assertEquals(expectedMovie, actualMovie.get());

    assertRequest(Envelope.newBuilder()
        .setId(1)
        .setFindMovieRequest(FindMovieRequest.newBuilder()
            .setImdb("0084787")
            .build())
        .build());
  }

  @Test
  void findMovie_ServerError() {
    mockResponse(Envelope.newBuilder()
        .setErrorResponse(ErrorResponse.newBuilder()
            .setCode(Code.UNKNOWN_REQUEST)
            .build())
        .build());

    assertThrows(WebApplicationException.class,
        () -> udpAdapter.findMovie("0084787"));
  }

  private void mockResponse(Envelope successResponse) {
    var responseFuture = new CompletableFuture<Envelope>();
    responseFuture.complete(successResponse);
    doReturn(responseFuture).when(udpClient).sendRequest(Mockito.any());
  }

  private void assertRequest(Envelope expectedRequest) {
    verify(udpClient).sendRequest(Mockito.eq(expectedRequest));
  }

}
