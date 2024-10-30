package models;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import methods.UserRepository;

public class User {

    private final int id;
    private final String name;
    private final String password;
    private final List<Note> notes;
    private OffsetDateTime lastConnection;
    private long totalConnectedTime;
    private final List<Integer> blockedUsers;

    // Constructor to initialize user attributes
    public User(int id, String name, String password, OffsetDateTime lastConnection, long totalConnectedTime) {
        this.id = id;
        this.name = name;
        this.password = password;
        this.notes = new ArrayList<>();
        this.lastConnection = lastConnection;
        this.totalConnectedTime = totalConnectedTime;
        this.blockedUsers = new ArrayList<>();
    }

    // Getters to access attributes
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

    public void setLastConnection(OffsetDateTime disconnectTime) {
        this.lastConnection = disconnectTime;
    }

    // Methods to manage blocked users
    public void blockUser(int userId) {
        if (!blockedUsers.contains(userId)) {
            blockedUsers.add(userId);
        }
    }

    public void unblockUser(int userId) {
        blockedUsers.remove(Integer.valueOf(userId));
    }

    public boolean isUserBlocked(int userId) {
        return blockedUsers.contains(userId);
    }

    public List<Integer> getBlockedUsers() {
        return blockedUsers;
    }
    
    public List<String> getBlockedUsersName() {
    List<String> blockedUserNames = new ArrayList<>();
    List<User> allUsers = UserRepository.getAllUsers();

    for (User user : allUsers) {
        if (blockedUsers.contains(user.getId())) {
            blockedUserNames.add(user.getName());
        }
    }
    return blockedUserNames;
}


    @Override
    public String toString() {
        return "User{"
                + "id=" + id
                + ", name='" + name + '\''
                + ", password='" + password + '\''
                + ", lastConnection=" + lastConnection
                + ", totalConnectedTime=" + totalConnectedTime
                + ", blockedUsers=" + blockedUsers
                + '}';
    }
}
