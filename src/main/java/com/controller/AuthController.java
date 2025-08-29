package com.controller;

import com.model.Club;
import com.model.Member;
import com.service.ClubService;
import com.service.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final MemberService memberService;
    private final ClubService clubService;
    private final JdbcTemplate jdbcTemplate;

    public AuthController(MemberService memberService, ClubService clubService, JdbcTemplate jdbcTemplate) {
        this.memberService = memberService;
        this.clubService = clubService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> requestBody) {
        String userType = (String) requestBody.get("userType");
        try {
            if (userType.equals("member")) {
                Member savedMember = memberService.createMember(
                        (String) requestBody.get("email"),
                        (String) requestBody.get("password"),
                        (String) requestBody.get("domain"),
                        (String) requestBody.get("srn"),
                        (String) requestBody.get("name"),
                        Integer.parseInt((String) requestBody.get("sem")),
                        (String) requestBody.get("dept"),
                        (String) requestBody.get("phoneno"),
                        (String) requestBody.get("gender")
                );
                String srn = (String) requestBody.get("srn");
                // Always insert into user table for authentication
                String userSql = "INSERT INTO user (username, password, role) VALUES (?, ?, ?)";
                jdbcTemplate.update(userSql, srn, requestBody.get("password"), "MEMBER");
                return ResponseEntity.ok().body(savedMember);
            } else if (userType.equals("clubHead") || userType.equals("club")) {
                Club savedClub = clubService.createClub(
                        (String) requestBody.get("clubName"),
                        (String) requestBody.get("email"),
                        (String) requestBody.get("password"),
                        (String) requestBody.get("facultyId"),
                        (String) requestBody.get("clubType"),
                        (String) requestBody.get("headSrn"),
                        (String) requestBody.get("name"),
                        Integer.parseInt((String) requestBody.get("sem")),
                        (String) requestBody.get("dept"),
                        (String) requestBody.get("phoneno"),
                        (String) requestBody.get("gender")
                );
                String srn = (String) requestBody.get("headSrn");
                String clubName = (String) requestBody.get("clubName");
                if (srn != null && clubName != null && !clubName.isEmpty()) {
                    // Insert into member table (headSrn is not a member, so skip)
                    // Insert into user table for authentication
                    String userSql = "INSERT INTO user (username, password, role) VALUES (?, ?, ?)";
                    jdbcTemplate.update(userSql, srn, requestBody.get("password"), "CLUB_HEAD");
                }
                return ResponseEntity.ok().body(savedClub);
            } else if (userType.equals("faculty")) {
                String email = (String) requestBody.get("email");
                String password = (String) requestBody.get("password");
                String facultyName = (String) requestBody.get("facultyName");
                String sql = "INSERT INTO faculty (email, password, faculty_name) VALUES (?, ?, ?)";
                jdbcTemplate.update(sql, email, password, facultyName);
                return ResponseEntity.ok().body("Faculty registered successfully");
            }
            return ResponseEntity.badRequest().body("Invalid user type");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Registration failed: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, Object> payload) {
        String userType = (payload.get("userType") instanceof String) ? (String) payload.get("userType") : null;
        String password = (payload.get("password") instanceof String) ? (String) payload.get("password") : null;

        Map<String, Object> resp = new HashMap<>();

        if ("member".equals(userType)) {
            String srn = (payload.get("srn") instanceof String) ? (String) payload.get("srn") : null;
            if (srn == null || password == null) {
                resp.put("error", "Missing credentials");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
            }
            Member member = memberService.findBySrn(srn);
            if (member == null) {
                resp.put("error", "Member not found");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(resp);
            }
            if (!password.equals(member.getPassword())) {
                resp.put("error", "Invalid password");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(resp);
            }
            resp.put("userIdentifier", member.getSrn());
            resp.put("role", "member");
            return ResponseEntity.ok(resp);
        }

        // For other user types accept and echo back identifier so frontend can proceed
        if ("clubHead".equals(userType)) {
            String clubName = (payload.get("clubName") instanceof String) ? (String) payload.get("clubName") : null;
            if (clubName == null) {
                resp.put("error", "Missing clubName");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
            }
            resp.put("userIdentifier", clubName);
            resp.put("role", "clubHead");
            return ResponseEntity.ok(resp);
        }

        if ("faculty".equals(userType)) {
            String email = (payload.get("email") instanceof String) ? (String) payload.get("email") : null;
            if (email == null) {
                resp.put("error", "Missing email");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
            }
            resp.put("userIdentifier", email);
            resp.put("role", "faculty");
            return ResponseEntity.ok(resp);
        }

        resp.put("error", "Unsupported userType");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
    }

    public static class LoginResponse {
        private String redirectUrl;
        private String userIdentifier;

        public LoginResponse(String redirectUrl, String userIdentifier) {
            this.redirectUrl = redirectUrl;
            this.userIdentifier = userIdentifier;
        }

        public String getRedirectUrl() {
            return redirectUrl;
        }

        public String getUserIdentifier() {
            return userIdentifier;
        }
    }
}