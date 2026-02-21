package com.example.tictactoe_global.controller;

import com.example.tictactoe_global.enums.GameMode;
import com.example.tictactoe_global.model.*;
import com.example.tictactoe_global.repository.MatchHistoryRepository;
import com.example.tictactoe_global.repository.UserRepository;
import com.example.tictactoe_global.service.GameService;
import com.example.tictactoe_global.service.RoomManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.util.Arrays;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@Slf4j
public class GameController {

    private final SimpMessagingTemplate messagingTemplate;
    private final RoomManager roomManager;
    private final GameService gameService;
    private final UserRepository userRepository;
    private final MatchHistoryRepository matchHistoryRepository;

    @MessageMapping("/game.create")
    @SendToUser("/topic/room")
    public GameMessage createRoom(@Payload GameMessage message, SimpMessageHeaderAccessor headerAccessor) {
        log.info("Received game.create from {}: {}", message.getSender(), message.getContent());
        String playerName = message.getSender();
        GameMode mode = GameMode.valueOf((String) message.getContent());
        boolean isPrivate = mode != GameMode.RANDOM;

        Room room = roomManager.createRoom(isPrivate, mode);
        Player player = new Player(headerAccessor.getSessionId(), playerName, "X", 0);
        room.getPlayers().add(player);

        if (mode == GameMode.AI) {
            Player aiPlayer = new Player("AI", "AI", "O", 0);
            room.getPlayers().add(aiPlayer);
            room.setStatus("playing");
        }

        log.info("Room created: {} for player: {} (Mode: {})", room.getId(), playerName, mode);
        return new GameMessage("ROOM_CREATED", room.getId(), "SYSTEM", room);
    }

    @MessageMapping("/game.quickmatch")
    @SendToUser("/topic/room")
    public GameMessage quickMatch(@Payload GameMessage message, SimpMessageHeaderAccessor headerAccessor) {
        log.info("Received game.quickmatch from {}", message.getSender());
        String playerName = message.getSender();
        Optional<Room> roomOpt = roomManager.findPublicWaitingRoom();

        if (roomOpt.isPresent()) {
            Room room = roomOpt.get();
            Player player = new Player(headerAccessor.getSessionId(), playerName, "O", 0);
            room.getPlayers().add(player);
            room.setStatus("playing");
            room.updateActivity();

            log.info("Matched {} to room {}", playerName, room.getId());
            messagingTemplate.convertAndSend("/topic/room/" + room.getId(),
                    new GameMessage("START", room.getId(), "SYSTEM", room));
            return new GameMessage("START", room.getId(), "SYSTEM", room);
        } else {
            Room room = roomManager.createRoom(false, GameMode.RANDOM);
            Player player = new Player(headerAccessor.getSessionId(), playerName, "X", 0);
            room.getPlayers().add(player);

            log.info("Created public room {} for {}", room.getId(), playerName);
            return new GameMessage("ROOM_CREATED", room.getId(), "SYSTEM", room);
        }
    }

    @MessageMapping("/game.rematch")
    public void handleRematch(@Payload GameMessage message, SimpMessageHeaderAccessor headerAccessor) {
        String roomId = message.getRoomId();
        roomManager.getRoom(roomId).ifPresent(room -> {
            room.getRematchRequests().add(headerAccessor.getSessionId());

            if (room.getMode() == GameMode.AI || room.getRematchRequests().size() >= 2) {
                Arrays.fill(room.getBoard(), null);
                room.setStatus("playing");
                room.setTurnIndex(0);
                room.updateActivity();
                room.setWinner(null);
                room.getRematchRequests().clear();

                messagingTemplate.convertAndSend("/topic/room/" + roomId,
                        new GameMessage("START", roomId, "SYSTEM", room));
            } else {
                messagingTemplate.convertAndSend("/topic/room/" + roomId,
                        new GameMessage("REMATCH_REQUESTED", roomId, message.getSender(), null));
            }
        });
    }

