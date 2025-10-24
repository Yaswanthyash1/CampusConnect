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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class MainController {

    private final RestTemplate restTemplate;
    private final String userServiceUrl;
    private final String requestServiceUrl;
    private final String projectServiceBaseUrl;
    private final String eventServiceUrl;

    public MainController(RestTemplate restTemplate, @Value("${user.service.url}") String userServiceUrl,
            @Value("${request.service.url}") String requestServiceUrl,
            @Value("${project.service.url}") String projectServiceBaseUrl,
            @Value("${event.service.url}") String eventServiceUrl) {
        this.restTemplate = restTemplate;
        this.userServiceUrl = userServiceUrl;
        this.requestServiceUrl = requestServiceUrl;
        this.projectServiceBaseUrl = projectServiceBaseUrl;
        this.eventServiceUrl = eventServiceUrl;
    }

    @GetMapping("/")
    public String showHomePage(HttpSession session, Model model) {
        addClubNameIfClubHead(session, model);
        return "home";
    }

    @GetMapping("/home")
    public String showHome(HttpSession session, Model model) {
        addClubNameIfClubHead(session, model);
        return "home";
    }

    private void addClubNameIfClubHead(HttpSession session, Model model) {
        String userType = (String) session.getAttribute("userType");
        String userIdentifier = (String) session.getAttribute("userIdentifier");
        Boolean isAuthenticated = (Boolean) session.getAttribute("isAuthenticated");

        // Set default values to avoid null pointer issues in template
        model.addAttribute("isClubHead", false);
        model.addAttribute("clubName", null);

        if (isAuthenticated != null && isAuthenticated && "clubHead".equalsIgnoreCase(userType)
                && userIdentifier != null) {
            // For club heads, userIdentifier is the club name itself
            model.addAttribute("clubName", userIdentifier);
            model.addAttribute("isClubHead", true);
        }
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

    // Show Add Event form from frontend (delegates creation to event-service @ 8085)
    @GetMapping("/add-event")
    public String showAddEventForm() {
        return "add-event";
    }

    @GetMapping("/add-project")
    public String showAddProjectForm(Model model) {
        return "add-project";
    }

    // Forward Add Event submission to event-service (multipart)
    @PostMapping("/addEvent")
    public String handleAddEvent(@RequestParam("clubname") String clubName,
                                 @RequestParam("eventname") String eventName,
                                 @RequestParam("description") String description,
                                 @RequestParam("location") String location,
                                 @RequestParam("type") String type,
                                 @RequestParam("timestamp") String timestamp,
                                 @RequestParam("budget") double budget,
                                 @RequestParam("registrationlink") String registrationLink,
                                 @RequestParam(value = "banner", required = false) MultipartFile banner,
                                 @RequestParam(value = "fromRequest", required = false) String fromRequest,
                                 RedirectAttributes redirectAttributes) {
        try {
            String url = eventServiceUrl + "/api/events";

            // Build multipart body
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("clubname", clubName);
            body.add("eventname", eventName);
            body.add("description", description);
            body.add("location", location);
            body.add("type", type);
            body.add("timestamp", timestamp);
            body.add("budget", String.valueOf(budget));
            body.add("registrationlink", registrationLink);
            if (fromRequest != null && !fromRequest.isBlank()) {
                body.add("fromRequest", fromRequest);
            }

            if (banner != null && !banner.isEmpty()) {
                ByteArrayResource fileResource = new ByteArrayResource(banner.getBytes()) {
                    @Override
                    public String getFilename() {
                        String name = banner.getOriginalFilename();
                        return name != null ? name : "banner";
                    }
                };
                body.add("banner", fileResource);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful() || response.getStatusCode().is3xxRedirection()) {
                redirectAttributes.addFlashAttribute("success", "Event created successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Failed to create event. Please try again.");
            }
        } catch (Exception e) {
            System.err.println("Error forwarding add event request: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error creating event: " + e.getMessage());
        }

        return "redirect:/add-event";
    }

    @PostMapping("/addProject")
    public String handleAddProject(@RequestParam("clubname") String clubName,
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
                               @RequestParam(value = "fromRequest", required = false) String fromRequest,
                               RedirectAttributes redirectAttributes) {
        try {
            String projectServiceUrl = projectServiceBaseUrl + "/addProject";

            // Create multipart request
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("clubname", clubName);
            body.add("projectname", projectName);
            body.add("description", description);
            body.add("category", category);
            body.add("priority", priority);
            body.add("startdate", startDate);
            body.add("enddate", endDate);
            body.add("budget", budget);
            body.add("teamsize", teamSize);
            body.add("technologies", technologies);
            body.add("objectives", objectives);
            body.add("deliverables", deliverables);
            body.add("mentor", mentor);
            body.add("status", status);

            // Add attachments if present
            if (attachments != null) {
                for (MultipartFile attachment : attachments) {
                    if (!attachment.isEmpty()) {
                        ByteArrayResource fileResource = new ByteArrayResource(attachment.getBytes()) {
                            @Override
                            public String getFilename() {
                                return attachment.getOriginalFilename();
                            }
                        };
                        // When using RestTemplate, set content-disposition by wrapping resource in HttpEntity if needed.
                        body.add("attachments", fileResource);
                    }
                }
            }

            // Set headers for multipart request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // Create the request entity
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            System.out.println(requestEntity);

            // Forward request to project service
            ResponseEntity<String> response = restTemplate.postForEntity(projectServiceUrl, requestEntity, String.class);

            // Treat 2xx and 3xx as success (project created / redirect). Otherwise show error.
            if (response.getStatusCode().is2xxSuccessful() || response.getStatusCode().is3xxRedirection()) {
                redirectAttributes.addFlashAttribute("success", "Project created successfully!");

                // If this project was created from a request, mark that request as completed
                if (fromRequest != null && !fromRequest.trim().isEmpty()) {
                    try {
                        Long requestId = Long.parseLong(fromRequest);
                        Map<String, Object> completeData = new HashMap<>();
                        completeData.put("requestId", requestId);
                        completeData.put("action", "complete");
                        completeData.put("is_completed", 1);

                        String completeUrl = requestServiceUrl + "/update-request-status";
                        ResponseEntity<String> completeResp = restTemplate.postForEntity(completeUrl, completeData, String.class);

                        if (completeResp.getStatusCode().is2xxSuccessful()) {
                            System.out.println("Successfully marked request " + requestId + " as completed after project creation");
                        } else {
                            System.err.println("Failed to mark request " + requestId + " as completed: " + completeResp.getBody());
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid fromRequest ID: " + fromRequest);
                    } catch (Exception e) {
                        System.err.println("Error marking request as completed: " + e.getMessage());
                    }
                }
            } else {
                redirectAttributes.addFlashAttribute("error", "Failed to create project. Please try again.");
            }
        } catch (Exception e) {
            System.err.println("Error forwarding project creation request: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error creating project: " + e.getMessage());
        }

        return "redirect:/add-project";
    }

    @GetMapping("/projects-dashboard")
    public String showProjectsDashboard(Model model) {
        try {
            String projectsUrl = projectServiceBaseUrl + "/projects-dashboard";
            ResponseEntity<java.util.Map> response = restTemplate.getForEntity(projectsUrl, java.util.Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // The project-service returns a JSON map with upcomingProjects and pastProjects
                Object upcoming = response.getBody().get("upcomingProjects");
                Object past = response.getBody().get("pastProjects");

                // Convert lists of maps so date fields become java.util.Date for Thymeleaf formatting
                java.util.List<java.util.Map<String, Object>> upcomingList = new java.util.ArrayList<>();
                java.util.List<java.util.Map<String, Object>> pastList = new java.util.ArrayList<>();

                if (upcoming instanceof java.util.List) {
                    for (Object item : (java.util.List) upcoming) {
                        if (item instanceof java.util.Map) {
                            java.util.Map<String, Object> map = new java.util.HashMap<>((java.util.Map) item);
                            convertDateFields(map);
                            upcomingList.add(map);
                        }
                    }
                }

                if (past instanceof java.util.List) {
                    for (Object item : (java.util.List) past) {
                        if (item instanceof java.util.Map) {
                            java.util.Map<String, Object> map = new java.util.HashMap<>((java.util.Map) item);
                            convertDateFields(map);
                            pastList.add(map);
                        }
                    }
                }

                model.addAttribute("upcomingProjects", upcomingList);
                model.addAttribute("pastProjects", pastList);
                return "projects-dashboard";
            } else {
                model.addAttribute("error", "Failed to fetch projects dashboard");
                return "error";
            }
        } catch (Exception e) {
            System.err.println("Error fetching projects dashboard: " + e.getMessage());
            model.addAttribute("error", "Error accessing projects dashboard");
            return "error";
        }
    }

    // Keep compatibility with pages or redirects that use `/projects` by redirecting
    // them to the frontend's projects dashboard which is backed by project-service.
    @GetMapping("/projects")
    public String redirectProjectsToDashboard() {
        return "redirect:/projects-dashboard";
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

    // Forward status update form from frontend to project-service
    @PostMapping("/project/update-status")
    public String handleUpdateProjectStatus(@RequestParam("projectId") Long projectId,
                                            @RequestParam("status") String status,
                                            RedirectAttributes redirectAttributes,
                                            HttpServletRequest request) {
        try {
            String url = projectServiceBaseUrl + "/project/update-status";

            org.springframework.util.LinkedMultiValueMap<String, String> body = new org.springframework.util.LinkedMultiValueMap<>();
            body.add("projectId", String.valueOf(projectId));
            body.add("status", status);

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);

            org.springframework.http.HttpEntity<org.springframework.util.MultiValueMap<String, String>> requestEntity =
                    new org.springframework.http.HttpEntity<>(body, headers);

            org.springframework.http.ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful() || response.getStatusCode().is3xxRedirection()) {
                redirectAttributes.addFlashAttribute("success", "Project status updated successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Failed to update project status. Please try again.");
            }
        } catch (Exception e) {
            System.err.println("Error forwarding update-status request: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error updating project status: " + e.getMessage());
        }

        // Try to redirect back to the page the user came from (Referer). If not available,
        // default to the projects dashboard which exists in the frontend.
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isEmpty()) {
            try {
                java.net.URI uri = new java.net.URI(referer);
                String path = uri.getPath();
                String query = uri.getQuery();
                String redirectTarget = (query != null && !query.isEmpty()) ? path + "?" + query : path;
                // Avoid redirecting to external hosts; only allow same-origin paths
                if (redirectTarget != null && !redirectTarget.isEmpty()) {
                    return "redirect:" + redirectTarget;
                }
            } catch (Exception ignored) {
                // fall through to default
            }
        }

        return "redirect:/projects-dashboard";
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
                if (userData.get("club") != null && !userData.get("club").toString().trim().isEmpty()) {
                    String clubValue = userData.get("club").toString().trim();
                    System.out.println("DEBUG: User has club from service: " + clubValue);
                    memberData.put("club", clubValue);
                } else {
                    System.out.println("DEBUG: User club from service is null or empty");
                }
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
                    if (userData.get("club") != null && !userData.get("club").toString().trim().isEmpty()) {
                        String clubValue = userData.get("club").toString().trim();
                        System.out.println("DEBUG: User has club from fallback service: " + clubValue);
                        memberData.put("club", clubValue);
                    } else {
                        System.out.println("DEBUG: User club from fallback service is null or empty");
                    }
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
                            if (userData.get("club") != null && !userData.get("club").toString().trim().isEmpty()) {
                                String clubValue = userData.get("club").toString().trim();
                                System.out.println("DEBUG: User has club from ID lookup service: " + clubValue);
                                memberData.put("club", clubValue);
                            } else {
                                System.out.println("DEBUG: User club from ID lookup service is null or empty");
                            }
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

        // Fetch all clubs for the dropdown (use unified club API)
        try {
            String clubServiceUrl = "http://localhost:8082/club-service/api/club/all-names";
            ResponseEntity<java.util.List<String>> clubsResponse = restTemplate.exchange(
                    clubServiceUrl,
                    org.springframework.http.HttpMethod.GET,
                    null,
                    new org.springframework.core.ParameterizedTypeReference<java.util.List<String>>() {
                    });
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

        // Fetch member's requests
        try {
            String requestServiceUrl = "http://localhost:8083/my-requests?memberId=" + userIdentifier;

            // Use ParameterizedTypeReference to properly deserialize the response
            ResponseEntity<java.util.List<java.util.Map<String, Object>>> requestsResponse = restTemplate.exchange(
                    requestServiceUrl,
                    org.springframework.http.HttpMethod.GET,
                    null,
                    new org.springframework.core.ParameterizedTypeReference<java.util.List<java.util.Map<String, Object>>>() {
                    });

            if (requestsResponse.getStatusCode().is2xxSuccessful() && requestsResponse.getBody() != null) {
                // Process the requests to ensure proper format for the template
                java.util.List<java.util.Map<String, Object>> processedRequests = new java.util.ArrayList<>();

                for (java.util.Map<String, Object> req : requestsResponse.getBody()) {
                    // Create a new map with the correct property names
                    java.util.Map<String, Object> processedReq = new java.util.HashMap<>(req);

                    // Fix the isCompleted field
                    if (req.containsKey("completed")) {
                        processedReq.put("completed", req.get("completed"));
                    } else {
                        processedReq.put("completed", false);
                    }

                    // Format timestamp if needed
                    if (req.containsKey("timestamp") && req.get("timestamp") != null) {
                        // Keep timestamp as is - we'll handle formatting in the template
                        processedReq.put("timestamp", req.get("timestamp"));
                    } else {
                        processedReq.put("timestamp", "N/A");
                    }

                    processedRequests.add(processedReq);
                }

                model.addAttribute("requests", processedRequests);
                System.out.println("Member requests fetched: " + processedRequests.size());
            } else {
                model.addAttribute("requests", java.util.Collections.emptyList());
            }
        } catch (

        Exception e) {
            System.err.println("Error fetching member requests: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("requests", java.util.Collections.emptyList());
            model.addAttribute("requests", java.util.Collections.emptyList());
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
                        "Club enrollment request submitted successfully! It has been sent to " + clubName
                                + " club head for approval.");
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
        System.out.println("DEBUG: Frontend showClubPage called for club: " + clubName);

        // Fetch club details from unified club API
        try {
            String clubDetailsUrl = "http://localhost:8082/club-service/api/club/" + clubName;
            ResponseEntity<Map> response = restTemplate.getForEntity(clubDetailsUrl, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                model.addAttribute("club", response.getBody());
                System.out.println("DEBUG: Club details fetched successfully");
            } else {
                model.addAttribute("club", java.util.Collections.emptyMap());
                System.out.println("DEBUG: Failed to fetch club details");
            }
        } catch (Exception e) {
            System.err.println("Error fetching club details: " + e.getMessage());
            model.addAttribute("club", java.util.Collections.emptyMap());
        }

        // Fetch club members from user-service
        java.util.List<java.util.Map<String, Object>> members = new java.util.ArrayList<>();
        try {
            String membersUrl = "http://localhost:8081/user-service/api/user/club-members?clubName=" + clubName;
            System.out.println("DEBUG: Calling user-service for members: " + membersUrl);

            ResponseEntity<java.util.List<java.util.Map<String, Object>>> membersResponse = restTemplate.exchange(
                    membersUrl,
                    org.springframework.http.HttpMethod.GET,
                    null,
                    new org.springframework.core.ParameterizedTypeReference<java.util.List<java.util.Map<String, Object>>>() {
                    });

            if (membersResponse.getStatusCode().is2xxSuccessful() && membersResponse.getBody() != null) {
                members = membersResponse.getBody();
                System.out.println("DEBUG: Successfully fetched " + members.size() + " members for club: " + clubName);

                // Debug each member
                for (java.util.Map<String, Object> member : members) {
                    System.out.println("DEBUG: Member - Name: " + member.get("name") +
                            ", SRN: " + member.get("srn") +
                            ", Club: " + member.get("club"));
                }
            } else {
                System.err.println("DEBUG: Failed to fetch members. Status: " + membersResponse.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println("ERROR: Failed to fetch club members: " + e.getMessage());
            e.printStackTrace();
        }

        model.addAttribute("members", members);
        model.addAttribute("clubName", clubName);
        System.out.println("DEBUG: Returning club view with " + members.size() + " members");

        return "club";
    }

    @GetMapping("/club/{clubName}/requests")
    public String showClubRequests(@PathVariable String clubName, Model model) {
        // Fetch club requests from unified club API
        try {
            String clubRequestsUrl = "http://localhost:8082/club-service/api/club/" + clubName + "/requests";
            ResponseEntity<java.util.List<java.util.Map<String, Object>>> requestsResponse = restTemplate.exchange(
                    clubRequestsUrl,
                    org.springframework.http.HttpMethod.GET,
                    null,
                    new org.springframework.core.ParameterizedTypeReference<java.util.List<java.util.Map<String, Object>>>() {
                    });
            if (requestsResponse.getStatusCode().is2xxSuccessful() && requestsResponse.getBody() != null) {
                model.addAttribute("requests", requestsResponse.getBody());
            } else {
                model.addAttribute("requests", java.util.Collections.emptyList());
            }
        } catch (Exception e) {
            System.err.println("Error fetching club requests: " + e.getMessage());
            model.addAttribute("requests", java.util.Collections.emptyList());
        }
        model.addAttribute("clubName", clubName);
        model.addAttribute("isClubHead", true);
        return "club-requests";
    }

    @PostMapping("/club/{clubName}/requests")
    public String handleClubRequests(@PathVariable String clubName,
            @RequestParam String action,
            @RequestParam(required = false) Long requestId,
            RedirectAttributes redirectAttributes) {
        try {
            System.out.println("DEBUG: Frontend handling club request for club: " + clubName +
                    ", action: " + action + ", requestId: " + requestId);

            // Forward the POST request to unified club API
            String clubServiceUrl = "http://localhost:8082/club-service/api/club/" + clubName + "/requests";

            // Prepare the request body
            MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();
            requestBody.add("action", action);
            if (requestId != null) {
                requestBody.add("requestId", requestId);
            }

            // Create headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            // Create the HTTP entity
            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // Make the POST request to club-service
            ResponseEntity<String> response = restTemplate.postForEntity(clubServiceUrl, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("Successfully forwarded request to club-service");
                redirectAttributes.addFlashAttribute("successMessage",
                        "Request " + action + "ed successfully!");
            } else {
                System.err.println("Error forwarding to club-service: " + response.getStatusCode());
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Failed to " + action + " request. Please try again.");
            }

        } catch (Exception e) {
            System.err.println("Exception forwarding POST to club-service: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage",
                    "An error occurred while processing the request. Please try again.");
        }

        return "redirect:/club/" + clubName + "/requests";
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

    @GetMapping("/events-dashboard")
    public String showEventsDashboard(Model model) {
        try {
            // Fetch upcoming events from event-service
            String upcomingUrl = eventServiceUrl + "/events/upcoming";
            ResponseEntity<java.util.List<java.util.Map<String, Object>>> upcomingResponse = restTemplate.exchange(
                    upcomingUrl,
                    org.springframework.http.HttpMethod.GET,
                    null,
                    new org.springframework.core.ParameterizedTypeReference<java.util.List<java.util.Map<String, Object>>>() {
                    });

            // Fetch past events from event-service
            String pastUrl = eventServiceUrl + "/events/past";
            ResponseEntity<java.util.List<java.util.Map<String, Object>>> pastResponse = restTemplate.exchange(
                    pastUrl,
                    org.springframework.http.HttpMethod.GET,
                    null,
                    new org.springframework.core.ParameterizedTypeReference<java.util.List<java.util.Map<String, Object>>>() {
                    });

            java.util.List<java.util.Map<String, Object>> upcomingEvents = upcomingResponse.getStatusCode().is2xxSuccessful() && upcomingResponse.getBody() != null
                    ? upcomingResponse.getBody()
                    : java.util.Collections.emptyList();

            java.util.List<java.util.Map<String, Object>> pastEvents = pastResponse.getStatusCode().is2xxSuccessful() && pastResponse.getBody() != null
                    ? pastResponse.getBody()
                    : java.util.Collections.emptyList();

            // Convert timestamp fields to java.util.Date so Thymeleaf #dates.format works
            try {
                for (java.util.Map<String, Object> ev : upcomingEvents) {
                    convertDateFieldsIfNeeded(ev, "timestamp");
                }
                for (java.util.Map<String, Object> ev : pastEvents) {
                    convertDateFieldsIfNeeded(ev, "timestamp");
                }
            } catch (Exception ignored) {
            }

            model.addAttribute("upcomingEvents", upcomingEvents);
            model.addAttribute("pastEvents", pastEvents);

        } catch (Exception e) {
            System.err.println("Error fetching events for dashboard: " + e.getMessage());
            e.printStackTrace();
            // Provide empty lists to avoid template errors
            model.addAttribute("upcomingEvents", java.util.Collections.emptyList());
            model.addAttribute("pastEvents", java.util.Collections.emptyList());
        }
        
        return "events-dashboard";
    }

    @GetMapping("/pending-tasks")
    public String showPendingTasks(Model model, HttpSession session) {
        String userIdentifier = (String) session.getAttribute("userIdentifier");
        Boolean isAuthenticated = (Boolean) session.getAttribute("isAuthenticated");
        String userType = (String) session.getAttribute("userType");

        if (isAuthenticated == null || !isAuthenticated || userIdentifier == null) {
            return "redirect:/login";
        }

        // Check if user is a club head or admin
        if (userType == null || (!userType.equalsIgnoreCase("clubHead") && !userType.equalsIgnoreCase("admin"))) {
            return "redirect:/home";
        }

        try {
            // Determine the club the user is head of. If the userType is "clubHead",
            // the session's userIdentifier is stored as the club name (frontend
            // convention),
            // so prefer that to avoid a failed SRN lookup. Otherwise try the user-service.
            String clubName = null;
            if (userType != null && userType.equalsIgnoreCase("clubHead") && userIdentifier != null) {
                clubName = userIdentifier;
                System.out.println("Club head (from session) club: " + clubName);
            } else {
                try {
                    String userUrl = userServiceUrl + "/user-service/api/user/details/srn?srn=" + userIdentifier;
                    ResponseEntity<Map> userResponse = restTemplate.getForEntity(userUrl, Map.class);
                    if (userResponse.getStatusCode().is2xxSuccessful() && userResponse.getBody() != null) {
                        clubName = (String) userResponse.getBody().get("club");
                        System.out.println("Club head's club (from user-service): " + clubName);
                    } else {
                        System.err.println(
                                "Failed to fetch user details for club lookup: " + userResponse.getStatusCode());
                    }
                } catch (Exception e) {
                    System.err.println("Error calling user-service for club lookup: " + e.getMessage());
                }
            }

            // Fetch all requests from request-service
            String requestUrl = "http://localhost:8083/club-requests";
            ResponseEntity<java.util.List<java.util.Map<String, Object>>> requestsResponse = restTemplate.exchange(
                    requestUrl,
                    org.springframework.http.HttpMethod.GET,
                    null,
                    new org.springframework.core.ParameterizedTypeReference<java.util.List<java.util.Map<String, Object>>>() {
                    });

            if (requestsResponse.getStatusCode().is2xxSuccessful() && requestsResponse.getBody() != null) {
                java.util.List<java.util.Map<String, Object>> allRequests = requestsResponse.getBody();

                System.out.println("DEBUG: Total requests fetched: " + allRequests.size());

                // Categorize requests by type
                java.util.List<java.util.Map<String, Object>> ideas = new java.util.ArrayList<>();
                java.util.List<java.util.Map<String, Object>> queries = new java.util.ArrayList<>();
                java.util.List<java.util.Map<String, Object>> projects = new java.util.ArrayList<>();
                java.util.List<java.util.Map<String, Object>> events = new java.util.ArrayList<>();
                java.util.List<java.util.Map<String, Object>> enrollments = new java.util.ArrayList<>();

                for (java.util.Map<String, Object> request : allRequests) {
                    String requestClub = request.get("clubName") == null ? null : request.get("clubName").toString();
                    String status = request.get("status") == null ? null : request.get("status").toString();
                    String type = request.get("type") == null ? null : request.get("type").toString();
                    Object isCompletedObj = null;
                    // The request-service may return completion in different shapes/names
                    if (request.containsKey("is_completed")) {
                        isCompletedObj = request.get("is_completed");
                    } else if (request.containsKey("isCompleted")) {
                        isCompletedObj = request.get("isCompleted");
                    } else if (request.containsKey("completed")) {
                        isCompletedObj = request.get("completed");
                    } else {
                        isCompletedObj = null;
                    }

                    // Normalize completed -> a String for logging/compatibility
                    String is_completed = (isCompletedObj == null) ? null : isCompletedObj.toString();

                    boolean clubMatch = (clubName != null && requestClub != null &&
                            requestClub.trim().equalsIgnoreCase(clubName.trim()));
                    boolean isPending = (status != null && status.trim().equalsIgnoreCase("pending"));
                    // treat null as not completed. If field present, interpret boolean/number/string properly
                    boolean isNotCompleted;
                    if (isCompletedObj == null) {
                        isNotCompleted = true;
                    } else if (isCompletedObj instanceof Boolean) {
                        isNotCompleted = !((Boolean) isCompletedObj);
                    } else if (isCompletedObj instanceof Number) {
                        isNotCompleted = ((Number) isCompletedObj).intValue() != 1;
                    } else {
                        String s = isCompletedObj.toString();
                        isNotCompleted = !("1".equals(s) || "true".equalsIgnoreCase(s));
                    }
                    boolean isAcceptedAndNotCompleted = (status != null && status.trim().equalsIgnoreCase("accepted")
                            && isNotCompleted);

                    if (clubMatch && (isPending || isAcceptedAndNotCompleted)) {
                        if (type != null) {
                            if (type.equalsIgnoreCase("idea")) {
                                ideas.add(request);
                            } else if (type.equalsIgnoreCase("query")) {
                                queries.add(request);
                            } else if (type.equalsIgnoreCase("project")) {
                                projects.add(request);
                            } else if (type.equalsIgnoreCase("event")) {
                                events.add(request);
                            } else if (type.equalsIgnoreCase("Club Enrollment")) {
                                enrollments.add(request);
                            }
                        }
                    }
                }

                System.out.println("DEBUG: Filtered requests - ideas: " + ideas.size() +
                        ", queries: " + queries.size() +
                        ", projects: " + projects.size() +
                        ", events: " + events.size() +
                        ", enrollments: " + enrollments.size());

                model.addAttribute("ideas", ideas);
                model.addAttribute("queries", queries);
                model.addAttribute("projects", projects);
                model.addAttribute("events", events);
                model.addAttribute("enrollments", enrollments);
            } else {
                model.addAttribute("ideas", java.util.Collections.emptyList());
                model.addAttribute("queries", java.util.Collections.emptyList());
                model.addAttribute("projects", java.util.Collections.emptyList());
                model.addAttribute("events", java.util.Collections.emptyList());
                model.addAttribute("enrollments", java.util.Collections.emptyList());
            }

        } catch (Exception e) {
            System.err.println("Error fetching pending tasks: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("ideas", java.util.Collections.emptyList());
            model.addAttribute("queries", java.util.Collections.emptyList());
            model.addAttribute("projects", java.util.Collections.emptyList());
            model.addAttribute("events", java.util.Collections.emptyList());
            model.addAttribute("enrollments", java.util.Collections.emptyList());
        }

        return "pending-tasks";
    }

    // Show processed (accepted/rejected) requests for a club
    @GetMapping("/club/{clubName}/processed-requests")
    public String showProcessedRequests(
            @PathVariable String clubName,
            @RequestParam(name = "status", required = false) String status,
            Model model,
            HttpSession session) {

        // Security check: Ensure the logged-in user is the head of this club
        String userIdentifier = (String) session.getAttribute("userIdentifier");
        String userType = (String) session.getAttribute("userType");
        Boolean isAuthenticated = (Boolean) session.getAttribute("isAuthenticated");

        System.out.println("DEBUG showProcessedRequests: userIdentifier=" + userIdentifier +
                ", userType=" + userType + ", isAuthenticated=" + isAuthenticated +
                ", clubName=" + clubName);

        if (isAuthenticated == null || !isAuthenticated ||
                (userType == null
                        || (!userType.equalsIgnoreCase("clubHead") && !userType.equalsIgnoreCase("clubHead")))) {
            System.out.println("DEBUG: Authentication or userType check failed, redirecting to login");
            return "redirect:/login"; // Or an error page
        }

        try {
            // For club heads, the userIdentifier is actually the club name
            // So we can skip the user service call and directly verify the club
            String headOfClub = userIdentifier; // userIdentifier is the club name for club heads

            System.out.println("DEBUG: Club head's club: '" + headOfClub + "', requested clubName: '" + clubName + "'");

            if (headOfClub == null) {
                System.out.println("DEBUG: User has no club assigned, redirecting to home");
                return "redirect:/home";
            }

            if (!clubName.equalsIgnoreCase(headOfClub)) {
                // If not the head, redirect or show an error
                System.out.println("DEBUG: Club mismatch - user's club: '" + headOfClub + "', requested: '" + clubName
                        + "', redirecting to home");
                return "redirect:/home";
            }
            System.out.println("DEBUG: Club verification passed, proceeding to fetch processed requests");

            // Call club-service to get processed requests for this club
            String clubServiceUrl = "http://localhost:8082/club-service/api/club/" + clubName + "/processed-requests";
            if (status != null && !status.isEmpty()) {
                clubServiceUrl += "?status=" + status;
            }

            System.out.println("DEBUG: Calling club service: " + clubServiceUrl);

            // Use ParameterizedTypeReference for safe deserialization
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    clubServiceUrl,
                    org.springframework.http.HttpMethod.GET,
                    null,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                    });

            System.out.println("DEBUG: Club service response status: " + response.getStatusCode());
            System.out.println("DEBUG: Club service response body: " + response.getBody());

            Map<String, Object> data = (response.getStatusCode().is2xxSuccessful() && response.getBody() != null)
                    ? response.getBody()
                    : new java.util.HashMap<>();

            // Process timestamps for accepted requests
            if (data.get("acceptedRequests") instanceof java.util.List) {
                java.util.List<Map<String, Object>> accepted = (java.util.List<Map<String, Object>>) data
                        .get("acceptedRequests");
                for (Map<String, Object> req : accepted) {
                    if (req.get("timestamp") instanceof String) {
                        try {
                            // Parse timestamp with timezone support
                            String timestampStr = (String) req.get("timestamp");
                            java.time.OffsetDateTime offsetDateTime = java.time.OffsetDateTime.parse(timestampStr);
                            // Convert to LocalDateTime for display
                            req.put("timestamp", offsetDateTime.toLocalDateTime());
                        } catch (java.time.format.DateTimeParseException e) {
                            System.err.println("Could not parse timestamp: " + req.get("timestamp"));
                            // Keep the original string if parsing fails
                            req.put("timestamp", req.get("timestamp"));
                        }
                    }
                }
            }

            // Process timestamps for rejected requests
            if (data.get("rejectedRequests") instanceof java.util.List) {
                java.util.List<Map<String, Object>> rejected = (java.util.List<Map<String, Object>>) data
                        .get("rejectedRequests");
                for (Map<String, Object> req : rejected) {
                    if (req.get("timestamp") instanceof String) {
                        try {
                            // Parse timestamp with timezone support
                            String timestampStr = (String) req.get("timestamp");
                            java.time.OffsetDateTime offsetDateTime = java.time.OffsetDateTime.parse(timestampStr);
                            // Convert to LocalDateTime for display
                            req.put("timestamp", offsetDateTime.toLocalDateTime());
                        } catch (java.time.format.DateTimeParseException e) {
                            System.err.println("Could not parse timestamp: " + req.get("timestamp"));
                            // Keep the original string if parsing fails
                            req.put("timestamp", req.get("timestamp"));
                        }
                    }
                }
            }

            model.addAttribute("acceptedRequests",
                    data.getOrDefault("acceptedRequests", java.util.Collections.emptyList()));
            model.addAttribute("rejectedRequests",
                    data.getOrDefault("rejectedRequests", java.util.Collections.emptyList()));
            model.addAttribute("clubName", data.getOrDefault("clubName", clubName));
            model.addAttribute("selectedStatus", data.getOrDefault("selectedStatus", status == null ? "" : status));
            model.addAttribute("isClubHead", true); // Since we've verified it

        } catch (Exception e) {
            System.err.println("Error fetching processed requests: " + e.getMessage());
            e.printStackTrace(); // For better debugging
            model.addAttribute("acceptedRequests", java.util.Collections.emptyList());
            model.addAttribute("rejectedRequests", java.util.Collections.emptyList());
            model.addAttribute("clubName", clubName);
            model.addAttribute("selectedStatus", status == null ? "" : status);
            model.addAttribute("isClubHead", false);
        }
        return "processed-requests";
    }

    @PostMapping("/handle-enrollment-request")
    @ResponseBody
    public ResponseEntity<String> handleEnrollmentRequest(
            @RequestParam("requestId") Long requestId,
            @RequestParam("action") String action,
            HttpSession session) {

        System.out.println("Handling enrollment request: requestId=" + requestId + ", action=" + action);

        String userIdentifier = (String) session.getAttribute("userIdentifier");
        Boolean isAuthenticated = (Boolean) session.getAttribute("isAuthenticated");
        String userType = (String) session.getAttribute("userType");

        if (isAuthenticated == null || !isAuthenticated || userIdentifier == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User is not authenticated");
        }

        // Check if user is a club head or admin
        if (userType == null || (!userType.equalsIgnoreCase("clubHead") && !userType.equalsIgnoreCase("admin"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User does not have permission");
        }
        try {
            // Get the club the user is head of
            String userUrl = userServiceUrl + "/user-service/api/user/details/srn?srn=" + userIdentifier;
            ResponseEntity<Map> userResponse = restTemplate.getForEntity(userUrl, Map.class);

            String clubName = null;
            if (userResponse.getStatusCode().is2xxSuccessful() && userResponse.getBody() != null) {
                clubName = (String) userResponse.getBody().get("club");
                System.out.println("Club head's club: " + clubName);
            }

            if (clubName == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Club head is not associated with any club");
            }

            // First, get the request details to verify it's for this club and to get member
            // ID
            String requestUrl = "http://localhost:8083/club-requests";
            ResponseEntity<java.util.List<java.util.Map<String, Object>>> requestsResponse = restTemplate.exchange(
                    requestUrl,
                    org.springframework.http.HttpMethod.GET,
                    null,
                    new org.springframework.core.ParameterizedTypeReference<java.util.List<java.util.Map<String, Object>>>() {
                    });

            String memberId = null;
            boolean requestFound = false;

            if (requestsResponse.getStatusCode().is2xxSuccessful() && requestsResponse.getBody() != null) {
                for (java.util.Map<String, Object> request : requestsResponse.getBody()) {
                    // Match the request ID
                    if (request.get("id") != null && request.get("id").toString().equals(requestId.toString())) {
                        // Verify it's for this club
                        String requestClub = (String) request.get("clubName");
                        String status = (String) request.get("status");
                        String type = (String) request.get("type");

                        if (requestClub != null && requestClub.trim().equalsIgnoreCase(clubName.trim()) &&
                                status != null && status.trim().equalsIgnoreCase("pending") &&
                                type != null && type.trim().equalsIgnoreCase("Club Enrollment")) {

                            memberId = (String) request.get("memberId");
                            requestFound = true;
                            break;
                        }
                    }
                }
            }

            if (!requestFound || memberId == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Valid enrollment request not found");
            }

            // Update the request status in request-service
            String updateUrl = "http://localhost:8083/update-request-status";

            Map<String, Object> requestData = new HashMap<>();
            requestData.put("requestId", requestId);
            requestData.put("action", action);
            requestData.put("clubName", clubName);

            ResponseEntity<String> updateResponse = restTemplate.postForEntity(
                    updateUrl,
                    requestData,
                    String.class);

            if (!updateResponse.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to update request status: " + updateResponse.getBody());
            }

            // If request was accepted, update the member's club in user-service
            if ("accept".equals(action)) {
                String userUpdateUrl = userServiceUrl + "/user-service/api/user/update-club";

                Map<String, Object> userData = new HashMap<>();
                userData.put("userId", memberId);
                userData.put("clubName", clubName);

                ResponseEntity<String> userUpdateResponse = restTemplate.postForEntity(
                        userUpdateUrl,
                        userData,
                        String.class);

                if (!userUpdateResponse.getStatusCode().is2xxSuccessful()) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Request status updated, but failed to update member's club: "
                                    + userUpdateResponse.getBody());
                }

                return ResponseEntity.ok("Enrollment request accepted and member added to club");
            } else {
                return ResponseEntity.ok("Enrollment request rejected");
            }

        } catch (Exception e) {
            System.err.println("Error handling enrollment request: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing request: " + e.getMessage());
        }
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

    @GetMapping("/club-head/members")
    public String showClubMembers(HttpSession session, Model model) {
        String clubName = (String) session.getAttribute("userIdentifier");
        System.out.println("DEBUG: Frontend showClubMembers called for club: '" + clubName + "'");

        // Fetch club details from club-service
        try {
            String clubDetailsUrl = "http://localhost:8082/club-service/api/club/" + clubName;
            ResponseEntity<Map> response = restTemplate.getForEntity(clubDetailsUrl, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                model.addAttribute("club", response.getBody());
                System.out.println("DEBUG: Club details fetched successfully");
            } else {
                model.addAttribute("club", java.util.Collections.emptyMap());
                System.out.println("DEBUG: Failed to fetch club details");
            }
        } catch (Exception e) {
            System.err.println("Error fetching club details: " + e.getMessage());
            model.addAttribute("club", java.util.Collections.emptyMap());
        }

        // Fetch club members from user-service
        java.util.List<java.util.Map<String, Object>> members = new java.util.ArrayList<>();
        try {
            String membersUrl = "http://localhost:8081/user-service/api/user/club-members?clubName=" + clubName;
            System.out.println("DEBUG: Calling user-service for members: " + membersUrl);

            ResponseEntity<java.util.List<java.util.Map<String, Object>>> membersResponse = restTemplate.exchange(
                    membersUrl,
                    org.springframework.http.HttpMethod.GET,
                    null,
                    new org.springframework.core.ParameterizedTypeReference<java.util.List<java.util.Map<String, Object>>>() {
                    });

            if (membersResponse.getStatusCode().is2xxSuccessful() && membersResponse.getBody() != null) {
                members = membersResponse.getBody();
                System.out.println("DEBUG: Successfully fetched " + members.size() + " members for club: " + clubName);
            } else {
                System.err.println("DEBUG: Failed to fetch members. Status: " + membersResponse.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println("ERROR: Failed to fetch club members: " + e.getMessage());
            e.printStackTrace();
        }

        model.addAttribute("members", members);
        model.addAttribute("clubName", clubName);
        System.out.println("DEBUG: Returning club view with " + members.size() + " members");
        return "club";
    }

    @GetMapping("/club")
    public String showClubPage(HttpSession session, Model model) {
        addClubNameIfClubHead(session, model);
        String clubName = (String) model.getAttribute("clubName");
        List<Map<String, Object>> members = Collections.emptyList();
        if (clubName != null && !clubName.trim().isEmpty()) {
            try {
                String url = userServiceUrl + "/user-service/api/user/club-members?clubName=" + clubName;
                System.out.println("DEBUG: Fetching club members from: " + url);
                ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    members = response.getBody();
                }
            } catch (Exception e) {
                System.err.println("Error fetching club members: " + e.getMessage());
            }
        }
        model.addAttribute("members", members);
        return "club";
    }

    @GetMapping("/club/{clubName}/manage-members")
    public String showManageMembers(@PathVariable String clubName, Model model) {
        try {
            String clubServiceUrl = "http://localhost:8082/club-service/api/club/club/" + clubName + "/manage-members";
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.getForEntity(clubServiceUrl, String.class);
            // The club-service returns a rendered HTML page, so we just forward it
            model.addAttribute("clubName", clubName);
            // If you want to parse members from JSON, you can change club-service to return
            // JSON and parse here
            // For now, just render the template
            return "manage-members";
        } catch (Exception e) {
            model.addAttribute("error", "Failed to fetch members: " + e.getMessage());
            return "manage-members";
        }
    }

    @GetMapping("/project-details/{id}")
    public String showProjectDetailsFrontend(@PathVariable Long id, Model model) {
        try {
            String url = projectServiceBaseUrl + "/api/project/" + id;
            System.out.println("Fetching project details from: " + url);
            ResponseEntity<java.util.Map> response = restTemplate.getForEntity(url, java.util.Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> projectMap = new java.util.HashMap<>(response.getBody());

                // Convert date-like fields to java.util.Date so Thymeleaf's #dates.format works
                convertDateFieldsIfNeeded(projectMap, "startDate", "endDate", "createdAt", "updatedAt");

                model.addAttribute("project", projectMap);

                // Add computed flags similar to project-service's template expectations
                String status = projectMap.get("status") != null ? projectMap.get("status").toString() : "";
                boolean isOngoing = "In Progress".equalsIgnoreCase(status);
                boolean isCompleted = "Completed".equalsIgnoreCase(status);
                model.addAttribute("isOngoing", isOngoing);
                model.addAttribute("isCompleted", isCompleted);

                return "project-details";
            } else {
                model.addAttribute("error", "Project not found or could not be fetched");
                return "redirect:/projects";
            }
        } catch (Exception e) {
            System.err.println("Error fetching project details: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error fetching project details");
            return "redirect:/projects";
        }
    }

    // Helper to convert ISO date strings and epoch timestamps into java.util.Date
    private void convertDateFieldsIfNeeded(java.util.Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val == null) continue;

            try {
                if (val instanceof Number) {
                    // epoch millis
                    long millis = ((Number) val).longValue();
                    map.put(key, new java.util.Date(millis));
                } else if (val instanceof String) {
                    String s = (String) val;
                    // Try ISO date-time / date formats
                    try {
                        java.time.Instant instant = null;
                        if (s.matches("^\\d{4}-\\d{2}-\\d{2}T.*Z$")) {
                            instant = java.time.Instant.parse(s);
                        } else if (s.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
                            instant = java.time.LocalDate.parse(s).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
                        } else if (s.matches("^\\d{4}-\\d{2}-\\d{2}.*")) {
                            // try parsing as OffsetDateTime
                            instant = java.time.OffsetDateTime.parse(s).toInstant();
                        }
                        if (instant != null) {
                            map.put(key, java.util.Date.from(instant));
                            continue;
                        }
                    } catch (Exception ignored) {
                    }

                    // fallback: try parse long millis
                    try {
                        long millis = Long.parseLong(s);
                        map.put(key, new java.util.Date(millis));
                    } catch (Exception ignored) {
                        // leave original string if parsing fails
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    // Utility method to parse and convert date fields in project maps
    private static void convertDateFields(java.util.Map<String, Object> map) {
        try {
            // Handle various possible representations
            Object sd = map.get("startDate");
            Object ed = map.get("endDate");
            java.time.ZoneId zone = java.time.ZoneId.systemDefault();

            if (sd instanceof String) {
                try {
                    java.time.LocalDate ld = java.time.LocalDate.parse((String) sd);
                    java.util.Date date = java.util.Date.from(ld.atStartOfDay(zone).toInstant());
                    map.put("startDate", date);
                } catch (Exception ex) {
                    // Try parsing as ISO_OFFSET_DATE_TIME
                    try {
                        java.time.OffsetDateTime odt = java.time.OffsetDateTime.parse((String) sd);
                        java.util.Date date = java.util.Date.from(odt.toInstant());
                        map.put("startDate", date);
                    } catch (Exception ignored) {
                    }
                }
            } else if (sd instanceof Number) {
                long epoch = ((Number) sd).longValue();
                map.put("startDate", new java.util.Date(epoch));
            }

            if (ed instanceof String) {
                try {
                    java.time.LocalDate ld = java.time.LocalDate.parse((String) ed);
                    java.util.Date date = java.util.Date.from(ld.atStartOfDay(zone).toInstant());
                    map.put("endDate", date);
                } catch (Exception ex) {
                    try {
                        java.time.OffsetDateTime odt = java.time.OffsetDateTime.parse((String) ed);
                        java.util.Date date = java.util.Date.from(odt.toInstant());
                        map.put("endDate", date);
                    } catch (Exception ignored) {
                    }
                }
            } else if (ed instanceof Number) {
                long epoch = ((Number) ed).longValue();
                map.put("endDate", new java.util.Date(epoch));
            }
        } catch (Exception e) {
            System.err.println("convertDateFields error: " + e.getMessage());
        }
    }

    @PostMapping("/request/mark-complete")
    @ResponseBody
    public ResponseEntity<String> markRequestComplete(@RequestParam("requestId") Long requestId, HttpSession session) {

        String userIdentifier = (String) session.getAttribute("userIdentifier");
        Boolean isAuthenticated = (Boolean) session.getAttribute("isAuthenticated");
        String userType = (String) session.getAttribute("userType");

        if (requestId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("requestId is required");
        }

        if (isAuthenticated == null || !isAuthenticated || userIdentifier == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User is not authenticated");
        }

        if (userType == null || (!userType.equalsIgnoreCase("clubHead") && !userType.equalsIgnoreCase("admin"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User does not have permission");
        }

        try {
            // Resolve club for clubHead users. Admins may mark any club's requests.
            String clubName = null;
            if (userType.equalsIgnoreCase("clubHead")) {
                clubName = userIdentifier; // frontend stores club name in userIdentifier for club heads
            }

            // Fetch all club requests and locate the requested one
            String requestUrl = requestServiceUrl + "/club-requests";
            ResponseEntity<java.util.List<java.util.Map<String, Object>>> requestsResponse = restTemplate.exchange(
                    requestUrl,
                    org.springframework.http.HttpMethod.GET,
                    null,
                    new org.springframework.core.ParameterizedTypeReference<java.util.List<java.util.Map<String, Object>>>() {
                    });

            if (!requestsResponse.getStatusCode().is2xxSuccessful() || requestsResponse.getBody() == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch requests");
            }

            boolean found = false;
            String requestClub = null;
            for (java.util.Map<String, Object> req : requestsResponse.getBody()) {
                Object idObj = req.get("id");
                if (idObj == null) idObj = req.get("requestId");
                if (idObj == null) idObj = req.get("request_id");

                Long idLong = null;
                if (idObj instanceof Number) {
                    idLong = ((Number) idObj).longValue();
                } else if (idObj instanceof String) {
                    try {
                        idLong = Long.parseLong((String) idObj);
                    } catch (NumberFormatException ignored) {
                    }
                }

                if (idLong != null && idLong.equals(requestId)) {
                    found = true;
                    Object clubObj = req.get("clubName");
                    requestClub = clubObj == null ? null : clubObj.toString();
                    break;
                }
            }

            if (!found) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Request not found");
            }

            // If user is club head, ensure the request belongs to their club
            if (clubName != null) {
                if (requestClub == null || !requestClub.trim().equalsIgnoreCase(clubName.trim())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Request does not belong to your club");
                }
            }

            // Call request-service to mark as completed
            String updateUrl = requestServiceUrl + "/update-request-status";
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("requestId", requestId);
            requestData.put("action", "complete");
            requestData.put("is_completed", 1);
            if (requestClub != null) requestData.put("clubName", requestClub);

            ResponseEntity<String> updateResponse = restTemplate.postForEntity(updateUrl, requestData, String.class);

            if (updateResponse.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.ok("Request marked completed");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to update request status: " + updateResponse.getStatusCode());
            }

        } catch (Exception e) {
            System.err.println("Error marking request complete: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing request");
        }
    }

    @PostMapping("/complete-request")
    @ResponseBody
    public ResponseEntity<String> handleCompleteRequest(@RequestParam("requestId") Long requestId,
                                                        @RequestParam(value = "action", required = false) String action,
                                                        HttpSession session) {
        // This endpoint exists because the frontend JS calls POST /complete-request with form-encoded body.
        String userIdentifier = (String) session.getAttribute("userIdentifier");
        Boolean isAuthenticated = (Boolean) session.getAttribute("isAuthenticated");
        String userType = (String) session.getAttribute("userType");

        if (requestId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("requestId is required");
        }
        if (isAuthenticated == null || !isAuthenticated || userIdentifier == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User is not authenticated");
        }
        if (userType == null || (!userType.equalsIgnoreCase("clubHead") && !userType.equalsIgnoreCase("admin"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User does not have permission");
        }

        // Only 'complete' action is supported here; ignore or reject other actions.
        if (action != null && !"complete".equalsIgnoreCase(action)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unsupported action");
        }

        try {
            // Determine club for clubHead users
            String clubName = null;
            if (userType.equalsIgnoreCase("clubHead")) {
                clubName = userIdentifier;
            }

            // Fetch requests from request-service to verify ownership
            String listUrl = requestServiceUrl + "/club-requests";
            ResponseEntity<java.util.List<java.util.Map<String, Object>>> listResp = restTemplate.exchange(
                    listUrl,
                    org.springframework.http.HttpMethod.GET,
                    null,
                    new org.springframework.core.ParameterizedTypeReference<java.util.List<java.util.Map<String, Object>>>() {
                    });

            if (!listResp.getStatusCode().is2xxSuccessful() || listResp.getBody() == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch requests");
            }

            boolean found = false;
            String requestClub = null;
            for (java.util.Map<String, Object> req : listResp.getBody()) {
                Object idObj = req.get("id");
                if (idObj == null) idObj = req.get("requestId");
                if (idObj == null) idObj = req.get("request_id");

                Long idLong = null;
                if (idObj instanceof Number) {
                    idLong = ((Number) idObj).longValue();
                } else if (idObj instanceof String) {
                    try { idLong = Long.parseLong((String) idObj); } catch (NumberFormatException ignored) {}
                }

                if (idLong != null && idLong.equals(requestId)) {
                    found = true;
                    Object clubObj = req.get("clubName");
                    requestClub = clubObj == null ? null : clubObj.toString();
                    break;
                }
            }

            if (!found) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Request not found");
            }

            if (clubName != null) {
                if (requestClub == null || !requestClub.trim().equalsIgnoreCase(clubName.trim())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Request does not belong to your club");
                }
            }

            // Forward update to request-service
            String updateUrl = requestServiceUrl + "/update-request-status";
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("requestId", requestId);
            requestData.put("action", "complete");
            requestData.put("is_completed", 1);
            if (requestClub != null) requestData.put("clubName", requestClub);

            ResponseEntity<String> updateResp = restTemplate.postForEntity(updateUrl, requestData, String.class);

            if (updateResp.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.ok("Request marked completed");
            } else {
                String body = updateResp.getBody();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to update request status: " + (body == null ? updateResp.getStatusCode() : body));
            }

        } catch (Exception e) {
            System.err.println("Error in /complete-request: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing request");
        }
    }
}
