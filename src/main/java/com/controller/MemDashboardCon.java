package com.controller;

import com.model.Member;
import com.model.Event;
import com.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Controller
public class MemDashboardCon {
    @Autowired
    private MemberService memberService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/member/{srn}")
    public String memberDashboard(@PathVariable String srn, Model model) {
        Member member = memberService.findBySrn(srn);
        if (member != null) {
            model.addAttribute("member", member);
            String clubsSql = "SELECT clubName FROM ClubMember WHERE SRN = ?";
            List<String> clubs = jdbcTemplate.queryForList(clubsSql, String.class, srn);
            model.addAttribute("clubs", clubs);
            String eventsSql = "SELECT * FROM event WHERE clubname IN (SELECT clubName FROM ClubMember WHERE SRN = ?)";
            List<Event> upcomingEvents = jdbcTemplate.query(eventsSql, new Object[]{srn}, (rs, rowNum) -> {
                Event event = new Event();
                event.setEventName(rs.getString("eventname"));
                event.setClubName(rs.getString("clubname"));
                event.setDescription(rs.getString("description"));
                event.setLocation(rs.getString("loc"));
                event.setType(rs.getString("type"));
                event.setTimestamp(rs.getTimestamp("timestamp"));
                event.setBudget(rs.getDouble("budget"));
                event.setRegistrationLink(rs.getString("registrationlink"));
                event.setBanner(rs.getBytes("banner"));
                return event;
            });
            model.addAttribute("upcomingEvents", upcomingEvents);
            return "member";
        } else {
            return "error";
        }
    }

    @PostMapping("/members/{srn}/applyClub")
    public String applyClub(@PathVariable String srn, @RequestParam String clubName, @RequestParam String message, Model model) {
        Member member = memberService.findBySrn(srn);
        if (member != null) {
            String insertSql = "INSERT INTO requests (srn, memberName, memberEmail, clubName, message) VALUES (?, ?, ?, ?, ?)";
            jdbcTemplate.update(insertSql, srn, member.getName(), member.getEmail(), clubName, message);
            return "redirect:/member/" + srn;
        } else {
            model.addAttribute("errorMessage", "Member not found");
            return "error";
        }
    }
}

