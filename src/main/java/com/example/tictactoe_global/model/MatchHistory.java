package com.example.tictactoe_global.model;

import com.example.tictactoe_global.enums.GameMode;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "match_history")
@Data
@NoArgsConstructor
public class MatchHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "player_x_name")
    private String playerXName;

    @Column(name = "player_o_name")
    private String playerOName;

    @Column(name = "winner_name")
    private String winnerName;

    @Enumerated(EnumType.STRING)
    private GameMode mode;

    @Column(name = "played_at")
    private LocalDateTime playedAt = LocalDateTime.now();
}
