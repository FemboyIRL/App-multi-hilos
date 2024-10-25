package models;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class User {

    private final int id;
    private final String name;
    private final String password;
    private final List<Note> notes;
    private OffsetDateTime lastConnection;
    private long totalConnectedTime;

    // Constructor para inicializar los atributos del usuario
    public User(int id, String name, String password, OffsetDateTime lastConnection, long totalConnectedTime) {
        this.id = id;
        this.name = name;
        this.password = password;
        this.notes = new ArrayList<>();
        this.lastConnection = lastConnection;
        this.totalConnectedTime = totalConnectedTime;
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

    public OffsetDateTime getLastConnection() {
        return lastConnection;
    }

    public long getTotalConnectedTime() {
        return totalConnectedTime;
    }

    public void addToTotalConnectedTime(long seconds) {
        this.totalConnectedTime += seconds;
    }
    
    public void setLastConnection(OffsetDateTime disconnectTime){
        this.lastConnection = disconnectTime;
    }

    @Override
    public String toString() {
        return "User{"
                + "id=" + id
                + ", name='" + name + '\''
                + ", password='" + password + '\''
                + '}';
    }
}
