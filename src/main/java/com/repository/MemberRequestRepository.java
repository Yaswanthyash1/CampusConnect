package com.repository;

import com.model.MemberRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MemberRequestRepository extends JpaRepository<MemberRequest, Long> {
    List<MemberRequest> findByType(String type);

    List<MemberRequest> findByMemberId(String memberId);

    // New methods
    List<MemberRequest> findByClubName(String clubName);

    List<MemberRequest> findByClubNameAndStatus(String clubName, String status);

    @Query("SELECT r FROM MemberRequest r WHERE (r.clubName = :clubName OR r.clubName IS NULL) AND r.status = :status ORDER BY r.timestamp DESC")
    List<MemberRequest> findPendingForClub(@Param("clubName") String clubName, @Param("status") String status);
}
