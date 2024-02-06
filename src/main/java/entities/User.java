package entities;

import anotations.Column;
import anotations.Entity;
import anotations.Id;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Arrays;

@Entity("users")
public class User {
    @Id
    @Column("id")
    private long id;
    @Column("username")
    private String username;
    @Column("age")
    private int age;
    @Column("registration_data")
    private LocalDate registration;

    public User(String username, int age, LocalDate registration) {
        this.username = username;
        this.age = age;
        this.registration = registration;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public LocalDate getRegistration() {
        return registration;
    }

    public void setRegistration(LocalDate registration) {
        this.registration = registration;
    }

    private Field getIdColumn(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(x -> x.isAnnotationPresent(Id.class))
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("Entity does not have primary key"));
    }
}
