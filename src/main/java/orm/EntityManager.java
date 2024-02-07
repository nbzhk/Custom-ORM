package orm;

import anotations.Column;
import anotations.Entity;
import anotations.Id;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EntityManager<E> implements DbContext<E> {
    private Connection connection;
    public EntityManager(Connection connection) {
        this.connection = connection;
    }
    @Override
    public boolean persist(E entity) throws IllegalAccessException, SQLException {
        Field idColumn = getIdColumn(entity.getClass());
        idColumn.setAccessible(true);
        Object idValue = idColumn.get(entity);

        if (idValue == null || (long) idValue <= 0) {
            return doInsert(entity);
        }

        return doUpdate(entity, idColumn);
    }

    private boolean doUpdate(E entity, Field idColumn) throws IllegalAccessException, SQLException {
        String tableName = getTableName(entity.getClass());
        String tableFields = getColumnsWithoutId(entity.getClass());
        String newValues = getColumnValuesWithoutId(entity);

        String updateQuery = String.format(
                "UPDATE %s SET %s = %s WHERE `id` = %d", tableName, tableFields, newValues, idColumn.getInt(idColumn)
        );

        return connection.prepareStatement(updateQuery).execute();
    }

    private boolean doInsert(E entity) throws IllegalAccessException, SQLException {
        String tableName = getTableName(entity.getClass());
        String tableFields = getColumnsWithoutId(entity.getClass());
        String tableValues = getColumnValuesWithoutId(entity);

        String insertQuery = String.format(
                "INSERT INTO %s (%s) VALUES(%s)", tableName, tableFields, tableValues);

        return connection.prepareStatement(insertQuery).execute();
    }

    private String getColumnsWithoutId(Class<?> aClass) {
        return Arrays.stream(aClass.getDeclaredFields())
                .filter(f -> !f.isAnnotationPresent(Id.class))
                .filter(f -> f.isAnnotationPresent(Column.class))
                .map(f -> f.getAnnotationsByType(Column.class))
                .map(a -> a[0].value())
                .collect(Collectors.joining(","));
    }

    private String getColumnValuesWithoutId(E entity) throws IllegalAccessException {
        Class<?> aClass = entity.getClass();
        List<Field> fields = Arrays.stream(aClass.getDeclaredFields())
                .filter(f -> !f.isAnnotationPresent(Id.class))
                .filter(f -> f.isAnnotationPresent(Column.class))
                .collect(Collectors.toList());

        List<String> values = new ArrayList<>();
        for (Field field : fields) {
            field.setAccessible(true);
            Object obj = field.get(entity);

            if (obj instanceof String || obj instanceof LocalDate) {
                values.add("'" + obj + "'");
            } else {
                values.add(obj.toString());
            }
        }

        return String.join(",", values);
    }

    private String getTableName(Class<?> aClass) {
        Entity[] typeAnnotations = aClass.getAnnotationsByType(Entity.class);

        if (typeAnnotations.length == 0) {
            throw new UnsupportedOperationException("Class must be Entity");
        }

        return typeAnnotations[0].value();
    }

    private Field getIdColumn(Class<?> aClass) {
        return Arrays.stream(aClass.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Id.class))
                .findFirst()
                .orElseThrow(() ->
                       new UnsupportedOperationException("Entity missing Id"));

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
