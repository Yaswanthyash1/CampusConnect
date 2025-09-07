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

    @GetMapping("/setUserClub")
    public String setUserClub(@RequestParam String srn, @RequestParam String clubName) {
        User user = userService.findBySrn(srn);
        if (user != null) {
            // Save club name to user
            user.setClub(clubName);
            userService.saveUser(user);
            return "User's club updated successfully.";
        } else {
            return "User not found.";
        }
    }

    @GetMapping("/club-members")
    public java.util.List<User> getUsersByClub(@RequestParam String clubName) {
        return userService.findUsersByClub(clubName);
    }
}
