package com.controller;

import com.service.ClubService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.model.Member;
import com.model.Club;
import com.service.MemberService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;

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
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginData) {
        String userType = loginData.get("userType");
        String password = loginData.get("password");

        if (userType.equals("member")) {
            String srn = loginData.get("srn");
            Member member = memberService.findBySrnAndPassword(srn, password);
            if (member != null) {
                return ResponseEntity.ok().body(new LoginResponse("/member/" + member.getSrn(), member.getSrn()));
            }
        } else if (userType.equals("clubHead")) {
            String clubName = loginData.get("clubName");
            Club club = clubService.findByClubNameAndPassword(clubName, password);
            if (club != null) {
                return ResponseEntity.ok().body(new LoginResponse("/club/" + club.getClubName(), club.getClubName()));
            }
        } else if (userType.equals("faculty")) {
            String facultyEmail = loginData.get("email");
            String sql = "SELECT * FROM faculty WHERE email = ? AND password = ?";
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, facultyEmail, password);
            if (!result.isEmpty()) {
                int facultyId = (int) result.get(0).get("id");
                return ResponseEntity.ok().body(new LoginResponse("/faculty/" + facultyId, String.valueOf(facultyId)));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
            }
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
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