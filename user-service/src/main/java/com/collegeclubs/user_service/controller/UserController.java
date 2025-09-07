package com.collegeclubs.user_service.controller;

import com.collegeclubs.user_service.model.Role;
import com.collegeclubs.user_service.model.User;
import com.collegeclubs.user_service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user-service/api/user")
public class UserController {
    @Autowired
    private UserService userService;

    @GetMapping("/admin")
    public String showAdminPage() {
        return "admin";
    }

    @GetMapping("/details")
    public User getUserDetails(@RequestParam String username) {
        return userService.findByUsername(username);
    }

    @GetMapping("/details/srn")
    public User getUserDetailsBySrn(@RequestParam String srn) {
        return userService.findBySrn(srn);
    }

    @GetMapping("/details/id")
    public User getUserDetailsById(@RequestParam Long id) {
        return userService.findById(id);
    }

    @PostMapping("/setUserClub")
    public String setUserClub(@RequestParam String srn, @RequestParam String clubName) {
        System.out.println("Setting club for SRN: " + srn + " to club: " + clubName);
        User user = userService.findBySrn(srn);
        if (user != null) {
            // Save club name to user
            user.setClub(clubName);
            userService.saveUser(user);
            System.out.println("User's club updated successfully for SRN: " + srn);
            return "User's club updated successfully.";
        } else {
            System.err.println("User not found for SRN: " + srn);
            return "User not found.";
        }
    }

    // Keep GET version for backward compatibility
    @GetMapping("/setUserClub")
    public String setUserClubGet(@RequestParam String srn, @RequestParam String clubName) {
        return setUserClub(srn, clubName);
    }

    @GetMapping("/club-members")
    public java.util.List<User> getUsersByClub(@RequestParam String clubName) {
        return userService.findUsersByClub(clubName);
    }

    @PostMapping("/update-club")
    public String updateUserClub(@RequestBody java.util.Map<String, Object> requestData) {
        try {
            String userId = (String) requestData.get("userId");
            String clubName = (String) requestData.get("clubName");

            if (userId == null || clubName == null) {
                return "Missing required parameters: userId and clubName";
            }

            System.out.println("Updating club for user ID: " + userId + " to club: " + clubName);

            // First try to find by ID if it's a number
            User user = null;
            try {
                Long id = Long.parseLong(userId);
                user = userService.findById(id);
            } catch (NumberFormatException e) {
                // If not a valid ID, try SRN
                user = userService.findBySrn(userId);
            }

            if (user != null) {
                // Save club name to user
                user.setClub(clubName);
                userService.saveUser(user);
                System.out.println("User's club updated successfully for user: " + userId);
                return "User's club updated successfully.";
            } else {
                System.err.println("User not found for ID/SRN: " + userId);
                return "User not found.";
            }
        } catch (Exception e) {
            System.err.println("Error updating user's club: " + e.getMessage());
            e.printStackTrace();
            return "Error updating user's club: " + e.getMessage();
        }
    }
}
