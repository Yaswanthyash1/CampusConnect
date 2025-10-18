package com.collegeclubs.project_service.service;

import com.collegeclubs.project_service.model.Project;
import com.collegeclubs.project_service.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    public Project saveProject(Project project) {
        return projectRepository.save(project);
    }

    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    public Optional<Project> getProjectById(Long id) {
        return projectRepository.findById(id);
    }

    public List<Project> getProjectsByClubName(String clubName) {
        return projectRepository.findByClubName(clubName);
    }

    public List<Project> getProjectsByStatus(String status) {
        return projectRepository.findByStatus(status);
    }

    public List<Project> getProjectsByCategory(String category) {
        return projectRepository.findByCategory(category);
    }

    public List<Project> getProjectsByPriority(String priority) {
        return projectRepository.findByPriority(priority);
    }

    public List<Project> getProjectsByClubAndStatus(String clubName, String status) {
        return projectRepository.findByClubNameAndStatus(clubName, status);
    }

    public List<Project> searchProjectsByKeyword(String keyword) {
        return projectRepository.findByKeyword(keyword);
    }

    public List<Project> getActiveProjects() {
        return projectRepository.findActiveProjects();
    }

    public List<Project> getAllProjectsOrderedByPriority() {
        return projectRepository.findAllOrderedByPriority();
    }

    public List<Project> getUpcomingProjects() {
        return projectRepository.findUpcomingProjects();
    }

    public List<Project> getPastProjects() {
        return projectRepository.findPastProjects();
    }

    public void deleteProject(Long id) {
        projectRepository.deleteById(id);
    }

    public Project updateProject(Project project) {
        return projectRepository.save(project);
    }

    public boolean existsById(Long id) {
        return projectRepository.existsById(id);
    }
}