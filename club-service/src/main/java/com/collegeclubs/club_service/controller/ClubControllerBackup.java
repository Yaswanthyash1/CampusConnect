// package com.collegeclubs.club_service.controller;

// import com.collegeclubs.club_service.model.Club;
// import com.collegeclubs.club_service.service.ClubService;
// import com.collegeclubs.club_service.repository.ClubRepository;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.jdbc.core.JdbcTemplate;
// import org.springframework.web.bind.annotation.*;
// import org.springframework.ui.Model;
// import org.springframework.web.multipart.MultipartFile;
// import org.springframework.stereotype.Controller;
// import org.springframework.http.ResponseEntity;

// import java.io.IOException;
// import java.sql.Timestamp;
// import java.time.LocalDateTime;
// import java.time.format.DateTimeFormatter;
// import java.util.List;
// import java.util.Map;

// @Controller
// public class ClubControllerBackup {
//     @Autowired
//     private ClubService clubService;

//     @Autowired
//     private ClubRepository clubRepository;

//     @Autowired
//     private JdbcTemplate jdbcTemplate;

//     @GetMapping("/club/{clubName}")
//     public String clubDashboard(@PathVariable String clubName, Model model) {
//         System.out.println("DEBUG: Accessing club dashboard for club: " + clubName);

//         // First try exact match
//         Club club = clubService.getClubDetailsByName(clubName);

//         // If not found, try case-insensitive match
//         if (club == null) {
//             System.out.println("DEBUG: Club not found with exact match, trying case-insensitive match");
//             String sql = "SELECT * FROM club WHERE LOWER(clubName) = LOWER(?)";
//             List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, clubName);

//             if (!results.isEmpty()) {
//                 Map<String, Object> clubData = results.get(0);
//                 club = new Club();
//                 club.setId((Long) clubData.get("id"));
//                 club.setClubName((String) clubData.get("clubName"));
//                 club.setDescription((String) clubData.get("description"));
//                 // Set other properties as needed
//                 System.out.println("DEBUG: Found club with case-insensitive match: " + club.getClubName());
//             }
//         }

//         if (club == null) {
//             System.out.println("DEBUG: Club not found: " + clubName);
//             model.addAttribute("message", "Club not found: " + clubName);
//             return "club-not-found";
//         }

//         List<Map<String, Object>> clubMembers = getClubMembersByName(clubName);
//         List<Map<String, Object>> clubEvents = getClubEvents(clubName);
//         model.addAttribute("club", club);
//         model.addAttribute("clubMembers", clubMembers);
//         model.addAttribute("clubEvents", clubEvents);
//         return "club";
//     }

//     @GetMapping("/club/{clubName}/requests")
//     public String showClubRequests(@PathVariable String clubName, Model model) {
//         System.out.println("DEBUG: Showing club requests page for club: " + clubName);

//         // Fetch pending requests from request-service
//         java.util.List<java.util.Map<String, Object>> pendingRequests = new java.util.ArrayList<>();

//         try {
//             org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
//             String requestServiceUrl = "http://localhost:8083/club-requests";

//             System.out.println("DEBUG: Fetching requests from request-service at: " + requestServiceUrl);

//             // Fetch all club requests and filter for this club and pending status
//             org.springframework.http.ResponseEntity<java.util.List<java.util.Map<String, Object>>> response = restTemplate
//                     .exchange(requestServiceUrl,
//                             org.springframework.http.HttpMethod.GET,
//                             null,
//                             new org.springframework.core.ParameterizedTypeReference<java.util.List<java.util.Map<String, Object>>>() {
//                             });

//             if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
//                 java.util.List<java.util.Map<String, Object>> allRequests = response.getBody();
//                 System.out.println("DEBUG: Total requests from request-service: " + allRequests.size());

//                 // Filter for this club and pending status
//                 for (java.util.Map<String, Object> request : allRequests) {
//                     String requestClub = (String) request.get("clubName");
//                     String status = (String) request.get("status");

//                     // Case-insensitive matching for club name and pending status
//                     boolean clubMatch = (requestClub != null &&
//                             requestClub.trim().equalsIgnoreCase(clubName.trim()));
//                     boolean isPending = (status != null &&
//                             status.trim().equalsIgnoreCase("pending"));

