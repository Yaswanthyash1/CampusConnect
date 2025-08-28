package com.controller;

import com.model.User;
import com.model.Role;
import com.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private com.service.MemberService memberService;

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("roles", Role.values());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@RequestParam String username,
                               @RequestParam String password,
                               @RequestParam String role,
                               Model model) {
        if (userService.findByUsername(username) != null) {
            model.addAttribute("error", "Username already registered");
            model.addAttribute("roles", Role.values());
            return "register";
        }
        Role userRole = Role.valueOf(role);
        userService.registerUser(username, password, userRole);
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam String username,
                              @RequestParam String password,
                              Model model) {
        // Check if username matches a member's email
        com.model.Member member = memberService.findByEmail(username);
        if (member != null && password.equals(member.getPassword())) {
            // Redirect to member dashboard using SRN
            return "redirect:/member/" + member.getSrn();
        }
        // Fallback to User login
        User existingUser = userService.findByUsername(username);
        if (existingUser != null && userService.getPasswordEncoder().matches(password, existingUser.getPassword())) {
            return "redirect:/home";
        } else {
            model.addAttribute("error", "Invalid username or password");
            return "login";
        }
    }

    @GetMapping("/member")
    public String showMemberPage() {
        return "member";
    }

    @GetMapping("/admin")
    public String showAdminPage() {
        return "admin";
    }
}
