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
        if (userIdentifier != null) {
            try {
                String url = userServiceUrl + "/user-service/api/user/details?username=" + userIdentifier;
                ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    model.addAttribute("member", response.getBody());
                }
            } catch (Exception e) {
                System.err.println("Error fetching member details for dashboard: " + e.getMessage());
            }
        }
        return "member";
    }

    @GetMapping("/member/{srn}")
    public String showMemberDetails(@PathVariable String srn, Model model) {
        try {
            // Fetch member details from user-service
            String userServiceUrl = "http://localhost:8081/user-service/api/user/details?username=" + srn;
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
