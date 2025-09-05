package com.collegeclubs.club_service.repository;

import com.collegeclubs.club_service.model.ClubMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClubMemberRepository extends JpaRepository<ClubMember, Long> {
    List<ClubMember> findBySrn(String srn);
}
