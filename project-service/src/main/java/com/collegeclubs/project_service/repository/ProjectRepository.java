package com.collegeclubs.project_service.repository;

import com.collegeclubs.project_service.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    // Find projects by club name
    List<Project> findByClubName(String clubName);

    // Find projects by status
    List<Project> findByStatus(String status);

    // Find projects by category
    List<Project> findByCategory(String category);

    // Find projects by priority
    List<Project> findByPriority(String priority);

    // Find projects by club name and status
    List<Project> findByClubNameAndStatus(String clubName, String status);

    // Custom query to find projects by keyword in name or description
    @Query("SELECT p FROM Project p WHERE LOWER(p.projectName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Project> findByKeyword(@Param("keyword") String keyword);

    // Find active projects (not completed)
    @Query("SELECT p FROM Project p WHERE p.status != 'Completed'")
    List<Project> findActiveProjects();

    // Find projects ordered by priority and creation date
    @Query("SELECT p FROM Project p ORDER BY CASE p.priority WHEN 'High' THEN 1 WHEN 'Medium' THEN 2 WHEN 'Low' THEN 3 END, p.createdAt DESC")
    List<Project> findAllOrderedByPriority();

    // Find upcoming projects (end date is in the future)
    @Query("SELECT p FROM Project p WHERE p.endDate > CURRENT_DATE ORDER BY p.endDate ASC")
    List<Project> findUpcomingProjects();

    // Find past projects (end date is in the past)
    @Query("SELECT p FROM Project p WHERE p.endDate < CURRENT_DATE ORDER BY p.endDate DESC")
    List<Project> findPastProjects();
}