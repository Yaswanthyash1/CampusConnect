package com.controller;

import com.model.Event;
import com.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@Controller
public class HomeController {
    @Autowired
    private EventService eventService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/home")
    public String home(Model model, @RequestParam(value = "userType", required = false) String userType,
                       @RequestParam(value = "userIdentifier", required = false) String userIdentifier) {
        List<Event> events = eventService.getAllEvents();
        model.addAttribute("events", events);
        // If userType is faculty, fetch all clubs
        if (userType != null && userType.equals("faculty")) {
            String sql = "SELECT * FROM club";
            List<Map<String, Object>> clubs = jdbcTemplate.queryForList(sql);
            model.addAttribute("clubs", clubs);
        }
        return "home";
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }
}
