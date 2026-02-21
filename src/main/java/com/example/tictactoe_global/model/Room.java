package com.example.tictactoe_global.model;

import com.example.tictactoe_global.enums.GameMode;
import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class Room {
    private String id;
    private String status = "waiting"; // "waiting", "playing", "finished"
    private List<Player> players = new ArrayList<>();
    private String[] board = new String[9];
    private int turnIndex = 0;
    private long lastActivity = System.currentTimeMillis();
    private boolean isPrivate = true;
    private GameMode mode;
    private String winner; // Name of the winner or "draw"
    private Set<String> rematchRequests = new HashSet<>();

    public Room(String id, boolean isPrivate, GameMode mode) {
        this.id = id;
        this.isPrivate = isPrivate;
        this.mode = mode;
        Arrays.fill(board, null);
    }

    public void updateActivity() {
        this.lastActivity = System.currentTimeMillis();
    }
}
