package hrytsenko.client;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import lombok.Builder;

@Path("/movies")
class MovieResource {

  @Inject
  UdpAdapter udpAdapter;

  @POST
  @Consumes("application/json")
  public void createMovie(Movie movie) {
    udpAdapter.createMovie(movie);
  }

  @GET
  @Path("/{imdb}")
  @Produces("application/json")
  public Movie findMovie(@PathParam("imdb") String imdb) {
    return udpAdapter.findMovie(imdb)
        .orElseThrow(() -> new NotFoundException("Movie not found"));
  }

  @Builder
  public record Movie(String imdb, String title) {

  }

}
