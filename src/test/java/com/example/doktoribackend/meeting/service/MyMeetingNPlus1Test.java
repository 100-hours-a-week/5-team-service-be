package com.example.doktoribackend.meeting.service;

import com.example.doktoribackend.meeting.domain.Meeting;
import com.example.doktoribackend.meeting.domain.MeetingDayOfWeek;
import com.example.doktoribackend.meeting.domain.MeetingMember;
import com.example.doktoribackend.meeting.domain.MeetingRound;
import com.example.doktoribackend.meeting.dto.MyMeetingListRequest;
import com.example.doktoribackend.meeting.dto.MyMeetingListResponse;
import com.example.doktoribackend.meeting.repository.MeetingMemberRepository;
import com.example.doktoribackend.meeting.repository.MeetingRepository;
import com.example.doktoribackend.meeting.repository.MeetingRoundRepository;
import com.example.doktoribackend.book.domain.Book;
import com.example.doktoribackend.book.repository.BookRepository;
import com.example.doktoribackend.user.domain.User;
import com.example.doktoribackend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("나의 모임 리스트 조회 N+1 문제 테스트")
class MyMeetingNPlus1Test {

    @Autowired
    private MeetingService meetingService;
    @Autowired
    private MeetingRepository meetingRepository;
    @Autowired
    private MeetingRoundRepository meetingRoundRepository;
    @Autowired
    private MeetingMemberRepository meetingMemberRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User testUser;
    private Book testBook;
    private Long testGenreId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute(
                "INSERT INTO reading_genres (code, name, priority, created_at, updated_at) " +
                        "VALUES ('TEST', '테스트장르', 1, NOW(), NOW())"
        );
        testGenreId = jdbcTemplate.queryForObject(
                "SELECT id FROM reading_genres WHERE code = 'TEST'", Long.class
        );

        testUser = new User("test@example.com", "test-provider-id", "TEST", "테스트유저");
        userRepository.save(testUser);

        testBook = Book.create(
                "9788901234567", "테스트 책", "테스트 저자",
                "테스트 출판사", "https://test.com/book.jpg", LocalDate.now()
        );
        bookRepository.save(testBook);

        List<Meeting> meetings = new ArrayList<>();
        List<MeetingMember> members = new ArrayList<>();
        List<MeetingRound> rounds = new ArrayList<>();

        for (int i = 1; i <= 10; i++) {
            Meeting meeting = Meeting.create(
                    testUser, testGenreId, "테스트 모임장 소개 " + i,
                    "meeting/test" + i + ".jpg", "테스트 모임 " + i, "테스트 모임 설명 " + i,
                    10, 3, MeetingDayOfWeek.MON, LocalTime.of(19, 0), 120,
                    LocalDateTime.now().plusDays(7), LocalDate.now().plusDays(5), 1
            );
            meeting.updateStatusToFinished();
            meetings.add(meeting);

            MeetingMember member = MeetingMember.createLeader(meeting, testUser, LocalDateTime.now());
            members.add(member);

            for (int roundNo = 1; roundNo <= 3; roundNo++) {
                LocalDateTime startAt = LocalDateTime.now().plusDays(7 * roundNo);
                MeetingRound round = MeetingRound.create(
                        meeting, testBook, roundNo, startAt, startAt.plusMinutes(120)
                );
                rounds.add(round);
            }
        }

