package com.repository;

import com.model.ClubMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClubMemberRepository extends JpaRepository<ClubMember, Long> {
    List<ClubMember> findBySrn(String srn);
}
