# Hibernate 

We are starting working with Hibernate on the new project. We will implement it step by step. 
Completed structure of project is described below:
![pic](Hibernate_Cinema_Uml.png)

Your task is to implement the following steps:
- add the `checkstyle maven plugin`. You can use the configuration from your previous projects.
- add required hibernate dependencies
- create `hibernate.cfg.xml` file
- create `HibernateUtil` class
- create `Movie` model class
- create `MovieDao` interface and `MovieDaoImpl` class
    ```java
      public interface MovieDao {
          Movie add(Movie movie);
      
          Optional<Movie> get(Long id);
      }
    ```
- create `MovieService` interface and `MovieServiceImpl` class
    ```java
      public interface MovieService {
          Movie add(Movie movie);
      
          Movie get(Long id);
      }
    ```
- create your custom unchecked DataProcessingException and throw it in the catch block on dao layer
- in the `mate/academy/Main.main()` method create instance of MovieService using injector and test all methods from MovieService
- use annotations and the annotation injector located in the `lib` folder

__!!! Important:__ you should have only `Movie` model with dao and service layer. Don't create other models and don't push them to PR.

#### [Try to avoid these common mistakes, while solving task](https://mate-academy.github.io/jv-program-common-mistakes/hibernate/configuration/configuration_checklist)
