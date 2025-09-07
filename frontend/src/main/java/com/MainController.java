package com;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
public class MainController {

    private final RestTemplate restTemplate;
    private final String userServiceUrl;
    private final String requestServiceUrl;

    public MainController(RestTemplate restTemplate, @Value("${user.service.url}") String userServiceUrl,
            @Value("${request.service.url}") String requestServiceUrl) {
        this.restTemplate = restTemplate;
        this.userServiceUrl = userServiceUrl;
        this.requestServiceUrl = requestServiceUrl;
    }

    @GetMapping("/")
    public String showHomePage() {
        return "home";
    }

    @GetMapping("/home")
    public String showHome() {
        return "home";
    }

    @GetMapping("/login")
    public String showLoginForm() {
        System.out.println("Rendering login page");
        return "login";
    }

    @GetMapping("/register")
    public String showRegisterForm() {
        return "register";
    }

    @GetMapping("/request")
    public String showRequestForm() {
        return "request";
    }

    @PostMapping("/register")
    public ResponseEntity<?> handleRegister(@RequestBody Map<String, Object> requestBody) {
        String url = userServiceUrl + "/user-service/api/auth/register";
        System.out.println("Forwarding registration request to: " + url);
        System.out.println("Request body: " + requestBody);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestBody, String.class);
            System.out.println("Response from user-service: " + response.getBody());
            return response;
        } catch (Exception e) {
            System.err.println("Error while forwarding registration request: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Couldn't Register. Please Try Again");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> handleLogin(@RequestBody Map<String, Object> requestBody, HttpServletRequest request) {
        String url = userServiceUrl + "/user-service/api/auth/login";
        System.out.println("Forwarding login request to: " + url);
        System.out.println("Request body: " + requestBody);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestBody, String.class);
            System.out.println("Response from user-service: " + response.getBody());

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // Login successful, store user details in session.
                ObjectMapper mapper = new ObjectMapper();
                @SuppressWarnings("unchecked")
                Map<String, Object> userDetails = mapper.readValue(response.getBody(), Map.class);

                HttpSession session = request.getSession(true); // Create session if it doesn't exist
                session.setAttribute("userType", userDetails.get("role"));
                session.setAttribute("userIdentifier", userDetails.get("userIdentifier"));
                session.setAttribute("isAuthenticated", true);
            }

            return response;
        } catch (Exception e) {
            System.err.println("Error while forwarding login request: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Couldn't Register. Please Try Again");
        }
    }

    @GetMapping("/userDetails")
    @ResponseBody
    public ResponseEntity<?> getUserDetails(HttpSession session) {
        Boolean isAuthenticated = (Boolean) session.getAttribute("isAuthenticated");
        if (isAuthenticated != null && isAuthenticated) {
            Map<String, Object> userDetails = Map.of(
                    "userType", session.getAttribute("userType"),
                    "userIdentifier", session.getAttribute("userIdentifier"));
            return ResponseEntity.ok(userDetails);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User is not authenticated");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> handleLogout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/member")
    public String showMemberPage(HttpSession session, Model model) {
        // Get the logged-in user's SRN/username from session
        String userIdentifier = (String) session.getAttribute("userIdentifier");
        Boolean isAuthenticated = (Boolean) session.getAttribute("isAuthenticated");

        if (isAuthenticated == null || !isAuthenticated || userIdentifier == null) {
            return "redirect:/login";
        }

        // Always provide a member object to avoid null pointer errors
        Map<String, Object> memberData = new java.util.HashMap<>();
        memberData.put("name", userIdentifier);
        memberData.put("srn", userIdentifier);
        memberData.put("email", "N/A");
        memberData.put("dept", "N/A");
        memberData.put("club", "N/A");
        memberData.put("domain", "N/A");
        memberData.put("sem", "N/A");
        memberData.put("phoneno", "N/A");
        memberData.put("gender", "N/A");

        try {
            String url = userServiceUrl + "/user-service/api/user/details/srn?srn=" + userIdentifier;
            System.out.println("Fetching user details from: " + url);
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> userData = response.getBody();
                System.out.println("User data received: " + userData);

                // Update memberData with actual data from service
                if (userData.get("name") != null)
                    memberData.put("name", userData.get("name"));
                if (userData.get("srn") != null)
                    memberData.put("srn", userData.get("srn"));
                if (userData.get("email") != null)
                    memberData.put("email", userData.get("email"));
                if (userData.get("dept") != null)
                    memberData.put("dept", userData.get("dept"));
                if (userData.get("club") != null)
                    memberData.put("club", userData.get("club"));
                if (userData.get("domain") != null)
                    memberData.put("domain", userData.get("domain"));
                if (userData.get("sem") != null)
                    memberData.put("sem", userData.get("sem"));
                if (userData.get("phoneno") != null)
                    memberData.put("phoneno", userData.get("phoneno"));
                if (userData.get("gender") != null)
                    memberData.put("gender", userData.get("gender"));
            } else {
                // If SRN lookup fails, try username lookup as fallback
                System.out.println(" I am 2");
                System.out.println("SRN lookup failed, trying username lookup");
                String fallbackUrl = userServiceUrl + "/user-service/api/user/details?username=" + userIdentifier;
                System.out.println("Fetching user details from: " + fallbackUrl);
                ResponseEntity<Map> fallbackResponse = restTemplate.getForEntity(fallbackUrl, Map.class);

                if (fallbackResponse.getStatusCode().is2xxSuccessful() && fallbackResponse.getBody() != null) {
                    Map<String, Object> userData = fallbackResponse.getBody();
                    System.out.println("User data received from fallback: " + userData);

                    // Update memberData with actual data from service
                    if (userData.get("name") != null)
                        memberData.put("name", userData.get("name"));
                    if (userData.get("srn") != null)
                        memberData.put("srn", userData.get("srn"));
                    if (userData.get("email") != null)
                        memberData.put("email", userData.get("email"));
                    if (userData.get("dept") != null)
                        memberData.put("dept", userData.get("dept"));
                    if (userData.get("club") != null)
                        memberData.put("club", userData.get("club"));
                    if (userData.get("domain") != null)
                        memberData.put("domain", userData.get("domain"));
                    if (userData.get("sem") != null)
                        memberData.put("sem", userData.get("sem"));
                    if (userData.get("phoneno") != null)
                        memberData.put("phoneno", userData.get("phoneno"));
                    if (userData.get("gender") != null)
                        memberData.put("gender", userData.get("gender"));
                } else {
                    System.err.println(
                            "Both SRN and username lookup failed. Status: " + fallbackResponse.getStatusCode());

                    // Try one more fallback - see if userIdentifier is actually a user ID

                    try {
                        Long userId = Long.parseLong(userIdentifier);
                        String idUrl = userServiceUrl + "/user-service/api/user/details/id?id=" + userId;
                        System.out.println("Trying ID lookup with: " + idUrl);
                        ResponseEntity<Map> idResponse = restTemplate.getForEntity(idUrl, Map.class);

                        if (idResponse.getStatusCode().is2xxSuccessful() && idResponse.getBody() != null) {
                            Map<String, Object> userData = idResponse.getBody();
                            System.out.println("User data received from ID lookup: " + userData);

                            // Update memberData with actual data from service
                            if (userData.get("name") != null)
                                memberData.put("name", userData.get("name"));
                            if (userData.get("srn") != null)
                                memberData.put("srn", userData.get("srn"));
                            if (userData.get("email") != null)
                                memberData.put("email", userData.get("email"));
                            if (userData.get("dept") != null)
                                memberData.put("dept", userData.get("dept"));
                            if (userData.get("club") != null)
                                memberData.put("club", userData.get("club"));
                            if (userData.get("domain") != null)
                                memberData.put("domain", userData.get("domain"));
                            if (userData.get("sem") != null)
                                memberData.put("sem", userData.get("sem"));
                            if (userData.get("phoneno") != null)
                                memberData.put("phoneno", userData.get("phoneno"));
                            if (userData.get("gender") != null)
                                memberData.put("gender", userData.get("gender"));
                        }
                    } catch (NumberFormatException nfe) {
                        System.err.println("UserIdentifier is not a valid ID: " + userIdentifier);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching member details for dashboard: " + e.getMessage());
            e.printStackTrace();
        }

        // Fetch all clubs for the dropdown
        try {
            String clubServiceUrl = "http://localhost:8082/api/clubs";
            ResponseEntity<java.util.List> clubsResponse = restTemplate.getForEntity(clubServiceUrl,
                    java.util.List.class);
            if (clubsResponse.getStatusCode().is2xxSuccessful() && clubsResponse.getBody() != null) {
                System.out.println("Clubs fetched successfully: " + clubsResponse.getBody());
                model.addAttribute("allClubs", clubsResponse.getBody());
            } else {
                System.err.println("Failed to fetch clubs. Status: " + clubsResponse.getStatusCode());
                model.addAttribute("allClubs", java.util.Collections.emptyList());
            }
        } catch (Exception e) {
            System.err.println("Error fetching clubs: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("allClubs", java.util.Collections.emptyList());
        }

        System.out.println("Member data for template: " + memberData);
        System.out.println("Member club value: " + memberData.get("club"));
        System.out.println("Member club is null: " + (memberData.get("club") == null));
        System.out.println("Member club equals 'N/A': " + "N/A".equals(memberData.get("club")));

        model.addAttribute("member", memberData);
        return "member";
    }

    @PostMapping("/member/apply-club")
    public String applyForClub(@RequestParam String srn, @RequestParam String clubName,
            HttpServletRequest request, RedirectAttributes redirectAttributes) {
        try {
            // Create club enrollment request
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("srn", srn);
            requestData.put("type", "Club Enrollment");
            requestData.put("description", "Request to join " + clubName + " club");
            requestData.put("clubName", clubName);
            requestData.put("status", "pending");

            // Send request to request-service
            String requestServiceUrl = "http://localhost:8083/api/requests";
            ResponseEntity<String> response = restTemplate.postForEntity(requestServiceUrl, requestData, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Club enrollment request submitted successfully!");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Failed to submit club enrollment request.");
            }
        } catch (Exception e) {
            System.err.println("Error submitting club enrollment request: " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Error submitting request: " + e.getMessage());
        }

        return "redirect:/member";
    }

    @GetMapping("/member/{srn}")
    public String showMemberDetails(@PathVariable String srn, Model model) {
        try {
            // Fetch member details from user-service
            String userServiceUrl = "http://localhost:8081/user-service/api/user/details/srn?srn=" + srn;
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.getForEntity(userServiceUrl, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                model.addAttribute("member", response.getBody());
                return "member-details";
            } else {
                return "redirect:/error?message=Could not find member with SRN: " + srn;
            }
        } catch (Exception e) {
            System.err.println("Error fetching member details: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/error?message=Error fetching member details";
        }
    }

    @GetMapping("/admin")
    public String showAdminPage() {
        return "admin";
    }

    @GetMapping("/club/{clubName}")
    public String showClubPage(@PathVariable String clubName, Model model) {
        model.addAttribute("clubName", clubName);
        return "club";
    }

    @GetMapping("/club/{clubName}/requests")
    public String showClubRequests(@PathVariable String clubName, Model model) {
        model.addAttribute("clubName", clubName);
        return "club-requests";
    }

    @GetMapping("/event")
    public String showEventPage() {
        return "event-details";
    }

    @GetMapping("/faculty/{faculty_id}")
    public String showFacultyPage(@PathVariable String faculty_id, Model model) {
        model.addAttribute("faculty_id", faculty_id);
        return "faculty";
    }

    @GetMapping("/events")
    public String showEventsPage() {
        return "events";
    }

    @PostMapping("/submit-request")
    public String handleSubmitRequest(@RequestParam("type") String type,
            @RequestParam("description") String description,
            @RequestParam(value = "memberId", required = false) String memberId,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        HttpSession session = request.getSession(false);
        if (memberId == null || memberId.trim().isEmpty()) {
            if (session != null && Boolean.TRUE.equals(session.getAttribute("isAuthenticated"))) {
                memberId = (String) session.getAttribute("userIdentifier");
            }
        }

        if (file == null || file.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "Please select a file to upload.");
            return "redirect:/request";
        }

        String filename = file.getOriginalFilename();
        if (filename == null) {
            redirectAttributes.addFlashAttribute("message", "Invalid file.");
            return "redirect:/request";
        }

        String ext = filename.contains(".") ? filename.substring(filename.lastIndexOf(".")).toLowerCase() : "";
        if (!(ext.equals(".pdf") || ext.equals(".doc") || ext.equals(".docx") || ext.equals(".txt"))) {
            redirectAttributes.addFlashAttribute("message",
                    "Invalid file type. Only PDF, DOC, DOCX, and TXT are allowed.");
            return "redirect:/request";
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("type", type);
            body.add("description", description);
            if (memberId != null) {
                body.add("memberId", memberId);
            }

            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
            body.add("file", fileResource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            String url = requestServiceUrl + "/submit-request";
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                redirectAttributes.addFlashAttribute("message", "Request submitted successfully.");
                return "redirect:/home";
            } else {
                redirectAttributes.addFlashAttribute("message", "Failed to submit request. Please try again.");
                return "redirect:/request";
            }

        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("message", "File upload failed: " + e.getMessage());
            return "redirect:/request";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Failed to submit request: " + e.getMessage());
            return "redirect:/request";
        }
    }
}
