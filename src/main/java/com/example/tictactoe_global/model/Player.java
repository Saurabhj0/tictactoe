package com.example.tictactoe_global.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Player {
    private String sessionId; // Replacing socketId with WebSocket sessionId
    private String name;
    private String symbol; // "X" or "O"
    private int score = 0;
}
