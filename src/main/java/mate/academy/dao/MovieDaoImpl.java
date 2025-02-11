package mate.academy.dao;

import mate.academy.lib.Dao;
import mate.academy.model.Movie;
import mate.academy.util.HibernateUtil;

@Dao
public class MovieDaoImpl implements MovieDao {
    /**
     * Default method from SessionFactory interface.
     * Open a Session and use it to obtain a value within the bounds of a transaction.
     * @param movie {@link Movie} entity to be saved
     * @return saved entity with not null id.
     */
    @Override
    public Movie add(Movie movie) {
        return HibernateUtil.getInstance()
                .fromTransaction(session -> {
                    session.persist(movie);
                    return movie;
                });
    }

    /**
     * fromSession is a default method from SessionFactory interface (Open a Session and use it to obtain a value.)
     * @param id {@link Long}
     * @return an entity retrieved from the database
     */
    @Override
    public Movie get(Long id) {
        return HibernateUtil.getInstance()
                .fromSession(session -> session.get(Movie.class, id));
    }
}
