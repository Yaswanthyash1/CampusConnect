package com.collegeclubs.request_service.service;

import com.collegeclubs.request_service.model.Request;
import com.collegeclubs.request_service.repository.RequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class RequestService {
    private static final Logger logger = LoggerFactory.getLogger(RequestService.class);

    @Autowired
    private RequestRepository requestRepository;

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
}
