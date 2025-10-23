package com.collegeclubs.project_service.controller;

import com.collegeclubs.project_service.model.Project;
import com.collegeclubs.project_service.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Controller
public class ProjectController {

    private static final Logger logger = LoggerFactory.getLogger(ProjectController.class);
    private static final String UPLOAD_DIR = System.getProperty("user.home") + "/nitk-project-uploads";

    @Autowired
    private ProjectService projectService;

    // New API endpoint that returns project data as JSON so the frontend can render the template
    @GetMapping("/api/project/{id}")
    @ResponseBody
    public ResponseEntity<?> getProjectAsJson(@PathVariable("id") Long id) {
        try {
            Project project = projectService.getProjectById(id).orElse(null);
            if (project == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Project not found");
            }
            return ResponseEntity.ok(project);
        } catch (Exception e) {
            logger.error("Error fetching project as JSON", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching project");
        }
    }

    @PostMapping("/addProject")
    public String addProject(@RequestParam("clubname") String clubName,
                             @RequestParam("projectname") String projectName,
                             @RequestParam("description") String description,
                             @RequestParam("category") String category,
                             @RequestParam("priority") String priority,
                             @RequestParam("startdate") String startDate,
                             @RequestParam("enddate") String endDate,
                             @RequestParam("budget") double budget,
                             @RequestParam("teamsize") int teamSize,
                             @RequestParam(value = "technologies", required = false) String technologies,
                             @RequestParam("objectives") String objectives,
                             @RequestParam("deliverables") String deliverables,
                             @RequestParam(value = "mentor", required = false) String mentor,
                             @RequestParam("status") String status,
                             @RequestParam(value = "attachments", required = false) MultipartFile[] attachments,
                             RedirectAttributes redirectAttributes) {

        try {
            logger.info("Received project creation request: {}", projectName);

            // Create project object
            Project project = new Project();
            project.setClubName(clubName);
            project.setProjectName(projectName);
            project.setDescription(description);
            project.setCategory(category);
            project.setPriority(priority);
            project.setStartDate(Date.valueOf(startDate));
            project.setEndDate(Date.valueOf(endDate));
            project.setBudget(budget);
            project.setTeamSize(teamSize);
            project.setTechnologies(technologies);
            project.setObjectives(objectives);
            project.setDeliverables(deliverables);
            project.setMentor(mentor);
            project.setStatus(status);

            // Handle file uploads
            if (attachments != null && attachments.length > 0) {
                List<String> filePaths = new ArrayList<>();

                // Ensure upload directory exists
                Path uploadPath = Paths.get(UPLOAD_DIR);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                for (MultipartFile file : attachments) {
                    if (!file.isEmpty()) {
                        String originalFilename = file.getOriginalFilename();
                        if (originalFilename != null) {
                            // Sanitize filename
                            String sanitizedFileName = System.currentTimeMillis() + "_" +
                                    originalFilename.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
                            Path filePath = uploadPath.resolve(sanitizedFileName);

                            try {
                                file.transferTo(filePath.toFile());
                                filePaths.add(filePath.toString());
                                logger.info("File uploaded: {}", sanitizedFileName);
                            } catch (IOException e) {
                                logger.error("Failed to upload file: {}", originalFilename, e);
                            }
                        }
                    }
                }

                // Store file paths as comma-separated string
                if (!filePaths.isEmpty()) {
                    project.setAttachments(String.join(",", filePaths));
                }
            }

            // Save project
            Project savedProject = projectService.saveProject(project);

            if (savedProject != null) {
                logger.info("Project created successfully with ID: {}", savedProject.getId());
                redirectAttributes.addFlashAttribute("success", "Project '" + projectName + "' created successfully!");
            } else {
                logger.error("Failed to save project");
                redirectAttributes.addFlashAttribute("error", "Failed to create project. Please try again.");
            }

        } catch (Exception e) {
            logger.error("Error creating project", e);
            redirectAttributes.addFlashAttribute("error", "Error creating project: " + e.getMessage());
        }

        return "redirect:/add-project";
    }

    @GetMapping("/projects")
    public String showAllProjects(@RequestParam(value = "status", required = false) String status,
                                  @RequestParam(value = "category", required = false) String category,
                                  @RequestParam(value = "priority", required = false) String priority,
                                  Model model) {
        List<Project> projects;

        if (status != null && !status.isEmpty()) {
            projects = projectService.getProjectsByStatus(status);
            model.addAttribute("filterStatus", status);
        } else if (category != null && !category.isEmpty()) {
            projects = projectService.getProjectsByCategory(category);
            model.addAttribute("filterCategory", category);
        } else if (priority != null && !priority.isEmpty()) {
            projects = projectService.getProjectsByPriority(priority);
            model.addAttribute("filterPriority", priority);
        } else {
            projects = projectService.getAllProjectsOrderedByPriority();
        }

        model.addAttribute("projects", projects);
        return "projects";
    }

    @GetMapping("/projects/upcoming")
    public String showUpcomingProjects(Model model) {
        List<Project> projects = projectService.getUpcomingProjects();
        model.addAttribute("projects", projects);
        model.addAttribute("pageTitle", "Upcoming Projects");
        model.addAttribute("filterType", "upcoming");
        return "projects";
    }

    @GetMapping("/projects/past")
    public String showPastProjects(Model model) {
        List<Project> projects = projectService.getPastProjects();
        model.addAttribute("projects", projects);
        model.addAttribute("pageTitle", "Past Projects");
        model.addAttribute("filterType", "past");
        return "projects";
    }

    @GetMapping("/project-details/{id}")
    public String showProjectDetails(@PathVariable("id") Long id, Model model) {
        Project project = projectService.getProjectById(id).orElse(null);
        if (project == null) {
            return "redirect:/projects";
        }
        model.addAttribute("project", project);

        // Calculate project progress based on dates and status
        boolean isOngoing = "In Progress".equals(project.getStatus());
        boolean isCompleted = "Completed".equals(project.getStatus());
        model.addAttribute("isOngoing", isOngoing);
        model.addAttribute("isCompleted", isCompleted);

        return "project-details";
    }

    @GetMapping("/projects/club/{clubName}")
    public String showProjectsByClub(@PathVariable("clubName") String clubName, Model model) {
        List<Project> projects = projectService.getProjectsByClubName(clubName);
        model.addAttribute("projects", projects);
        model.addAttribute("clubName", clubName);
        return "projects";
    }

    @GetMapping("/projects/search")
    public String searchProjects(@RequestParam("keyword") String keyword, Model model) {
        List<Project> projects = projectService.searchProjectsByKeyword(keyword);
        model.addAttribute("projects", projects);
        model.addAttribute("searchKeyword", keyword);
        return "projects";
    }

    @PostMapping("/project/update-status")
    public String updateProjectStatus(@RequestParam("projectId") Long projectId,
                                      @RequestParam("status") String status,
                                      RedirectAttributes redirectAttributes) {
        try {
            Project project = projectService.getProjectById(projectId).orElse(null);
            if (project != null) {
                project.setStatus(status);
                projectService.updateProject(project);
                redirectAttributes.addFlashAttribute("success", "Project status updated successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Project not found!");
            }
        } catch (Exception e) {
            logger.error("Error updating project status", e);
            redirectAttributes.addFlashAttribute("error", "Error updating project status: " + e.getMessage());
        }
        return "redirect:/projects";
    }

    @PostMapping("/project/delete/{id}")
    public String deleteProject(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            if (projectService.existsById(id)) {
                projectService.deleteProject(id);
                redirectAttributes.addFlashAttribute("success", "Project deleted successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Project not found!");
            }
        } catch (Exception e) {
            logger.error("Error deleting project", e);
            redirectAttributes.addFlashAttribute("error", "Error deleting project: " + e.getMessage());
        }
        return "redirect:/projects";
    }

