package com.collegeclubs.user_service.repository;

import com.collegeclubs.user_service.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Removed unused methods to simplify the repository
}