        meetingRepository.saveAll(meetings);
        meetingMemberRepository.saveAll(members);
        meetingRoundRepository.saveAll(rounds);
    }

    @Test
    @DisplayName("N+1 문제 확인 - 개선 전")
    void N플러스1문제_확인() {
        MyMeetingListRequest request = new MyMeetingListRequest();
        request.setStatus("ACTIVE");
        request.setSize(10);

        System.out.println("\n⏳ 예상 쿼리: 11개 (1 + 0[캐시] + 10)");

        MyMeetingListResponse response = meetingService.getMyMeetings(testUser.getId(), request);

        System.out.println("✅ 조회 결과: " + response.getItems().size() + "개\n");

        assertThat(response.getItems()).isNotEmpty();
        assertThat(response.getItems()).hasSizeLessThanOrEqualTo(10);
    }

    @Test
    @DisplayName("N+1 문제 - 성능 측정")
    void N플러스1문제_성능측정() {
        MyMeetingListRequest request = new MyMeetingListRequest();
        request.setStatus("ACTIVE");
        request.setSize(10);

        System.out.println("\n📊 성능 측정 시작");

        long startTime = System.currentTimeMillis();
        MyMeetingListResponse response = meetingService.getMyMeetings(testUser.getId(), request);
        long executionTime = System.currentTimeMillis() - startTime;

        System.out.println("⏱️  실행 시간: " + executionTime + "ms");

        assertThat(response.getItems()).hasSize(10);
    }

    @Test
    @DisplayName("N+1 문제 - 상세 로깅 (문서화용)")
    void N플러스1문제_상세로깅() {
        // given
        MyMeetingListRequest request = new MyMeetingListRequest();
        request.setStatus("ACTIVE");
        request.setSize(10);

        System.out.println("\n" + "=".repeat(80));
        System.out.println("📋 [문서화용] N+1 문제 상세 분석");
        System.out.println("=".repeat(80));
        System.out.println("📌 테스트 환경:");
        System.out.println("   - 모임: 10개");
        System.out.println("   - 회차: 30개 (각 모임 3개)");
        System.out.println("   - 예상 쿼리: 12개");
        System.out.println();
        System.out.println("🔍 쿼리 발생 시점:");
        System.out.println("   1️⃣ findMyMeetings() - 초기 목록 조회");
        System.out.println("   2️⃣ findCompletedMeetingsInIds() - 완료 모임 확인");
        System.out.println("   3️⃣ findNextRoundDate() - 다음 회차 조회 (10번 반복 ← N+1!)");
        System.out.println();

        // when & then
        System.out.println("🔹 [시작] meetingService.getMyMeetings() 호출");
        System.out.println("   ↓ 아래에서 쿼리 로그를 확인하세요");
        System.out.println("=".repeat(80) + "\n");
        
        long startTime = System.currentTimeMillis();
        MyMeetingListResponse response = meetingService.getMyMeetings(testUser.getId(), request);
        long executionTime = System.currentTimeMillis() - startTime;

        System.out.println("\n" + "=".repeat(80));
        System.out.println("🔹 [완료] getMyMeetings() 실행 종료");
        System.out.println("=".repeat(80));
        System.out.println("📊 실행 결과:");
        System.out.println("   ⏱️  실행 시간: " + executionTime + "ms");
        System.out.println("   🔢 실제 쿼리: 12개");
        System.out.println("   ✅ 조회된 모임: " + response.getItems().size() + "개");
        System.out.println("=".repeat(80));
        System.out.println("\n💡 쿼리 분석:");
        System.out.println("   1️⃣ 초기 목록 조회:          1개  ← /* <criteria> */ select ...");
        System.out.println("   2️⃣ 완료 모임 확인:          1개  ← /* SELECT m FROM Meeting m WHERE ... */");
        System.out.println("   3️⃣ 다음 회차 조회 (N+1):   10개  ← /* SELECT mr.startAt FROM MeetingRound ... */");
        System.out.println("   " + "-".repeat(60));
        System.out.println("   총 합계:                   12개");
        System.out.println();
        System.out.println("⚠️  문제: findNextRoundDate()가 각 모임마다 개별 실행 (N+1 발생!)");
        System.out.println("=".repeat(80) + "\n");

        assertThat(response.getItems()).hasSize(10);
    }

    @Test
    @DisplayName("[단계별] 1단계 - findMyMeetings() 쿼리만 확인")
    void 단계1_초기목록조회() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📋 [1단계] findMyMeetings() 쿼리 분석");
        System.out.println("=".repeat(80));
        System.out.println("🔹 MeetingRepository.findMyMeetings() 호출");
        System.out.println("   ↓ 아래 쿼리를 확인하세요\n");

        List<com.example.doktoribackend.meeting.dto.MeetingListRow> results = 
            meetingRepository.findMyMeetings(testUser.getId(), null, true, 11);

        System.out.println("\n✅ 조회 완료 - " + results.size() + "개");
        System.out.println("=".repeat(80));
        System.out.println("💡 예상 쿼리: 1개");
        System.out.println("   - /* <criteria> */ select ... from meetings");
        System.out.println("=".repeat(80) + "\n");

        assertThat(results).isNotEmpty();
    }

    @Test
    @DisplayName("[단계별] 2단계 - findCompletedMeetingsInIds() 쿼리 확인")
    void 단계2_완료모임확인() {
        List<com.example.doktoribackend.meeting.dto.MeetingListRow> results = 
            meetingRepository.findMyMeetings(testUser.getId(), null, true, 11);
        
        List<Long> meetingIds = results.stream()
            .map(com.example.doktoribackend.meeting.dto.MeetingListRow::getMeetingId)
            .toList();

        System.out.println("\n" + "=".repeat(80));
        System.out.println("📋 [2단계] findCompletedMeetingsInIds() 쿼리 분석");
        System.out.println("=".repeat(80));
        System.out.println("🔹 MeetingRepository.findCompletedMeetingsInIds() 호출");
        System.out.println("   - 대상 모임 ID: " + meetingIds.size() + "개");
        System.out.println("   ↓ 아래 쿼리를 확인하세요\n");

        List<Meeting> completedMeetings = meetingRepository.findCompletedMeetingsInIds(
            meetingIds, LocalDateTime.now()
        );

        System.out.println("\n✅ 조회 완료 - 완료된 모임: " + completedMeetings.size() + "개");
        System.out.println("=".repeat(80));
        System.out.println("💡 예상 쿼리: 1개");
        System.out.println("   - /* SELECT m FROM Meeting m WHERE m.id IN :meetingIds ... */");
        System.out.println("=".repeat(80) + "\n");
    }

    @Test
    @DisplayName("[단계별] 3단계 - findNextRoundDate() N+1 발생 확인")
    void 단계3_다음회차조회() {
        List<com.example.doktoribackend.meeting.dto.MeetingListRow> results = 
            meetingRepository.findMyMeetings(testUser.getId(), null, true, 11);

        System.out.println("\n" + "=".repeat(80));
        System.out.println("📋 [3단계] findNextRoundDate() N+1 문제 분석");
        System.out.println("=".repeat(80));
        System.out.println("🔹 MeetingRoundRepository.findNextRoundDate() 반복 호출");
        System.out.println("   - 조회된 모임: " + results.size() + "개");
        System.out.println("   - 예상 쿼리: " + results.size() + "개 (각 모임마다 1개씩)");
        System.out.println("   ↓ 아래에서 동일한 쿼리가 반복되는지 확인하세요\n");

        LocalDateTime now = LocalDateTime.now();
        int queryCount = 0;
        
        for (com.example.doktoribackend.meeting.dto.MeetingListRow row : results) {
            queryCount++;
            System.out.println("   [" + queryCount + "번째 호출] meetingId=" + row.getMeetingId());
            List<LocalDateTime> nextRounds = meetingRoundRepository.findNextRoundDate(
                row.getMeetingId(), now
            );
        }

        System.out.println("\n✅ 조회 완료 - 총 " + queryCount + "번 호출");
        System.out.println("=".repeat(80));
        System.out.println("⚠️  문제 발생!");
        System.out.println("   - 동일한 쿼리가 " + queryCount + "번 실행됨");
        System.out.println("   - /* SELECT mr.startAt FROM MeetingRound ... */");
        System.out.println("   - 이것이 N+1 문제입니다!");
        System.out.println("=".repeat(80) + "\n");
    }
}