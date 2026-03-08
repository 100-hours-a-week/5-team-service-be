package com.example.doktoribackend.room.scheduler;

import com.example.doktoribackend.room.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRoomScheduler {

    private final ChatRoomService chatRoomService;

    @Scheduled(fixedRate = 60_000)
    @SchedulerLock(name = "endExpiredChatRooms", lockAtMostFor = "55s")
    public void endExpiredChatRooms() {
        chatRoomService.endExpiredChatRooms();
    }
}
