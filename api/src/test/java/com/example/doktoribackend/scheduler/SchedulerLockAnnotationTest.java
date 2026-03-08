package com.example.doktoribackend.scheduler;

import com.example.doktoribackend.bookReport.service.BookReportSchedulerService;
import com.example.doktoribackend.notification.service.NotificationSchedulerService;
import com.example.doktoribackend.zoom.service.ZoomLinkSchedulerService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("스케줄러 분산 락 어노테이션 검증")
class SchedulerLockAnnotationTest {

    @Test
    @DisplayName("모든 @Scheduled 메서드에 @SchedulerLock이 적용되어 있다")
    void allScheduledMethods_haveSchedulerLock() {
        List<Class<?>> schedulerClasses = List.of(
                NotificationSchedulerService.class,
                ZoomLinkSchedulerService.class,
                BookReportSchedulerService.class,
                MeetingScheduler.class
        );

        for (Class<?> clazz : schedulerClasses) {
            List<Method> scheduledMethods = Arrays.stream(clazz.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(Scheduled.class))
                    .toList();

            assertThat(scheduledMethods)
                    .as("%s의 @Scheduled 메서드가 존재해야 합니다", clazz.getSimpleName())
                    .isNotEmpty();

            for (Method method : scheduledMethods) {
                assertThat(method.isAnnotationPresent(SchedulerLock.class))
                        .as("%s.%s()에 @SchedulerLock이 없습니다", clazz.getSimpleName(), method.getName())
                        .isTrue();
            }
        }
    }

    @Test
    @DisplayName("@SchedulerLock name은 메서드별로 고유하다")
    void schedulerLock_namesAreUnique() {
        List<Class<?>> schedulerClasses = List.of(
                NotificationSchedulerService.class,
                ZoomLinkSchedulerService.class,
                BookReportSchedulerService.class,
                MeetingScheduler.class
        );

        List<String> lockNames = schedulerClasses.stream()
                .flatMap(clazz -> Arrays.stream(clazz.getDeclaredMethods()))
                .filter(m -> m.isAnnotationPresent(SchedulerLock.class))
                .map(m -> m.getAnnotation(SchedulerLock.class).name())
                .toList();

        assertThat(lockNames).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("lockAtMostFor는 각 스케줄 주기보다 짧게 설정되어 있다")
    void schedulerLock_lockAtMostForIsWithinSchedulePeriod() {
        assertLockAtMostFor(NotificationSchedulerService.class, "sendReviewDeadline24hNotifications", "55m");
        assertLockAtMostFor(NotificationSchedulerService.class, "sendReviewDeadline30mNotifications", "25m");
        assertLockAtMostFor(ZoomLinkSchedulerService.class, "createZoomLinksForUpcomingMeetings", "55s");
        assertLockAtMostFor(BookReportSchedulerService.class, "failStalePendingReports", "4m");
        assertLockAtMostFor(MeetingScheduler.class, "updateExpiredRecruitmentStatus", "23h");
        assertLockAtMostFor(MeetingScheduler.class, "completeExpiredRounds", "55m");
    }

    private void assertLockAtMostFor(Class<?> clazz, String methodName, String expected) {
        Method method = Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> m.getName().equals(methodName))
                .findFirst()
                .orElseThrow(() -> new AssertionError(methodName + " 메서드를 찾을 수 없습니다"));

        SchedulerLock lock = method.getAnnotation(SchedulerLock.class);
        assertThat(lock).as("%s.%s()에 @SchedulerLock이 없습니다", clazz.getSimpleName(), methodName).isNotNull();
        assertThat(lock.lockAtMostFor())
                .as("%s.%s()의 lockAtMostFor가 올바르지 않습니다", clazz.getSimpleName(), methodName)
                .isEqualTo(expected);
    }
}
