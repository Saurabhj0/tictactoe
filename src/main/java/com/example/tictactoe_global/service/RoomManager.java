package com.example.tictactoe_global.service;

import com.example.tictactoe_global.enums.GameMode;
import com.example.tictactoe_global.model.Room;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoomManager {
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public Room createRoom(boolean isPrivate, GameMode mode) {
        String id = generateRoomId();
        Room room = new Room(id, isPrivate, mode);
        rooms.put(id, room);
        return room;
    }

    public Optional<Room> getRoom(String id) {
        return Optional.ofNullable(rooms.get(id));
    }

    public void removeRoom(String id) {
        rooms.remove(id);
    }

    public Map<String, Room> getAllRooms() {
        return rooms;
    }

    public Optional<Room> findPublicWaitingRoom() {
        return rooms.values().stream()
                .filter(r -> !r.isPrivate() && "waiting".equals(r.getStatus()) && r.getPlayers().size() < 2)
                .findFirst();
    }

    private String generateRoomId() {
        String id;
        do {
            id = String.format("%06d", random.nextInt(1000000));
        } while (rooms.containsKey(id));
        return id;
    }
}
