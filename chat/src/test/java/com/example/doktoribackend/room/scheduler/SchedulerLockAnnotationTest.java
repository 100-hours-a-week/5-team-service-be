package com.example.doktoribackend.room.scheduler;

import com.example.doktoribackend.room.service.WaitingRoomSseService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("chat 스케줄러 분산 락 어노테이션 검증")
class SchedulerLockAnnotationTest {

    @Test
    @DisplayName("비즈니스 스케줄러 메서드에 @SchedulerLock이 적용되어 있다")
    void businessScheduledMethods_haveSchedulerLock() {
        List<Method> scheduledMethods = Arrays.stream(ChatRoomScheduler.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Scheduled.class))
                .toList();

        assertThat(scheduledMethods).isNotEmpty();
        for (Method method : scheduledMethods) {
            assertThat(method.isAnnotationPresent(SchedulerLock.class))
                    .as("%s()에 @SchedulerLock이 없습니다", method.getName())
                    .isTrue();
        }
    }

    @Test
    @DisplayName("heartbeat는 각 서버가 독립 실행해야 하므로 @SchedulerLock을 적용하지 않는다")
    void heartbeat_doesNotHaveSchedulerLock() {
        Method heartbeat = Arrays.stream(WaitingRoomSseService.class.getDeclaredMethods())
                .filter(m -> m.getName().equals("sendHeartbeat"))
                .findFirst()
                .orElseThrow();

        assertThat(heartbeat.isAnnotationPresent(SchedulerLock.class)).isFalse();
    }

    @Test
    @DisplayName("endExpiredChatRooms의 lockAtMostFor는 fixedRate(60s)보다 짧다")
    void endExpiredChatRooms_lockAtMostForIsWithinFixedRate() {
        Method method = Arrays.stream(ChatRoomScheduler.class.getDeclaredMethods())
                .filter(m -> m.getName().equals("endExpiredChatRooms"))
                .findFirst()
                .orElseThrow();

        SchedulerLock lock = method.getAnnotation(SchedulerLock.class);
        assertThat(lock.lockAtMostFor()).isEqualTo("55s");
    }
}