//                     if (clubMatch && isPending) {
//                         pendingRequests.add(request);
//                     }
//                 }

//                 System.out
//                         .println("DEBUG: Found " + pendingRequests.size() + " pending requests for club: " + clubName);
//             }
//         } catch (Exception e) {
//             System.err.println("Error fetching requests: " + e.getMessage());
//             e.printStackTrace();
//         }

//         // Set model attributes for the template
//         model.addAttribute("requests", pendingRequests);
//         model.addAttribute("clubName", clubName);
//         model.addAttribute("isClubHead", true);

//         return "club-requests";
//     }

//     @PostMapping("/club/{clubName}/requests")
//     public String handleClubRequests(@PathVariable String clubName, @RequestParam String action,
//             @RequestParam(required = false) Long requestId) {
//         System.out.println("DEBUG: Handling club request for club: " + clubName + ", action: " + action
//                 + ", requestId: " + requestId);
//         // action = "accept" or "reject"
//         try {
//             if (requestId != null) {
//                 // Call the request microservice to update the request status
//                 org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
//                 String requestServiceUrl = "http://localhost:8083";
//                 String apiUrl = requestServiceUrl + "/update-request-status";

//                 System.out.println("DEBUG: Calling request service at: " + apiUrl);

//                 java.util.Map<String, Object> requestBody = new java.util.HashMap<>();
//                 requestBody.put("requestId", requestId);
//                 requestBody.put("action", action);
//                 requestBody.put("clubName", clubName);

//                 try {
//                     String result = restTemplate.postForObject(apiUrl, requestBody, String.class);
//                     System.out.println("DEBUG: Request service response: " + result);
//                 } catch (Exception ex) {
//                     System.err.println("ERROR: Failed to call request service: " + ex.getMessage());
//                     ex.printStackTrace();
//                 }

//                 // Optionally, for enroll, also update user service as before
//                 if ("accept".equals(action)) {
//                     // Fetch request details from microservice to check type
//                     String getReqUrl = requestServiceUrl + "/request/" + requestId;
//                     System.out.println("DEBUG: Fetching request details from: " + getReqUrl);

//                     try {
//                         org.springframework.http.ResponseEntity<java.util.Map<String, Object>> resp = restTemplate
//                                 .exchange(
//                                         getReqUrl,
//                                         org.springframework.http.HttpMethod.GET,
//                                         null,
//                                         new org.springframework.core.ParameterizedTypeReference<java.util.Map<String, Object>>() {
//                                         });

//                         if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
//                             Object typeObj = resp.getBody().get("type");
//                             Object srnObj = resp.getBody().get("memberId");
//                             System.out.println("Request type: " + typeObj + ", Member ID: " + srnObj);

//                             // Handle both "enroll" and "Club Enrollment" types (case insensitive)
//                             if (typeObj != null &&
//                                     (typeObj.toString().toLowerCase().contains("enroll"))
//                                     && srnObj != null) {
//                                 System.out.println("DEBUG: Found enrollment request, updating user club");
//                                 String userServiceUrl = "http://localhost:8081";
//                                 String userApiUrl = userServiceUrl + "/user-service/api/user" + "/setUserClub";

//                                 // Create form data using MultiValueMap
//                                 org.springframework.util.MultiValueMap<String, String> userBody = new org.springframework.util.LinkedMultiValueMap<>();
//                                 userBody.add("srn", srnObj.toString());
//                                 userBody.add("clubName", clubName);

//                                 // Set proper headers for form data
//                                 org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
//                                 headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);

//                                 org.springframework.http.HttpEntity<org.springframework.util.MultiValueMap<String, String>> request = new org.springframework.http.HttpEntity<>(
//                                         userBody, headers);

//                                 System.out.println("Updating user club for SRN: " + srnObj + " to club: " + clubName);
//                                 try {
//                                     org.springframework.http.ResponseEntity<String> response = restTemplate
//                                             .postForEntity(userApiUrl, request, String.class);
//                                     System.out.println("User service response: " + response.getBody());
//                                 } catch (Exception ex) {
//                                     System.err.println("Failed to call user service: " + ex.getMessage());
//                                     ex.printStackTrace();
//                                 }
//                             }
//                         } else {
//                             System.err.println("Failed to get request details, status code: " + resp.getStatusCode());
//                         }
//                     } catch (Exception ex) {
//                         System.err.println("ERROR: Failed to fetch request details: " + ex.getMessage());
//                         ex.printStackTrace();
//                     }
//                 }
//             } else {
//                 System.err.println("ERROR: No requestId provided for action: " + action);
//             }
//         } catch (Exception e) {
//             System.err.println("ERROR: Exception handling club request: " + e.getMessage());
//             e.printStackTrace();
//         }
//         return "redirect:/club/" + clubName + "/requests";
//     }

