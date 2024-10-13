package models;

public class User {
    private final int id;
    private final String name; 
    private final String password;

    // Constructor para inicializar los atributos del usuario
    public User(int id, String name, String password) {
        this.id = id;
        this.name = name;
        this.password = password;
    }

    // Getters para acceder a los atributos
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
