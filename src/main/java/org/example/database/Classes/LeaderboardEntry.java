package org.example.database.Classes;

public class LeaderboardEntry {
    private final long userId;
    private final int score;

    public LeaderboardEntry(long userId, int score) {
        this.userId = userId;
        this.score = score;
    }

    public long getUserId() {
        return userId;
    }

    public int getScore() {
        return score;
    }
}