//     @GetMapping("/api/club/{clubName}/requests")
//     @ResponseBody
//     public java.util.List<java.util.Map<String, Object>> getClubRequests(@PathVariable String clubName) {
//         // Fetch pending requests from request-service
//         java.util.List<java.util.Map<String, Object>> pendingRequests = new java.util.ArrayList<>();

//         try {
//             org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
//             String requestServiceUrl = "http://localhost:8083/club-requests";

//             System.out.println("DEBUG: Fetching requests from request-service at: " + requestServiceUrl);

//             // Fetch all club requests and filter for this club and pending status
//             org.springframework.http.ResponseEntity<java.util.List<java.util.Map<String, Object>>> response = restTemplate
//                     .exchange(requestServiceUrl,
//                             org.springframework.http.HttpMethod.GET,
//                             null,
//                             new org.springframework.core.ParameterizedTypeReference<java.util.List<java.util.Map<String, Object>>>() {
//                             });

//             if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
//                 java.util.List<java.util.Map<String, Object>> allRequests = response.getBody();

//                 System.out.println("DEBUG: Total requests from request-service: " + allRequests.size());

//                 // Debug: print all club names and statuses
//                 for (java.util.Map<String, Object> request : allRequests) {
//                     System.out.println("DEBUG: Request - clubName: " + request.get("clubName") +
//                             ", status: " + request.get("status") +
//                             ", type: " + request.get("type") +
//                             ", memberId: " + request.get("memberId") +
//                             ", id: " + request.get("id"));
//                 }

//                 // Filter for this club and pending status
//                 for (java.util.Map<String, Object> request : allRequests) {
//                     String requestClub = (String) request.get("clubName");
//                     String status = (String) request.get("status");

//                     System.out.println("DEBUG: Comparing - Request club: '" + requestClub +
//                             "' with current club: '" + clubName +
//                             "', status: '" + status + "'");

//                     // More flexible matching - trim and ignore case for both club name and status
//                     boolean clubMatch = (requestClub != null && clubName != null &&
//                             clubName.trim().equalsIgnoreCase(requestClub.trim()));
//                     boolean statusMatch = (status != null &&
//                             status.trim().toLowerCase().equals("pending"));

//                     System.out.println("DEBUG: clubMatch=" + clubMatch + ", statusMatch=" + statusMatch);

//                     if (clubMatch && statusMatch) {
//                         System.out.println("DEBUG: Adding request to pendingRequests");
//                         pendingRequests.add(request);
//                     }
//                 }

//                 System.out
//                         .println("DEBUG: Found " + pendingRequests.size() + " pending requests for club: " + clubName);
//             } else {
//                 System.err.println(
//                         "ERROR: Failed to get requests from request-service, status code: " + response.getStatusCode());
//             }
//         } catch (Exception e) {
//             System.err.println("Error fetching requests: " + e.getMessage());
//             e.printStackTrace();
//         }

//         System.out.println("DEBUG: Returning " + pendingRequests.size() + " pending requests for club: " + clubName);
//         return pendingRequests;
//     }

//     // Backward compatibility endpoint for frontend service
//     @GetMapping("/club/{clubName}/requests")
//     @ResponseBody
//     public java.util.List<java.util.Map<String, Object>> getClubRequestsCompat(@PathVariable String clubName) {
//         // Delegate to the main API method
//         return getClubRequests(clubName);
//     }

//     @GetMapping("/api/club/{clubName}/processed-requests")
//     @ResponseBody
//     public java.util.Map<String, Object> getProcessedRequests(@PathVariable String clubName,
//             @RequestParam(name = "status", required = false) String status) {
//         // Fetch both accepted and rejected requests from request-service
//         java.util.List<java.util.Map<String, Object>> acceptedRequests = new java.util.ArrayList<>();
//         java.util.List<java.util.Map<String, Object>> rejectedRequests = new java.util.ArrayList<>();

