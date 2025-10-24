package com.collegeclubs.request_service.service;

import com.collegeclubs.request_service.model.Request;
import com.collegeclubs.request_service.repository.RequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;
import java.util.Map;

@Service
public class RequestService {
    private static final Logger logger = LoggerFactory.getLogger(RequestService.class);

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private RestTemplate restTemplate = new RestTemplate();

    /**
     * Sync existing requests with projects and events on startup
     * This runs automatically when the service starts
     */
    @PostConstruct
    public void syncExistingRequestsOnStartup() {
        // Delay the sync to allow other services to start
        new Thread(() -> {
            try {
                Thread.sleep(15000); // Wait 15 seconds for other services to start
                logger.info("Starting automatic sync of existing requests with projects and events...");
                syncAllAcceptedRequests();
            } catch (InterruptedException e) {
                logger.warn("Startup sync interrupted: {}", e.getMessage());
            } catch (Exception e) {
                logger.error("Error during startup sync: {}", e.getMessage(), e);
            }
        }).start();
    }

    /**
     * Sync all accepted requests with existing projects and events
     * Uses API calls since each service has its own database
     */
    @Transactional
    public int syncAllAcceptedRequests() {
        logger.info("=== SYNCING ALL ACCEPTED REQUESTS WITH PROJECTS/EVENTS ===");

        try {
            // Get all accepted requests that are not completed
            List<Request> acceptedRequests = requestRepository.findAll().stream()
                .filter(r -> "accepted".equals(r.getStatus()) &&
                           (r.isCompleted() == null || !r.isCompleted()))
                .collect(java.util.stream.Collectors.toList());

            logger.info("Found {} accepted incomplete requests to check", acceptedRequests.size());

            int markedCount = 0;

            for (Request request : acceptedRequests) {
                if (request.getClubName() == null || request.getDescription() == null) {
                    logger.debug("Skipping request id={} - missing clubName or description", request.getId());
                    continue;
                }

                boolean shouldMarkCompleted = false;

                // Check for matching project
                try {
                    String projectUrl = "http://localhost:8084/api/projects/search?clubName="
                        + java.net.URLEncoder.encode(request.getClubName(), "UTF-8")
                        + "&projectName=" + java.net.URLEncoder.encode(request.getDescription(), "UTF-8");

                    logger.debug("Checking for matching project: {}", projectUrl);
                    ResponseEntity<List> projectResponse = restTemplate.getForEntity(projectUrl, List.class);

                    if (projectResponse.getStatusCode().is2xxSuccessful() &&
                        projectResponse.getBody() != null && !projectResponse.getBody().isEmpty()) {
                        shouldMarkCompleted = true;
                        logger.info("Found matching project for request id={} (clubName='{}', description='{}')",
                            request.getId(), request.getClubName(), request.getDescription());
                    }
                } catch (Exception e) {
                    logger.debug("No matching project for request id={}: {}", request.getId(), e.getMessage());
                }

                // Check for matching event if no project found
                if (!shouldMarkCompleted) {
                    try {
                        String eventUrl = "http://localhost:8085/api/events/search?clubName="
                            + java.net.URLEncoder.encode(request.getClubName(), "UTF-8")
                            + "&eventName=" + java.net.URLEncoder.encode(request.getDescription(), "UTF-8");

                        logger.debug("Checking for matching event: {}", eventUrl);
                        ResponseEntity<List> eventResponse = restTemplate.getForEntity(eventUrl, List.class);

                        if (eventResponse.getStatusCode().is2xxSuccessful() &&
                            eventResponse.getBody() != null && !eventResponse.getBody().isEmpty()) {
                            shouldMarkCompleted = true;
                            logger.info("Found matching event for request id={} (clubName='{}', description='{}')",
                                request.getId(), request.getClubName(), request.getDescription());
                        }
                    } catch (Exception e) {
                        logger.debug("No matching event for request id={}: {}", request.getId(), e.getMessage());
                    }
                }

                // Mark as completed if match found
                if (shouldMarkCompleted) {
                    request.setCompleted(true);
                    requestRepository.save(request);
                    markedCount++;
                    logger.info("Marked request id={} as completed", request.getId());
                } else {
                    logger.debug("No match found for request id={} (clubName='{}', description='{}')",
                        request.getId(), request.getClubName(), request.getDescription());
                }
            }

            logger.info("=== SYNC COMPLETE: Marked {} total requests as completed ===", markedCount);
            return markedCount;

        } catch (Exception e) {
            logger.error("Error during sync: {}", e.getMessage(), e);
            return 0;
        }
    }

