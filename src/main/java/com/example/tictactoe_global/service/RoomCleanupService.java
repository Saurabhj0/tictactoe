package com.example.tictactoe_global.service;

import com.example.tictactoe_global.model.Room;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.Map;

@Service
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class RoomCleanupService {

    private final RoomManager roomManager;

    @Scheduled(fixedRate = 60000) // Every 1 minute
    public void cleanupRooms() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Room>> iterator = roomManager.getAllRooms().entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, Room> entry = iterator.next();
            Room room = entry.getValue();

            // Case B/C: Inactive or Finished for too long
            if (now - room.getLastActivity() > 1800000) { // 30 minutes
                log.info("Cleaning up inactive room: {}", room.getId());
                iterator.remove();
            } else if ("finished".equals(room.getStatus()) && now - room.getLastActivity() > 120000) { // 2 minutes
                log.info("Cleaning up finished room: {}", room.getId());
                iterator.remove();
            }
        }
    }
}
