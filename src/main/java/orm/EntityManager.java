package orm;

import anotations.Column;
import anotations.Entity;
import anotations.Id;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EntityManager<E> implements DbContext<E> {
    private final Connection connection;

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

        return doUpdate(entity,(long) idValue);
    }

    private boolean doInsert(E entity) throws IllegalAccessException, SQLException {
        String tableName = getTableName(entity.getClass());
        List<String> tableFields = getColumnsWithoutId(entity.getClass());
        List<String> tableValues = getColumnValuesWithoutId(entity);

        String insertQuery = String.format(
                "INSERT INTO %s (%s) VALUES(%s)", tableName,
                String.join(", ", tableFields),
                String.join(", ", tableValues));

        return connection.prepareStatement(insertQuery).execute();
    }

    private boolean doUpdate(E entity, long idValue) throws IllegalAccessException, SQLException {
        String tableName = getTableName(entity.getClass());
        List<String> tableFields = getColumnsWithoutId(entity.getClass());
        List<String> tableValues = getColumnValuesWithoutId(entity);

        List<String> setStatements = new ArrayList<>();
        for (int i = 0; i < tableFields.size(); i++) {
            String statement = tableFields.get(i) + " = " + tableValues.get(i);

            setStatements.add(statement);
        }

        String updateQuery = String.format(
                "UPDATE %s SET %s WHERE `id` = %d", tableName,
                String.join(", ", setStatements), idValue);

        System.out.println();

        return connection.prepareStatement(updateQuery).execute();
    }

    @Override
    public boolean delete(E toDelete) {
        return false;
    }

    @Override
    public Iterable<E> find(Class<E> table) throws SQLException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
       return find(table, null);
    }

    @Override
    public Iterable<E> find(Class<E> table, String where) throws SQLException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Statement statement = connection.createStatement();
        String tableName = getTableName(table);

        String query = String.format(
                "SELECT * FROM %s %s", tableName, where != null ? "WHERE " + where : "");

        ResultSet resultSet = statement.executeQuery(query);

        List<E> result = new ArrayList<>();
        while (resultSet.next()) {
            E entity = table.getDeclaredConstructor().newInstance();
            fillEntity(table, resultSet, entity);
            result.add(entity);
        }

        return result;
    }

    @Override
    public E findFirst(Class<E> table) throws SQLException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        return findFirst(table, null);
    }

    @Override
    public E findFirst(Class<E> table, String where) throws SQLException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Statement statement = connection.createStatement();
        String tableName = getTableName(table);

        String query = String.format(
                "SELECT * FROM %s %s LIMIT 1", tableName, where != null ? "WHERE " + where : "");

        ResultSet resultSet = statement.executeQuery(query);
        E entity = table.getDeclaredConstructor().newInstance();


        resultSet.next();
        fillEntity(table, resultSet, entity);

        return entity;
    }

    public void doCreate(Class<E> entityClass) throws SQLException {
        String tableName = getTableName(entityClass);
        String query = String.format(
                "CREATE TABLE %s (id INT PRIMARY KEY AUTO_INCREMENT, %s)",
                tableName, getAllFieldsAndDataTypes(entityClass)
        );

        PreparedStatement statement = connection.prepareStatement(query);

        statement.execute();
    }

    private String getAllFieldsAndDataTypes(Class<E> entityClass) {
        Field[] fields = Arrays.stream(entityClass.getDeclaredFields())
                .filter(f -> !f.isAnnotationPresent(Id.class))
                .filter(f -> f.isAnnotationPresent(Column.class))
                .toArray(Field[]::new);

        List<String> result = new ArrayList<>();
        for (Field field : fields) {
            String fieldName = field.getAnnotationsByType(Column.class)[0].value();
            Class<?> fieldType = field.getType();

            String type = "";
            if (fieldType == Integer.class || fieldType == int.class) {
                type = "INT";
            } else if (fieldType == String.class) {
                type = "VARCHAR(255)";
            } else if (fieldType == LocalDate.class) {
                type = "DATE";
            }

            result.add(fieldName + " " + type);
        }

        return String.join(", ", result);
    }

    private void fillEntity(Class<E> table, ResultSet resultSet, E entity) throws SQLException, IllegalAccessException {
        Field[] declaredFields = Arrays.stream(table.getDeclaredFields()).toArray(Field[]::new);

        for (Field field : declaredFields) {
            field.setAccessible(true);
            fillField(field, resultSet, entity);
        }
    }

    private void fillField(Field field, ResultSet resultSet, E entity) throws SQLException, IllegalAccessException {
        field.setAccessible(true);

        String fieldName = field.getAnnotationsByType(Column.class)[0].value();

        if (field.getType() == int.class || field.getType() == Integer.class) {
            field.set(entity, resultSet.getInt(fieldName));
        } else if (field.getType() == long.class || field.getType() == Long.class) {
            field.set(entity, resultSet.getLong(fieldName));
        } else if (field.getType() == LocalDate.class) {
            field.set(entity, LocalDate.parse(resultSet.getString(fieldName)));
        } else {
            field.set(entity, resultSet.getString(fieldName));
        }
    }

    private List<String> getColumnsWithoutId(Class<?> aClass) {
        return Arrays.stream(aClass.getDeclaredFields())
                .filter(f -> !f.isAnnotationPresent(Id.class))
                .filter(f -> f.isAnnotationPresent(Column.class))
                .map(f -> f.getAnnotationsByType(Column.class))
                .map(a -> a[0].value())
                .collect(Collectors.toList());
    }

    private List<String> getColumnValuesWithoutId(E entity) throws IllegalAccessException {
        Class<?> aClass = entity.getClass();
        List<Field> fields = Arrays.stream(aClass.getDeclaredFields())
                .filter(f -> !f.isAnnotationPresent(Id.class))
                .filter(f -> f.isAnnotationPresent(Column.class))
                .toList();

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

        return values;
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
}
