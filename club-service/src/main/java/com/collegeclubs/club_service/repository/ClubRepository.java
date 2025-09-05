package com.collegeclubs.club_service.repository;

import com.collegeclubs.club_service.model.Club;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubRepository extends JpaRepository<Club, Long> {
}
