package mate.academy.util;

import mate.academy.model.Movie;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {
    private static SessionFactory instance = initSessionFactory();

    private static SessionFactory initSessionFactory() {
        return new Configuration()
                .addAnnotatedClass(Movie.class)
                .buildSessionFactory();
    }

    public static SessionFactory getInstance() {
        return instance;
    }
}
