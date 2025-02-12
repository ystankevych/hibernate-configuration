package mate.academy;

import mate.academy.lib.Injector;
import mate.academy.model.Movie;
import mate.academy.service.MovieService;

public class Main {
    private static final Injector injector = Injector.getInstance("mate.academy");
    private static final MovieService service =
            (MovieService) injector.getInstance(MovieService.class);

    public static void main(String[] args) {
        Movie movie = new Movie();
        movie.setTitle("Sex in the City");
        movie.setDescription("Drama");
        //add
        movie = service.add(movie);
        // at this point movie saved to the db and movie id is not null
        assert movie.getId() != null;

        //getById
        Movie byId = service.get(movie.getId());
        // movie successfully obtained from the db
        assert byId != null;
    }
}
