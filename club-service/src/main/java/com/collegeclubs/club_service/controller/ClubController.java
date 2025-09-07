package com.collegeclubs.club_service.controller;

import com.collegeclubs.club_service.model.Club;
import com.collegeclubs.club_service.service.ClubService;
import com.collegeclubs.club_service.repository.ClubRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.ui.Model;
import java.util.*;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/club-service/api/club")
public class ClubController {
    @Autowired
    private ClubService clubService;
    @Autowired
    private ClubRepository clubRepository; // (can be removed if not used elsewhere)
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // --- Club Registration ---
    @PostMapping("/register")
    public ResponseEntity<String> registerClub(@RequestBody Map<String, Object> clubData) {
        try {
            clubService.registerClub(clubData);
            return ResponseEntity.ok("Club registered successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error registering club: " + e.getMessage());
        }
    }

    // --- Get all clubs (names only) ---
    @GetMapping("/all-names")
    public List<String> getAllClubs() {
        try {
            String sql = "SELECT clubName FROM club";
            return jdbcTemplate.queryForList(sql, String.class);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    // --- Get club by name ---
    @GetMapping("/{clubName}")
    public ResponseEntity<Club> getClubByName(@PathVariable String clubName) {
        try {
            Club club = clubService.getClubDetailsByName(clubName);
            if (club == null) {
                String sql = "SELECT * FROM club WHERE LOWER(clubName) = LOWER(?)";
                List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, clubName);
                if (!results.isEmpty()) {
                    Map<String, Object> clubData = results.get(0);
                    club = new Club();
                    club.setId((Long) clubData.get("id"));
                    club.setClubName((String) clubData.get("clubName"));
                    club.setDescription((String) clubData.get("description"));
                }
            }
            if (club == null)
                return ResponseEntity.notFound().build();
            return ResponseEntity.ok(club);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // --- Club dashboard (HTML, for compatibility) ---
    @GetMapping("/dashboard/{clubName}")
    public String clubDashboard(@PathVariable String clubName, Model model) {
        Club club = clubService.getClubDetailsByName(clubName);
        if (club == null) {
            String sql = "SELECT * FROM club WHERE LOWER(clubName) = LOWER(?)";
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, clubName);
            if (!results.isEmpty()) {
                Map<String, Object> clubData = results.get(0);
                club = new Club();
                club.setId((Long) clubData.get("id"));
                club.setClubName((String) clubData.get("clubName"));
                club.setDescription((String) clubData.get("description"));
            }
        }
        if (club == null) {
            model.addAttribute("message", "Club not found: " + clubName);
            return "club-not-found";
        }
        List<Map<String, Object>> clubMembers = getClubMembersByName(clubName);
        List<Map<String, Object>> clubEvents = getClubEvents(clubName);
        model.addAttribute("club", club);
        model.addAttribute("clubMembers", clubMembers);
        model.addAttribute("clubEvents", clubEvents);
        return "club";
    }

    // --- Club requests (API) ---
    @GetMapping("/{clubName}/requests")
    public List<Map<String, Object>> getClubRequests(@PathVariable String clubName) {
        List<Map<String, Object>> pendingRequests = new ArrayList<>();
        try {
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            String requestServiceUrl = "http://localhost:8083/club-requests";
            org.springframework.http.ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    requestServiceUrl,
                    org.springframework.http.HttpMethod.GET, null,
                    new org.springframework.core.ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> allRequests = response.getBody();
                for (Map<String, Object> request : allRequests) {
                    String requestClub = (String) request.get("clubName");
                    String status = (String) request.get("status");
                    if (requestClub != null && requestClub.trim().equalsIgnoreCase(clubName.trim()) &&
                            status != null && status.trim().equalsIgnoreCase("pending")) {
                        pendingRequests.add(request);
                    }
                }
            }
        } catch (Exception e) {
        }
        return pendingRequests;
    }

    // --- Club requests (update) ---
    @PostMapping("/{clubName}/requests")
    public ResponseEntity<String> handleClubRequests(@PathVariable String clubName, @RequestParam String action,
            @RequestParam(required = false) Long requestId) {
        try {
            if (requestId != null) {
                org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
                String requestServiceUrl = "http://localhost:8083/update-request-status";
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("requestId", requestId);
                requestBody.put("action", action);
                requestBody.put("clubName", clubName);
                restTemplate.postForObject(requestServiceUrl, requestBody, String.class);
            }
            // if request is enrollment and is accpeted by club head then update user's club field in the table via user service
            
            return ResponseEntity.ok("Request processed");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    // --- Processed requests ---
    @GetMapping("/{clubName}/processed-requests")
    public Map<String, Object> getProcessedRequests(@PathVariable String clubName,
            @RequestParam(name = "status", required = false) String status) {
        List<Map<String, Object>> acceptedRequests = new ArrayList<>();
        List<Map<String, Object>> rejectedRequests = new ArrayList<>();
        try {
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            String requestServiceUrl = "http://localhost:8083/club-requests";
            org.springframework.http.ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    requestServiceUrl,
                    org.springframework.http.HttpMethod.GET, null,
                    new org.springframework.core.ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> allRequests = response.getBody();
                for (Map<String, Object> request : allRequests) {
                    String requestClub = (String) request.get("clubName");
                    String requestStatus = (String) request.get("status");
                    boolean clubMatch = (requestClub != null && clubName != null &&
                            clubName.trim().equalsIgnoreCase(requestClub.trim()));
                    if (clubMatch) {
                        if (requestStatus != null && requestStatus.trim().equalsIgnoreCase("accepted")) {
                            acceptedRequests.add(request);
                        } else if (requestStatus != null && requestStatus.trim().equalsIgnoreCase("rejected")) {
                            rejectedRequests.add(request);
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("acceptedRequests", acceptedRequests);
        responseMap.put("rejectedRequests", rejectedRequests);
        responseMap.put("clubName", clubName);
        responseMap.put("selectedStatus", status == null ? "" : status.toLowerCase());
        responseMap.put("isClubHead", true);
        return responseMap;
    }

    // --- Helper methods ---
    private List<Map<String, Object>> getClubMembersByName(String clubName) {
        String query = "SELECT * FROM member WHERE club = ?";
        return jdbcTemplate.queryForList(query, clubName);
    }

    private List<Map<String, Object>> getClubEvents(String clubName) {
        String query = "SELECT * FROM event WHERE clubname = ?";
        List<Map<String, Object>> rawEvents = jdbcTemplate.queryForList(query, clubName);
        List<Map<String, Object>> events = new ArrayList<>();
        for (Map<String, Object> raw : rawEvents) {
            Map<String, Object> event = new HashMap<>();
            event.put("id", raw.get("id"));
            event.put("eventname", raw.get("eventname"));
            event.put("type", raw.get("type"));
            event.put("loc", raw.get("loc"));
            event.put("clubname", raw.get("clubname"));
            event.put("budget", raw.get("budget"));
            event.put("description", raw.get("description"));
            event.put("registration_link", raw.get("registrationlink"));
            event.put("timestamp", raw.get("timestamp"));
            events.add(event);
        }
        return events;
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

}
