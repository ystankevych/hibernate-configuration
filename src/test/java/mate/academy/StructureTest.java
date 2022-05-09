package mate.academy;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class StructureTest {
    public static final String ROOT_FOLDER = "src/main";
    private static final List<String> requiredClasses = List
            .of("HibernateUtil", "Movie", "MovieDaoImpl",
                    "MovieServiceImpl", "DataProcessingException");
    private static final List<String> requiredInterfaces = List
            .of("MovieDao", "MovieService");
    private static List<Class> allClasses = new ArrayList<>();

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

    @Test
    public void structure_hibernateConfigFileExists_OK() {
        Optional<File> optionalResourcesFolder = Arrays.stream(
                Objects.requireNonNull(new File(ROOT_FOLDER).listFiles()))
                .filter(f -> f.isDirectory() && f.getName().equals("resources"))
                .findAny();
        if (optionalResourcesFolder.isEmpty()) {
            Assert.fail("You should create src/main/resources folder");
        }
        File[] files = optionalResourcesFolder.get().listFiles();
        Optional<File> hibernateConfigFile = Arrays.stream(files)
                .filter(f -> f.isFile() && f.getName().equals("hibernate.cfg.xml")).findAny();
        if (hibernateConfigFile.isEmpty()) {
            Assert.fail("You should create \"hibernate.cfg.xml\" file in resources folder");
        }
    }

    @Test
    public void structure_requiredClassesExists_Ok() {
        requiredClasses.forEach(c -> checkExistence(c, "class"));
        requiredInterfaces.forEach(i -> checkExistence(i, "interface"));
    }

    @Test
    public void structure_requiredDaoMethodsExists_Ok() {
        checkMethod("MovieDao", "get", "Optional", "Long");
        checkMethod("MovieDao", "add", "Movie", "Movie");
    }

    @Test
    public void structure_requiredServiceMethodsExists_Ok() {
        checkMethod("MovieService", "get", "Movie", "Long");
        checkMethod("MovieService", "add", "Movie", "Movie");
    }

    @Test
    public void structure_hibernateUtilClassCheck_Ok() {
        Class hibernateUtil = allClasses.stream()
                .filter(c -> c.getSimpleName().equals("HibernateUtil"))
                .findAny().get();
        Optional<Constructor> optionalPrivateConstructor = Arrays
                .stream(hibernateUtil.getDeclaredConstructors())
                .filter(c -> c.getParameterTypes().length == 0
                        && Modifier.toString(c.getModifiers()).equals("private"))
                .findAny();
        if (optionalPrivateConstructor.isEmpty()) {
            Assert.fail("You should add a private default constructor to HibernateUtil class"
                    + "in order to prevent creating HibernateUtil objects.");
        }
        Optional<Field> optionalSessionFactory = Arrays.stream(hibernateUtil.getDeclaredFields())
                .filter(f -> f.getType().getName().equals("org.hibernate.SessionFactory"))
                .findAny();
        if (optionalSessionFactory.isEmpty()) {
            Assert.fail("You should have some SessionFactory in your HibernateUtil class");
        }
        Field sessionFactoryField = optionalSessionFactory.get();
        if (!Modifier.isStatic(sessionFactoryField.getModifiers())) {
            Assert.fail("SessionFactory field should be static");
        }
        Optional<Method> optionalGetSessionFactoryMethod = Arrays.stream(
                hibernateUtil.getDeclaredMethods())
                .filter(m -> m.getReturnType().getSimpleName().equals("SessionFactory")
                && Modifier.isPublic(m.getModifiers()))
                .findAny();
        if (optionalGetSessionFactoryMethod.isEmpty()) {
            Assert.fail("You should create public method, that return "
                    + "SessionFactory instance in HibernateUtil class");
        }
    }

    private void checkMethod(String testedClass, String testedMethod,
            String returnType, String parameter) {
        Class testedClazz = allClasses.stream().filter(c -> c.getSimpleName().equals(testedClass))
                .findAny().get();
        List<Method> allMethods = Arrays.stream(testedClazz.getDeclaredMethods())
                .filter(m -> m.getName().equals(testedMethod))
                .collect(Collectors.toList());
        if (allMethods.size() == 0) {
            Assert.fail(
                    "You should create method called \"" + testedMethod + "\" in " + testedClazz);
        }
        Optional<Method> method = allMethods.stream()
                .filter(m -> m.getParameterCount() == 1
                        && Arrays.stream(m.getParameterTypes())
                        .findAny()
                        .get().getSimpleName().equals(parameter))
                .findAny();
        if (method.isEmpty()) {
            Assert.fail("You should create \"" + method + "\" method in " + testedClazz);
        }
        if (!method.get().getReturnType().getSimpleName().equals(returnType)) {
            Assert.fail("Method \"" + method.get().getName() + "\" in " + testedClazz.getName()
                    + " should have \"" + returnType + "\" return type");
        }
    }

    private void checkExistence(String name, String type) {
        Optional<Class> optionalClass = allClasses.stream()
                .filter(c -> c.getSimpleName().equals(name))
                .findAny();
        if (optionalClass.isEmpty()) {
            Assert.fail("You should create " + type + " called " + name
                    + ". Create this " + type + " or check naming");
        }
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
