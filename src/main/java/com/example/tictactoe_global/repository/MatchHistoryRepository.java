package com.example.tictactoe_global.repository;

import com.example.tictactoe_global.model.MatchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MatchHistoryRepository extends JpaRepository<MatchHistory, UUID> {
}
