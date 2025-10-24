package com.collegeclubs.request_service.controller;

import com.collegeclubs.request_service.model.Request;
import com.collegeclubs.request_service.service.RequestService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@Controller
public class RequestController {
    private static final Logger logger = LoggerFactory.getLogger(RequestController.class);

    private static final String UPLOAD_DIR = System.getProperty("user.home") + "/nitk-uploads";

    @Autowired
    private RequestService requestService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/club-requests")
    @ResponseBody
    public ResponseEntity<List<Request>> viewRequests() {
        try {
            // Return only requests that are not completed (is_completed IS NULL or false)
            List<Request> requests = requestService.getAllNotCompleted();
            System.out.println("DEBUG viewRequests: Returning not-completed requests: " + requests.size());

            // Debug output of all requests
            for (Request req : requests) {
                System.out.println("DEBUG request: id=" + req.getId() +
                        ", memberId=" + req.getMemberId() +
                        ", type=" + req.getType() +
                        ", clubName=" + req.getClubName() +
                        ", status=" + req.getStatus());
            }

            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            System.err.println("Error fetching club requests: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    @GetMapping("/my-requests")
    @ResponseBody
    public ResponseEntity<List<Request>> myRequests(@RequestParam("memberId") String memberId) {
        try {
            List<Request> requests = requestService.getRequestsByMemberId(memberId);
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            System.err.println("Error fetching member requests: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    @PostMapping("/update-request-status")
    @ResponseBody
    public ResponseEntity<String> updateRequestStatus(@RequestBody Map<String, Object> requestData) {
        try {
            Long requestId = Long.valueOf(requestData.get("requestId").toString());
            String action = (String) requestData.get("action");
            String clubName = (String) requestData.get("clubName");

            // Handle "complete" action separately: set is_completed = 1
            if ("complete".equalsIgnoreCase(action)) {
                String updateCompleteSql = "UPDATE request SET is_completed = 1 WHERE id = ?";
                int updatedRows = jdbcTemplate.update(updateCompleteSql, requestId);
                if (updatedRows > 0) {
                    System.out.println("Request " + requestId + " marked as completed (is_completed=1)");
                    return ResponseEntity.ok("Request marked completed");
                } else {
                    return ResponseEntity.notFound().build();
                }
            }

            String status;
            if ("accept".equalsIgnoreCase(action)) {
                status = "accepted";
            } else if ("reject".equalsIgnoreCase(action)) {
                status = "rejected";
            } else {
                return ResponseEntity.badRequest().body("Invalid action");
            }

            // First, get the request details to get the member ID
            String getRequestSql = "SELECT member_id, clubName, type FROM request WHERE id = ?";
            Map<String, Object> requestDetails = jdbcTemplate.queryForMap(getRequestSql, requestId);
            String memberId = (String) requestDetails.get("member_id");
            String requestClubName = (String) requestDetails.get("clubName");
            String requestType = (String) requestDetails.get("type");

            // Update the request status
            String sql = "UPDATE request SET status = ? WHERE id = ?";
            int updated = jdbcTemplate.update(sql, status, requestId);

            if (updated > 0) {
                System.out.println("Request " + requestId + " status updated to: " + status);

                // If the request is accepted and it's a Club Enrollment request, update user's club
                if ("accepted".equals(status) && memberId != null && requestClubName != null &&
                    "Club Enrollment".equalsIgnoreCase(requestType)) {
                    try {
                        // Call user service to update user's club
                        String userServiceUrl = "http://localhost:8081/user-service/api/user/update-club";

                        Map<String, Object> updateData = new HashMap<>();
                        updateData.put("userId", memberId);
                        updateData.put("clubName", requestClubName);

                        System.out.println("Club Enrollment request accepted - Calling user service to update club for user: " + memberId + " to club: " + requestClubName);
                        System.out.println("Request data being sent: " + updateData);

                        ResponseEntity<String> userResponse = restTemplate.postForEntity(userServiceUrl, updateData, String.class);

                        if (userResponse.getStatusCode().is2xxSuccessful()) {
                            System.out.println("Successfully updated user's club field for Club Enrollment: " + userResponse.getBody());
                        } else {
                            System.err.println("Failed to update user's club field for Club Enrollment. Status: " + userResponse.getStatusCode() + ", Body: " + userResponse.getBody());
                        }
                    } catch (Exception e) {
                        System.err.println("Error calling user service to update club for Club Enrollment: " + e.getMessage());
                        e.printStackTrace();
                        // Don't fail the whole operation if user service call fails
                    }
                } else if ("accepted".equals(status)) {
                    System.out.println("Request accepted but not a Club Enrollment request (type: " + requestType + "), skipping user club update");
                }

                return ResponseEntity.ok("Request status updated successfully");
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            System.err.println("Error updating request status: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating request status");
        }
    }

    @GetMapping("/request/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getRequestById(@PathVariable Long id) {
        try {
            String sql = "SELECT * FROM request WHERE id = ?";
            Map<String, Object> request = jdbcTemplate.queryForMap(sql, id);

            // Convert member_id to memberId for backward compatibility
            if (request.containsKey("member_id")) {
                request.put("memberId", request.get("member_id"));
            }

            return ResponseEntity.ok(request);
        } catch (Exception e) {
            System.err.println("Error fetching request: " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/request/file/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        Optional<Request> opt = requestService.getRequestById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Request req = opt.get();
        String filePath = req.getFilePath();
        if (filePath == null || filePath.trim().isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return ResponseEntity.notFound().build();
            }
            Resource resource = new UrlResource(path.toUri());
            String filename = req.getFileName();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (MalformedURLException e) {
            logger.error("Invalid file URL", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/api/requests")
    @ResponseBody
    public ResponseEntity<String> createRequest(@RequestBody Map<String, Object> requestData) {
        try {
            String srn = (String) requestData.get("srn");
            String type = (String) requestData.get("type");
            String description = (String) requestData.get("description");
            String clubName = (String) requestData.get("clubName");
            String status = (String) requestData.get("status");

            System.out.println("DEBUG createRequest: srn=" + srn +
                    ", type=" + type +
                    ", clubName=" + clubName +
                    ", status=" + status);

            if (srn == null || type == null || description == null) {
                return ResponseEntity.badRequest().body("Missing required fields");
            }

            // Create the request in database with pending status
            String sql = "INSERT INTO request (member_id, type, description, status, timestamp, file_path, clubName) VALUES (?, ?, ?, ?, NOW(), NULL, ?)";
            String requestStatus = status != null ? status : "pending";

            System.out.println("DEBUG SQL: " + sql +
                    " with values: [" + srn + ", " + type + ", " + description +
                    ", " + requestStatus + ", " + clubName + "]");

            jdbcTemplate.update(sql, srn, type, description, requestStatus, clubName);

            return ResponseEntity.ok("Request submitted successfully and sent to club head for approval");
        } catch (Exception e) {
            System.err.println("Error creating request: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating request");
        }
    }

    @PostMapping("/submit-request")
    public String submitRequest(@RequestParam("type") String type,
            @RequestParam("description") String description,
            @RequestParam(value = "memberId", required = false) String memberId,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        // fallback: if memberId not provided in form, attempt to read from cookie set
        // at login
        if (memberId == null || memberId.trim().isEmpty()) {
            try {
                var cookies = request.getCookies();
                if (cookies != null) {
                    for (var c : cookies) {
                        if ("userIdentifier".equals(c.getName())) {
                            memberId = c.getValue();
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to read cookies for memberId fallback: {}", e.getMessage());
            }
        }

        logger.info("Received submit-request: type={}, memberId={}, fileName={}",
                type, memberId, file == null ? null : file.getOriginalFilename());

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
            // Ensure upload directory exists
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Sanitize filename and create destination path
            String sanitizedFileName = System.currentTimeMillis() + "_"
                    + filename.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
            Path filePath = uploadPath.resolve(sanitizedFileName);

            // Save the file
            file.transferTo(filePath.toFile());

            if (memberId == null || memberId.trim().isEmpty()) {
                memberId = "unknown";
            }

            // get club name for the member from user service
            String clubName = "Unknown Club";
            try {
                String userServiceUrl = "http://localhost:8081";
                String fallbackUrl = userServiceUrl + "/user-service/api/user/details/srn?srn=" + memberId;
                System.out.println("Fetching user details from: " + fallbackUrl);
                ResponseEntity<Map> fallbackResponse = restTemplate.getForEntity(fallbackUrl, Map.class);

                if (fallbackResponse.getStatusCode().is2xxSuccessful() && fallbackResponse.getBody() != null) {
                    Map<String, Object> userData = fallbackResponse.getBody();
                    System.out.println("User data received from fallback: " + userData);

                    if (userData.get("club") != null && !userData.get("club").toString().trim().isEmpty()) {
                        clubName = userData.get("club").toString();
                        System.out.println("Club name found from fallback: " + clubName);
                    } else {
                        System.out.println("Club name not found in fallback user data");
                    }
                }
            } catch (Exception e) {
                logger.error("Error calling user service for memberId={}: {}", memberId, e.getMessage());
            }

            // Save request in DB
            Request req = new Request();
            req.setType(type);
            req.setDescription(description);
            req.setMemberId(memberId);
            req.setFilePath(filePath.toString());
            req.setStatus("pending");
            req.setClubName(clubName);

            Request saved = requestService.saveRequest(req);
            if (saved != null && saved.getId() != null) {
                logger.info("Saved Request id={}, memberId={}, type={}", saved.getId(), saved.getMemberId(),
                        saved.getType());
                redirectAttributes.addFlashAttribute("message", "Request submitted successfully.");
            } else {
                logger.error("Failed to persist Request for memberId={}", memberId);
                redirectAttributes.addFlashAttribute("message", "Failed to save request. Try again later.");
            }

        } catch (IOException e) {
            logger.error("File upload failed", e);
            redirectAttributes.addFlashAttribute("message", "File upload failed: " + e.getMessage());
        } catch (Exception ex) {
            logger.error("Failed to save Request", ex);
            redirectAttributes.addFlashAttribute("message", "Failed to save request: " + ex.getMessage());
        }

        // Redirect member back to their home page
        return "redirect:/home";
    }

    @GetMapping("/request-details/{id}")
    public String showRequestDetails(@PathVariable("id") Long id, Model model) {
        Optional<Request> requestOpt = requestService.getRequestById(id);
        if (requestOpt.isEmpty()) {
            return "redirect:/pending-tasks";
        }

        Request request = requestOpt.get();
        model.addAttribute("request", request);
        return "request-details";
    }

    @GetMapping("/club/{clubName}/processed-requests")
    @ResponseBody
    public ResponseEntity<List<Request>> getProcessedRequestsForClub(@PathVariable String clubName) {
        try {
            List<Request> processedRequests = requestService.getProcessedRequestsByClub(clubName);
            return ResponseEntity.ok(processedRequests);
        } catch (Exception e) {
            System.err.println("Error fetching processed requests: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    @GetMapping("/member/{memberId}/pending-tasks")
    @ResponseBody
    public ResponseEntity<List<Request>> getPendingTasksForMember(@PathVariable String memberId) {
        try {
            logger.info("Fetching pending tasks for memberId: {}", memberId);
            // Query for pending OR accepted and not completed requests for the member
            String sql = "SELECT * FROM request WHERE member_id = ? AND (status = 'pending' OR (status = 'accepted' AND (is_completed != 1 OR is_completed IS NULL)))";
            logger.info("SQL Query: {} | Params: [{}]", sql, memberId);
            List<Request> pendingTasks = jdbcTemplate.query(
                sql,
                new Object[]{memberId},
                (rs, rowNum) -> {
                    Request req = new Request();
                    req.setId(rs.getLong("id"));
                    req.setMemberId(rs.getString("member_id"));
                    req.setType(rs.getString("type"));
                    req.setDescription(rs.getString("description"));
                    req.setStatus(rs.getString("status"));
                    req.setTimestamp(rs.getTimestamp("timestamp"));
                    req.setFilePath(rs.getString("file_path"));
                    req.setClubName(rs.getString("clubName"));
                    Object completedObj = rs.getObject("is_completed");
                    boolean completed = false;
                    if (completedObj instanceof Boolean) {
                        completed = (Boolean) completedObj;
                    } else if (completedObj instanceof Number) {
                        completed = ((Number) completedObj).intValue() == 1;
                    } else if (completedObj != null) {
                        completed = completedObj.toString().equals("1") || completedObj.toString().equalsIgnoreCase("true");
                    }
                    req.setCompleted(completed);
                    return req;
                }
            );
            logger.info("Pending tasks found: {}", pendingTasks.size());
            return ResponseEntity.ok(pendingTasks);
        } catch (Exception e) {
            logger.error("Error fetching pending tasks for member {}: {}", memberId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    @GetMapping("/club/{clubName}/pending-tasks")
    @ResponseBody
    public ResponseEntity<List<Request>> getPendingTasksForClub(@PathVariable String clubName) {
        try {
            logger.info("Fetching pending tasks for club: {}", clubName);
            String sql = "SELECT * FROM request WHERE clubName = ? AND (status = 'pending' OR (status = 'accepted' AND (is_completed = 0 OR is_completed IS NULL)))";
            List<Request> pendingTasks = jdbcTemplate.query(
                sql,
                new Object[]{clubName},
                (rs, rowNum) -> {
                    Request req = new Request();
                    req.setId(rs.getLong("id"));
                    req.setMemberId(rs.getString("member_id"));
                    req.setType(rs.getString("type"));
                    req.setDescription(rs.getString("description"));
                    req.setStatus(rs.getString("status"));
                    req.setTimestamp(rs.getTimestamp("timestamp"));
                    req.setFilePath(rs.getString("file_path"));
                    req.setClubName(rs.getString("clubName"));
                    Object completedObj = rs.getObject("is_completed");
                    boolean completed = false;
                    if (completedObj instanceof Boolean) {
                        completed = (Boolean) completedObj;
                    } else if (completedObj instanceof Number) {
                        completed = ((Number) completedObj).intValue() == 1;
                    } else if (completedObj != null) {
                        completed = completedObj.toString().equals("1") || completedObj.toString().equalsIgnoreCase("true");
                    }
                    req.setCompleted(completed);
                    return req;
                }
            );
            logger.info("Pending tasks found for club {}: {}", clubName, pendingTasks.size());
            for (Request req : pendingTasks) {
                logger.info("DEBUG club pending: id={}, type={}, clubName={}, status={}, isCompleted={}", req.getId(), req.getType(), req.getClubName(), req.getStatus(), req.isCompleted());
            }
            return ResponseEntity.ok(pendingTasks);
        } catch (Exception e) {
            logger.error("Error fetching pending tasks for club {}: {}", clubName, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }
}
