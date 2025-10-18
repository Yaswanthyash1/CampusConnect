package com.collegeclubs.event_service.repository;

import com.collegeclubs.event_service.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByClubNameIn(List<String> clubNames);

    // Find events whose timestamp is after the provided time (upcoming)
    List<Event> findByTimestampAfter(Timestamp time);

    // Find events whose timestamp is before or equal to the provided time (past)
    List<Event> findByTimestampBefore(Timestamp time);

    // Ordered variants
    List<Event> findByTimestampAfterOrderByTimestampAsc(Timestamp time);

    List<Event> findByTimestampBeforeOrderByTimestampDesc(Timestamp time);
}