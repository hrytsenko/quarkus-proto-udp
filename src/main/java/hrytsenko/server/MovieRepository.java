package hrytsenko.server;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
class MovieRepository {

  Map<String, Movie> movies = new ConcurrentHashMap<>();

  void createMovie(Movie movie) {
    log.info("Create movie {}", movie);
    movies.put(movie.imdb(), movie);
  }

  Optional<Movie> findMovie(String imdb) {
    log.info("Find movie {}", imdb);
    return Optional.ofNullable(movies.get(imdb));
  }

  @Builder
  public record Movie(String imdb, String title) {

  }

}
