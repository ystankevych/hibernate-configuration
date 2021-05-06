# Hibernate 

We are starting working with Hibernate on the new project. We will implement it step by step. 
Completed structure of project is described below:
![pic](Hibernate_Cinema_Uml.png)

Your task is to implement the following steps:
- add the `checkstyle maven plugin`. You can use the configuration from your previous projects.
- add required hibernate dependencies
- create `hibernate.cfg.xml` file
- complete implementation of `mate/academy/model/Movie.java` class
- complete implementation of `mate/academy/dao/impl/MovieDaoImpl.java` class
- create your custom unchecked DataProcessingException and throw it in the catch block at dao layer.
- complete implementation of `mate/academy/service/impl/MovieServiceImpl.java` class
- make `mate/academy/Main.java` working (means be able to run main method without any errors)
- use the annotation injector located in the `lib` folder
