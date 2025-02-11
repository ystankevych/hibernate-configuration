package mate.academy.dao;

import mate.academy.model.Movie;

public interface MovieDao {
    Movie add(Movie movie);

    Movie get(Long id);
}