//         try {
//             org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
//             String requestServiceUrl = "http://localhost:8083/club-requests";

//             // Fetch all club requests and filter for this club and processed status
//             org.springframework.http.ResponseEntity<java.util.List<java.util.Map<String, Object>>> response = restTemplate
//                     .exchange(requestServiceUrl,
//                             org.springframework.http.HttpMethod.GET,
//                             null,
//                             new org.springframework.core.ParameterizedTypeReference<java.util.List<java.util.Map<String, Object>>>() {
//                             });

//             if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
//                 java.util.List<java.util.Map<String, Object>> allRequests = response.getBody();

//                 System.out.println("DEBUG Processed: Total requests from request-service: " + allRequests.size());

//                 // Filter for this club and processed status
//                 for (java.util.Map<String, Object> request : allRequests) {
//                     String requestClub = (String) request.get("clubName");
//                     String requestStatus = (String) request.get("status");

//                     // More flexible matching - trim and ignore case for club name
//                     boolean clubMatch = (requestClub != null && clubName != null &&
//                             clubName.trim().equalsIgnoreCase(requestClub.trim()));

//                     if (clubMatch) {
//                         if (requestStatus != null && requestStatus.trim().equalsIgnoreCase("accepted")) {
//                             System.out.println("DEBUG: Adding accepted request: " + request);
//                             acceptedRequests.add(request);
//                         } else if (requestStatus != null && requestStatus.trim().equalsIgnoreCase("rejected")) {
//                             System.out.println("DEBUG: Adding rejected request: " + request);
//                             rejectedRequests.add(request);
//                         }
//                     }
//                 }
//             }
//         } catch (Exception e) {
//             System.err.println("Error fetching processed requests: " + e.getMessage());
//             e.printStackTrace();
//         }

//         // Create response object
//         java.util.Map<String, Object> response = new java.util.HashMap<>();
//         response.put("acceptedRequests", acceptedRequests);
//         response.put("rejectedRequests", rejectedRequests);
//         response.put("clubName", clubName);
//         response.put("selectedStatus", status == null ? "" : status.toLowerCase());
//         response.put("isClubHead", true);

//         return response;
//     }

//     @GetMapping("/club/{clubName}/manage-members")
//     public String manageMembers(@PathVariable String clubName, Model model) {
//         // TODO: Fetch members from database or microservice
//         // For now, use empty list
//         java.util.List<java.util.Map<String, Object>> members = new java.util.ArrayList<>();
//         model.addAttribute("members", members);
//         model.addAttribute("clubName", clubName);
//         return "manage-members";
//     }

//     private List<Map<String, Object>> getClubMembersByName(String clubName) {
//         String query = "SELECT * FROM member WHERE club = ?";
//         return jdbcTemplate.queryForList(query, clubName);
//     }

//     public List<Map<String, Object>> getClubEvents(String clubName) {
//         String query = "SELECT * FROM event WHERE clubname = ?";
//         List<Map<String, Object>> rawEvents = jdbcTemplate.queryForList(query, clubName);
//         List<Map<String, Object>> events = new java.util.ArrayList<>();
//         for (Map<String, Object> raw : rawEvents) {
//             Map<String, Object> event = new java.util.HashMap<>();
//             event.put("id", raw.get("id"));
//             event.put("eventname", raw.get("eventname"));
//             event.put("type", raw.get("type"));
//             event.put("venue", raw.get("venue"));
//             event.put("clubname", raw.get("clubname"));
//             event.put("budget", raw.get("budget"));
//             event.put("description", raw.get("description"));
//             // Normalize registration link
//             event.put("registration_link", raw.get("registrationlink"));
//             // Normalize timestamp
//             Object ts = raw.get("timestamp");
//             if (ts != null && !(ts instanceof java.util.Date)) {
//                 try {
//                     if (ts instanceof String) {
//                         event.put("timestamp", java.sql.Timestamp.valueOf(((String) ts).replace('T', ' ')));
//                     } else {
//                         event.put("timestamp", null);
//                     }
//                 } catch (Exception e) {
//                     event.put("timestamp", null);
//                 }
//             } else {
//                 event.put("timestamp", ts);
//             }
//             events.add(event);
//         }
//         return events;
//     }
// }
