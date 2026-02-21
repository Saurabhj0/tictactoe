package com.example.tictactoe_global.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "display_name", unique = true, nullable = false)
    private String displayName;

    private int wins = 0;
    private int losses = 0;
    private int draws = 0;

    public User(String displayName) {
        this.displayName = displayName;
    }
}
