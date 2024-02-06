package orm;

import java.sql.Connection;

public class EntityManager<E> implements DbContext<E> {
    private Connection connection;
    public EntityManager(Connection connection) {
        this.connection = connection;
    }
    @Override
    public boolean persist(E entity) {
        return false;
    }

    @Override
    public Iterable<E> find(Class<E> table) {
        return null;
    }

    @Override
    public Iterable<E> find(Class<E> table, String where) {
        return null;
    }

    @Override
    public E findFirst(Class<E> table) {
        return null;
    }

    @Override
    public E findFirst(Class<E> table, String where) {
        return null;
    }
}
