package com.controller;

import com.model.MemberRequest;
import com.service.MemberRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class DebugController {
    @Autowired
    private MemberRequestService memberRequestService;

    @GetMapping("/debug/requests")
    public ResponseEntity<List<MemberRequest>> getRequests(@RequestParam("memberId") String memberId) {
        List<MemberRequest> requests = memberRequestService.getRequestsByMemberId(memberId);
        return ResponseEntity.ok(requests);
    }
}

