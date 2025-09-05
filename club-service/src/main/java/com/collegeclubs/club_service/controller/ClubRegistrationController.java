package com.collegeclubs.club_service.controller;

import com.collegeclubs.club_service.model.Club;
import com.collegeclubs.club_service.service.ClubService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ClubRegistrationController {

    @Autowired
    private ClubService clubService;

    @PostMapping("/register-club")
    public String registerClub(@RequestBody Map<String, Object> clubData) {
        try {
            System.out.println("Received club registration data: " + clubData);
            System.out.println("Keys in the data: " + clubData.keySet());

            // Log all keys and values to debug
            clubData.forEach((key, value) -> System.out.println("Key: " + key + ", Value: " + value + ", Type: "
                    + (value != null ? value.getClass().getName() : "null")));

            // Use clubService to register the club
            Club savedClub = clubService.registerClub(clubData);
            System.out.println("Controller: Club registered successfully with ID: " + savedClub.getId());

            return "Club registered successfully";
        } catch (Exception e) {
            System.err.println("Error registering club: " + e.getMessage());
            e.printStackTrace();
            return "Error registering club: " + e.getMessage();
        }
    }
}