    @GetMapping("/project/download/{projectId}/{fileIndex}")
    public ResponseEntity<Resource> downloadProjectFile(@PathVariable Long projectId,
                                                        @PathVariable int fileIndex) {
        try {
            Project project = projectService.getProjectById(projectId).orElse(null);
            if (project == null || project.getAttachments() == null) {
                return ResponseEntity.notFound().build();
            }

            String[] filePaths = project.getAttachments().split(",");
            if (fileIndex < 0 || fileIndex >= filePaths.length) {
                return ResponseEntity.notFound().build();
            }

            Path filePath = Paths.get(filePaths[fileIndex].trim());
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(filePath.toUri());
            String filename = filePath.getFileName().toString();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(resource);

        } catch (Exception e) {
            logger.error("Error downloading project file", e);
            return ResponseEntity.internalServerError().build();
        }
    }


    @GetMapping("/projects-dashboard")
    public ResponseEntity<?> projectsDashboard() {
        // Get upcoming projects (end date is in the future)
        List<Project> upcomingProjects = projectService.getUpcomingProjects();

        // Get past projects (end date is in the past)
        List<Project> pastProjects = projectService.getPastProjects();

        // Build a simple response payload so the frontend can render the template
        Map<String, Object> payload = new HashMap<>();
        payload.put("upcomingProjects", upcomingProjects);
        payload.put("pastProjects", pastProjects);

        return ResponseEntity.ok(payload);
    }
}

