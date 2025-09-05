package com.collegeclubs.club_service.controller;

import com.collegeclubs.club_service.model.Club;
import com.collegeclubs.club_service.service.ClubService;
import com.collegeclubs.club_service.repository.ClubRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ClubController {
    @Autowired
    private ClubService clubService;
    @Autowired
    private ClubRepository clubRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/club/{clubName}")
    public String clubDashboard(@PathVariable String clubName, Model model) {
        Club club = clubService.getClubDetailsByName(clubName);
        if (club == null) {
            return "club-not-found";
        }
        List<Map<String, Object>> clubMembers = getClubMembersByName(clubName);
        List<Map<String, Object>> clubEvents = getClubEvents(clubName);
        model.addAttribute("club", club);
        model.addAttribute("clubMembers", clubMembers);
        model.addAttribute("clubEvents", clubEvents);
        return "club";
    }

    @GetMapping("/event")
    public String showEventForm(Model model) {
        String sql = "SELECT * FROM event";
        List<Map<String, Object>> events = jdbcTemplate.queryForList(sql);
        model.addAttribute("events", events);
        return "event";
    }

    @GetMapping("/add-event")
    public String showAddEventForm(Model model, @RequestParam(name = "requestId", required = false) Long requestId) {
        if (requestId != null) {
            model.addAttribute("requestId", requestId);
        }
        return "add-event";
    }

    @GetMapping("/test-add-event")
    public String testAddEvent() {
        System.out.println("=== Test Add Event Endpoint ===");
        return "add-event";
    }

    @GetMapping("/test-db")
    @ResponseBody
    public String testDatabase() {
        try {
            // Test if we can query the event table
            String sql = "SELECT COUNT(*) FROM event";
            int count = jdbcTemplate.queryForObject(sql, Integer.class);
            return "Database connection successful. Event table has " + count + " records.";
        } catch (Exception e) {
            return "Database error: " + e.getMessage();
        }
    }

    // @GetMapping("/faculty/{faculty_id}")
    // public String getFacultyClubs(@PathVariable("faculty_id") int facultyId,
    // Model model) {
    // try {
    // String selectFacultySql = "SELECT * FROM faculty WHERE id = ?";
    // Map<String, Object> faculty = jdbcTemplate.queryForMap(selectFacultySql,
    // facultyId);
    // String facultyName = (String) faculty.get("faculty_name");
    // String selectClubsSql = "SELECT c.*, COUNT(cm.SRN) AS memberCount,
    // SUM(e.budget) AS totalBudget " +
    // "FROM club c " +
    // "LEFT JOIN clubmember cm ON c.clubName = cm.clubName " +
    // "LEFT JOIN event e ON c.clubName = e.clubname " +
    // "WHERE c.faculty_id = ? " +
    // "GROUP BY c.id";
    // List<Map<String, Object>> clubs = jdbcTemplate.queryForList(selectClubsSql,
    // facultyId);
    // model.addAttribute("facultyName", facultyName);
    // model.addAttribute("clubs", clubs);
    // return "faculty";
    // } catch (Exception e) {
    // e.printStackTrace();
    // return "error";
    // }
    // }

    @PostMapping("/addEvent")
    public String addEvent(@RequestParam("clubname") String clubName,
            @RequestParam("eventname") String eventName,
            @RequestParam("description") String description,
            @RequestParam("location") String location,
            @RequestParam("type") String type,
            @RequestParam("timestamp") String timestamp,
            @RequestParam("budget") double budget,
            @RequestParam("registrationlink") String registrationLink,
            @RequestParam(value = "banner", required = false) MultipartFile banner,
            @RequestParam(value = "requestId", required = false) Long requestId,
            Model model) {

        System.out.println("=== Event Creation Started ===");
        System.out.println("Club Name: " + clubName);
        System.out.println("Event Name: " + eventName);
        System.out.println("Description: " + description);
        System.out.println("Location: " + location);
        System.out.println("Type: " + type);
        System.out.println("Timestamp: " + timestamp);
        System.out.println("Budget: " + budget);
        System.out.println("Registration Link: " + registrationLink);
        System.out.println("Banner: " + (banner != null ? banner.getOriginalFilename() : "null"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        LocalDateTime parsedTimestamp = LocalDateTime.parse(timestamp, formatter);
        byte[] bannerBytes = null;
        try {
            if (banner != null && !banner.isEmpty()) {
                bannerBytes = banner.getBytes();
            }
        } catch (IOException e) {
            model.addAttribute("error", "Failed to process banner file: " + e.getMessage());
            return "add-event";
        }
        String insertSql = "INSERT INTO event (clubname, eventname, description, loc, type, timestamp, budget, registrationlink, banner) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            int result = jdbcTemplate.update(insertSql, clubName, eventName, description, location, type,
                    Timestamp.valueOf(parsedTimestamp), budget, registrationLink, bannerBytes);
            if (result > 0) {
                model.addAttribute("success", "Event created successfully!");
                // TODO: Update request status via microservice call if needed
            } else {
                model.addAttribute("error", "Event creation failed. Please try again.");
            }
        } catch (Exception e) {
            model.addAttribute("error", "Failed to create event: " + e.getMessage());
            return "add-event";
        }
        return "add-event";
    }

    @PostMapping("/club/{clubName}/requests")
    public String handleClubRequests(@PathVariable String clubName, @RequestParam String action,
            @RequestParam(required = false) Long requestId) {
        // action = "accept" or "reject"
        try {
            if (requestId != null) {
                // Call the request microservice to update the request status
                org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
                String requestServiceUrl = "http://localhost:8083";
                String apiUrl = requestServiceUrl + "/update-request-status";
                java.util.Map<String, Object> requestBody = new java.util.HashMap<>();
                requestBody.put("requestId", requestId);
                requestBody.put("action", action);
                requestBody.put("clubName", clubName);
                restTemplate.postForObject(apiUrl, requestBody, String.class);
                // Optionally, for enroll, also update user service as before
                if ("accept".equals(action)) {
                    // Fetch request details from microservice to check type
                    String getReqUrl = requestServiceUrl + "/request/" + requestId;
                    org.springframework.http.ResponseEntity<java.util.Map> resp = restTemplate.getForEntity(getReqUrl,
                            java.util.Map.class);
                    if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                        Object typeObj = resp.getBody().get("type");
                        Object srnObj = resp.getBody().get("memberId");
                        if (typeObj != null && "enroll".equals(typeObj.toString()) && srnObj != null) {
                            String userServiceUrl = "http://localhost:8081";
                            String userApiUrl = userServiceUrl + "/user-service/api/user" + "/setUserClub";
                            java.util.Map<String, String> userBody = new java.util.HashMap<>();
                            userBody.put("srn", srnObj.toString());
                            userBody.put("clubName", clubName);
                            try {
                                restTemplate.postForObject(userApiUrl, userBody, String.class);
                            } catch (Exception ex) {
                                System.err.println("Failed to call user service: " + ex.getMessage());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:/club/" + clubName + "/requests";
    }

    @GetMapping("/club/{clubName}/requests")
    public String viewClubRequests(@PathVariable String clubName, Model model) {
        // TODO: Fetch pending requests from microservice
        // For now, use empty list
        java.util.List<java.util.Map<String, Object>> pendingRequests = new java.util.ArrayList<>();
        model.addAttribute("requests", pendingRequests);
        // For now, mark the view as club head view so approve/reject buttons are shown.
        // Integrate with your auth later.
        model.addAttribute("isClubHead", true);
        // Provide clubName so the template can post approve/reject actions to the
        // correct club
        model.addAttribute("clubName", clubName);
        return "club-requests";
    }

    @GetMapping("/club/{clubName}/processed-requests")
    public String viewProcessedRequests(@PathVariable String clubName,
            @RequestParam(name = "status", required = false) String status,
            Model model) {
        // TODO: Fetch both accepted and rejected requests from microservice
        // For now, use empty lists
        java.util.List<java.util.Map<String, Object>> acceptedRequests = new java.util.ArrayList<>();
        java.util.List<java.util.Map<String, Object>> rejectedRequests = new java.util.ArrayList<>();
        model.addAttribute("acceptedRequests", acceptedRequests);
        model.addAttribute("rejectedRequests", rejectedRequests);
        model.addAttribute("clubName", clubName);
        // Keep selectedStatus for backward compatibility (not required)
        model.addAttribute("selectedStatus", status == null ? "" : status.toLowerCase());
        model.addAttribute("isClubHead", true);
        return "processed-requests";
    }

    @GetMapping("/club/{clubName}/manage-members")
    public String manageMembers(@PathVariable String clubName, Model model) {
        // TODO: Fetch members from database or microservice
        // For now, use empty list
        java.util.List<java.util.Map<String, Object>> members = new java.util.ArrayList<>();
        model.addAttribute("members", members);
        model.addAttribute("clubName", clubName);
        return "manage-members";
    }

    private List<Map<String, Object>> getClubMembersByName(String clubName) {
        String query = "SELECT * FROM member WHERE club = ?";
        return jdbcTemplate.queryForList(query, clubName);
    }

    public List<Map<String, Object>> getClubEvents(String clubName) {
        String query = "SELECT * FROM event WHERE clubname = ?";
        List<Map<String, Object>> rawEvents = jdbcTemplate.queryForList(query, clubName);
        List<Map<String, Object>> events = new java.util.ArrayList<>();
        for (Map<String, Object> raw : rawEvents) {
            Map<String, Object> event = new java.util.HashMap<>();
            event.put("id", raw.get("id"));
            event.put("eventname", raw.get("eventname"));
            event.put("type", raw.get("type"));
            event.put("loc", raw.get("loc"));
            event.put("clubname", raw.get("clubname"));
            event.put("budget", raw.get("budget"));
            event.put("description", raw.get("description"));
            // Normalize registration link
            event.put("registration_link", raw.get("registrationlink"));
            // Normalize timestamp
            Object ts = raw.get("timestamp");
            if (ts != null && !(ts instanceof java.util.Date)) {
                try {
                    if (ts instanceof String) {
                        event.put("timestamp", java.sql.Timestamp.valueOf(((String) ts).replace('T', ' ')));
                    } else {
                        event.put("timestamp", null);
                    }
                } catch (Exception e) {
                    event.put("timestamp", null);
                }
            } else {
                event.put("timestamp", ts);
            }
            events.add(event);
        }
        return events;
    }
}
