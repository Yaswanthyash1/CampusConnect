package com.collegeclubs.user_service.controller;

import com.collegeclubs.user_service.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user-service/api/auth")
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> requestBody) {
        System.out.println("Received registration data: " + requestBody);
        try {
            userService.register(requestBody);
            String userType = (String) requestBody.get("userType");
            if ("club".equalsIgnoreCase(userType)) {
                // Call club-service to register the club in clubdb
                String clubServiceUrl = "http://localhost:8082/api/register-club";
                org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
                restTemplate.postForObject(clubServiceUrl, requestBody, String.class);
            }
            System.out.println(userType + " registered successfully.");
            return ResponseEntity.ok(userType + " registered successfully");
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid user type: " + e.getMessage());
            return ResponseEntity.badRequest().body("Invalid user type");
        } catch (Exception e) {
            System.err.println("Error during registration: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Registration failed: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, Object> payload) {
        System.out.println("Received login data: " + payload);
        String userType = (String) payload.get("userType");
        String identifier;
        String password = (String) payload.get("password");
        Map<String, Object> resp = new HashMap<>();

        try {
            boolean isValid = false;
            String role = "";

            if ("member".equalsIgnoreCase(userType)) {
                identifier = (String) payload.get("srn");
                isValid = userService.validateUser(identifier, password);
                role = "member";
                resp.put("userIdentifier", identifier);
            } else if ("faculty".equalsIgnoreCase(userType)) {
                identifier = (String) payload.get("email");
                isValid = userService.validateFaculty(identifier, password);
                role = "faculty";
                resp.put("userIdentifier", identifier);
            } else if ("club".equalsIgnoreCase(userType) || "clubHead".equalsIgnoreCase(userType)) {
                identifier = (String) payload.get("clubName");
                System.out.println("Attempting club login with clubName: " + identifier);
                isValid = userService.validateClub(identifier, password);
                role = "clubHead";
                resp.put("userIdentifier", identifier);
            } else {
                resp.put("error", "Invalid user type");
                return ResponseEntity.badRequest().body(resp);
            }

            if (isValid) {
                resp.put("role", role);
                return ResponseEntity.ok(resp);
            } else {
                resp.put("error", "Invalid credentials");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(resp);
            }
        } catch (Exception e) {
            resp.put("error", "Login failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
        }
    }
}