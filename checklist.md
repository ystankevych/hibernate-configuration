# Common mistakes

* You should have only `Movie` model with dao and service layer. Don't create other models and don't push them to PR. 
* You don't need to set id for your entity explicitly. It'll be done by Hibernate.
* Don't add dependencies that you don't use to `pom.xml`.
* Don't add alfa version of dependencies, they might work unpredictable and cause problems.
* Use try with finally, where you use transaction and try with resources with Read operations.
* Add a private default constructor to `HibernateUtil` class in order to prevent creating `HibernateUtil` objects.
* Don't create a default constructor when it is not needed.
* Remember to add `catch` blocks for operations of all types on DAO layer.
* Do not push redundant files or folders (iml, .idea, target, etc).
* If you have problems connecting to MySql because of timezone issues, check out this [article](https://stackoverflow.com/questions/930900/how-do-i-set-the-time-zone-of-mysql).
* Be attentive with checkstyle plugin. You have to add it in the right place:

```xml
<project>
  <!-- some code -->
  <build>
    <plugins>
      <plugin>
        <!-- here should be checkstyle plugin -->
      </plugin>
    </plugins>
    <pluginManagement>
      <!-- some code -->
    </pluginManagement>
  </build>
</project>
```
