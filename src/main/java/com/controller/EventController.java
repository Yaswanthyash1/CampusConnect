package com.controller;

import com.model.Event;
import com.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.sql.Timestamp;
import java.time.Instant;

@Controller
public class EventController {

    @Autowired
    private EventService eventService;

    @GetMapping("/events/all")
    // Changed mapping to /events/all to avoid conflict with EventsController which owns /events
    public String showEvents(Model model) {
        model.addAttribute("events", eventService.getAllEvents());
        return "events";     // ✅ matches events.html
    }

    @GetMapping("/event-details/{id}")
    public String showEventDetails(@PathVariable("id") Long id, Model model) {
        Event event = eventService.getEventById(id);
        model.addAttribute("event", event);
        // determine whether the event is upcoming (timestamp after now)
        boolean isUpcoming = false;
        if (event != null && event.getTimestamp() != null) {
            Timestamp now = Timestamp.from(Instant.now());
            isUpcoming = event.getTimestamp().after(now);
        }
        model.addAttribute("isUpcoming", isUpcoming);
        return "event-details";
    }
}
