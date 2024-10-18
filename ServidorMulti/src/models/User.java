package models;

import java.util.ArrayList;
import java.util.List;

public class User {
    private final int id;
    private final String name; 
    private final String password;
    private final List<Note> notes; 

    // Constructor para inicializar los atributos del usuario
    public User(int id, String name, String password) {
        this.id = id;
        this.name = name;
        this.password = password;
        this.notes = new ArrayList<>();
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
    
    public void addNote(Note note) {
        notes.add(note);
    }

    public List<Note> getNotes() {
        return notes;
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
