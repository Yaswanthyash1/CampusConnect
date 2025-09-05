package com.collegeclubs.request_service.repository;

import com.collegeclubs.request_service.model.Request;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RequestRepository extends JpaRepository<Request, Long> {
    List<Request> findByType(String type);

    List<Request> findByMemberId(String memberId);

    // New methods
    List<Request> findByClubName(String clubName);

    List<Request> findByClubNameAndStatus(String clubName, String status);

    @Query("SELECT r FROM Request r WHERE (r.clubName = :clubName OR r.clubName IS NULL) AND r.status = :status ORDER BY r.timestamp DESC")
    List<Request> findPendingForClub(@Param("clubName") String clubName, @Param("status") String status);

    @Query("SELECT r FROM Request r WHERE (r.clubName = :clubName AND r.status = :status AND r.type != 'enroll') OR (r.clubName IS NULL AND r.status = :status AND r.type != 'enroll') ORDER BY r.timestamp DESC")
    List<Request> findAcceptedNonEnrollRequests(@Param("clubName") String clubName, @Param("status") String status);

    @Query("SELECT r FROM Request r WHERE r.status = 'accepted' AND r.type != 'enroll' ORDER BY r.timestamp DESC")
    List<Request> findAllAcceptedNonEnrollRequests();
}
