import entities.User;
import orm.EntityManager;
import orm.MyConnector;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;

import static orm.MyConnector.getConnection;

public class Main {

    public static void main(String[] args) throws SQLException, IllegalAccessException {
        MyConnector.createConnection("root", "1234", "custom_orm");
        Connection connection = getConnection();

        EntityManager<User> userEntityManager = new EntityManager<>(connection);

        User user = new User("Pesho", 20, LocalDate.now());
        user.setId(3);
        user.setAge(99);
        userEntityManager.persist(user);

    }
}
