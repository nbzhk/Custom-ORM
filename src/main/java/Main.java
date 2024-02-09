import entities.User;
import orm.EntityManager;
import orm.MyConnector;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;

import static orm.MyConnector.getConnection;

public class Main {

    public static void main(String[] args) throws SQLException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException {
        MyConnector.createConnection("root", "1234", "custom_orm");
        Connection connection = getConnection();

        EntityManager<User> userEntityManager = new EntityManager<>(connection);
        User userToAdd = new User("Pesho", 23, LocalDate.now());

        userEntityManager.doCreate(User.class);
        userEntityManager.persist(userToAdd);

        Iterable<User> user = userEntityManager.find(User.class, "id > 1");

        System.out.println(user);

    }
}
