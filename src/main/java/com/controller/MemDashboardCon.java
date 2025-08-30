package com.controller;

import com.model.Event;
import com.model.Member;
import com.model.MemberRequest;
import com.service.MemberRequestService;
import com.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;

@Controller
public class MemDashboardCon {
    @Autowired
    private MemberService memberService;
    @Autowired
    private MemberRequestService memberRequestService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/member/{srn}")
    public String memberDashboard(@PathVariable String srn, Model model) {
        Member member = memberService.findBySrn(srn);
        if (member != null) {
            model.addAttribute("member", member);

            // There is no clubmember table in this schema. Provide empty club list so page still renders.
            List<String> clubs = Collections.emptyList();
            model.addAttribute("clubs", clubs);

            // Since we cannot determine clubs for this member (no clubmember table), do not query events.
            List<Event> upcomingEvents = Collections.emptyList();
            model.addAttribute("upcomingEvents", upcomingEvents);

            // Fetch requests made by this member (show status)
            List<MemberRequest> requests = memberRequestService.getRequestsByMemberId(srn);
            model.addAttribute("requests", requests);

            // Query all clubs for the dropdown
            List<String> allClubs = jdbcTemplate.query("SELECT clubName FROM club", (rs, rowNum) -> rs.getString("clubName"));
            model.addAttribute("allClubs", allClubs);

            return "member";
        } else {
            return "error";
        }
    }

    @PostMapping("/members/{srn}/applyClub")
    public String applyClub(@PathVariable String srn, @RequestParam String clubName, @RequestParam String message, Model model) {
        Member member = memberService.findBySrn(srn);
        if (member != null) {
            // Create a MemberRequest entity and save via service so status is tracked
            MemberRequest req = new MemberRequest();
            req.setMemberId(srn);
            req.setType("join");
            req.setDescription(message);
            req.setClubName(clubName);
            // status will default to "pending" via entity
            memberRequestService.saveRequest(req);
            return "redirect:/member/" + srn;
        } else {
            model.addAttribute("errorMessage", "Member not found");
            return "error";
        }
    }

    @PostMapping("/member/apply-club")
    public String applyForClub(@RequestParam String clubName, @RequestParam String srn, Model model) {
        // Create a new member request for joining a club
        MemberRequest request = new MemberRequest();
        request.setMemberId(srn);
        request.setClubName(clubName);
        request.setType("enroll");
        request.setDescription("Request to join club " + clubName);
        request.setStatus("pending");
        request.setTimestamp(new java.sql.Timestamp(System.currentTimeMillis()));
        memberRequestService.saveRequest(request);
        // Redirect back to member dashboard
        return "redirect:/member/" + srn;
    }
}
