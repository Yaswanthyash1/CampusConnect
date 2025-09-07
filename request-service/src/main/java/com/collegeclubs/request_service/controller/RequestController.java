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
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

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

    @GetMapping("/club-requests")
    public String viewRequests(Model model) {
        List<Request> requests = requestService.getAllRequests();
        model.addAttribute("requests", requests);
        return "club-requests";
    }

    @GetMapping("/my-requests")
    public String myRequests(@RequestParam("memberId") String memberId, Model model) {
        // Return requests submitted by this member (SRN). Shows status but no action
        // buttons.
        List<Request> requests = requestService.getRequestsByMemberId(memberId);
        model.addAttribute("requests", requests);
        model.addAttribute("isClubHead", false);
        model.addAttribute("clubName", "");
        return "club-requests";
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

            if (srn == null || type == null || description == null) {
                return ResponseEntity.badRequest().body("Missing required fields");
            }

            // Create the request in database
            String sql = "INSERT INTO request (member_id, type, description, status, timestamp, file_path) VALUES (?, ?, ?, ?, NOW(), NULL)";
            jdbcTemplate.update(sql, srn, type, description, status != null ? status : "pending");

            return ResponseEntity.ok("Request submitted successfully");
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

            // Save request in DB
            Request req = new Request();
            req.setType(type);
            req.setDescription(description);
            req.setMemberId(memberId);
            req.setFilePath(filePath.toString());
            req.setStatus("pending");

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
}