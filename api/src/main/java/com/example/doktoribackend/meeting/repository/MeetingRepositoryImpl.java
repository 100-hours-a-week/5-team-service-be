package com.example.doktoribackend.meeting.repository;

import com.example.doktoribackend.meeting.domain.Meeting;
import com.example.doktoribackend.meeting.domain.MeetingMember;
import com.example.doktoribackend.meeting.domain.MeetingMemberStatus;
import com.example.doktoribackend.meeting.domain.MeetingRound;
import com.example.doktoribackend.meeting.domain.MeetingStatus;
import com.example.doktoribackend.meeting.dto.MeetingListRow;
import com.example.doktoribackend.meeting.dto.MeetingListRequest;
import com.example.doktoribackend.meeting.dto.MeetingSearchRequest;
import com.example.doktoribackend.book.domain.Book;
import com.example.doktoribackend.reading.domain.ReadingGenre;
import com.example.doktoribackend.user.domain.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static com.example.doktoribackend.meeting.domain.MeetingMemberStatus.APPROVED;

@Repository
public class MeetingRepositoryImpl implements MeetingRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<MeetingListRow> findMeetingList(MeetingListRequest request, int limit) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<MeetingListRow> query = cb.createQuery(MeetingListRow.class);
        Root<Meeting> meeting = query.from(Meeting.class);
        Join<Meeting, User> leader = meeting.join("leaderUser", JoinType.INNER);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.isNull(meeting.get("deletedAt")));
        predicates.add(cb.equal(meeting.get("status"), MeetingStatus.RECRUITING));
        predicates.add(cb.greaterThanOrEqualTo(meeting.get("recruitmentDeadline"), LocalDate.now()));

        if (request.getCursorId() != null) {
            predicates.add(cb.lt(meeting.get("id"), request.getCursorId()));
        }
        if (request.getReadingGenres() != null && !request.getReadingGenres().isEmpty()) {
            Join<Meeting, ReadingGenre> genre = meeting.join("readingGenre", JoinType.INNER);
            predicates.add(genre.get("code").in(request.getReadingGenres()));
        }
        if (request.getDayOfWeek() != null && !request.getDayOfWeek().isEmpty()) {
            predicates.add(meeting.get("dayOfWeek").in(request.getDayOfWeek()));
        }
        if (request.getRoundCount() != null) {
            int roundCount = request.getRoundCountValue();
            if (roundCount == 1) {
                predicates.add(cb.equal(meeting.get("roundCount"), 1));
            } else if (roundCount == 3) {
                predicates.add(cb.between(meeting.get("roundCount"), 3, 4));
            } else if (roundCount == 5) {
                predicates.add(cb.between(meeting.get("roundCount"), 5, 8));
            }
        }

        Predicate startTimePredicate = buildStartTimePredicate(cb, meeting, request.getStartTimeValues());
        if (startTimePredicate != null) {
            predicates.add(startTimePredicate);
        }

        // Join + projection to avoid N+1 when mapping leader nickname.
        query.select(cb.construct(MeetingListRow.class,
                        meeting.get("id"),
                        meeting.get("meetingImagePath"),
                        meeting.get("title"),
                        meeting.get("readingGenreId"),
                        leader.get("nickname"),
                        meeting.get("capacity"),
                        meeting.get("currentCount"),
                        meeting.get("recruitmentDeadline")
                ))
                .where(predicates.toArray(new Predicate[0]))
                .orderBy(cb.desc(meeting.get("id")));

        TypedQuery<MeetingListRow> typedQuery = entityManager.createQuery(query);
        typedQuery.setMaxResults(limit);
        return typedQuery.getResultList();
    }

    private Predicate buildStartTimePredicate(
            CriteriaBuilder cb,
            Root<Meeting> meeting,
            List<LocalTime> startTimeFrom
    ) {
        if (startTimeFrom == null || startTimeFrom.isEmpty()) {
            return null;
        }

        List<Predicate> slots = new ArrayList<>();
        for (LocalTime time : startTimeFrom) {
            if (time == null) {
                continue;
            }
            LocalTime slotStart = time;
            LocalTime slotEnd = toSlotEnd(time);
            Predicate slotPredicate = slotEnd == null
                    ? cb.greaterThanOrEqualTo(meeting.get("startTime"), slotStart)
                    : cb.and(
                            cb.greaterThanOrEqualTo(meeting.get("startTime"), slotStart),
                            cb.lessThan(meeting.get("startTime"), slotEnd)
                    );
            slots.add(slotPredicate);
        }

        if (slots.isEmpty()) {
            return null;
        }
        return cb.or(slots.toArray(new Predicate[0]));
    }

    private LocalTime toSlotEnd(LocalTime start) {
        if (LocalTime.of(9, 0).equals(start)) {
            return LocalTime.of(14, 0);
        }
        if (LocalTime.of(14, 0).equals(start)) {
            return LocalTime.of(19, 0);
        }
        if (LocalTime.of(19, 0).equals(start)) {
            return null;
        }
        return null;
    }

    @Override
    public List<MeetingListRow> searchMeetings(MeetingSearchRequest request, int limit) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<MeetingListRow> query = cb.createQuery(MeetingListRow.class);
        Root<Meeting> meeting = query.from(Meeting.class);
        Join<Meeting, User> leader = meeting.join("leaderUser", JoinType.INNER);

        // 기본 조건
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.isNull(meeting.get("deletedAt")));
        predicates.add(cb.greaterThanOrEqualTo(meeting.get("recruitmentDeadline"), LocalDate.now()));

        // 검색 조건: 책 제목 OR 모임 제목
        Predicate searchCondition = buildSearchCondition(cb, query, meeting, request.getKeywordTrimmed());
        if (searchCondition != null) {
            predicates.add(searchCondition);
        }

        // 기존 필터 조건들
        if (request.getCursorId() != null) {
            predicates.add(cb.lt(meeting.get("id"), request.getCursorId()));
        }
        if (request.getReadingGenres() != null && !request.getReadingGenres().isEmpty()) {
            Join<Meeting, ReadingGenre> genre = meeting.join("readingGenre", JoinType.INNER);
            predicates.add(genre.get("code").in(request.getReadingGenres()));
        }
        if (request.getDayOfWeek() != null && !request.getDayOfWeek().isEmpty()) {
            predicates.add(meeting.get("dayOfWeek").in(request.getDayOfWeek()));
        }
        if (request.getRoundCount() != null) {
            int roundCount = request.getRoundCountValue();
            if (roundCount == 1) {
                predicates.add(cb.equal(meeting.get("roundCount"), 1));
            } else if (roundCount == 3) {
                predicates.add(cb.between(meeting.get("roundCount"), 3, 4));
            } else if (roundCount == 5) {
                predicates.add(cb.between(meeting.get("roundCount"), 5, 8));
            }
        }

        Predicate startTimePredicate = buildStartTimePredicate(cb, meeting, request.getStartTimeValues());
        if (startTimePredicate != null) {
            predicates.add(startTimePredicate);
        }

        query.select(cb.construct(MeetingListRow.class,
                        meeting.get("id"),
                        meeting.get("meetingImagePath"),
                        meeting.get("title"),
                        meeting.get("readingGenreId"),
                        leader.get("nickname"),
                        meeting.get("capacity"),
                        meeting.get("currentCount"),
                        meeting.get("recruitmentDeadline")
                ))
                .where(predicates.toArray(new Predicate[0]))
                .orderBy(
                        // 책 제목 매칭 우선
                        cb.asc(buildBookTitleMatchOrder(cb, query, meeting, request.getKeywordTrimmed())),
                        // RECRUITING 우선
                        cb.asc(cb.selectCase()
                                .when(cb.equal(meeting.get("status"), MeetingStatus.RECRUITING), 0)
                                .otherwise(1)),
                        // 최신순
                        cb.desc(meeting.get("createdAt")),
                        cb.desc(meeting.get("id"))
                );

        TypedQuery<MeetingListRow> typedQuery = entityManager.createQuery(query);
        typedQuery.setMaxResults(limit);
        return typedQuery.getResultList();
    }

    private Predicate buildSearchCondition(
            CriteriaBuilder cb,
            CriteriaQuery<?> query,
            Root<Meeting> meeting,
            String keyword
    ) {
        if (keyword == null || keyword.isEmpty()) {
            return null;
        }

        // 서브쿼리: 책 제목 검색
        Subquery<Long> bookTitleSubquery = query.subquery(Long.class);
        Root<MeetingRound> round = bookTitleSubquery.from(MeetingRound.class);
        Join<MeetingRound, Book> book = round.join("book", JoinType.INNER);
        
        bookTitleSubquery.select(cb.literal(1L))
                .where(
                        cb.equal(round.get("meeting"), meeting),
                        cb.like(cb.lower(book.get("title")), "%" + keyword.toLowerCase() + "%")
                );

        // 모임 제목 검색
        Predicate meetingTitleMatch = cb.like(cb.lower(meeting.get("title")), "%" + keyword.toLowerCase() + "%");

        // OR 조건
        return cb.or(cb.exists(bookTitleSubquery), meetingTitleMatch);
    }

    private Expression<Integer> buildBookTitleMatchOrder(
            CriteriaBuilder cb,
            CriteriaQuery<?> query,
            Root<Meeting> meeting,
            String keyword
    ) {
        if (keyword == null || keyword.isEmpty()) {
            return cb.literal(1);
        }

        // 서브쿼리: 책 제목 매칭 여부
        Subquery<Long> bookTitleSubquery = query.subquery(Long.class);
        Root<MeetingRound> round = bookTitleSubquery.from(MeetingRound.class);
        Join<MeetingRound, Book> book = round.join("book", JoinType.INNER);
        
        bookTitleSubquery.select(cb.literal(1L))
                .where(
                        cb.equal(round.get("meeting"), meeting),
                        cb.like(cb.lower(book.get("title")), "%" + keyword.toLowerCase() + "%")
                );

        // 책 제목 매칭이면 0, 아니면 1
        return cb.<Integer>selectCase()
                .when(cb.exists(bookTitleSubquery), 0)
                .otherwise(1);
    }

    @Override
    public List<MeetingListRow> findMyMeetings(Long userId, Long cursorId, boolean activeOnly, int limit) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<MeetingListRow> query = cb.createQuery(MeetingListRow.class);
        Root<Meeting> meeting = query.from(Meeting.class);
        Join<Meeting, User> leader = meeting.join("leaderUser", JoinType.INNER);

        // MeetingMember 서브쿼리로 나의 모임만 필터링
        Subquery<Long> memberSubquery = query.subquery(Long.class);
        Root<MeetingMember> memberRoot =
                memberSubquery.from(MeetingMember.class);
        
        // APPROVED와 PENDING 모두 포함
        memberSubquery.select(memberRoot.get("meeting").get("id"))
                .where(
                        cb.equal(memberRoot.get("user").get("id"), userId),
                        cb.or(
                                cb.equal(memberRoot.get("status"), 
                                        APPROVED),
                                cb.equal(memberRoot.get("status"), 
                                        MeetingMemberStatus.PENDING)
                        )
                );

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.isNull(meeting.get("deletedAt")));
        predicates.add(meeting.get("id").in(memberSubquery));

        // status=ACTIVE 필터: RECRUITING or FINISHED
        if (activeOnly) {
            predicates.add(cb.or(
                    cb.equal(meeting.get("status"), MeetingStatus.RECRUITING),
                    cb.equal(meeting.get("status"), MeetingStatus.FINISHED)
            ));
        } else {
            // status=INACTIVE 필터: CANCELED
            predicates.add(cb.equal(meeting.get("status"), MeetingStatus.CANCELED));
        }

        if (cursorId != null) {
            predicates.add(cb.lt(meeting.get("id"), cursorId));
        }

        query.select(cb.construct(MeetingListRow.class,
                        meeting.get("id"),
                        meeting.get("meetingImagePath"),
                        meeting.get("title"),
                        meeting.get("readingGenreId"),
                        leader.get("nickname"),
                        meeting.get("capacity"),
                        meeting.get("currentCount"),
                        meeting.get("recruitmentDeadline")
                ))
                .where(predicates.toArray(new Predicate[0]))
                .orderBy(cb.desc(meeting.get("id")));

        TypedQuery<MeetingListRow> typedQuery = entityManager.createQuery(query);
        typedQuery.setMaxResults(limit);
        return typedQuery.getResultList();
    }

    @Override
    public List<MeetingListRow> findMyTodayMeetings(Long userId, java.time.LocalDate today) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<MeetingListRow> query = cb.createQuery(MeetingListRow.class);
        Root<Meeting> meeting = query.from(Meeting.class);
        Join<Meeting, User> leader = meeting.join("leaderUser", JoinType.INNER);

        // MeetingMember 서브쿼리: 나의 모임 (APPROVED + PENDING)
        Subquery<Long> memberSubquery = query.subquery(Long.class);
        Root<MeetingMember> memberRoot =
                memberSubquery.from(MeetingMember.class);
        
        memberSubquery.select(memberRoot.get("meeting").get("id"))
                .where(
                        cb.equal(memberRoot.get("user").get("id"), userId),
                        cb.or(
                                cb.equal(memberRoot.get("status"), 
                                        APPROVED),
                                cb.equal(memberRoot.get("status"), 
                                        MeetingMemberStatus.PENDING)
                        )
                );

        // MeetingRound 서브쿼리: 오늘 날짜의 회차가 있는 모임
        Subquery<Long> todayRoundSubquery = query.subquery(Long.class);
        Root<MeetingRound> roundRoot = todayRoundSubquery.from(MeetingRound.class);
        
        todayRoundSubquery.select(roundRoot.get("meeting").get("id"))
                .where(
                        cb.equal(cb.function("DATE", java.time.LocalDate.class, roundRoot.get("startAt")), today)
                );

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.isNull(meeting.get("deletedAt")));
        predicates.add(meeting.get("id").in(memberSubquery));
        predicates.add(meeting.get("id").in(todayRoundSubquery));
        
        // ACTIVE만 (RECRUITING, FINISHED)
        predicates.add(cb.or(
                cb.equal(meeting.get("status"), MeetingStatus.RECRUITING),
                cb.equal(meeting.get("status"), MeetingStatus.FINISHED)
        ));

        query.select(cb.construct(MeetingListRow.class,
                        meeting.get("id"),
                        meeting.get("meetingImagePath"),
                        meeting.get("title"),
                        meeting.get("readingGenreId"),
                        leader.get("nickname"),
                        meeting.get("capacity"),
                        meeting.get("currentCount"),
                        meeting.get("recruitmentDeadline")
                ))
                .where(predicates.toArray(new Predicate[0]))
                .orderBy(cb.desc(meeting.get("id")));

        return entityManager.createQuery(query).getResultList();
    }

    
}
