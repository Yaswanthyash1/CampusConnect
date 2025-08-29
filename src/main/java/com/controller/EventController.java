package com.controller;

import com.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class EventController {

    @Autowired
    private EventService eventService;

    @GetMapping("/events")   // ✅ plural
    public String showEvents(Model model) {
        model.addAttribute("events", eventService.getAllEvents());
        return "events";     // ✅ matches events.html
    }

    @GetMapping("/event-details/{id}")
    public String showEventDetails(@PathVariable("id") Long id, Model model) {
        model.addAttribute("event", eventService.getEventById(id));
        return "event-details";
    }
}