    @Transactional
    public Request saveRequest(Request request) {
        logger.info("Attempting to save request: memberId={}, type={}, description={}", 
                   request.getMemberId(), request.getType(), request.getDescription());
        
        try {
            Request saved = requestRepository.saveAndFlush(request);
            if (saved != null && saved.getId() != null) {
                logger.info("Request saved successfully - id={} memberId={}", saved.getId(), saved.getMemberId());
            } else {
                logger.error("Request save returned null or no ID for memberId={}", request.getMemberId());
            }
            return saved;
        } catch (Exception e) {
            logger.error("Error saving request for memberId={}: {}", request.getMemberId(), e.getMessage(), e);
            throw e; // Re-throw to let the controller handle it
        }
    }

    public List<Request> getAllRequests() {
        return requestRepository.findAll();
    }

    public List<Request> getAllNotCompleted() {
        return requestRepository.findAllNotCompleted();
    }

    public List<Request> getRequestsByStatusNotCompleted(String status) {
        return requestRepository.findByStatusAndNotCompleted(status);
    }

    public List<Request> getRequestsByType(String type) {
        return requestRepository.findByType(type);
    }

    public List<Request> getRequestsByMemberId(String memberId) {
        return requestRepository.findByMemberId(memberId);
    }

    public Optional<Request> getRequestById(Long id) {
        return requestRepository.findById(id);
    }

    public List<Request> getRequestsByClubName(String clubName) {
        return requestRepository.findByClubName(clubName);
    }

    public List<Request> getRequestsByClubNameAndStatus(String clubName, String status) {
        // For pending requests, include general requests (clubName IS NULL)
        if ("pending".equalsIgnoreCase(status)) {
            return requestRepository.findPendingForClub(clubName, status);
        } else if ("accepted".equalsIgnoreCase(status)) {
            // For accepted requests, include those with the specific clubName AND those with clubName IS NULL (for general requests)
            return requestRepository.findAcceptedNonEnrollRequests(clubName, status);
        }
        return requestRepository.findByClubNameAndStatus(clubName, status);
    }

    public List<Request> getAllAcceptedNonEnrollRequests() {
        return requestRepository.findAllAcceptedNonEnrollRequests();
    }

    public List<Request> getProcessedRequestsByClub(String clubName) {
        return requestRepository.findProcessedRequestsByClub(clubName);
    }

    /**
     * Mark requests as completed when a project or event is created
     * @param clubName The club name to match
     * @param description The description (project name or event name) to match
     */
    @Transactional
    public void markRequestsAsCompleted(String clubName, String description) {
        logger.info("=== MARKING REQUESTS AS COMPLETED ===");
        logger.info("Looking for requests with clubName='{}' and description='{}'", clubName, description);

        // First, let's see all accepted requests for debugging
        List<Request> allAcceptedRequests = requestRepository.findAll().stream()
            .filter(r -> "accepted".equals(r.getStatus()))
            .collect(java.util.stream.Collectors.toList());
        logger.info("Total accepted requests in DB: {}", allAcceptedRequests.size());
        for (Request r : allAcceptedRequests) {
            logger.info("  Request id={}, clubName='{}', description='{}', isCompleted={}",
                r.getId(), r.getClubName(), r.getDescription(), r.isCompleted());
        }

        List<Request> matchingRequests = requestRepository.findByClubNameAndDescription(clubName, description);

        if (matchingRequests != null && !matchingRequests.isEmpty()) {
            logger.info("Found {} matching request(s) to mark as completed", matchingRequests.size());

            for (Request request : matchingRequests) {
                logger.info("Marking request id={} as completed (was: {})", request.getId(), request.isCompleted());
                request.setCompleted(true);
                requestRepository.save(request);
                logger.info("Successfully marked request id={} as completed", request.getId());
            }
        } else {
            logger.warn("NO MATCHING REQUESTS FOUND! clubName='{}', description='{}'", clubName, description);
        }
        logger.info("=== END MARKING REQUESTS ===");
    }
}
