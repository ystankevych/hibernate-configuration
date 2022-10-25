package mate.academy;

import static org.mockito.Mockito.description;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mockStatic;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.exceptions.verification.VerificationInOrderFailure;

public class FunctionalityTest extends AbstractTest {
    private static List<Class> allClasses = new ArrayList<>();
    private MockedStatic mockedStatic;

    @BeforeClass
    public static void initTest() {
        try {
            allClasses = getClasses("mate.academy");
            if (allClasses.size() == 0) {
                Assert.fail("You should not rename base mate.academy package and project"
                        + " name should not contain spaces or some cyrillic letters in path");
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not load classes ", e);
        }
    }

    @After
    public void removeMock() {
        if (mockedStatic != null) {
            mockedStatic.close();
        }
    }

    @Override
    protected Class<?>[] entities() {
        return new Class[]{getClass("Movie")};
    }

    @Test
    public void functionality_addAndGetMovie_Ok() {
        mockSessionFactory(getSessionFactory());
        Class movieClass = getClass("Movie");
        Class dataProcessingExceptionClass = getClass("DataProcessingException");
        Object testMovie = getTestMovie();
        Object addedMovie;
        try {
            addedMovie = invokeAddMethod(testMovie);
        } catch (Exception e) {
            if (ExceptionUtils.indexOfThrowable(e, dataProcessingExceptionClass) != -1) {
                Assert.fail("Failed to add movie to db with exception" + e.getCause().getCause());
            }
            throw new RuntimeException("Failed to add movie " + testMovie, e);
        }

        Field movieIdField = Arrays.stream(movieClass.getDeclaredFields())
                .filter(f -> f.getType().getSimpleName().equals("Long"))
                .findAny()
                .get();
        Long movieId;
        try {
            movieIdField.setAccessible(true);
            movieId = (Long) movieIdField.get(addedMovie);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Could not get id from movie, added to db " + addedMovie, e);
        }
        Assert.assertNotNull("Failed to add movie to db through \"add(Movie movie)\" method. "
                + "Added movie should not be null.", addedMovie);
        Assert.assertNotNull("The movie added to the database must not have null id.",
                movieId);
        Optional optionalMovieFromDb;
        try {
            optionalMovieFromDb = invokeGetMethod(movieId);
        } catch (Exception e) {
            if (ExceptionUtils.indexOfThrowable(e, dataProcessingExceptionClass) != -1) {
                Assert.fail("Failed to get movie to db with exception" + e.getCause().getCause());
            }
            throw new RuntimeException("Failed to add movie " + testMovie, e);
        }
        Object movieFromDb = optionalMovieFromDb.get();
        Assert.assertNotNull("Failed to get movie from db through \"get(Long id)\" method. "
                + "Movie should not be null.", movieFromDb);

        List<Field> nullFields = Arrays.stream(movieClass.getDeclaredFields()).filter(
                f -> {
                    try {
                        f.setAccessible(true);
                        return f.get(movieFromDb) == null;
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("Failed to get field "
                                + f + " value for movie " + movieFromDb, e);
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
            mockSessionFactory(mockedSessionFactory);
            Object testMovie = getTestMovie();
            Mockito.when(mockedSession.save(testMovie)).thenThrow(new RuntimeException());
            doThrow(new RuntimeException()).when(mockedSession).persist(testMovie);
            Class dataProcessingExceptionClass = getClass("DataProcessingException");

            try {
                invokeAddMethod(testMovie);
            } catch (Exception e) {
                if (ExceptionUtils.indexOfThrowable(e, dataProcessingExceptionClass) != -1) {
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
                }
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
        mockSessionFactory(mockedSessionFactory);
        Class movieClass = getClass("Movie");
        Long movieId = 1L;
        Mockito.when(mockedSession.get(movieClass, movieId)).thenReturn(null);
        invokeGetMethod(movieId);
        Mockito.verify(mockedSession, description("You should close session in \"get(Long id)\" "
                + "method in dao layer after getting movie from db. You can use"
                + " try-with-resources for this purpose.")).close();
    }

    private Object getInstance(String className) {
        Class movieDaoImpl = getClass(className);
        try {
            return movieDaoImpl.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException
                | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException("Could not get instance of " + className + " class.", e);
        }
    }

    private Object getTestMovie() {
        Object testMovie;
        Class movieClass = getClass("Movie");
        try {
            testMovie = movieClass.getConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Could not get constructor of Movie class, "
                    + "you should create a default constructor in Movie entity.", e);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Could not create instance of Movie class.", e);
        }
        Arrays.stream(movieClass.getDeclaredFields())
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

    private void mockSessionFactory(SessionFactory sessionFactory) {
        Class hibernateUtilClass = getClass("HibernateUtil");
        Method getSessionFactoryMethod = Arrays.stream(hibernateUtilClass.getDeclaredMethods())
                .filter(m -> m.getReturnType().getSimpleName().equals("SessionFactory")
                        && Modifier.isPublic(m.getModifiers()))
                .findAny()
                .get();
        Optional<Field> optionalSessionFactory = Arrays
                .stream(hibernateUtilClass.getDeclaredFields())
                .filter(f -> f.getType().getSimpleName().equals("SessionFactory"))
                .findAny();
        if (optionalSessionFactory.isEmpty()) {
            Assert.fail("Your HibernateUtil class should have SessionFactory field.");
        }
        Field field = optionalSessionFactory.get();
        mockedStatic = mockStatic(hibernateUtilClass);
        try {
            field.setAccessible(true);
            Mockito.when(getSessionFactoryMethod.invoke(null)).thenReturn(sessionFactory);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to mock getSessionFactory method", e);
        }
    }

    private Object invokeAddMethod(Object movie) {
        Class movieDaoImplClass = getClass("MovieDaoImpl");
        Object movieDaoImplInstance = getInstance("MovieDaoImpl");
        movieDaoImplClass.cast(movieDaoImplInstance);
        Class movieClass = getClass("Movie");

        Method addMethod;
        try {
            addMethod = movieDaoImplClass.getMethod("add", movieClass);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Could not get \"add(Movie movie)\" method", e);
        }

        Object addedMovie;
        try {
            addedMovie = addMethod.invoke(movieDaoImplInstance, movie);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Could not invoke method add " + addMethod, e);
        }
        return addedMovie;
    }

    private Optional invokeGetMethod(Long id) {
        Class movieDaoImplClass = getClass("MovieDaoImpl");
        Object movieDaoImplInstance = getInstance("MovieDaoImpl");
        movieDaoImplClass.cast(movieDaoImplInstance);
        Method getMethod;
        try {
            getMethod = movieDaoImplClass.getMethod("get", Long.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Could not get \"get(Long id)\" method", e);
        }

        Optional optionalMovieFromDb;
        try {
            optionalMovieFromDb = (Optional) getMethod.invoke(movieDaoImplInstance, id);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Could not invoke \"get(Long id)\" method", e);
        }
        return optionalMovieFromDb;
    }

    private Class getClass(String name) {
        return allClasses.stream()
                .filter(c -> c.getSimpleName().equals(name))
                .findAny()
                .get();
    }

    private static List<Class> getClasses(String packageName)
            throws ClassNotFoundException, IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        assert classLoader != null;
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        List<File> dirs = new ArrayList<>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            dirs.add(new File(resource.getFile()));
        }
        ArrayList<Class> classes = new ArrayList<Class>();
        for (File directory : dirs) {
            classes.addAll(findClasses(directory, packageName));
        }
        return classes;
    }

    private static List<Class> findClasses(File directory, String packageName)
            throws ClassNotFoundException {
        List<Class> classes = new ArrayList<>();
        if (!directory.exists()) {
            return classes;
        }
        File[] files = directory.listFiles();
        for (File file : files) {
            try {
                if (file.isDirectory()) {
                    assert !file.getName().contains(".");
                    classes.addAll(findClasses(file, packageName + "." + file.getName()));
                } else if (file.getName().endsWith(".class")) {
                    classes.add(Class.forName(
                            packageName + '.' + file.getName()
                                    .substring(0, file.getName().length() - 6)));
                }
            } catch (NoClassDefFoundError e) {
                if (e.getMessage().contains("HibernateUtil")) {
                    Assert.fail("Could not establish connection with db. You should create "
                            + "\"hibernate.cfg.xml\" file in resources folder with all "
                            + "necessary configurations");
                } else {
                    throw new RuntimeException("Could not initialize class.", e);
                }
            }
        }
        return classes;
    }
}
