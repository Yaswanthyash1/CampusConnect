package com.controller;

import com.model.Club;
import com.model.MemberRequest;
import com.service.ClubService;
import com.service.MemberRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Controller
public class ClubController {
    @Autowired
    private ClubService clubService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private MemberRequestService memberRequestService;
    @Autowired
    private com.repository.MemberRepository memberRepository;
    @Autowired
    private com.service.MemberService memberService;

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
    public String showAddEventForm() {
        System.out.println("=== Add Event Form Requested ===");
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

    @GetMapping("/faculty/{faculty_id}")
    public String getFacultyClubs(@PathVariable("faculty_id") int facultyId, Model model) {
        try {
            String selectFacultySql = "SELECT * FROM faculty WHERE id = ?";
            Map<String, Object> faculty = jdbcTemplate.queryForMap(selectFacultySql, facultyId);
            String facultyName = (String) faculty.get("faculty_name");
            String selectClubsSql = "SELECT c.*, COUNT(cm.SRN) AS memberCount, SUM(e.budget) AS totalBudget " +
                    "FROM club c " +
                    "LEFT JOIN clubmember cm ON c.clubName = cm.clubName " +
                    "LEFT JOIN event e ON c.clubName = e.clubname " +
                    "WHERE c.faculty_id = ? " +
                    "GROUP BY c.id";
            List<Map<String, Object>> clubs = jdbcTemplate.queryForList(selectClubsSql, facultyId);
            model.addAttribute("facultyName", facultyName);
            model.addAttribute("clubs", clubs);
            return "faculty";
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
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
            int result = jdbcTemplate.update(insertSql, clubName, eventName, description, location, type, Timestamp.valueOf(parsedTimestamp), budget, registrationLink, bannerBytes);
            if (result > 0) {
                model.addAttribute("success", "Event created successfully!");
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
                java.util.Optional<com.model.MemberRequest> opt = memberRequestService.getRequestById(requestId);
                if (opt.isPresent()) {
                    com.model.MemberRequest req = opt.get();
                    if ("accept".equals(action)) {
                        req.setStatus("accepted");
                        req.setClubName(clubName);
                        // Set member's club field
                        String srn = req.getMemberId();
                        if (srn != null) {
                            memberService.setMemberClub(srn, clubName);
                        }
                    } else if ("reject".equals(action)) {
                        req.setStatus("rejected");
                        req.setClubName(clubName);
                    }
                    memberRequestService.saveRequest(req);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:/club/" + clubName + "/requests";
    }

    @GetMapping("/club/{clubName}/requests")
    public String viewClubRequests(@PathVariable String clubName, Model model) {
        // Fetch pending MemberRequest entities for this club so Thymeleaf can access properties like memberId via getters
        List<MemberRequest> pendingRequests = memberRequestService.getRequestsByClubNameAndStatus(clubName, "pending");
        model.addAttribute("requests", pendingRequests);
        // For now, mark the view as club head view so approve/reject buttons are shown. Integrate with your auth later.
        model.addAttribute("isClubHead", true);
        // Provide clubName so the template can post approve/reject actions to the correct club
        model.addAttribute("clubName", clubName);
        return "club-requests";
    }

    @GetMapping("/club/{clubName}/processed-requests")
    public String viewProcessedRequests(@PathVariable String clubName,
                                        @RequestParam(name = "status", required = false) String status,
                                        Model model) {
        // Always fetch both accepted and rejected requests so club head can see both lists
        List<MemberRequest> acceptedRequests = memberRequestService.getRequestsByClubNameAndStatus(clubName, "accepted");
        List<MemberRequest> rejectedRequests = memberRequestService.getRequestsByClubNameAndStatus(clubName, "rejected");
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
        java.util.List<com.model.Member> members = memberRepository.findByClub(clubName);
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