    @MessageMapping("/game.join")
    public void joinRoom(@Payload GameMessage message, SimpMessageHeaderAccessor headerAccessor) {
        String roomId = message.getRoomId();
        String playerName = message.getSender();
        Optional<Room> roomOpt = roomManager.getRoom(roomId);

        if (roomOpt.isPresent()) {
            Room room = roomOpt.get();
            if (room.getPlayers().size() < 2) {
                Player player = new Player(headerAccessor.getSessionId(), playerName, "O", 0);
                room.getPlayers().add(player);
                room.setStatus("playing");
                room.updateActivity();

                messagingTemplate.convertAndSend("/topic/room/" + roomId,
                        new GameMessage("START", roomId, "SYSTEM", room));
            } else {
                sendError(headerAccessor.getSessionId(), "Room is full");
            }
        } else {
            sendError(headerAccessor.getSessionId(), "Room not found");
        }
    }

    @MessageMapping("/game.move")
    public void handleMove(@Payload GameMessage message, SimpMessageHeaderAccessor headerAccessor) {
        String roomId = message.getRoomId();
        log.info("Received move from {}: {} in room {}", message.getSender(), message.getContent(), roomId);
        int boardIndex = (int) message.getContent();
        Optional<Room> roomOpt = roomManager.getRoom(roomId);

        if (roomOpt.isPresent()) {
            Room room = roomOpt.get();
            Player currentPlayer = room.getPlayers().get(room.getTurnIndex());

            if (currentPlayer.getSessionId().equals(headerAccessor.getSessionId())) {
                if (room.getBoard()[boardIndex] == null) {
                    room.getBoard()[boardIndex] = currentPlayer.getSymbol();
                    room.updateActivity();

                    checkGameState(room);

                    if ("playing".equals(room.getStatus()) && room.getMode() == GameMode.AI) {
                        handleAiMove(room);
                    }

                    messagingTemplate.convertAndSend("/topic/room/" + roomId,
                            new GameMessage("UPDATE", roomId, "SYSTEM", room));
                }
            }
        }
    }

    @MessageMapping("/game.reaction")
    public void handleReaction(@Payload GameMessage message) {
        messagingTemplate.convertAndSend("/topic/room/" + message.getRoomId(), message);
    }

    private void handleAiMove(Room room) {
        String aiSymbol = room.getPlayers().get(1).getSymbol();
        String playerSymbol = room.getPlayers().get(0).getSymbol();
        // Default to impossible for now
        int aiMove = gameService.getBestMove(room.getBoard(), aiSymbol, playerSymbol, "Impossible");
        if (aiMove != -1) {
            room.getBoard()[aiMove] = aiSymbol;
            checkGameState(room);
        }
    }

    private void checkGameState(Room room) {
        Player currentPlayer = room.getPlayers().get(room.getTurnIndex());
        if (gameService.checkWin(room.getBoard(), currentPlayer.getSymbol())) {
            room.setStatus("finished");
            room.setWinner(currentPlayer.getName());
            saveMatchResult(room);
        } else if (gameService.checkDraw(room.getBoard())) {
            room.setStatus("finished");
            room.setWinner("draw");
            saveMatchResult(room);
        } else {
            room.setTurnIndex(1 - room.getTurnIndex());
        }
    }

    private void saveMatchResult(Room room) {
        MatchHistory history = new MatchHistory();
        history.setPlayerXName(room.getPlayers().get(0).getName());
        history.setPlayerOName(room.getPlayers().size() > 1 ? room.getPlayers().get(1).getName() : "AI");
        history.setWinnerName(room.getWinner());
        history.setMode(room.getMode());
        matchHistoryRepository.save(history);

        // Update user stats
        room.getPlayers().forEach(p -> {
            userRepository.findByDisplayName(p.getName()).ifPresent(user -> {
                if ("draw".equals(room.getWinner())) {
                    user.setDraws(user.getDraws() + 1);
                } else if (p.getName().equals(room.getWinner())) {
                    user.setWins(user.getWins() + 1);
                } else {
                    user.setLosses(user.getLosses() + 1);
                }
                userRepository.save(user);
            });
        });
    }

    private void sendError(String sessionId, String errorMessage) {
        messagingTemplate.convertAndSendToUser(sessionId, "/topic/errors",
                new GameMessage("ERROR", null, "SYSTEM", errorMessage));
    }
}
