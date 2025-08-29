package com.controller;

import com.model.Event;
import com.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
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

    @GetMapping("/request")
    public String requestPage() {
        return "request";
    }

    @PostMapping("/upload-request")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "Please select a file to upload.");
            return "redirect:/request";
        }
        String filename = file.getOriginalFilename();
        String ext = filename != null && filename.contains(".") ? filename.substring(filename.lastIndexOf(".")).toLowerCase() : "";
        if (!(ext.equals(".pdf") || ext.equals(".doc") || ext.equals(".docx") || ext.equals(".txt"))) {
            redirectAttributes.addFlashAttribute("message", "Invalid file type. Only PDF, DOC, DOCX, and TXT are allowed.");
            return "redirect:/request";
        }
        try {
            File uploadDir = new File("uploads");
            if (!uploadDir.exists()) uploadDir.mkdirs();
            File dest = new File(uploadDir, filename);
            file.transferTo(dest);
            redirectAttributes.addFlashAttribute("message", "File uploaded successfully: " + filename);
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("message", "File upload failed: " + e.getMessage());
        }
        return "redirect:/request";
    }
}
