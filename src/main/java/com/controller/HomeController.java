package com.controller;

import com.model.Event;
import com.model.MemberRequest;
import com.service.ClubService;
import com.service.EventService;
import com.service.MemberRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class HomeController {
    @Autowired
    private EventService eventService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private MemberRequestService memberRequestService;
    @Autowired
    private ClubService clubService;

    @GetMapping("/home")
    public String home(Model model, @RequestParam(value = "userType", required = false) String userType,
                       @RequestParam(value = "userIdentifier", required = false) String userIdentifier) {
        List<Event> events = eventService.getAllEvents();
        model.addAttribute("events", events);
        // If userType is faculty, fetch all clubs
        if (userType != null && userType.equals("faculty")) {
            String sql = "SELECT * FROM club";
            List<Map<String, Object>> clubs = jdbcTemplate.queryForList(sql);
            model.addAttribute("clubs", clubs);
        }
        return "home";
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/request")
    public String requestPage() {
        return "request";
    }

    @PostMapping("/upload-request")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "Please select a file to upload.");
            return "redirect:/request";
        }
        String filename = file.getOriginalFilename();
        String ext = filename != null && filename.contains(".") ? filename.substring(filename.lastIndexOf(".")).toLowerCase() : "";
        if (!(ext.equals(".pdf") || ext.equals(".doc") || ext.equals(".docx") || ext.equals(".txt"))) {
            redirectAttributes.addFlashAttribute("message", "Invalid file type. Only PDF, DOC, DOCX, and TXT are allowed.");
            return "redirect:/request";
        }
        try {
            File uploadDir = new File("uploads");
            if (!uploadDir.exists()) uploadDir.mkdirs();
            File dest = new File(uploadDir, filename);
            file.transferTo(dest);
            redirectAttributes.addFlashAttribute("message", "File uploaded successfully: " + filename);
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("message", "File upload failed: " + e.getMessage());
        }
        return "redirect:/request";
    }

    @GetMapping("/pending-tasks")
    public String pendingTasks(Model model, @RequestParam(value = "userIdentifier", required = false) String userIdentifier) {
        System.out.println("=== PENDING TASKS DEBUG ===");
        System.out.println("Received userIdentifier: " + userIdentifier);

        if (userIdentifier != null) {
            // Assuming userIdentifier for clubhead is their SRN, and we can get clubName from it
            String clubName = clubService.getClubNameByHeadSrn(userIdentifier);
            System.out.println("Found clubName: " + clubName);

            if (clubName != null) {
                List<MemberRequest> acceptedRequests = memberRequestService.getRequestsByClubNameAndStatus(clubName, "accepted");
                System.out.println("Found " + acceptedRequests.size() + " accepted requests");

                // Filter out 'enroll' type requests and categorize them
                List<MemberRequest> ideas = filterAndCategorizeRequests(acceptedRequests, "Idea");
                List<MemberRequest> queries = filterAndCategorizeRequests(acceptedRequests, "Query");
                List<MemberRequest> projects = filterAndCategorizeRequests(acceptedRequests, "Project");
                List<MemberRequest> events = filterAndCategorizeRequests(acceptedRequests, "Event");

                System.out.println("Ideas: " + ideas.size());
                System.out.println("Queries: " + queries.size());
                System.out.println("Projects: " + projects.size());
                System.out.println("Events: " + events.size());

                model.addAttribute("ideas", ideas);
                model.addAttribute("queries", queries);
                model.addAttribute("projects", projects);
                model.addAttribute("events", events);
            } else {
                System.out.println("Club not found for userIdentifier: " + userIdentifier);
                // If userIdentifier doesn't match a club head, try getting all accepted requests
                List<MemberRequest> allAcceptedRequests = memberRequestService.getAllAcceptedNonEnrollRequests();
                System.out.println("Found " + allAcceptedRequests.size() + " total accepted requests");

                model.addAttribute("ideas", filterAndCategorizeRequests(allAcceptedRequests, "Idea"));
                model.addAttribute("queries", filterAndCategorizeRequests(allAcceptedRequests, "Query"));
                model.addAttribute("projects", filterAndCategorizeRequests(allAcceptedRequests, "Project"));
                model.addAttribute("events", filterAndCategorizeRequests(allAcceptedRequests, "Event"));
            }
        } else {
            System.out.println("No userIdentifier provided");
            // If no userIdentifier, show all accepted requests
            List<MemberRequest> allAcceptedRequests = memberRequestService.getAllAcceptedNonEnrollRequests();
            System.out.println("Found " + allAcceptedRequests.size() + " total accepted requests");

            model.addAttribute("ideas", filterAndCategorizeRequests(allAcceptedRequests, "Idea"));
            model.addAttribute("queries", filterAndCategorizeRequests(allAcceptedRequests, "Query"));
            model.addAttribute("projects", filterAndCategorizeRequests(allAcceptedRequests, "Project"));
            model.addAttribute("events", filterAndCategorizeRequests(allAcceptedRequests, "Event"));
        }
        return "pending-tasks";
    }

    private List<MemberRequest> filterAndCategorizeRequests(List<MemberRequest> requests, String type) {
        return requests.stream()
                .filter(req -> req.getType() != null && req.getType().equalsIgnoreCase(type))
                .filter(req -> req.isCompleted() == null || !req.isCompleted()) // Exclude completed requests
                .collect(java.util.stream.Collectors.toList());
    }

    @PostMapping("/complete-request")
    public String completeRequest(@RequestParam("requestId") Long requestId,
                                  @RequestParam("action") String action,
                                  RedirectAttributes redirectAttributes) {
        System.out.println("=== COMPLETING REQUEST ===");
        System.out.println("Request ID: " + requestId + ", Action: " + action);

        try {
            Optional<MemberRequest> requestOpt = memberRequestService.getRequestById(requestId);
            if (requestOpt.isPresent()) {
                MemberRequest request = requestOpt.get();
                // Mark the request as completed
                request.setStatus("completed");
                request.setCompleted(true); // This will set isCompleted = 1 in the database
                memberRequestService.saveRequest(request);
                System.out.println("Request " + requestId + " marked as completed with isCompleted = 1");
                redirectAttributes.addFlashAttribute("success", "Request completed successfully!");
            } else {
                System.out.println("Request not found: " + requestId);
                redirectAttributes.addFlashAttribute("error", "Request not found!");
            }
        } catch (Exception e) {
            System.out.println("Error completing request: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error completing request: " + e.getMessage());
        }

        return "redirect:/pending-tasks";
    }
}
