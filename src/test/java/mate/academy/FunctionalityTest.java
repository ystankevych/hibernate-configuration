package mate.academy;

import static org.mockito.Mockito.description;
import static org.mockito.Mockito.inOrder;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import mate.academy.dao.MovieDao;
import mate.academy.dao.MovieDaoImpl;
import mate.academy.exception.DataProcessingException;
import mate.academy.lib.Injector;
import mate.academy.model.Movie;
import mate.academy.service.MovieService;
import mate.academy.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.exceptions.verification.VerificationInOrderFailure;

public class FunctionalityTest extends AbstractTest {
    private Injector injector = Injector.getInstance("mate.academy");

    @Override
    protected Class<?>[] entities() {
        return new Class[]{Movie.class};
    }

    @Test
    public void functionality_addAndGetMovie_Ok() {
        setSessionFactory(getSessionFactory());
        MovieService movieService = (MovieService) injector
                .getInstance(MovieService.class);
        Movie testMovie = getTestMovie();
        Movie addedMovie = movieService.add(testMovie);
        Assert.assertNotNull("Failed to add movie to db through \"add(Movie movie)\" method. "
                + "Added movie should not be null.", addedMovie);
        Assert.assertNotNull("The movie added to the database must not have null id.",
                addedMovie.getId());
        Movie movieFromDB = movieService.get(addedMovie.getId());
        Assert.assertNotNull("Failed to get movie from db through \"get(Long id)\" method."
                + "Movie should not be null.", movieFromDB);
        List<Field> nullFields = Arrays.stream(Movie.class.getDeclaredFields()).filter(
                f -> {
                    try {
                        f.setAccessible(true);
                        return f.get(movieFromDB) == null;
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("Failed to get field "
                                + f + " value for movie " + movieFromDB, e);
                    }
                }
        ).collect(Collectors.toList());
        Assert.assertEquals("We should not get null fields after getting movie "
                + testMovie + " from db.", 0, nullFields.size());
    }

    @Test
    public void add_closeSessionAndRollbackTransaction_ok() {
        try {
            SessionFactory mockedSessionFactory = Mockito.mock(SessionFactory.class);
            Session mockedSession = Mockito.mock(Session.class);
            Transaction mockedTransaction = Mockito.mock(Transaction.class);
            Mockito.when(mockedSessionFactory.openSession()).thenReturn(mockedSession);
            Mockito.when(mockedSession.beginTransaction()).thenReturn(mockedTransaction);
            setSessionFactory(mockedSessionFactory);
            Movie movie = getTestMovie();
            Mockito.when(mockedSession.save(movie)).thenThrow(new RuntimeException());
            MovieDao movieDao = new MovieDaoImpl();
            try {
                movieDao.add(movie);
            } catch (DataProcessingException e) {
                Mockito.verify(mockedTransaction,
                        description("You should close transaction in catch block "
                                + "if something went wrong while saving movie.")).rollback();
                Mockito.verify(mockedSession, description("You should close session with db "
                        + "in \"add(Movie movie)\" method after adding Movie do db."))
                        .close();
                InOrder inOrder = inOrder(mockedTransaction, mockedSession);

                inOrder.verify(mockedTransaction).rollback();
                inOrder.verify(mockedSession).close();
                return;
            } catch (Exception e) {
                Assert.fail("It's better to catch general \"Exception\" or \"RuntimeException\" "
                        + "instead of specific one in \"add(Movie movie)\" method.");
            }
            Assert.fail(
                    "You should throw DataProcessingException in the catch block on dao layer.");
        } catch (VerificationInOrderFailure ex) {
            Assert.fail("You should not use try-with-resources in \"add(Movie movie)\" method, "
                    + "as it closes session before catch block, so you could not rollback "
                    + "transaction. You should close session in finally block in order to make"
                    + " transaction rollback.");
        }
    }

    @Test
    public void get_closeSession_ok() {
        SessionFactory mockedSessionFactory = Mockito.mock(SessionFactory.class);
        Session mockedSession = Mockito.mock(Session.class);
        Mockito.when(mockedSessionFactory.openSession()).thenReturn(mockedSession);
        setSessionFactory(mockedSessionFactory);
        MovieDao movieDao = new MovieDaoImpl();
        Long movieId = 1L;
        Mockito.when(mockedSession.get(Movie.class, movieId)).thenReturn(null);
        movieDao.get(movieId);
        Mockito.verify(mockedSession, description("You should close session in \"get(Long id)\" "
                + "method in dao layer after getting movie from db. You can use"
                + " try-with-resources for this purpose.")).close();
    }

    private Movie getTestMovie() {
        Movie testMovie;
        try {
            testMovie = Movie.class.getConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Could not get constructor of Movie class, "
                    + "you should create a default constructor in Movie entity.", e);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Could not create instance of Movie class.", e);
        }
        Arrays.stream(Movie.class.getDeclaredFields())
                .filter(f -> f.getType().getSimpleName().equals("String"))
                .forEach(f -> {
                    f.setAccessible(true);
                    try {
                        f.set(testMovie, f.getName());
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("Could not set value to movie instance.");
                    }
                });
        return testMovie;
    }

    private void setSessionFactory(SessionFactory sessionFactory) {
        Optional<Field> optionalSessionFactory = Arrays
                .stream(HibernateUtil.class.getDeclaredFields())
                .filter(f -> f.getType().getSimpleName().equals("SessionFactory"))
                .findAny();
        if (optionalSessionFactory.isEmpty()) {
            Assert.fail("Your HibernateUtil class should have SessionFactory field.");
        }
        Field field = optionalSessionFactory.get();
        field.setAccessible(true);
        try {
            field.set(null, sessionFactory);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to set SessionFactory to HibernateUtil class.", e);
        }
    }
}
