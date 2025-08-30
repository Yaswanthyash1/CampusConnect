package com.service;

import com.model.MemberRequest;
import com.repository.MemberRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MemberRequestService {
    private static final Logger logger = LoggerFactory.getLogger(MemberRequestService.class);

    @Autowired
    private MemberRequestRepository memberRequestRepository;

    public MemberRequest saveRequest(MemberRequest request) {
        MemberRequest saved = memberRequestRepository.saveAndFlush(request);
        if (saved != null) {
            logger.info("MemberRequest saved (service) id={} memberId={}", saved.getId(), saved.getMemberId());
        } else {
            logger.error("MemberRequest save returned null for memberId={}", request.getMemberId());
        }
        return saved;
    }

    public List<MemberRequest> getAllRequests() {
        return memberRequestRepository.findAll();
    }

    public List<MemberRequest> getRequestsByType(String type) {
        return memberRequestRepository.findByType(type);
    }

    public List<MemberRequest> getRequestsByMemberId(String memberId) {
        return memberRequestRepository.findByMemberId(memberId);
    }

    public Optional<MemberRequest> getRequestById(Long id) {
        return memberRequestRepository.findById(id);
    }

    public List<MemberRequest> getRequestsByClubName(String clubName) {
        return memberRequestRepository.findByClubName(clubName);
    }

    public List<MemberRequest> getRequestsByClubNameAndStatus(String clubName, String status) {
        // For pending requests, include general requests (clubName IS NULL)
        if ("pending".equalsIgnoreCase(status)) {
            return memberRequestRepository.findPendingForClub(clubName, status);
        } else if ("accepted".equalsIgnoreCase(status)) {
            // For accepted requests, include those with the specific clubName AND those with clubName IS NULL (for general requests)
            return memberRequestRepository.findAcceptedNonEnrollRequests(clubName, status);
        }
        return memberRequestRepository.findByClubNameAndStatus(clubName, status);
    }

    public List<MemberRequest> getAllAcceptedNonEnrollRequests() {
        return memberRequestRepository.findAllAcceptedNonEnrollRequests();
    }
}
