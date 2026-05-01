package org.example.database.Classes;

public class User {
    public String userId;
    public String username;
    public String roleName;
    public int devScore;
    public int qaScore;

    public User(String userId, String username, String roleName, int devScore, int qaScore) {
        this.userId = userId;
        this.username = username;
        this.roleName = roleName;
        this.devScore = devScore;
        this.qaScore = qaScore;
    }

    @Override
    public String toString() {
        return String.format("%s (%s) - Dev: %d | QA: %d", username, roleName, devScore, qaScore);
    }
}
