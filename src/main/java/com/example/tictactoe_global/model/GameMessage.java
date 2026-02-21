package com.example.tictactoe_global.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameMessage {
    private String type; // JOIN, MOVE, REACTION, REMATCH, ERROR, ROOM_CREATED, START, UPDATE
    private String roomId;
    private String sender;
    private Object content;
}
