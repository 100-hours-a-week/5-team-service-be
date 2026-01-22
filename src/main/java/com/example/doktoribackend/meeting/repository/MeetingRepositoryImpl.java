package com.example.doktoribackend.meeting.repository;

import com.example.doktoribackend.meeting.domain.Meeting;
import com.example.doktoribackend.meeting.domain.MeetingStatus;
import com.example.doktoribackend.meeting.dto.MeetingListItem;
import com.example.doktoribackend.meeting.dto.MeetingListRequest;
import com.example.doktoribackend.user.domain.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class MeetingRepositoryImpl implements MeetingRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<MeetingListItem> findMeetingList(MeetingListRequest request, int limit) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<MeetingListItem> query = cb.createQuery(MeetingListItem.class);
        Root<Meeting> meeting = query.from(Meeting.class);
        Join<Meeting, User> leader = meeting.join("leaderUser", JoinType.INNER);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.isNull(meeting.get("deletedAt")));
        predicates.add(cb.equal(meeting.get("status"), MeetingStatus.RECRUITING));

        if (request.getCursorId() != null) {
            predicates.add(cb.lt(meeting.get("id"), request.getCursorId()));
        }
        if (request.getReadingGenreId() != null) {
            predicates.add(cb.equal(meeting.get("readingGenreId"), request.getReadingGenreId()));
        }
        if (request.getDayOfWeek() != null && !request.getDayOfWeek().isEmpty()) {
            predicates.add(meeting.get("dayOfWeek").in(request.getDayOfWeek()));
        }
        if (request.getRoundCount() != null) {
            int roundCount = request.getRoundCount();
            if (roundCount == 1) {
                predicates.add(cb.equal(meeting.get("roundCount"), 1));
            } else if (roundCount == 3) {
                predicates.add(cb.between(meeting.get("roundCount"), 3, 4));
            } else if (roundCount == 5) {
                predicates.add(cb.between(meeting.get("roundCount"), 5, 8));
            }
        }

        Predicate startTimePredicate = buildStartTimePredicate(cb, meeting, request.getStartTimeFrom());
        if (startTimePredicate != null) {
            predicates.add(startTimePredicate);
        }

        // Join + projection to avoid N+1 when mapping leader nickname.
        query.select(cb.construct(MeetingListItem.class,
                        meeting.get("id"),
                        meeting.get("meetingImagePath"),
                        meeting.get("title"),
                        meeting.get("readingGenreId"),
                        leader.get("nickname"),
                        meeting.get("capacity"),
                        meeting.get("currentCount")
                ))
                .where(predicates.toArray(new Predicate[0]))
                .orderBy(cb.desc(meeting.get("id")));

        TypedQuery<MeetingListItem> typedQuery = entityManager.createQuery(query);
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

    
}
